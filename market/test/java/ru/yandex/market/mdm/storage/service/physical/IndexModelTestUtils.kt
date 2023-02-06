package ru.yandex.market.mdm.storage.service.physical

import ru.yandex.market.mdm.http.MdmAttributeValues
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.lib.model.mdm.I18nStrings
import ru.yandex.market.mdm.lib.model.mdm.ProtoAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.ProtoAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.ProtoEntity

fun entity(attrs: Map<Long, List<ProtoEntity>>): ProtoEntity {
    val builder = ProtoEntity.newBuilder()
    attrs.forEach{ (mdmAttributeId, structValues) ->
        builder.putMdmAttributeValues(mdmAttributeId, ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addAllValues(structValues.map{ ProtoAttributeValue.newBuilder().setStruct(it).build() })
            .build())
    }
    return builder.build()
}

fun entityStr(mdmAttributeId: Long, value: String): ProtoEntity {
    val builder = ProtoEntity.newBuilder()
        .putMdmAttributeValues(mdmAttributeId, strVal(value))
    return builder.build()
}

fun entityInt64(mdmAttributeId: Long, value: Long): ProtoEntity {
    val builder = ProtoEntity.newBuilder()
        .putMdmAttributeValues(mdmAttributeId, ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setInt64(value).build()).build())
    return builder.build()
}

fun strVal(value: String): MdmAttributeValues {
    return ProtoAttributeValues.newBuilder().addValues(ProtoAttributeValue.newBuilder()
        .setString(
            MdmBase.I18nStrings.newBuilder()
            .addI18NString(MdmBase.I18nString.newBuilder().setLangId(I18nStrings.RU_LANG_ID).setString(value))).build())
        .build()
}
