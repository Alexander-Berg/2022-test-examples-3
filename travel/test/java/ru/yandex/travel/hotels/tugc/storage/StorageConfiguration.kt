package ru.yandex.travel.hotels.tugc.storage

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class StorageConfiguration {
    @Bean
    open fun createFavoriteStorage(): FavoriteStorage = FavoriteStorage()
}