import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.beagle.beagle.core.entities.unit import Unit
from mail.beagle.beagle.storage.exceptions import UnitNotFound


@pytest.mark.asyncio
class TestUnitMapper:
    @pytest.fixture
    def unit_entity(self, org, rands):
        return Unit(
            org_id=org.org_id,
            external_id=rands(),
            external_type=rands(),
            name=rands()
        )

    @pytest.fixture
    async def unit(self, storage, unit_entity):
        return await storage.unit.create(unit_entity)

    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.unit.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create(self, randn, storage, unit_entity, func_now):
        serial = await storage.serial.get(unit_entity.org_id)
        serial.unit_id = randn()
        await storage.serial.save(serial)
        unit = await storage.unit.create(unit_entity)
        unit_entity.unit_id = serial.unit_id
        unit_entity.created = unit_entity.updated = func_now
        assert unit_entity == unit

    async def test_get(self, storage, unit):
        assert unit == await storage.unit.get(unit.org_id, unit.unit_id)

    async def test_get_not_found(self, storage, randn):
        with pytest.raises(UnitNotFound):
            await storage.unit.get(randn(), randn())

    async def test_delete(self, storage, unit):
        await storage.unit.delete(unit)
        with pytest.raises(UnitNotFound):
            await storage.unit.get(unit.org_id, unit.unit_id)

    async def test_save(self, randn, rands, storage, unit, func_now):
        unit.external_id = rands()
        unit.external_type = rands()
        unit.name = rands()
        unit.uid = randn()
        unit.username = rands()
        updated = await storage.unit.save(unit)
        unit.updated = func_now
        assert unit == updated \
            and unit == await storage.unit.get(unit.org_id, unit.unit_id)

    @pytest.mark.asyncio
    class TestFind:
        async def test_org_id_filter(self, storage, units_with_other_org, org):
            assert_that(
                [unit async for unit in storage.unit.find(org_id=org.org_id)],
                contains_inanyorder(*[unit for unit in units_with_other_org if unit.org_id == org.org_id])
            )
