package com.example.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Extension modifier that intercepts the Enter key on hardware / virtual keyboards
 * and shifts focus to the next input field.
 */
fun Modifier.nextOnEnter(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    this.onPreviewKeyEvent { event ->
        if ((event.key == Key.Enter || event.key == Key.NumPadEnter) && event.type == KeyEventType.KeyUp) {
            if (!focusManager.moveFocus(FocusDirection.Next)) {
                focusManager.moveFocus(FocusDirection.Down)
            }
            true
        } else {
            false
        }
    }
}

fun Modifier.nextOnEnter(customFocusManager: FocusManager): Modifier = this.onPreviewKeyEvent { event ->
    if ((event.key == Key.Enter || event.key == Key.NumPadEnter) && event.type == KeyEventType.KeyUp) {
        if (!customFocusManager.moveFocus(FocusDirection.Next)) {
            customFocusManager.moveFocus(FocusDirection.Down)
        }
        true
    } else {
        false
    }
}

/**
 * Standard KeyboardActions for non-description fields to move focus on IME Next action.
 */
fun defaultNextKeyboardActions(focusManager: FocusManager): KeyboardActions = KeyboardActions(
    onNext = {
        if (!focusManager.moveFocus(FocusDirection.Next)) {
            focusManager.moveFocus(FocusDirection.Down)
        }
    },
    onDone = {
        if (!focusManager.moveFocus(FocusDirection.Next)) {
            focusManager.moveFocus(FocusDirection.Down)
        }
    }
)

/**
 * Composable remember helper for standard next KeyboardActions.
 */
@Composable
fun rememberNextKeyboardActions(customFocusManager: FocusManager? = null): KeyboardActions {
    val focusManager = customFocusManager ?: LocalFocusManager.current
    return remember(focusManager) {
        defaultNextKeyboardActions(focusManager)
    }
}

/**
 * Standard KeyboardOptions with ImeAction.Next.
 */
fun nextKeyboardOptions(
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
): KeyboardOptions = KeyboardOptions(
    keyboardType = keyboardType,
    imeAction = imeAction
)
