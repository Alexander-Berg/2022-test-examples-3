import pytest

from mail.payments.payments.storage.exceptions import MerchantPreregistrationNotFound


@pytest.mark.asyncio
async def test_create_method_creates(storage, create_merchant_preregistration_entity):
    preregistration_entity = await create_merchant_preregistration_entity()
    preregistration_created = await storage.merchant_preregistration.create(preregistration_entity)
    assert preregistration_created == preregistration_entity


@pytest.mark.asyncio
async def test_get_or_create_method_creates(storage, create_merchant_preregistration_entity):
    preregistration_entity = await create_merchant_preregistration_entity()
    preregistration_created, is_created = await storage.merchant_preregistration.get_or_create(
        preregistration_entity, lookup_fields=('uid',))
    assert is_created
    assert preregistration_created == preregistration_entity


@pytest.mark.asyncio
async def test_get_or_create_method_gets(storage, merchant_preregistration):
    preregistration_fetched, is_created = await storage.merchant_preregistration.get_or_create(
        merchant_preregistration,
        lookup_fields=('uid',),
    )
    assert not is_created
    assert preregistration_fetched == merchant_preregistration


@pytest.mark.asyncio
async def test_get_method_gets(storage, merchant_uid, merchant_preregistration):
    preregistration_fetched = await storage.merchant_preregistration.get(uid=merchant_preregistration.uid)
    assert merchant_preregistration == preregistration_fetched


@pytest.mark.asyncio
async def test_get_method_raises_not_found_exception(storage, randn):
    with pytest.raises(MerchantPreregistrationNotFound):
        await storage.merchant_preregistration.get(uid=randn())


@pytest.mark.asyncio
async def test_save_method_saves(storage, merchant_preregistration, randn):
    inn = str(randn())
    merchant_preregistration.data.preregister_data.inn = inn
    preregistration_saved = await storage.merchant_preregistration.save(merchant_preregistration)
    assert preregistration_saved.data.preregister_data.inn == inn
