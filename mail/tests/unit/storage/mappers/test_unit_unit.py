import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, equal_to

from mail.beagle.beagle.core.entities.unit_subscription import UnitSubscription
from mail.beagle.beagle.core.entities.unit_unit import UnitUnit
from mail.beagle.beagle.storage.exceptions import UnitUnitNotFound


@pytest.mark.asyncio
class TestUnitUnitMapper:
    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.unit_unit.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create(self, storage, unit_unit_entity, func_now):
        unit_unit = await storage.unit_unit.create(unit_unit_entity)
        unit_unit_entity.created = unit_unit_entity.updated = func_now
        assert unit_unit_entity == unit_unit

    async def test_get(self, storage, unit_unit):
        assert unit_unit == await storage.unit_unit.get(unit_unit.org_id, unit_unit.unit_id, unit_unit.parent_unit_id)

    async def test_get_not_found(self, storage, randn):
        with pytest.raises(UnitUnitNotFound):
            await storage.unit_unit.get(randn(), randn(), randn())

    async def test_delete(self, storage, unit_unit):
        await storage.unit_unit.delete(unit_unit)
        with pytest.raises(UnitUnitNotFound):
            await storage.unit_unit.get(unit_unit.org_id, unit_unit.unit_id, unit_unit.parent_unit_id)

    async def test_find_parent_unit_subscription_mail_list_id(self, mail_list, parent_unit, unit, storage, unit_unit):
        await storage.unit_subscription.create(UnitSubscription(
            org_id=parent_unit.org_id,
            mail_list_id=mail_list.mail_list_id,
            unit_id=parent_unit.unit_id,
        ))

        kwargs = {
            'org_id': unit_unit.org_id,
            'parent_unit_subscription_mail_list_id': mail_list.mail_list_id
        }

        async for returned_unit_unit in storage.unit_unit.find(**kwargs):
            assert all((
                returned_unit_unit.unit_id == unit_unit.unit_id,
                returned_unit_unit.unit == unit
            ))

    @pytest.mark.asyncio
    class TestFind:
        @pytest.fixture
        async def unit_units(self, storage, units_with_other_org):
            return [
                await storage.unit_unit.create(UnitUnit(
                    org_id=child.org_id,
                    unit_id=child.unit_id,
                    parent_unit_id=parent.unit_id,
                ))
                for parent in units_with_other_org
                for child in units_with_other_org
                if parent.org_id == child.org_id and parent.unit_id < child.unit_id
            ]

        async def test_org_id_filter(self, storage, org_id, unit_units):
            assert_that(
                await alist(storage.unit_unit.find(org_id=org_id)),
                contains_inanyorder(*[unit_unit for unit_unit in unit_units if unit_unit.org_id == org_id])
            )

        async def test_unit_id_filter(self, storage, unit_units):
            unit_unit = unit_units[0]
            assert_that(
                await alist(storage.unit_unit.find(org_id=unit_unit.org_id, unit_id=unit_unit.unit_id)),
                equal_to([unit_unit]),
            )

        async def test_unit_ids_filter(self, storage, org_id, unit_units):
            unit_ids = {unit_unit.unit_id for unit_unit in unit_units if unit_unit.org_id == org_id}
            unit_ids.remove(next(iter(unit_ids)))
            expected = [
                unit_unit
                for unit_unit in unit_units
                if unit_unit.org_id == org_id and unit_unit.unit_id in unit_ids
            ]
            assert_that(
                await alist(storage.unit_unit.find(org_id=org_id, unit_ids=list(unit_ids))),
                contains_inanyorder(*expected),
            )

        async def test_parent_unit_id_filter(self, storage, unit_units):
            org_id = unit_units[0].org_id
            parent_unit_id = unit_units[0].parent_unit_id
            expected = [
                unit_unit
                for unit_unit in unit_units
                if unit_unit.org_id == org_id and unit_unit.parent_unit_id == parent_unit_id
            ]
            assert_that(
                await alist(storage.unit_unit.find(org_id=org_id, parent_unit_id=parent_unit_id)),
                contains_inanyorder(*expected),
            )

        @pytest.mark.parametrize('field', ('parent_unit_id', 'unit_id'))
        async def test_order(self, storage, org_id, unit_units, field):
            found = await alist(storage.unit_unit.find(org_id=org_id, order_by=field))
            assert found == sorted(found, key=lambda unit_unit: getattr(unit_unit, field))
