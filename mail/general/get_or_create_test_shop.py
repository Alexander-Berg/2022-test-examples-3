from typing import Optional

from mail.payments.payments.core.actions.base.db import BaseDBAction
from mail.payments.payments.core.entities.enums import ShopType
from mail.payments.payments.core.entities.shop import Shop, ShopSettings
from mail.payments.payments.core.exceptions import ShopNotFoundError
from mail.payments.payments.storage.exceptions import ShopNotFound


class GetTestShopOrEnsureTestShopAction(BaseDBAction):
    """
    Get Shop or ensure default shop exists (get or create default shop).
    Given shop_id just get shop by uid, shop_id pair, otherwise
    get default merchant shop, and if it does not exist create one.
    """

    transact = False

    def __init__(self, uid: int, shop_id: Optional[int] = None):
        super().__init__()
        self.uid = uid
        self.shop_id = shop_id

    async def handle(self) -> Shop:
        if self.shop_id is not None:
            try:
                return await self.storage.shop.get(uid=self.uid, shop_id=self.shop_id)
            except ShopNotFound:
                raise ShopNotFoundError(
                    message='Shop not found',
                    params={'shop_id': self.shop_id},
                )

        test_shop_entity = Shop(
            uid=self.uid,
            name='Тестовый',
            shop_type=ShopType.Test,
            settings=ShopSettings(),
        )
        shop, _ = await self.storage.shop.get_or_create(
            test_shop_entity,
            lookup_fields=('uid', 'shop_type',),
        )
        return shop
