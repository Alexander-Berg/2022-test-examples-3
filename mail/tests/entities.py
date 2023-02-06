import pytest

from mail.beagle.beagle.core.entities.enums import SubscriptionType
from mail.beagle.beagle.core.entities.mail_list import MailList, MailListType
from mail.beagle.beagle.core.entities.mail_list_responsible import MailListResponsible
from mail.beagle.beagle.core.entities.organization import Organization
from mail.beagle.beagle.core.entities.serial import Serial
from mail.beagle.beagle.core.entities.smtp_cache import (
    CacheResponse, CacheSubscription, CacheValue, Recipient, SMTPCache
)
from mail.beagle.beagle.core.entities.unit import Unit
from mail.beagle.beagle.core.entities.unit_subscription import UnitSubscription
from mail.beagle.beagle.core.entities.unit_unit import UnitUnit
from mail.beagle.beagle.core.entities.unit_user import UnitUser
from mail.beagle.beagle.core.entities.user import User
from mail.beagle.beagle.core.entities.user_subscription import UserSubscription


@pytest.fixture
def org_id(randn):
    return randn()


@pytest.fixture
def create_org(storage):
    async def _inner(org_id):
        async with storage.conn.begin():
            org = await storage.organization.create(Organization(org_id=org_id))
            await storage.serial.create(Serial(org_id=org_id))
        return org

    return _inner


@pytest.fixture
async def org(storage, org_id, create_org):
    return await create_org(org_id)


@pytest.fixture
async def other_org(storage, org_id, create_org):
    return await create_org(org_id + 1)


@pytest.fixture
def create_mail_list(storage, randn, rands):
    async def _inner(org_id):
        return await storage.mail_list.create(MailList(
            org_id=org_id,
            mail_list_type=MailListType.MANUAL,
            uid=randn(),
            username=rands(),
        ))

    return _inner


@pytest.fixture
async def mail_list(org, create_mail_list):
    return await create_mail_list(org.org_id)


@pytest.fixture
async def mail_lists(org, other_org, create_mail_list):
    return [
        await create_mail_list(org_id)
        for org_id in (org.org_id, other_org.org_id)
        for _ in range(3)
    ]


@pytest.fixture
def create_user_entity(randn, rands):
    def _inner(org_id, uid=None):
        uid = uid or randn()
        return User(
            org_id=org_id,
            uid=uid,
            username=rands(),
            first_name=rands(),
            last_name=rands(),
        )

    return _inner


@pytest.fixture
def create_user(storage, create_user_entity):
    async def _inner(*args, **kwargs):
        return await storage.user.create(create_user_entity(*args, **kwargs))

    return _inner


@pytest.fixture
async def user(create_user, org):
    return await create_user(org.org_id)


@pytest.fixture
async def users(create_user, org):
    return [
        await create_user(org.org_id)
        for _ in range(5)
    ]


@pytest.fixture
async def users_with_other_org(create_user, org, other_org):
    return [
        await create_user(org_id)
        for org_id in (org.org_id, other_org.org_id)
        for _ in range(3)
    ]


@pytest.fixture
def create_unit_entity(rands):
    def _inner(org_id, external_id=None, uid=None):
        external_id = external_id or rands()
        return Unit(
            org_id=org_id,
            external_id=external_id,
            external_type=rands(),
            name=rands(),
            uid=uid,
        )

    return _inner


@pytest.fixture
def create_unit(storage, create_unit_entity):
    async def _inner(*args, **kwargs):
        return await storage.unit.create(create_unit_entity(*args, **kwargs))

    return _inner


@pytest.fixture
async def unit(storage, create_unit, org):
    return await create_unit(org.org_id)


@pytest.fixture
async def parent_unit(storage, create_unit, org):
    return await create_unit(org.org_id)


@pytest.fixture
async def units(storage, org, create_unit):
    return [await create_unit(org.org_id) for _ in range(5)]


@pytest.fixture
async def units_with_other_org(storage, org, other_org, create_unit):
    return [
        await create_unit(org_id)
        for org_id in (org.org_id, other_org.org_id)
        for _ in range(5)
    ]


@pytest.fixture
async def mail_list_responsible(storage, org, mail_list, user):
    return await storage.mail_list_responsible.create(MailListResponsible(
        org_id=org.org_id,
        mail_list_id=mail_list.mail_list_id,
        uid=user.uid,
    ))


@pytest.fixture
async def mail_list_responsibles(storage, org, mail_list, users):
    mail_list_responsibles = []
    for user in users:
        responsible = await storage.mail_list_responsible.create(MailListResponsible(
            org_id=org.org_id,
            mail_list_id=mail_list.mail_list_id,
            uid=user.uid,
        ))
        mail_list_responsibles.append(responsible)
    return mail_list_responsibles


@pytest.fixture
def unit_unit_entity(unit, parent_unit):
    return UnitUnit(
        org_id=unit.org_id,
        unit_id=unit.unit_id,
        parent_unit_id=parent_unit.unit_id,
    )


