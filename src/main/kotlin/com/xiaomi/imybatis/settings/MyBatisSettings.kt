package com.xiaomi.imybatis.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

/**
 * Application-level settings for imybatis plugin
 */
@State(name = "imybatis.Settings", storages = [Storage("imybatis.xml")])
@Service(Service.Level.APP)
class MyBatisSettings : PersistentStateComponent<MyBatisSettings.State> {

    data class State(
        var enableNavigation: Boolean = true,
        var enableCompletion: Boolean = true,
        var enableInspection: Boolean = true,
        var indexOnStartup: Boolean = true,
        var maxIndexTime: Int = 60 // seconds
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): MyBatisSettings {
            return ApplicationManager.getApplication().getService(MyBatisSettings::class.java)
        }
    }
}
