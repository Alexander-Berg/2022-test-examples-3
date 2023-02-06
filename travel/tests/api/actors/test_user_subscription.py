import pytest

from asyncio import run
from datetime import datetime

from travel.avia.subscriptions.app.api.exceptions import NoAccess, EmailNotFound
from travel.avia.subscriptions.app.model.schemas import Filter, MinPrice
from travel.avia.subscriptions.app.model.storage import UpsertAction


SOME_SUBSCRIPTION = {
    'subscription_type': 'price',
    'subscription_code': 'c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru',
}


@pytest.mark.usefixtures('fill_db')
def test_delete(user_subscription_actor):
    result = run(user_subscription_actor.delete(
        subscriptions=[
            {
                'subscription_type': 'promo',
                'subscription_code': 'travel_news',
            },
            {
                'subscription_type': 'price',
                'subscription_code': 'c213_c2_2017-12-21_2018-12-05_business_1_0_1_com',
            },
            {
                'subscription_type': 'price',
                'subscription_code': 'c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru',
            }
        ],
        credentials=(('cookie', 'cookie_val_1'),),
        email='mikhail@lomonosov.ru'
    ))

    assert result == {
        'promo': {'travel_news'},
        'price': {
            'c213_c2_2017-12-21_2018-12-05_business_1_0_1_com',
            'c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru',
        }
    }