@pytest.fixture
def create_unit_unit(storage):
    async def _inner(org_id, unit_id, parent_unit_id):
        return await storage.unit_unit.create(UnitUnit(
            org_id=org_id,
            unit_id=unit_id,
            parent_unit_id=parent_unit_id,
        ))

    return _inner


@pytest.fixture
async def unit_unit(storage, unit_unit_entity):
    return await storage.unit_unit.create(unit_unit_entity)


@pytest.fixture
def unit_subscription_entity(unit, mail_list):
    return UnitSubscription(
        org_id=unit.org_id,
        mail_list_id=mail_list.mail_list_id,
        unit_id=unit.unit_id,
    )


@pytest.fixture
def create_unit_subscription(storage):
    async def _inner(org_id, mail_list_id, unit_id):
        return await storage.unit_subscription.create(UnitSubscription(
            org_id=org_id,
            mail_list_id=mail_list_id,
            unit_id=unit_id,
        ))

    return _inner


@pytest.fixture
def user_subscription_entity(mail_list, user):
    return UserSubscription(
        org_id=mail_list.org_id,
        mail_list_id=mail_list.mail_list_id,
        uid=user.uid,
        subscription_type=SubscriptionType.INBOX,
    )


@pytest.fixture
async def unit_subscription(storage, unit_subscription_entity):
    return await storage.unit_subscription.create(unit_subscription_entity)


@pytest.fixture
async def unit_subscriptions(storage, org, mail_list, units):
    unit_subscriptions = []
    for unit in units:
        subscription = await storage.unit_subscription.create(UnitSubscription(
            org_id=org.org_id,
            mail_list_id=mail_list.mail_list_id,
            unit_id=unit.unit_id,
            subscription_type=SubscriptionType.INBOX,
        ))
        unit_subscriptions.append(subscription)
    return unit_subscriptions


@pytest.fixture
def create_unit_user_entity():
    def _inner(unit, user):
        assert unit.org_id == user.org_id
        return UnitUser(
            org_id=unit.org_id,
            unit_id=unit.unit_id,
            uid=user.uid,
        )

    return _inner


@pytest.fixture
def unit_user_entity(org, unit, user):
    return UnitUser(org_id=org.org_id, unit_id=unit.unit_id, uid=user.uid)


@pytest.fixture
def create_unit_user(storage, create_unit_user_entity):
    async def _inner(*args, **kwargs):
        return await storage.unit_user.create(create_unit_user_entity(*args, **kwargs))

    return _inner


@pytest.fixture
async def unit_user(storage, org_id, unit, user, create_unit_user):
    return await create_unit_user(unit, user)


@pytest.fixture
def create_user_subscription(storage):
    async def _inner(org_id, mail_list_id, uid):
        return await storage.user_subscription.create(UserSubscription(
            org_id=org_id,
            mail_list_id=mail_list_id,
            uid=uid,
            subscription_type=SubscriptionType.INBOX,
        ))

    return _inner


@pytest.fixture
async def user_subscription(storage, user_subscription_entity):
    return await storage.user_subscription.create(user_subscription_entity)


@pytest.fixture
async def user_subscriptions(storage, org, mail_list, users):
    user_subscriptions = []
    for user in users:
        subscription = await storage.user_subscription.create(
            UserSubscription(
                org_id=org.org_id,
                mail_list_id=mail_list.mail_list_id,
                uid=user.uid,
                subscription_type=SubscriptionType.INBOX,
            )
        )
        user_subscriptions.append(subscription)
    return user_subscriptions


@pytest.fixture
def local_part(rands):
    return rands()


@pytest.fixture
def master_domain():
    return 'master.ru'


@pytest.fixture
def alias_domain():
    return 'alias.ru'


@pytest.fixture
def master_email_to(local_part, master_domain):
    return f'{local_part}@{master_domain}'


@pytest.fixture
def alias_email_to(local_part, alias_domain):
    return f'{local_part}@{alias_domain}'


@pytest.fixture
def email_to(master_email_to):
    return master_email_to


@pytest.fixture
def cache_subscriptions(users, mail_list, local_part):
    subscriptions = [CacheSubscription(uid=user.uid, org_id=user.org_id, local_part=user.username) for user in users]
    subscriptions.append(CacheSubscription(local_part=local_part, org_id=mail_list.org_id, uid=mail_list.uid))
    return subscriptions


@pytest.fixture
def smtp_cache_value(rands, cache_subscriptions):
    return CacheValue(subscriptions=cache_subscriptions)


@pytest.fixture
def smtp_cache_entity(mail_list, rands, smtp_cache_value):
    return SMTPCache(org_id=mail_list.org_id, uid=mail_list.uid, value=smtp_cache_value)


@pytest.fixture
async def smtp_cache(storage, smtp_cache_entity):
    return await storage.smtp_cache.create_or_update(smtp_cache_entity)


@pytest.fixture
def recipients(users, mail_list, email_to, master_domain):
    recipients = [Recipient(email=f'{user.username}@{master_domain}', uid=user.uid) for user in users]
    recipients.append(Recipient(email=email_to, uid=mail_list.uid))
    return recipients


@pytest.fixture
def cache_response(rands, recipients):
    return CacheResponse(subscriptions=recipients)
