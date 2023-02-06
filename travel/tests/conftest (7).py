import datetime
from unittest import mock

import pytest
import random
import os

from asyncio import run
from itertools import count, product
from decorator import contextmanager
from sqlalchemy import MetaData, create_engine, event
from sqlalchemy.orm import Session, sessionmaker as sa_session_maker
from sqlalchemy.pool import StaticPool

import travel.avia.subscriptions.app.model.db as db_models

from travel.avia.subscriptions.app.api.consts import TOKEN_AUTH_TYPE
from travel.avia.subscriptions.app.api.init_db import MdbAPI
from travel.avia.subscriptions.app.api.interactor.user_confirm import (
    UserConfirmActor
)
from travel.avia.subscriptions.app.api.interactor.user_subscription_list import (
    UserSubscriptionListActor
)
from travel.avia.subscriptions.app.api.interactor.user_price_change_subscription import (
    UserPriceChangeSubscriptionActor
)
from travel.avia.subscriptions.app.api.interactor.user_promo_subscription import (
    UserPromoSubscriptionActor
)
from travel.avia.subscriptions.app.api.interactor.user_subscription import (
    UserSubscriptionActor
)
from travel.avia.subscriptions.app.api.interactor.user_token import (
    UserTokenActor
)
from travel.avia.subscriptions.app.lib.dicts import PointKeyResolver
from travel.avia.subscriptions.app.lib.qkey import qkey_from_params
from travel.avia.subscriptions.app.model.storage import DatabaseStorage


DICT_CLASS = (
    'travel.avia.subscriptions'
    '.app.lib.dicts.Dict'
)

SENDER_CLASS = (
    'travel.avia.subscriptions.app'
    '.lib.sender.TransactionalApi'
)


@pytest.fixture()
def PromoSubscription():
    return DatabaseStorage(db_models.PromoSubscription)


@pytest.fixture()
def TravelVertical():
    return DatabaseStorage(db_models.TravelVertical)


@pytest.fixture()
def Email():
    return DatabaseStorage(db_models.Email)


@pytest.fixture()
def UserAuthType():
    return DatabaseStorage(db_models.UserAuthType)


@pytest.fixture()
def UserAuth():
    return DatabaseStorage(db_models.UserAuth)


@pytest.fixture()
def User():
    return DatabaseStorage(db_models.User)


@pytest.fixture()
def UserPromoSubscription():
    return DatabaseStorage(db_models.UserPromoSubscription)


@pytest.fixture()
def PriceChangeSubscription():
    return DatabaseStorage(db_models.PriceChangeSubscription)


@pytest.fixture()
def UserPriceChangeSubscription():
    return DatabaseStorage(db_models.UserPriceChangeSubscription)


@pytest.fixture()
def engine():
    pg = lambda s: os.environ.get(f'PG_LOCAL_{s.upper()}')
    engine = create_engine(
        f'postgresql://{pg("user")}:{pg("password")}@localhost:{pg("port")}/{pg("database")}',
        poolclass=StaticPool,
        echo=False,
    )
    yield engine
    engine.dispose()


# https://gist.github.com/absent1706/3ccc1722ea3ca23a5cf54821dbc813fb#gistcomment-3107613
@pytest.fixture(autouse=True)
def truncate_db(request, engine):
    def has_model():
        for name in request.fixturenames:
            if hasattr(db_models, name):
                return True
        return False

    # Запустим после теста
    yield
    # Применим фикстуру там, где действительно используется база
    if 'no_truncate' in request.keywords or not has_model():
        return

    meta = MetaData(bind=engine, reflect=True)
    con = engine.connect()
    trans = con.begin()
    for table in meta.sorted_tables:
        con.execute(f'TRUNCATE TABLE "{table.name}" RESTART IDENTITY CASCADE;')
    trans.commit()


@pytest.fixture()
def session(engine) -> Session:
    db_models.Base.metadata.create_all(engine)
    Session = sa_session_maker(engine)
    return Session()


@pytest.fixture()
def session_provider(session):
    @contextmanager
    def sp(*args, **kwargs) -> Session:
        try:
            yield session
            session.commit()
        except:
            session.rollback()
            raise

    return sp


@pytest.fixture()
def blackbox():
    class MockBlackbox:
        def __init__(self):
            self.emails = {}

        def list_emails(self, uid):
            return self.emails.get(uid, [])

    return MockBlackbox()


@pytest.fixture()
def mailbox():
    return []


@pytest.fixture()
def point_key_resolver(mocker):
    none_on = []

    class MockDict:
        def __init__(self, *_, **__):  # noqa
            pass

        @staticmethod
        async def get(pk):
            if pk in none_on:
                return None

            return mocker.Mock(TitleDefault=pk)

    class MockPointKeyResolver(PointKeyResolver):
        @staticmethod
        def set_return_none_on(*keys):
            nonlocal none_on
            none_on = keys

    mocker.patch(DICT_CLASS, side_effect=MockDict)
    return MockPointKeyResolver()


