package ru.yandex.market.mdm.lib.model.mdm

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.lib.testutils.BaseAppTestClass

class MdmEntityChangesUtilTest : BaseAppTestClass() {

    fun createSimpleMdmEntity(mdmId: Long = 0,
                              mdmEntityTypeId: Long = 0,
                              values: HashMap<Long, MdmAttributeValues> = HashMap(),
                              version: MdmVersion = MdmVersion()): MdmEntity {
        return MdmEntity(mdmId, mdmEntityTypeId, values, version)
    }

    fun createSimpleMdmAttributeValues(mdmAttributeId: Long = 0,
                                       values: List<MdmAttributeValue> = ArrayList(),
                                       version: MdmVersion = MdmVersion()): MdmAttributeValues {
        return MdmAttributeValues(mdmAttributeId, values, version)
    }

    @Test
    fun `should compare the mdmEntity with an empty newEntity`() {
        val oldValues: HashMap<Long, MdmAttributeValues> = HashMap()
        val oldAttributeValues: MdmAttributeValues = createSimpleMdmAttributeValues()
        oldValues.put(0, oldAttributeValues)
        val oldEntity: MdmEntity = createSimpleMdmEntity(values = oldValues)
        val newEntity: MdmEntity = createSimpleMdmEntity()
        val changes: List<MdmEntityChange> = MdmEntityChangesUtil.findChange(newEntity, oldEntity)
        assertSoftly {
            changes.size shouldBe 1
            changes.get(0).mdmId shouldBe 0
            changes.get(0).newValue shouldBe null
            changes.get(0).previousValue shouldBe oldAttributeValues
        }
    }

    @Test
    fun `should compare the mdmEntity with an empty oldEntity`() {
        val oldEntity: MdmEntity = createSimpleMdmEntity()
        val newValues: HashMap<Long, MdmAttributeValues> = HashMap()
        val newAttributeValues: MdmAttributeValues = createSimpleMdmAttributeValues()
        newValues.put(0, newAttributeValues)
        val newEntity: MdmEntity = createSimpleMdmEntity(values = newValues)
        val changes: List<MdmEntityChange> = MdmEntityChangesUtil.findChange(newEntity, oldEntity)
        assertSoftly {
            changes.size shouldBe 1
            changes.get(0).mdmId shouldBe 0
            changes.get(0).newValue shouldBe newAttributeValues
            changes.get(0).previousValue shouldBe null
        }
    }

    @Test
    fun `should compare the mdmEntity with the same MdmAttributeValues`() {
        val sameMdmEntity: MdmEntity = createSimpleMdmEntity()
        val listAttributeValue: ArrayList<MdmAttributeValue> = ArrayList()
        listAttributeValue.add(MdmAttributeValue(bool = true))
        listAttributeValue.add(MdmAttributeValue(struct = sameMdmEntity))

        val oldValues: HashMap<Long, MdmAttributeValues> = HashMap()
        val oldAttributeValues: MdmAttributeValues = createSimpleMdmAttributeValues(values = listAttributeValue)
        oldValues.put(0, oldAttributeValues)
        val oldEntity: MdmEntity = createSimpleMdmEntity(values = oldValues)

        val newValues: HashMap<Long, MdmAttributeValues> = HashMap()
        val newAttributeValues: MdmAttributeValues = createSimpleMdmAttributeValues(values = listAttributeValue)
        newValues.put(0, newAttributeValues)
        val newEntity: MdmEntity = createSimpleMdmEntity(values = newValues)

        val changes: List<MdmEntityChange> = MdmEntityChangesUtil.findChange(newEntity, oldEntity)
        assertSoftly {
            changes.size shouldBe 0
        }
    }

    @Test
    fun `should compare the mdmEntity with the different MdmAttributeValues by struct`() {
        val listOldEntityAttributeValue: ArrayList<MdmAttributeValue> = ArrayList()
        listOldEntityAttributeValue.add(MdmAttributeValue(bool = true))
        listOldEntityAttributeValue.add(MdmAttributeValue(struct = createSimpleMdmEntity(mdmId = 0)))

        val oldValues: HashMap<Long, MdmAttributeValues> = HashMap()
        val oldAttributeValues: MdmAttributeValues = createSimpleMdmAttributeValues(values = listOldEntityAttributeValue)
        oldValues.put(0, oldAttributeValues)
        val oldEntity: MdmEntity = createSimpleMdmEntity(values = oldValues)

        val listNewEntityAttributeValue: ArrayList<MdmAttributeValue> = ArrayList()
        listNewEntityAttributeValue.add(MdmAttributeValue(bool = true))
        listNewEntityAttributeValue.add(MdmAttributeValue(struct = createSimpleMdmEntity(mdmId = 1)))

        val newValues: HashMap<Long, MdmAttributeValues> = HashMap()
        val newAttributeValues: MdmAttributeValues = createSimpleMdmAttributeValues(values = listNewEntityAttributeValue)
        newValues.put(0, newAttributeValues)
        val newEntity: MdmEntity = createSimpleMdmEntity(values = newValues)

        val changes: List<MdmEntityChange> = MdmEntityChangesUtil.findChange(newEntity, oldEntity)
        assertSoftly {
            changes.size shouldBe 1
            changes.get(0).mdmId shouldBe 0
            changes.get(0).newValue shouldBe newAttributeValues
            changes.get(0).previousValue shouldBe oldAttributeValues
        }
    }

    @Test
    fun `should compare the mdmEntity with the different MdmAttributeValues by bool`() {
        val sameMdmEntity: MdmEntity = createSimpleMdmEntity()
        val listOldEntityAttributeValue: ArrayList<MdmAttributeValue> = ArrayList()
        listOldEntityAttributeValue.add(MdmAttributeValue(bool = true))
        listOldEntityAttributeValue.add(MdmAttributeValue(struct = sameMdmEntity))

        val oldValues: HashMap<Long, MdmAttributeValues> = HashMap()
        val oldAttributeValues: MdmAttributeValues = createSimpleMdmAttributeValues(values = listOldEntityAttributeValue)
        oldValues.put(0, oldAttributeValues)
        val oldEntity: MdmEntity = createSimpleMdmEntity(values = oldValues)

        val listNewEntityAttributeValue: ArrayList<MdmAttributeValue> = ArrayList()
        listNewEntityAttributeValue.add(MdmAttributeValue(bool = false))
        listNewEntityAttributeValue.add(MdmAttributeValue(struct = sameMdmEntity))

        val newValues: HashMap<Long, MdmAttributeValues> = HashMap()
        val newAttributeValues: MdmAttributeValues = createSimpleMdmAttributeValues(values = listNewEntityAttributeValue)
        newValues.put(0, newAttributeValues)
        val newEntity: MdmEntity = createSimpleMdmEntity(values = newValues)

        val changes: List<MdmEntityChange> = MdmEntityChangesUtil.findChange(newEntity, oldEntity)
        assertSoftly {
            changes.size shouldBe 1
            changes.get(0).mdmId shouldBe 0
            changes.get(0).newValue shouldBe newAttributeValues
            changes.get(0).previousValue shouldBe oldAttributeValues
        }
    }
}
