import pytest

from hamcrest import assert_that, contains_inanyorder, instance_of, match_equality

from mail.beagle.beagle.core.actions.smtp_cache import GenerateSMTPCacheAction
from mail.beagle.beagle.core.entities.enums import SubscriptionType
from mail.beagle.beagle.core.entities.smtp_cache import CacheSubscription, SMTPCache
from mail.beagle.beagle.core.entities.unit_subscription import UnitSubscription
from mail.beagle.beagle.interactions import BlackBoxClient
from mail.beagle.beagle.storage.exceptions import SMTPCacheNotFound
from mail.beagle.beagle.storage.mappers.mail_list import MailListMapper
from mail.beagle.beagle.utils.helpers import without_none


@pytest.mark.asyncio
class TestGenerateSMTPCacheAction:
    @pytest.fixture
    def gen_amount(self):
        return 1

    @pytest.fixture
    async def unit(self, randn, org, create_unit):
        return await create_unit(org_id=org.org_id, uid=randn())

    @pytest.fixture
    def email(self, randmail):
        return randmail()

    @pytest.fixture(params=list(SubscriptionType))
    def subscription_type(self, request):
        return request.param

    @pytest.fixture
    def blackbox_returned_func(self):
        def _handle(uids):
            return {
                'users': [
                    {
                        'id': str(uid),
                        'address-list': [{'default': True, 'address': f'{uid}@yandex.ru'}]
                    } for uid in uids
                ]
            }

        return _handle

    @pytest.fixture(autouse=True)
    async def setup(self, mock_blackbox, blackbox_returned_func,
                    unit_unit, parent_unit, unit, unit_subscription, unit_user, email, storage, mocker,
                    mail_list, mock_response_json, subscription_type, gen_amount):
        mocker.spy(MailListMapper, 'find')
        mocker.spy(BlackBoxClient, 'userinfo_by_uids')

        async def blackbox_handler(request):
            uids = request.query.get('uid').split(',')
            result = blackbox_returned_func(uids)
            return mock_response_json(result)

        for _ in range(gen_amount):
            mock_blackbox(blackbox_handler)

        await storage.unit_subscription.create(UnitSubscription(
            org_id=parent_unit.org_id,
            mail_list_id=mail_list.mail_list_id,
            unit_id=parent_unit.unit_id,
            subscription_type=subscription_type,
        ))

    @pytest.fixture
    def mail_list_id(self, mail_list):
        return mail_list.mail_list_id

    @pytest.fixture
    def uids(self, mail_list):
        return [mail_list.uid]

    @pytest.fixture
    def returned_func(self, org, mail_list_id, uids):
        async def _inner():
            params = without_none({
                'org_id': org.org_id,
                'mail_list_id': mail_list_id,
                'uids': uids,
            })
            return await GenerateSMTPCacheAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_mail_list_mapper(self, returned, org, mail_list_id, uids):
        MailListMapper.find.assert_called_once_with(
            match_equality(instance_of(MailListMapper)),
            org.org_id,
            mail_list_id=mail_list_id,
            uids=uids
        )

    async def test_userinfo(self, returned, mail_list, parent_unit, unit, unit_unit, unit_user):
        BlackBoxClient.userinfo_by_uids.assert_called_once_with(
            match_equality(instance_of(BlackBoxClient)),
            match_equality(contains_inanyorder(mail_list.uid, unit.uid, unit_user.uid))
        )

    @pytest.mark.parametrize('gen_amount', (1, 2))
    async def test_cache(self, returned_func, blackbox_returned_func, email, unit_user, unit, storage, org, mail_list,
                         gen_amount):
        with pytest.raises(SMTPCacheNotFound):
            await storage.smtp_cache.get(org.org_id, mail_list.uid)

        for _ in range(gen_amount):
            await returned_func()

        smtp_cache: SMTPCache = await storage.smtp_cache.get(org.org_id, mail_list.uid)
        blackbox_response = blackbox_returned_func((mail_list.uid, unit.uid, unit_user.uid))
        assert_that(
            smtp_cache.value.subscriptions,
            contains_inanyorder(*[
                CacheSubscription(
                    uid=int(user['id']),
                    org_id=org.org_id,
                    local_part=user['address-list'][0]['address'].rsplit('@', 1)[0]
                )
                for user in blackbox_response['users']
            ])
        )
