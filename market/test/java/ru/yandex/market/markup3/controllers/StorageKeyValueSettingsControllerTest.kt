package ru.yandex.market.markup3.controllers

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.dto.InspectionLevel
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.mbo.storage.StorageKeyValueService

class StorageKeyValueSettingsControllerTest : BaseAppTest() {
    @Autowired
    private lateinit var keyValueService: StorageKeyValueService

    private lateinit var storageKeyValueSettingsController: StorageKeyValueSettingsController

    @Before
    fun setup() {
        storageKeyValueSettingsController = StorageKeyValueSettingsController(keyValueService)
    }

    @Test
    fun `Test inspection level settings`() {
        storageKeyValueSettingsController.updateSettings(mapOf(InspectionLevel.INSPECTION_LEVEL_KEY to false))
        val settings = storageKeyValueSettingsController.getSettings()
        settings[InspectionLevel.INSPECTION_LEVEL_KEY] shouldBe false
    }
}
