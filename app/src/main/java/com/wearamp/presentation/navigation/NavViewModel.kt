package com.wearamp.presentation.navigation

import androidx.lifecycle.ViewModel
import com.wearamp.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    val userPreferences: UserPreferences
) : ViewModel()
