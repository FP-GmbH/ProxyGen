package me.fup.joy.proxygen.sample.ui.actions

import me.fup.joy.proxygen.core.ProxyGen

@ProxyGen
abstract class ALoginScreenActions(
    private val onLoginCompletedAction: () -> Unit,
) {

    abstract fun onLoginCompleted()
}