import pytest

from mail.beagle.beagle.core.entities.organization import Organization
from mail.beagle.beagle.core.entities.serial import Serial
from mail.beagle.beagle.storage.exceptions import SerialNotFound


@pytest.mark.asyncio
class TestSerialMapper:
    @pytest.fixture(autouse=True)
    async def organization(self, storage, org_id):
        return await storage.organization.create(Organization(org_id=org_id))

    @pytest.fixture
    def serial_entity(self, org_id):
        return Serial(org_id=org_id)

    @pytest.fixture
    async def serial(self, storage, serial_entity):
        return await storage.serial.create(serial_entity)

    async def test_create(self, storage, serial_entity):
        serial = await storage.serial.create(serial_entity)
        assert serial_entity == serial

    async def test_get(self, storage, org_id, serial):
        assert serial == await storage.serial.get(org_id)

    async def test_get_not_found(self, storage, org_id):
        with pytest.raises(SerialNotFound):
            await storage.serial.get(org_id)

    async def test_save(self, storage, org_id, serial):
        serial.mail_list_id += 1
        serial.unit_id += 1
        updated_serial = await storage.serial.save(serial)
        assert serial == updated_serial \
            and serial == await storage.serial.get(org_id)