@pytest.fixture()
def user_confirm_actor(
    User, Email, user_token_actor, session_provider,
    blackbox, mailbox, mocker
):
    class Sender:
        @staticmethod
        async def send(to_email, args, **_):
            mailbox.append((to_email, args))
    sender_factory = mocker.patch(SENDER_CLASS, side_effect=Sender)

    return UserConfirmActor(
        user=User,
        email=Email,
        user_token_actor=user_token_actor,
        session_provider=session_provider,
        blackbox=blackbox,
        single_opt_in_sender=sender_factory(),
        double_opt_in_sender=sender_factory(),
    )


@pytest.fixture()
def user_price_actor(
    PriceChangeSubscription, TravelVertical, Email, UserAuthType, UserAuth,
    User, UserPriceChangeSubscription, session_provider, user_confirm_actor,
    point_key_resolver, blackbox
):
    return UserPriceChangeSubscriptionActor(
        travel_vertical=TravelVertical,
        user=User,
        user_auth=UserAuth,
        user_auth_type=UserAuthType,
        email=Email,
        price_change_subscription=PriceChangeSubscription,
        user_price_change_subscription=UserPriceChangeSubscription,
        user_confirm_actor=user_confirm_actor,
        session_provider=session_provider,
        point_key_resolver=point_key_resolver,
        blackbox=blackbox,
    )


@pytest.fixture()
def user_promo_actor(
    PromoSubscription, TravelVertical, Email, UserAuthType, UserAuth, User, UserPromoSubscription,
    user_confirm_actor, session_provider, blackbox
):
    return UserPromoSubscriptionActor(
        promo_subscription=PromoSubscription,
        travel_vertical=TravelVertical,
        email=Email,
        user_auth_type=UserAuthType,
        user_auth=UserAuth,
        user=User,
        user_promo_subscription=UserPromoSubscription,
        user_confirm_actor=user_confirm_actor,
        session_provider=session_provider,
        blackbox=blackbox
    )


@pytest.fixture()
def user_subscription_actor(
    Email, User, UserAuth, UserAuthType, TravelVertical, blackbox,
    user_promo_actor, user_price_actor, session_provider
):
    return UserSubscriptionActor(
        email=Email,
        travel_vertical=TravelVertical,
        user=User,
        user_auth=UserAuth,
        user_auth_type=UserAuthType,
        user_promo_subscription_actor=user_promo_actor,
        user_price_change_subscription_actor=user_price_actor,
        blackbox=blackbox,
        session_provider=session_provider,
    )


@pytest.fixture()
def user_subscription_list_actor(
    Email, User, UserAuth, UserAuthType, session_provider,
    user_price_actor, user_promo_actor
):
    return UserSubscriptionListActor(
        email=Email,
        user=User,
        user_auth=UserAuth,
        user_auth_type=UserAuthType,
        session_provider=session_provider,
        user_promo_subscription_actor=user_promo_actor,
        user_price_change_subscription_actor=user_price_actor,
    )


@pytest.fixture()
def user_token_actor(
    Email, UserAuthType, UserAuth, UserPromoSubscription, User, session_provider
):
    actor = UserTokenActor(
        email=Email,
        user_auth_type=UserAuthType,
        user_auth=UserAuth,
        user_promo_subscription=UserPromoSubscription,
        user=User,
        session_provider=session_provider
    )

    return actor


@pytest.fixture()
def qkey_factory():
    def generate(
        point_from_key=None, point_to_key=None,
        date_forward=None, date_backward=None,
        klass=None, adults=None, children=None,
        infants=None, national_version=None,
        start=None, set_date_backward_none=False
    ):
        start = start or datetime.datetime.now()
        if date_forward is None:
            date_forward = start + datetime.timedelta(random.randint(3, 20))
        if set_date_backward_none:
            date_backward = None
        elif date_backward is None:
            date_backward = date_forward + datetime.timedelta(random.randint(1, 10))

        return qkey_from_params(
            point_from_key or random.choice(['c213', 's65646']),
            point_to_key or random.choice(['c213', 's65646']),
            date_forward,
            date_backward,
            klass or random.choice(['economy', 'business']),
            random.randint(1, 3) if adults is None else adults,
            random.randint(0, 3) if children is None else children,
            random.randint(0, 3) if infants is None else infants,
            national_version or random.choice(['ru', 'com'])
        )

    return generate


@pytest.fixture()
def qid_factory(qkey_factory):
    def generate(return_qkey=False):
        qkey = qkey_factory()
        qid = f'171206-144220-171.ticket.plane.{qkey}.ru'
        if return_qkey:
            return qid, qkey

        return qid

    return generate


