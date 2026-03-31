package com.wearamp.tile

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.MultiButtonLayout
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.wearamp.MainActivity
import com.wearamp.service.WearAmpMediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "PlaybackTileService"

private const val ID_PLAY_PAUSE = "play_pause"
private const val ID_SKIP_NEXT = "skip_next"
private const val ID_SKIP_PREV = "skip_prev"
private const val RESOURCES_VERSION = "1"

/** How long to wait for the player state to propagate after a button action (ms). */
private const val ACTION_STATE_PROPAGATION_DELAY_MS = 150L

/** Maximum connection attempts before giving up waiting for [MediaController]. */
private const val MAX_CONNECTION_ATTEMPTS = 20

/** How long to wait between each connection attempt (ms). */
private const val CONNECTION_POLL_INTERVAL_MS = 100L

/**
 * Wear OS Tile that shows the current playback state and quick controls
 * (play/pause, skip forward/back) without opening the full app.
 *
 * When something is playing the tile shows track title, artist, and
 * transport buttons.  Tapping the track title opens the Now Playing screen.
 * When nothing is playing the tile shows a quick-launch button instead.
 *
 * The tile connects to [WearAmpMediaService] via [MediaController] and
 * requests a re-render whenever the play state or current track changes.
 */
class PlaybackTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // ── Tile lifecycle ────────────────────────────────────────────────────────

    override fun onTileAddEvent(requestParams: EventBuilders.TileAddEvent) = connectController()
    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) = connectController()
    override fun onTileRemoveEvent(requestParams: EventBuilders.TileRemoveEvent) = releaseController()
    override fun onTileLeaveEvent(requestParams: EventBuilders.TileLeaveEvent) = releaseController()

    // ── Tile request ──────────────────────────────────────────────────────────

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val deviceParams = requestParams.deviceParameters
        val clickId = requestParams.currentState.lastClickableId

        val future = SettableFuture.create<TileBuilders.Tile>()
        serviceScope.launch {
            try {
                val controller = awaitController()
                handleAction(clickId, controller)
                // Give the player a moment to reflect the new state after an action.
                if (clickId.isNotEmpty()) delay(ACTION_STATE_PROPAGATION_DELAY_MS)
                future.set(buildTile(this@PlaybackTileService, deviceParams, controller))
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )

    // ── MediaController helpers ───────────────────────────────────────────────

    private fun connectController() {
        if (mediaController?.isConnected == true || controllerFuture != null) return
        val token = SessionToken(this, ComponentName(this, WearAmpMediaService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val mc = future.get()
                    mediaController = mc
                    // Clear the in-flight future so a reconnect is possible if the
                    // controller later becomes disconnected.
                    controllerFuture = null
                    mc.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) =
                            requestTileUpdate()
                        override fun onMediaItemTransition(item: MediaItem?, reason: Int) =
                            requestTileUpdate()
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to media service", e)
                    controllerFuture = null
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun releaseController() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    /** Wait up to 2 s for the controller to connect, then return whatever we have. */
    private suspend fun awaitController(): MediaController? {
        if (mediaController?.isConnected == true) return mediaController
        if (controllerFuture == null) connectController()
        for (i in 1..MAX_CONNECTION_ATTEMPTS) {
            if (mediaController?.isConnected == true) return mediaController
            delay(CONNECTION_POLL_INTERVAL_MS)
        }
        return mediaController
    }

    private fun handleAction(clickId: String, controller: MediaController?) {
        controller ?: return
        when (clickId) {
            ID_PLAY_PAUSE -> if (controller.isPlaying) controller.pause() else controller.play()
            ID_SKIP_NEXT -> controller.seekToNextMediaItem()
            ID_SKIP_PREV -> controller.seekToPreviousMediaItem()
        }
    }

    private fun requestTileUpdate() =
        getUpdater(this).requestUpdate(PlaybackTileService::class.java)

    override fun onDestroy() {
        releaseController()
        serviceScope.cancel()
        super.onDestroy()
    }
}

// ── Layout builders (top-level functions to keep the service class lean) ─────

private val tileColors = Colors(
    /* primary   */ 0xFF7FCFFF.toInt(),
    /* onPrimary */ 0xFF000000.toInt(),
    /* surface   */ 0xFF303030.toInt(),
    /* onSurface */ 0xFFFFFFFF.toInt()
)

private fun buildTile(
    context: Context,
    deviceParameters: DeviceParametersBuilders.DeviceParameters,
    controller: MediaController?
): TileBuilders.Tile =
    TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(
            TimelineBuilders.Timeline.fromLayoutElement(
                buildLayout(context, deviceParameters, controller)
            )
        )
        // Freshness driven by Player.Listener events; no periodic refresh needed.
        .setFreshnessIntervalMillis(0L)
        .build()

private fun buildLayout(
    context: Context,
    deviceParameters: DeviceParametersBuilders.DeviceParameters,
    controller: MediaController?
): LayoutElementBuilders.LayoutElement {
    val title = controller?.currentMediaItem?.mediaMetadata?.title?.toString()
    val artist = controller?.currentMediaItem?.mediaMetadata?.artist?.toString()
    val isPlaying = controller?.isPlaying ?: false
    return if (!title.isNullOrEmpty()) {
        buildPlayingLayout(context, deviceParameters, isPlaying, title, artist)
    } else {
        buildIdleLayout(context, deviceParameters)
    }
}

/** Tile shown while a track is loaded (playing or paused). */
private fun buildPlayingLayout(
    context: Context,
    deviceParameters: DeviceParametersBuilders.DeviceParameters,
    isPlaying: Boolean,
    title: String,
    artist: String?
): LayoutElementBuilders.LayoutElement {
    val playPauseChar = if (isPlaying) "⏸" else "▶"

    // Tapping the title opens the full Now Playing screen.
    val primaryLabel = Text.Builder(context, title)
        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
        .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
        .setMaxLines(2)
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setClickable(openAppClickable(context))
                .build()
        )
        .build()

    val buttons = MultiButtonLayout.Builder()
        .addButtonContent(
            Button.Builder(context, loadClickable(ID_SKIP_PREV))
                .setTextContent("⏮")
                .setButtonColors(ButtonColors.secondaryButtonColors(tileColors))
                .build()
        )
        .addButtonContent(
            Button.Builder(context, loadClickable(ID_PLAY_PAUSE))
                .setTextContent(playPauseChar)
                .setButtonColors(ButtonColors.primaryButtonColors(tileColors))
                .build()
        )
        .addButtonContent(
            Button.Builder(context, loadClickable(ID_SKIP_NEXT))
                .setTextContent("⏭")
                .setButtonColors(ButtonColors.secondaryButtonColors(tileColors))
                .build()
        )
        .build()

    val layoutBuilder = PrimaryLayout.Builder(deviceParameters)
        .setPrimaryLabelTextContent(primaryLabel)
        .setContent(buttons)

    if (!artist.isNullOrEmpty()) {
        layoutBuilder.setSecondaryLabelTextContent(
            Text.Builder(context, artist)
                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                .setColor(ColorBuilders.argb(0xCCFFFFFF.toInt()))
                .setMaxLines(1)
                .build()
        )
    }

    return layoutBuilder.build()
}

/** Tile shown when nothing is loaded / the media service is not active. */
private fun buildIdleLayout(
    context: Context,
    deviceParameters: DeviceParametersBuilders.DeviceParameters
): LayoutElementBuilders.LayoutElement =
    PrimaryLayout.Builder(deviceParameters)
        .setPrimaryLabelTextContent(
            Text.Builder(context, "WearAmp")
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
                .build()
        )
        .setContent(
            Button.Builder(context, openAppClickable(context))
                .setTextContent("▶")
                .setButtonColors(ButtonColors.primaryButtonColors(tileColors))
                .build()
        )
        .setSecondaryLabelTextContent(
            Text.Builder(context, "Nothing playing")
                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                .setColor(ColorBuilders.argb(0xCCFFFFFF.toInt()))
                .build()
        )
        .build()

// ── Clickable factories ───────────────────────────────────────────────────────

/** A [LoadAction] clickable that re-triggers [TileService.onTileRequest]. */
private fun loadClickable(clickId: String): ModifiersBuilders.Clickable =
    ModifiersBuilders.Clickable.Builder()
        .setId(clickId)
        .setOnClick(ActionBuilders.LoadAction.Builder().build())
        .build()

/** A [LaunchAction] clickable that opens [MainActivity]. */
private fun openAppClickable(context: Context): ModifiersBuilders.Clickable =
    ModifiersBuilders.Clickable.Builder()
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build()
                )
                .build()
        )
        .build()
