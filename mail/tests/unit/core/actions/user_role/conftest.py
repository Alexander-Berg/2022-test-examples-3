import pytest

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.core.entities.userinfo import UserInfo


@pytest.fixture
def user_email():
    return 'qq@ya.ru'


@pytest.fixture
def default_user_email():
    return 'qq@yandex.ru'


@pytest.fixture
def user_uid():
    return 1


@pytest.fixture
def info(user_uid, default_user_email):
    return UserInfo(uid=user_uid, default_email=default_user_email)


@pytest.fixture
def role():
    return MerchantRole.VIEWER


@pytest.fixture(autouse=True)
def blackbox_mock(blackbox_client_mocker, info):
    with blackbox_client_mocker('userinfo', info) as mock:
        yield mock


@pytest.fixture
async def user_entity(storage, user_uid, user_email):
    return User(uid=user_uid, email=user_email)


@pytest.fixture
async def user(storage, user_entity):
    return await storage.user.create(user_entity)


@pytest.fixture
async def user_role_entity(storage, merchant, role, user_uid):
    return UserRole(uid=user_uid, merchant_id=str(merchant.uid), role=role)


@pytest.fixture
async def user_role(storage, user, merchant, user_role_entity):
    return await storage.user_role.create(user_role_entity)
