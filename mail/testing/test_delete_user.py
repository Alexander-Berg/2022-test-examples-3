import pytest

from mail.payments.payments.core.actions.testing.delete_user import DeleteUserAction
from mail.payments.payments.core.entities.enums import FunctionalityType, MerchantRole, ModerationType
from mail.payments.payments.core.entities.functionality import MerchantFunctionality, PaymentsFunctionalityData
from mail.payments.payments.core.entities.moderation import Moderation
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.report import Report
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.storage import Storage
from mail.payments.payments.storage.mappers.order.order import FindOrderParams


@pytest.fixture
async def create_related_objects(merchant_uid, merchant, shop, service_merchant, storage: Storage):
    uid = merchant_uid
    await storage.moderation.create(
        Moderation(
            uid=uid,
            moderation_type=ModerationType.MERCHANT,
            revision=1,
            functionality_type=FunctionalityType.PAYMENTS
        )
    )
    await storage.report.create(Report(uid=uid))
    await storage.report.create(Report(uid=uid))
    await storage.user_role.create(UserRole(merchant_id=uid, uid=uid, role=MerchantRole.OWNER))
    order = await storage.order.create(Order(uid=uid, shop_id=shop.shop_id))
    order2 = await storage.order.create(Order(uid=uid, shop_id=shop.shop_id))
    await storage.transaction.create(Transaction(uid=uid, order_id=order.order_id))
    await storage.transaction.create(Transaction(uid=uid, order_id=order2.order_id))

    await storage.functionality.create(
        MerchantFunctionality(uid=uid, functionality_type=FunctionalityType.PAYMENTS, data=PaymentsFunctionalityData())
    )


@pytest.mark.asyncio
async def test_delete_user(storage: Storage, merchant_uid, create_related_objects):
    async def count(query):
        return len([_ async for _ in query])

    def query():
        return [
            storage.moderation.find(uid=merchant_uid),
            storage.report.find(uid=merchant_uid),
            storage.user_role.find(uid=merchant_uid),
            storage.transaction.find(uid=merchant_uid),
            storage.order.find(FindOrderParams(uid=merchant_uid)),
            storage.service_merchant.find(uid=merchant_uid),
            storage.merchant.find(uid=merchant_uid)
        ]

    for q in query():
        assert await count(q) > 0

    await DeleteUserAction(uid=merchant_uid).run()

    for q in query():
        assert await count(q) == 0
