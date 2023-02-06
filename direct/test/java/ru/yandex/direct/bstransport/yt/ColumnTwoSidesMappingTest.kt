package ru.yandex.direct.bstransport.yt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.bstransport.yt.repository.*
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeProtoUtils
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree

class ColumnTwoSidesMappingTest {
    @Test
    fun getValueForUInt64ColumnTest() {
        val mapping = ColumnTwoSidesMapping<ColumnMappingTestMessage, Long, ColumnMappingTestMessage.Builder>(
            uint64Column("ULongColumn"), { m -> m.uint64Field }, { value, builder -> builder.uint64Field = value!! }
        )
        val node = YTree.mapBuilder().key("ULongColumn").value(YTree.unsignedIntegerNode(123L)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setUint64Field(123L).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForUInt64NullColumnTest() {
        val mapping = ColumnTwoSidesMapping<ColumnMappingTestMessage, Long, ColumnMappingTestMessage.Builder>(
            uint64Column("ULongColumn"), { m -> m.uint64Field }, { value, builder -> builder.uint64Field = value!! }
        )
        val intNullValue: Int? = null
        val node = YTree.mapBuilder().key("ULongColumn").value(YTree.unsignedIntegerOrNullNode(intNullValue)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setUint64Field(0L).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)

    }

    @Test
    fun getValueForInt64ColumnTest() {
        val mapping = ColumnTwoSidesMapping<ColumnMappingTestMessage, Long, ColumnMappingTestMessage.Builder>(
            uint64Column("LongColumn"), { m -> m.int64Field }, { value, builder -> builder.int64Field = value!! }
        )
        val node = YTree.mapBuilder().key("LongColumn").value(YTree.longOrNullNode(null)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setInt64Field(0L).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)

    }

    @Test
    fun getValueForInt64NullColumnTest() {
        val mapping = ColumnTwoSidesMapping<ColumnMappingTestMessage, Long, ColumnMappingTestMessage.Builder>(
            uint64Column("LongColumn"), { m -> m.int64Field }, { value, builder -> builder.int64Field = value!! }
        )
        val node = YTree.mapBuilder().key("LongColumn").value(YTree.longNode(123L)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setInt64Field(123L).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)

    }

    @Test
    fun getValueForStringColumnTest() {
        val mapping = ColumnTwoSidesMapping(
            stringColumn("StringColumn"),
            { m: ColumnMappingTestMessage -> m.stringField },
            { value, builder: ColumnMappingTestMessage.Builder -> builder.stringField = value!! }
        )
        val node = YTree.mapBuilder().key("StringColumn").value(YTree.stringNode("some_text")).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setStringField("some_text").build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForStringNullColumnTest() {
        val mapping = ColumnTwoSidesMapping(
            stringColumn("StringColumn"),
            { m: ColumnMappingTestMessage -> m.stringField },
            { value, builder: ColumnMappingTestMessage.Builder -> builder.stringField = value!! }
        )
        val node = YTree.mapBuilder().key("StringColumn").value(YTree.stringOrNullNode(null)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setStringField("").build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)

    }

    @Test
    fun getValueForDoubleColumnTest() {
        val mapping = ColumnTwoSidesMapping(
            doubleColumn("DoubleColumn"),
            { m: ColumnMappingTestMessage -> m.doubleField },
            { value, builder: ColumnMappingTestMessage.Builder -> builder.doubleField = value!! }
        )
        val node = YTree.mapBuilder().key("DoubleColumn").value(YTree.doubleNode(12.435)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setDoubleField(12.435).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForDoubleNullColumnTest() {
        val mapping = ColumnTwoSidesMapping(
            doubleColumn("DoubleColumn"),
            { m: ColumnMappingTestMessage -> m.doubleField },
            { value, builder: ColumnMappingTestMessage.Builder -> builder.doubleField = value!! }
        )
        val node = YTree.mapBuilder().key("DoubleColumn").value(YTree.doubleOrNullNode(null)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setDoubleField(0.0).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForProtoColumnTest() {
        val mapping = ColumnTwoSidesMapping(
            protoColumn("ProtoColumn", ColumnMappingTestMessage.ProtoField::class.java),
            { m: ColumnMappingTestMessage -> m.protoField },
            { value, builder: ColumnMappingTestMessage.Builder -> builder.protoField = value }
        )
        val proto = ColumnMappingTestMessage.ProtoField.newBuilder().setField1(1L).setField2("text").build()
        val node = YTree.mapBuilder().key("ProtoColumn").value(YTreeProtoUtils.marshal(proto)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setProtoField(proto).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForProtoNullColumnTest() {
        val mapping = ColumnTwoSidesMapping(
            protoColumn("ProtoColumn", ColumnMappingTestMessage.ProtoField::class.java),
            { m: ColumnMappingTestMessage -> m.protoField },
            { value, builder: ColumnMappingTestMessage.Builder ->
                builder.protoField = value ?: ColumnMappingTestMessage.ProtoField.newBuilder().build()
            }
        )
        val node = YTree.mapBuilder().key("ProtoColumn").value(YTree.entityNode()).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder()
            .setProtoField(ColumnMappingTestMessage.ProtoField.newBuilder().build())
            .build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForCustomColumnTest() {
        val mapping = ColumnTwoSidesMapping(
            customColumn("CustomColumn"),
            { m: ColumnMappingTestMessage -> m.listFieldList },
            { value, builder: ColumnMappingTestMessage.Builder ->
                value?.listNode()
                    ?.map {
                        it.stringValue()
                    }
                    ?.forEach { builder.addListField(it) }
            }
        )
        val node = YTree.mapBuilder().key("CustomColumn")
            .value(YTree.listBuilder().value("text").value("text2").value("text10").buildList()).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder()
            .addAllListField(mutableListOf("text", "text2", "text10")).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForCustomNullColumnTest() {
        val mapping = ColumnTwoSidesMapping(
            customColumn("CustomColumn"),
            { m: ColumnMappingTestMessage -> m.listFieldList },
            { value, builder: ColumnMappingTestMessage.Builder ->
                value?.listNode()
                    ?.map {
                        it.stringValue()
                    }
                    ?.forEach { builder.addListField(it) }
            }
        )
        val node = YTree.mapBuilder().key("CustomColumn")
            .value(YTree.entityNode()).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder()
            .addAllListField(mutableListOf()).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForBoolColumnTest() {
        val mapping = ColumnTwoSidesMapping<ColumnMappingTestMessage, Boolean, ColumnMappingTestMessage.Builder>(
            boolColumn("BoolColumn"), { m -> m.boolField }, { value, builder -> builder.boolField = value!! }
        )
        val node = YTree.mapBuilder().key("BoolColumn").value(YTree.booleanNode(true)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setBoolField(true).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }

    @Test
    fun getValueForBoolNullColumnTest() {
        val mapping = ColumnTwoSidesMapping<ColumnMappingTestMessage, Boolean, ColumnMappingTestMessage.Builder>(
            boolColumn("BoolColumn"), { m -> m.boolField }, { value, builder -> builder.boolField = value!! }
        )
        val node = YTree.mapBuilder().key("BoolColumn").value(YTree.booleanOrNullNode(null)).buildMap()
        val builder = ColumnMappingTestMessage.newBuilder()
        mapping.mapToProtoBuilder(builder, node)

        val expectedMessage = ColumnMappingTestMessage.newBuilder().setBoolField(false).build()
        assertThat(builder.build()).isEqualToComparingFieldByField(expectedMessage)
    }
}
