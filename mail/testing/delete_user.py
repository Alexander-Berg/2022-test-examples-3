from mail.payments.payments.core.actions.base.db import BaseDBAction


class DeleteUserAction(BaseDBAction):
    '''
    See https://wiki.yandex-team.ru/finsrv/swat/payments/#snestiiztestingapolzovateljasnezhochekoauth
    '''
    transact = True

    def __init__(self, uid: int):
        super().__init__()
        self.uid = uid

    async def handle(self) -> None:
        await self.storage.moderation.delete_by_uid(uid=self.uid)
        await self.storage.report.delete_by_uid(uid=self.uid)
        await self.storage.user_role.delete_by_merchant_id(merchant_id=str(self.uid))
        await self.storage.transaction.delete_by_uid(uid=self.uid)
        await self.storage.order.delete_by_uid(uid=self.uid)
        await self.storage.service_merchant.delete_by_uid(uid=self.uid)
        await self.storage.functionality.delete_by_uid(uid=self.uid)
        await self.storage.merchant.delete_by_uid(uid=self.uid)
