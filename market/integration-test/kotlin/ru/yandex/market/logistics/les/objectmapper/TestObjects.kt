package ru.yandex.market.logistics.les.objectmapper

import ru.yandex.market.logistics.les.base.crypto.EncryptedString
import ru.yandex.market.logistics.les.objectmapper.testmodel.CompositeEncryptedPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.InstantPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.ListPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.NoDefaultConstructorPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.NoDefaultConstructorWithAnnotationsPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.OptionalEventPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.PrivateFieldPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.PrivateFieldWithAnnotationPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.SimplePayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.SimplePayloadType
import java.time.Instant
import java.util.Optional

val SIMPLE_PAYLOAD = SimplePayload(SimplePayloadType.SIMPLE)
val SIMPLE_PAYLOAD_EMPTY = SimplePayload()
val NO_DEFAULT_CONSTRUCTOR_PAYLOAD = NoDefaultConstructorPayload("str")
val NO_DEFAULT_CONSTRUCTOR_PAYLOAD_WITH_ANNOTATIONS = NoDefaultConstructorWithAnnotationsPayload("str")
val INSTANT_PAYLOAD = InstantPayload(Instant.parse("2021-10-07T23:50:00Z"))
val PRIVATE_FIELD_PAYLOAD_EMPTY = PrivateFieldPayload()
val PRIVATE_FIELD_PAYLOAD = PrivateFieldPayload("str")
val PRIVATE_FIELD_WITH_ANNOTATION_PAYLOAD = PrivateFieldWithAnnotationPayload("str")
val OPTIONAL_EVENT_PAYLOAD_EMPTY = OptionalEventPayload(Optional.empty())
val OPTIONAL_EVENT_PAYLOAD_NULL = OptionalEventPayload()
val LIST_PAYLOAD = ListPayload(listOf("1", "2"))
val COMPOSITE_ENCRYPTED = CompositeEncryptedPayload(EncryptedString("testKey"), "testValue")
