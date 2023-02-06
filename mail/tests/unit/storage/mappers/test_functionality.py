import uuid

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_length

from mail.payments.payments.core.entities.enums import FunctionalityType, YandexPayPaymentGatewayType
from mail.payments.payments.core.entities.functionality import (
    MerchantFunctionality, PaymentsFunctionalityData, YandexPayMerchantFunctionalityData,
    YandexPayPaymentGatewayFunctionalityData
)
from mail.payments.payments.storage.exceptions import DuplicateFunctionalityStorageError
from mail.payments.payments.storage.mappers.functionality import FunctionalityDataDumper, FunctionalityDataMapper

DATETIME = utcnow()


@pytest.fixture
def functionality(merchant):
    return MerchantFunctionality(
        uid=merchant.uid,
        functionality_type=FunctionalityType.PAYMENTS,
        data=PaymentsFunctionalityData(),
        created=DATETIME,
        updated=DATETIME,
    )


@pytest.fixture
def create_functionality(storage):
    async def _create_functionality(functionality):
        return await storage.functionality.create(functionality)
    return _create_functionality


@pytest.mark.parametrize('obj', (
    MerchantFunctionality(
        uid=1,
        functionality_type=FunctionalityType.PAYMENTS,
        data=PaymentsFunctionalityData(),
        created=DATETIME,
        updated=DATETIME,
    ),
    MerchantFunctionality(
        uid=1,
        functionality_type=FunctionalityType.YANDEX_PAY,
        data=YandexPayPaymentGatewayFunctionalityData(
            gateway_id='123',
            payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
            partner_id=uuid.uuid4()
        ),
        created=DATETIME,
        updated=DATETIME,
    ),
    MerchantFunctionality(
        uid=1,
        functionality_type=FunctionalityType.YANDEX_PAY,
        data=YandexPayMerchantFunctionalityData(
            partner_id=uuid.uuid4(),
            merchant_gateway_id='m-gw-id',
            merchant_desired_gateway='sberbank',
        ),
        created=DATETIME,
        updated=DATETIME,
    ),
))
def test_mapping(obj):
    unmapped = FunctionalityDataDumper()(obj)
    unmapped = {
        MerchantFunctionality.__name__ + '__' + key: value
        for key, value in unmapped.items()
    }
    mapped = FunctionalityDataMapper()(unmapped)
    assert mapped == obj


class TestFunctionalityMapper:
    @pytest.fixture(autouse=True)
    def now(self, mocker):
        now = DATETIME
        mocker.patch('mail.payments.payments.storage.mappers.functionality.func.now', mocker.Mock(return_value=now))
        return now

    @pytest.mark.asyncio
    async def test_create(self, functionality, create_functionality):
        created = await create_functionality(functionality)

        assert_that(
            created,
            equal_to(functionality)
        )

    @pytest.mark.asyncio
    async def test_create_duplicate(self, functionality, create_functionality):
        await create_functionality(functionality)

        with pytest.raises(DuplicateFunctionalityStorageError):
            await create_functionality(functionality)

    @pytest.mark.asyncio
    async def test_get(self, functionality, storage, create_functionality):
        functionality = await create_functionality(functionality)

        from_db = await storage.functionality.get(functionality.uid, functionality.functionality_type)

        assert from_db == functionality

    @pytest.mark.asyncio
    async def test_get_not_found(self, functionality, storage, create_functionality):
        with pytest.raises(MerchantFunctionality.DoesNotExist):
            await storage.functionality.get(functionality.uid, functionality.functionality_type)

    @pytest.mark.asyncio
    async def test_save(self, create_functionality, storage, merchant):
        functionality = MerchantFunctionality(
            uid=merchant.uid,
            functionality_type=FunctionalityType.YANDEX_PAY,
            data=YandexPayPaymentGatewayFunctionalityData(
                payment_gateway_type=YandexPayPaymentGatewayType.PSP,
                partner_id=uuid.uuid4(),
                gateway_id='123'
            ),
            created=DATETIME,
            updated=DATETIME,
        )
        functionality = await create_functionality(functionality)
        functionality.data.gateway_id = '456'

        await storage.functionality.save(functionality)

        from_db = await storage.functionality.get(functionality.uid, functionality.functionality_type)
        assert from_db == functionality

    @pytest.mark.asyncio
    async def test_ignore_created_during_save(self, functionality, create_functionality, storage):
        functionality = await create_functionality(functionality)

        functionality.created = utcnow()
        await storage.functionality.save(functionality)

        from_db = await storage.functionality.get(functionality.uid, functionality.functionality_type)
        assert from_db.created != functionality.created

    @pytest.mark.asyncio
    async def test_delete_by_uid(self, functionality, create_functionality, storage):
        functionality = await create_functionality(functionality)

        await storage.functionality.delete_by_uid(functionality.uid)

        assert_that(
            await storage.functionality.find_by_uid(functionality.uid),
            has_length(0),
        )
