package ru.yandex.direct.chassis.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.tmatesoft.svn.core.wc.SVNClientManager
import ru.yandex.direct.chassis.util.DirectAppsConfEntry
import ru.yandex.direct.chassis.app.ChassisJobLauncher
import ru.yandex.startrek.client.Session

@Configuration
@Import(AppConfiguration::class)
class ChassisTestConfiguration {

    @Bean
    fun chassisJobLauncher(): ChassisJobLauncher? = null

    @Bean
    fun svnClientManager(): SVNClientManager? {
        return null
    }

    fun startrekSession(): Session? {
        return null
    }

    @Bean
    fun directApps(): List<DirectAppsConfEntry> {
        return listOf()
    }
}