def test_delete_with_no_access(
    TravelVertical, UserAuthType, approve_all_users,
    session, user_subscription_actor
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    run(user_subscription_actor.put(
        email='vasya@pupkin.ru',
        credentials=(
            ('session', '123456789abc'),
        ),
        subscriptions=[SOME_SUBSCRIPTION],
        source='VALERA',
        travel_vertical_name='avia',
        national_version='com',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
    ))
    # Подтвердили всех пользователей
    approve_all_users()
    # не существующий тип credential
    with pytest.raises(NoAccess):
        run(user_subscription_actor.delete(
            subscriptions=[],
            credentials=(('unknown_auth_type', 'bad'),),
            email='vasya@pupkin.ru'
        ))

    # неверные аутенфикационные данные
    with pytest.raises(NoAccess):
        run(user_subscription_actor.delete(
            subscriptions=[],
            credentials=(('session', 'incorrect'),),
            email='vasya@pupkin.ru'
        ))


def test_delete_with_unknown_email(
    TravelVertical, UserAuthType, approve_all_users,
    session, user_subscription_actor
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    run(user_subscription_actor.put(
        email='vasya@pupkin.ru',
        credentials=(
            ('session', '123456789abc'),
        ),
        subscriptions=[SOME_SUBSCRIPTION],
        source='VALERA',
        travel_vertical_name='avia',
        national_version='com',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
    ))
    # Подтвердили всех пользователей
    approve_all_users()

    with pytest.raises(EmailNotFound):
        run(user_subscription_actor.delete(
            subscriptions=[SOME_SUBSCRIPTION],
            credentials=(('session', '123456789abc'),),
            email='incorrect@mail.ru'
        ))


def test_delete_with_passport_access(
    TravelVertical, UserAuthType, approve_all_users,
    blackbox, session, user_subscription_actor
):
    # Зададим доступы в blackbox
    blackbox.emails['uid-1'] = ['mikhail@lomonosov.ru', 'vasya@pupkin.ru']
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    run(user_subscription_actor.put(
        email='vasya@pupkin.ru',
        credentials=(
            ('session', '123456789abc'),
        ),
        subscriptions=[SOME_SUBSCRIPTION],
        source='VALERA',
        travel_vertical_name='avia',
        national_version='com',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
    ))
    # Подтвердили всех пользователей
    approve_all_users()
    # passportId  - зарезервированный credential
    result = run(user_subscription_actor.delete(
        subscriptions=[SOME_SUBSCRIPTION],
        credentials=(('passportId', 'uid-1'),),
        email='vasya@pupkin.ru'
    ))
    assert result == {
        SOME_SUBSCRIPTION['subscription_type']: {
            SOME_SUBSCRIPTION['subscription_code']
        }
    }


def test_delete_only_approved_and_non_deleted_on_authenticate_step(
    TravelVertical, UserAuthType, User,
    session, user_subscription_actor
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    run(user_subscription_actor.put(
        email='vasya@pupkin.ru',
        credentials=(
            ('session', '123456789abc'),
        ),
        subscriptions=[SOME_SUBSCRIPTION],
        source='VALERA',
        travel_vertical_name='avia',
        national_version='com',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
    ))
    # Сейчас пользователь mikhail@lomonosov.ru не подтвержден
    # Поэтому этот пользователь сейчас не доступен
    with pytest.raises(NoAccess):
        run(user_subscription_actor.delete(
            subscriptions=[SOME_SUBSCRIPTION],
            credentials=(('session', '123456789abc'),),
            email='vasya@pupkin.ru'
        ))
    # Подтвердим и удалим пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=1),
        values=dict(
            deleted_at=datetime.utcnow(),
            approved_at=datetime.utcnow(),
        )
    )
    # Удаленный пользователь также недоступен
    with pytest.raises(NoAccess):
        run(user_subscription_actor.delete(
            subscriptions=[SOME_SUBSCRIPTION],
            credentials=(('session', '123456789abc'),),
            email='vasya@pupkin.ru'
        ))


def test_put(
    PromoSubscription, TravelVertical, UserAuthType,
    PriceChangeSubscription, session, user_subscription_actor,
    qkey_factory
):
    qkey = qkey_factory()
    promo = PromoSubscription(session).create(
        code='travel_news',
        national_version='com',
        language='en'
    )
    travel_vertical = TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    UserAuthType(session).get_or_create(name='cookie')

    result = run(user_subscription_actor.put(
        email='vasya@pupkin.ru',
        credentials=(
            ('session', '123456789abc'),
            ('cookie', 'cookie-val'),
        ),
        subscriptions=[
            {
                'subscription_type': 'price',
                'subscription_code': qkey,
                'subscription_params': {
                    'date_range': 2,
                    'min_price': MinPrice(
                        time=datetime.now(),
                        value=123.12,
                        currency='RUB'
                    ),
                    'filter_': Filter()
                }
            },
            {
                'subscription_type': 'promo',
                'subscription_code': 'travel_news',
            },
            {
                'subscription_type': 'some_new_type',
                'subscription_code': 'some_code',
            },
        ],
        source='VALERA',
        travel_vertical_name='avia',
        national_version='com',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
    ))

    assert result.keys() == {'price', 'promo', 'some_new_type'}
    assert len(result['some_new_type'][UpsertAction.UPDATE]) == 0
    assert len(result['some_new_type'][UpsertAction.INSERT]) == 0
    assert len(result['promo'][UpsertAction.UPDATE]) == 0
    assert len(result['promo'][UpsertAction.INSERT]) == 2
    assert len(result['price'][UpsertAction.UPDATE]) == 0
    assert len(result['price'][UpsertAction.INSERT]) == 2

    created_subscriptions = result['promo'][UpsertAction.INSERT]
    for index, subscription in enumerate(created_subscriptions, 1):
        assert subscription.user_id == index
        assert subscription.promo_subscription_id == promo.id
        assert subscription.travel_vertical_id == travel_vertical.id
        assert subscription.source == 'VALERA'

    created_subscriptions = result['price'][UpsertAction.INSERT]
    price_subscription = PriceChangeSubscription(session).get(qkey=qkey)
    assert price_subscription is not None
    for index, subscription in enumerate(created_subscriptions, 1):
        assert subscription.user_id == index
        assert subscription.price_change_subscription_id == price_subscription.id
        assert subscription.travel_vertical_id == travel_vertical.id
        assert subscription.source == 'VALERA'

    result = run(user_subscription_actor.put(
        email='vaSYa@pupkin.ru',
        credentials=(
            ('session', '123456789abc'),
            ('cookie', 'cookie-val'),
        ),
        subscriptions=[
            {
                'subscription_type': 'price',
                'subscription_code': qkey,
                'subscription_params': {
                    'date_range': 2,
                    'min_price': MinPrice(
                        time=datetime.now(),
                        value=123.12,
                        currency='RUB'
                    ),
                    'filter_': Filter()
                }
            },
            {
                'subscription_type': 'promo',
                'subscription_code': 'travel_news',
            },
            {
                'subscription_type': 'some_new_type',
                'subscription_code': 'some_code',
            },
        ],
        source='VALERA',
        travel_vertical_name='avia',
        national_version='com',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
    ))

    assert len(result['some_new_type'][UpsertAction.UPDATE]) == 0
    assert len(result['some_new_type'][UpsertAction.INSERT]) == 0
    assert len(result['promo'][UpsertAction.UPDATE]) == 2
    assert len(result['promo'][UpsertAction.INSERT]) == 0
    assert len(result['price'][UpsertAction.UPDATE]) == 2
    assert len(result['price'][UpsertAction.INSERT]) == 0


def test_put_ignore_bad_subscriptions(
    PromoSubscription, TravelVertical, UserAuthType,
    PriceChangeSubscription, session, user_subscription_actor,
    qkey_factory, point_key_resolver
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    # Замоканный point_key_resolver игнорирует тип точки
    point_key_resolver.set_return_none_on(123)
    qkey = qkey_factory(point_from_key='c123')
    result = run(user_subscription_actor.put(
        email='vasya@pupkin.ru',
        credentials=(
            ('session', '123456789abc'),
        ),
        subscriptions=[
            {
                'subscription_type': 'price',
                # данных о точке вылета нет в справочнике
                'subscription_code': qkey,
            },
            {
                'subscription_type': 'promo',
                # несуществующая промо-подписка
                'subscription_code': 'travel_news',
            },
        ],
        source='VALERA',
        travel_vertical_name='avia',
        national_version='com',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
    ))
    assert len(result['promo'][UpsertAction.INSERT]) == 0
    assert len(result['price'][UpsertAction.INSERT]) == 0