@pytest.fixture(autouse=True)
def mock_auth_token(UserAuthType, session):
    counter = count()

    def set_mocked_token(mapper, connection, target):
        token_type = UserAuthType(session).get(name=TOKEN_AUTH_TYPE)
        if token_type is not None and target.user_auth_type_id == token_type.id:
            target.auth_value = f'token-{next(counter)}'

    event.listen(db_models.UserAuth, 'before_insert', set_mocked_token)
    yield
    event.remove(db_models.UserAuth, 'before_insert', set_mocked_token)


@pytest.fixture()
def approve_all_users(User, session):
    def approve_all():
        users = User(session).find()
        for user in users:
            user.approved_at = datetime.datetime.utcnow()

        session.flush(users)

    return approve_all


@pytest.fixture()
def all_credentials(UserAuth, UserAuthType, session):
    types = UserAuthType(session).find()
    values = UserAuth(session).find()

    return [
        (auth_type.name, auth_value.auth_value)
        for auth_type in types
        for auth_value in values
        if auth_type.id == auth_value.user_auth_type_id
    ]


@pytest.fixture()
def fill_db(
    PromoSubscription, TravelVertical, UserAuthType,
    approve_all_users, session, user_subscription_actor,
    user_token_actor
):
    def defer():
        # Подтвердим всех пользователей
        approve_all_users()

    _create_many_versions_of_subscriptions(PromoSubscription, session, 'travel_news')
    _create_many_versions_of_subscriptions(PromoSubscription, session, 'other_promo')
    _create_many_versions_of_subscriptions(PromoSubscription, session, 'yet_other_promo')
    TravelVertical(session).get_or_create(name='avia')
    TravelVertical(session).get_or_create(name='train')
    UserAuthType(session).create(name='session')
    UserAuthType(session).create(name='cookie')
    UserAuthType(session).create(name=TOKEN_AUTH_TYPE)

    run(
        user_subscription_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            subscriptions=[
                _promo('travel_news'),
                _price('c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru'),
            ],
            source='WIZARD',
            travel_vertical_name='train',
            national_version='ru',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )

    run(
        user_subscription_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            subscriptions=[
                _promo('travel_news'),
                _price('c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru'),
            ],
            source='WIZARD',
            travel_vertical_name='train',
            national_version='com',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )

    run(
        user_subscription_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('cookie', 'cookie_val_1'),),
            subscriptions=[
                _promo('travel_news'),
                _price('c213_c2_2017-12-21_2018-01-05_economy_1_0_1_com'),
            ],
            source='WIZARD',
            travel_vertical_name='train',
            national_version='com',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )

    run(
        user_subscription_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('cookie', 'cookie_val_1'),),
            subscriptions=[
                _promo('other_promo'),
                _price('c213_c2_2017-12-21_2018-12-05_business_1_0_1_com'),
            ],
            source='WIZARD',
            travel_vertical_name='train',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )

    run(
        user_subscription_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('cookie', 'cookie_val_1'),),
            subscriptions=[
                _promo('yet_other_promo'),
                _price('c213_c22_2018-11-21_2018-12-05_economy_3_3_1_com'),
            ],
            source='WIZARD',
            travel_vertical_name='train',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )

    run(
        user_subscription_actor.put(
            email='yet_other@yet_email.ru',
            credentials=(('cookie', 'cookie_val_2'),),
            subscriptions=[
                _promo('yet_other_promo'),
                _price('c213_c22_2018-11-21_2018-12-05_economy_3_3_1_com'),
            ],
            source='WIZARD',
            travel_vertical_name='train',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Vitaly',
        )
    )

    run(
        user_subscription_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('cookie', 'cookie_val_1'),),
            subscriptions=[
                _promo('other_promo'),
                _price('c213_c2_2017-12-21_2018-12-05_business_1_0_1_com'),
            ],
            source='WIZARD',
            travel_vertical_name='train',
            national_version='com',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )
    defer()


def _create_many_versions_of_subscriptions(PromoSubscription, session, code):
    for n, l in product(('com', 'ru'), ('ru', 'en')):
        PromoSubscription(session).create(
            code=code, national_version=n, language=l
        )


def _promo(code):
    return {
        'subscription_type': 'promo',
        'subscription_code': code,
    }


def _price(qkey):
    return {
        'subscription_type': 'price',
        'subscription_code': qkey,
    }


@pytest.fixture(autouse=True, scope='session')
def exclude_network_interaction_in_mdb_api():
    with mock.patch.object(MdbAPI, 'get_cluster_info') as m_get_cluster_info:
        m_get_cluster_info.return_value = mock.Mock(instances=[])
        yield
