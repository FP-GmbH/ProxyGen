package me.fup.joy.proxygen.sample.ui.actions

import me.fup.joy.proxygen.core.ProxyGen

@ProxyGen
open class LoginScreenActions(
    private val onLoginCompletedAction: () -> Unit,
) {

    open fun onLoginCompleted() {
        onLoginCompletedAction()
    }
}