package ru.yandex.market.mdm.storage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.mdm.storage.service.physical.MdmAuditEventYtModelUtil
import ru.yandex.market.mdm.storage.service.physical.MdmEntitySnapshotYtModel
import ru.yandex.market.mdm.storage.service.physical.MdmYtTableModel
import ru.yandex.market.yt.util.table.model.YtTableModel

@Profile("test")
@Configuration
open class TestMdmStorageYmlLoaderConfig : MdmStorageYmlLoaderConfig() {
    @Value("\${mdm-storage-api.mdm-entity-storage-path}")
    private val basePath = ""
    @Value("\${mdm-storage-api.drop-on-startup:false}")
    private val dropOnStartupEnabled = false
    @Value("\${environment}")
    private val environment = ""

    @Bean
    override fun mdmYtStorageSettings(): YtStorageFixedSettings {
        return YtStorageFixedSettings(dropOnStartupEnabled, listOf(
            MdmYtTableModel.loadFromYamlConfig(basePath, "yt_storage_settings/golden_msku.yml", environment),
            MdmYtTableModel.loadFromYamlConfig(basePath, "yt_storage_settings/silver_ssku.yml", environment),
            MdmYtTableModel.loadFromYamlConfig(basePath, "yt_storage_settings/object_no_audit.yml", environment),
        ))
    }

    @Bean
    override fun ytTableModelForAuditEvent(): YtTableModel {
        return MdmAuditEventYtModelUtil.loadFromYamlConfig(basePath, "yt_storage_settings/event.yml", environment)
    }

    @Bean
    override fun ytTableModelsForEntitySnapshot(): Map<Long, MdmEntitySnapshotYtModel> {
        return mapOf(
            GOLD_MSKU_ENTITY_TYPE_ID to MdmEntitySnapshotYtModel.loadFromYamlConfig(basePath,
                "yt_storage_settings/golden_msku_snapshot.yml", environment),
            SILVER_SSKU to MdmEntitySnapshotYtModel.loadFromYamlConfig(basePath,
                "yt_storage_settings/silver_ssku_snapshot.yml", environment)
        )
    }

    companion object {
        const val GOLD_MSKU_ENTITY_TYPE_ID = 215906716L
        const val SILVER_SSKU = 884303947L
    }
}
