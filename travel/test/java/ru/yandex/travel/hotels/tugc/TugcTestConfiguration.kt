package ru.yandex.travel.hotels.tugc

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

import ru.yandex.travel.hotels.cluster_permalinks.ClusterPermalinkDataProvider

@Profile("test")
@Configuration
open class TugcTestConfiguration {
    @Bean
    @Primary
    open fun clusterPermalinkDataProvider(): ClusterPermalinkDataProvider = Mockito.mock(ClusterPermalinkDataProvider::class.java)
}
