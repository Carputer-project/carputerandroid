package com.carputer.android.ui

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

class PageNavController {
    var navigateLeft: () -> Unit = {}
    var navigateRight: () -> Unit = {}
    var navigateUp: () -> Boolean = { true }
    var navigateDown: () -> Boolean = { true }
    var activateFocus: () -> Unit = {}
    var handleEscape: () -> Unit = {}
    val focusIndex: MutableIntState = mutableIntStateOf(-1)
    val focusActionName: MutableState<String> = mutableStateOf("")
}
