import pytest

from asyncio import run
from contextlib import contextmanager
from dataclasses import asdict
from datetime import datetime, timedelta
from freezegun import freeze_time
from sqlalchemy import event

from travel.avia.subscriptions.app.api.exceptions import EmailNotFound, NoAccess, UnknownPointsError
from travel.avia.subscriptions.app.lib.qkey import qkey_from_params, structure_from_qkey
from travel.avia.subscriptions.app.model.db import PriceChangeSubscription
from travel.avia.subscriptions.app.model.schemas import Filter, MinPrice
from travel.avia.subscriptions.app.model.storage import UpsertAction


def test_put(
    user_price_actor, User, UserAuthType, TravelVertical,
    PriceChangeSubscription, session, qkey_factory
):
    travel_vertical = TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey = qkey_factory()
    filter_ = Filter(airlines=[1, 2, 3])
    min_price = MinPrice(datetime.now(), value=1.01, currency='RUB')
    result = run(
        user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qid=None,
            qkey=qkey,
            filter_=filter_,
            min_price=min_price
        )
    )

    created = result[UpsertAction.INSERT]
    updated = result[UpsertAction.UPDATE]
    assert len(created) == 1
    assert len(updated) == 0
    assert created[0].user_id == 1
    assert created[0].travel_vertical_id == travel_vertical.id
    assert created[0].source == 'VALERA'
    assert created[0].applied_filters == [filter_]
    assert created[0].last_seen_min_price == min_price
    price_change_subscription = PriceChangeSubscription(session).get(
        id=created[0].price_change_subscription_id
    )
    assert_price_change_subscription_has_qkey(price_change_subscription, qkey)

    # Нужно подтвердить пользователя в дальнейшем
    assert User(session).get(id=created[0].user_id).approved_at is None
    assert User(session).get(id=created[0].user_id).name == 'Anatoly'


def test_put_same_subscription_and_update_user_subscription(
    user_price_actor, UserAuthType,
    TravelVertical, session, qkey_factory
):
    travel_vertical = TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey = qkey_factory()
    filter_1 = Filter(airlines=[1, 2, 3])
    min_price_1 = MinPrice(datetime.now(), value=1.01, currency='RUB')
    result = run(
        user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qid=None,
            qkey=qkey,
            filter_=filter_1,
            min_price=min_price_1
        )
    )
    assert len(result[UpsertAction.INSERT]) == 1
    assert len(result[UpsertAction.UPDATE]) == 0

    filter_2 = Filter(filter_url_postfix='url')
    min_price_2 = MinPrice(datetime.now(), value=200, currency='RUB')
    result = run(
        user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA2',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qid=None,
            qkey=qkey,
            filter_=filter_2,
            min_price=min_price_2
        )
    )

    created = result[UpsertAction.INSERT]
    updated = result[UpsertAction.UPDATE]
    assert len(created) == 0
    assert len(updated) == 1
    assert updated[0].updated_at is not None
    assert updated[0].user_id == 1
    assert updated[0].travel_vertical_id == travel_vertical.id
    assert updated[0].source == 'VALERA2'
    assert updated[0].applied_filters == [filter_2]
    assert updated[0].last_seen_min_price == min_price_2


def test_put_expand_filter_to_date_range(
    user_price_actor, UserAuthType, PriceChangeSubscription,
    TravelVertical, session, qkey_factory
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    # Сделаем заведомо нерелевантную подписку
    qkey_1 = qkey_factory(
        date_forward=datetime.now() - timedelta(1),
        date_backward=None,
        set_date_backward_none=True
    )
    qkey_params = asdict(structure_from_qkey(qkey_1))
    qkey_params.pop('date_forward')
    qkey_params['set_date_backward_none'] = True

    run(user_price_actor.put(
        email='vasya@pupkin.ru',
        credentials=(('session', '123456789abc'),),
        source='VALERA',
        travel_vertical_name='avia',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
        qid=None,
        qkey=qkey_1,
    ))
    qkey_2 = qkey_factory(
        date_forward=datetime.now() + timedelta(3),
        **qkey_params
    )
    run(user_price_actor.put(
        email='vasya@pupkin.ru',
        credentials=(('session', '123456789abc'),),
        source='VALERA',
        travel_vertical_name='avia',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
        qid=None,
        qkey=qkey_2,
    ))

    # Проверяем, что фильтров нет
    assert len(PriceChangeSubscription(session).get(qkey=qkey_1).filtered_minprices) == 0
    assert len(PriceChangeSubscription(session).get(qkey=qkey_2).filtered_minprices) == 0

    filter_ = Filter(filter_url_postfix='url')
    # На день раньше текущей даты
    qkey_3 = qkey_factory(
        date_forward=datetime.now() + timedelta(1),
        **qkey_params
    )
    run(user_price_actor.put(
        email='vasya@pupkin.ru',
        credentials=(('session', '123456789abc'),),
        source='VALERA',
        travel_vertical_name='avia',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
        qid=None,
        qkey=qkey_3,
        filter_=filter_,
        date_range=3
    ))

    # Проверяем, что фильтры появились только у релевантных подписок
    assert len(PriceChangeSubscription(session).get(qkey=qkey_1).filtered_minprices) == 0
    assert len(PriceChangeSubscription(session).get(qkey=qkey_2).filtered_minprices) == 1
    assert len(PriceChangeSubscription(session).get(qkey=qkey_3).filtered_minprices) == 1

    # Проверяем, что фильтр верный
    for qkey in qkey_2, qkey_3:
        subscription = PriceChangeSubscription(session).get(qkey=qkey)
        assert subscription.filtered_minprices[0].filter == filter_


def test_put_expand_filter_to_date_range_already_has_that_filter(
    user_price_actor, UserAuthType, PriceChangeSubscription,
    TravelVertical, session, qkey_factory
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    filter_ = Filter(filter_url_postfix='url')
    qkey_1 = qkey_factory(
        date_forward=datetime.now() + timedelta(3),
        date_backward=None,
        set_date_backward_none=True
    )
    qkey_params = asdict(structure_from_qkey(qkey_1))
    qkey_params.pop('date_forward')
    qkey_params['set_date_backward_none'] = True

    run(user_price_actor.put(
        email='vasya@pupkin.ru',
        credentials=(('session', '123456789abc'),),
        source='VALERA',
        travel_vertical_name='avia',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
        qid=None,
        qkey=qkey_1,
        filter_=filter_
    ))
    assert len(PriceChangeSubscription(session).get(qkey=qkey_1).filtered_minprices) == 1
    qkey_2 = qkey_factory(
        date_forward=datetime.now() + timedelta(1),
        **qkey_params
    )
    run(user_price_actor.put(
        email='vasya@pupkin.ru',
        credentials=(('session', '123456789abc'),),
        source='VALERA',
        travel_vertical_name='avia',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
        qid=None,
        qkey=qkey_2,
        filter_=filter_,
        date_range=3
    ))
    assert len(PriceChangeSubscription(session).get(qkey=qkey_2).filtered_minprices) == 1
    # Новый фильтр не добавился
    subscription = PriceChangeSubscription(session).get(qkey=qkey_1)
    assert len(subscription.filtered_minprices) == 1
    assert subscription.filtered_minprices[0].filter == filter_


def test_put_points_not_found_in_dicts(
    UserAuthType, TravelVertical, session, qkey_factory,
    point_key_resolver, user_price_actor
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')

    # Замоканный point_key_resolver игнорирует тип точки
    point_key_resolver.set_return_none_on(123, 321)
    qkeys = (
        qkey_factory(point_from_key='c123'),
        qkey_factory(point_to_key='c123'),
        qkey_factory(point_from_key='c321', point_to_key='c123'),
    )
    for qkey in qkeys:
        with pytest.raises(UnknownPointsError):
            run(user_price_actor.put(
                email='vasya@pupkin.ru',
                credentials=(('session', '123456789abc'),),
                source='VALERA',
                travel_vertical_name='avia',
                language='en',
                timezone='Asia/Yekaterinburg',
                name='Anatoly',
                qid=None,
                qkey=qkey,
            ))


def test_subscribed_on_direction(
    user_price_actor, session, TravelVertical,
    UserAuthType, qkey_factory, func_now_patcher,
    approve_all_users
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    date_range = 3
    filter_ = Filter(filter_url_postfix='url')
    qkey = qkey_factory(date_forward=datetime(2020, 12, 11))
    qkey_params = asdict(structure_from_qkey(qkey))
    qkey_params.pop('date_forward')
    with func_now_patcher('2020-12-12'):
        run(user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qid=None,
            qkey=qkey,
            filter_=filter_,
            date_range=date_range
        ))
        # Подтвердим пользователей
        approve_all_users()
        expected_subscribed = {
            'subscribed': True,
            'filter': filter_.filter_url_postfix,
            'qkey': qkey,
            'date_range': date_range,
        }
        expected_nonsubscribed = {
            'subscribed': False,
            'filter': None,
            'qkey': None,
            'date_range': None,
        }

        # Проверим диапозон дат, включающий нерелевантные даты и даты,
        # выходящие за диапозон date_range
        for index, qk in enumerate(user_price_actor.expand_qkey_date_range(
            qkey=qkey, date_range=date_range + 1, only_relevant=False
        )):
            actual = run(user_price_actor.subscribed_on_direction(
                email='vasya@pupkin.ru',
                credentials=(('session', '123456789abc'),),
                qkey=qk
            ))
            date_forward = structure_from_qkey(qk).date_forward
            if datetime.now() <= date_forward or date_range < index:
                assert actual == expected_subscribed
            else:
                assert actual == expected_nonsubscribed

            with_unknown_auth = run(user_price_actor.subscribed_on_direction(
                email='vasya@pupkin.ru',
                credentials=(('session', '123456789abc_unknown'),),
                qkey=qk
            ))
            assert with_unknown_auth == expected_nonsubscribed


def test_subscribed_on_direction_get_last_updated(
    user_price_actor, session, TravelVertical,
    UserAuthType, qkey_factory, func_now_patcher,
    approve_all_users
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    UserAuthType(session).get_or_create(name='cookie')
    date_range = 3
    filter_1 = Filter(filter_url_postfix='url1')
    filter_2 = Filter(filter_url_postfix='url2')
    qkey = qkey_factory(date_forward=datetime(2020, 12, 12))
    qkey_params = asdict(structure_from_qkey(qkey))
    qkey_params.pop('date_forward')
    with func_now_patcher('2020-12-12'):
        run(user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qid=None,
            qkey=qkey,
            filter_=filter_1,
            date_range=date_range
        ))
        # Подтвердим пользователей
        approve_all_users()
        run(user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('cookie', 'cookie_val'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qid=None,
            qkey=qkey,
            filter_=filter_2,
            date_range=date_range
        ))
        # Последнея обновленная подписка среди всех
        # подтвержденных пользователей - первая
        actual = run(user_price_actor.subscribed_on_direction(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'), ('cookie', 'cookie_val')),
            qkey=qkey
        ))
        assert actual['filter'] == filter_1.filter_url_postfix
        # Подтвердим последнего второго
        approve_all_users()
        # Теперь Последнея обновленная подписка - у второго
        actual = run(user_price_actor.subscribed_on_direction(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'), ('cookie', 'cookie_val')),
            qkey=qkey
        ))
        assert actual['filter'] == filter_2.filter_url_postfix


def test_subscribed_on_direction_without_date_backward_in_query(
    user_price_actor, session, TravelVertical,
    UserAuthType, qkey_factory, func_now_patcher,
    approve_all_users
):
    # Отрицательный тест на проверку работы метода без date_backward
    # в запросе: искомый qkey  не содержит date_backward
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey_1 = qkey_factory(date_forward=datetime(2020, 12, 12))
    qkey_params = asdict(structure_from_qkey(qkey_1))
    qkey_2 = qkey_factory(
        set_date_backward_none=True,
        **qkey_params
    )
    with func_now_patcher('2020-12-12'):
        run(user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey_1,
        ))
        # Подтвердим пользователей
        approve_all_users()
        # искомый qkey не содержит date_backward,
        # поэтому считаем, что не подписан
        actual = run(user_price_actor.subscribed_on_direction(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            qkey=qkey_2
        ))
        assert not actual['subscribed']


def test_subscribed_on_direction_without_date_backward_in_db(
    user_price_actor, session, TravelVertical,
    UserAuthType, qkey_factory, func_now_patcher,
    approve_all_users
):
    # Отрицательный тест на проверку работы метода без date_backward
    # в базе данных: оригинальный qkey  не содержит date_backward
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey_1 = qkey_factory(date_forward=datetime(2020, 12, 12))
    qkey_params = asdict(structure_from_qkey(qkey_1))
    qkey_2 = qkey_factory(
        set_date_backward_none=True,
        **qkey_params
    )
    with func_now_patcher('2020-12-12'):
        run(user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey_2,
        ))
        approve_all_users()
        # теперь оригинальный qkey не содержит date_backward,
        # поэтому считаем, что не подписан
        actual = run(user_price_actor.subscribed_on_direction(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            qkey=qkey_1
        ))
        assert not actual['subscribed']


def test_subscribed_on_direction_date_backward_must_have_same_timedelta(
    user_price_actor, session, TravelVertical,
    UserAuthType, qkey_factory, func_now_patcher,
    approve_all_users
):
    # Отрицательный тест на проверку корректности date_backward
    # в случае, если находится в диапозоне date_range, но
    # timedelta от оригинала не та же, что у date_forward
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    date_range = 3
    qkey_1 = qkey_factory(date_forward=datetime(2020, 12, 12))
    qkey_params = asdict(structure_from_qkey(qkey_1))
    date_forward = qkey_params.pop('date_forward') + timedelta(1)
    date_backward = qkey_params.pop('date_backward') + timedelta(2)
    qkey_2 = qkey_factory(
        date_forward=date_forward,
        date_backward=date_backward,
        **qkey_params
    )
    with func_now_patcher('2020-12-12'):
        run(user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey_1,
            date_range=date_range
        ))
        approve_all_users()
        actual = run(user_price_actor.subscribed_on_direction(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            qkey=qkey_2
        ))
        assert not actual['subscribed']


@freeze_time("2020-12-13")
def test_get(
    TravelVertical, UserAuthType, approve_all_users,
    session, user_price_actor, qkey_factory
):
    TravelVertical(session).get_or_create(name='avia')
    TravelVertical(session).get_or_create(name='train')
    UserAuthType(session).get_or_create(name='session')
    qkey1 = 'c1_c2_2020-12-13_None_economy_1_0_0_com'
    qkey2 = 'c1_c2_2020-12-13_None_business_1_0_0_ru'
    qkey3 = 'c1_c2_2020-12-13_2020-12-13_business_1_0_0_ru'

    run(
        user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey1
        )
    )
    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='WIZARD',
            travel_vertical_name='train',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey2
        )
    )
    run(
        user_price_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            source='WIZARD',
            travel_vertical_name='train',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey3
        )
    )
    approve_all_users()

    subscriptions = run(
        user_price_actor.get_subscriptions_list(
            credentials=(('session', '123456789abc'),),
        )
    )

    assert subscriptions == [
        {
            'email': 'vasya@pupkin.ru', 'subscription_code': qkey1,
            'national_version': 'com', 'language': 'en', 'source': 'VALERA',
            'name': '1-2, 13.12',
            'url': ('https://flights.yandex.com/search?fromBlock=emailAlert&fromId=c1'
                    '&toId=c2&when=2020-12-13&return_date=&adult_seats=1'
                    '&children_seats=0&infant_seats=0&klass=economy&lang=en'),
        }, {
            'email': 'vasya@pupkin.ru', 'subscription_code': qkey3,
            'national_version': 'ru', 'language': 'ru', 'source': 'WIZARD',
            'name': '1-2, 13.12',
            'url': ('https://avia.yandex.ru/search?fromBlock=emailAlert&fromId=c1'
                    '&toId=c2&when=2020-12-13&return_date=2020-12-13&adult_seats=1'
                    '&children_seats=0&infant_seats=0&klass=business&lang=ru'),
        }, {
            'email': 'mikhail@lomonosov.ru', 'subscription_code': qkey2,
            'national_version': 'ru', 'language': 'ru', 'source': 'WIZARD',
            'name': '1-2, 13.12',
            'url': ('https://avia.yandex.ru/search?fromBlock=emailAlert&fromId=c1'
                    '&toId=c2&when=2020-12-13&return_date=&adult_seats=1'
                    '&children_seats=0&infant_seats=0&klass=business&lang=ru'),
        }
    ]

    subscriptions = run(
        user_price_actor.get_subscriptions_list(
            credentials=(('session', '123456789abc'),),
            email='mikhail@lomonosov.ru'
        )
    )
    assert len(subscriptions) == 1
    assert subscriptions == [
        {
            'email': 'mikhail@lomonosov.ru', 'subscription_code': qkey2,
            'national_version': 'ru', 'language': 'ru', 'source': 'WIZARD',
            'name': '1-2, 13.12',
            'url': ('https://avia.yandex.ru/search?fromBlock=emailAlert&fromId=c1'
                    '&toId=c2&when=2020-12-13&return_date=&adult_seats=1'
                    '&children_seats=0&infant_seats=0&klass=business&lang=ru'),
        }
    ]

    subscriptions = run(
        user_price_actor.get_subscriptions_list(
            credentials=(('session', '123456789abc'),),
            email='unknown_email@kremlin.ru'
        )
    )
    assert len(subscriptions) == 0


@freeze_time("2020-12-13")
def test_get_with_many_credentials(
    TravelVertical, UserAuthType, approve_all_users,
    session, user_price_actor, qkey_factory
):
    TravelVertical(session).get_or_create(name='avia')
    TravelVertical(session).get_or_create(name='train')
    UserAuthType(session).get_or_create(name='session')
    UserAuthType(session).get_or_create(name='cookie')
    qkey = 'c1_c2_2020-12-13_None_economy_1_0_0_ru'

    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('cookie', 'cookie_val'),),
            source='WIZARD',
            travel_vertical_name='train',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    approve_all_users()

    actual = run(user_price_actor.get_subscriptions_list(
        credentials=(
            ('session', '123456789abc'),
            ('cookie', 'cookie_val_1'),
        ),
        email='mikhail@lomonosov.ru'
    ))
    assert actual == [{
        'email': 'mikhail@lomonosov.ru', 'subscription_code': qkey,
        'national_version': 'ru', 'language': 'ru', 'source': 'VALERA',
        'name': '1-2, 13.12',
        'url': ('https://avia.yandex.ru/search?fromBlock=emailAlert&fromId=c1'
                '&toId=c2&when=2020-12-13&return_date=&adult_seats=1'
                '&children_seats=0&infant_seats=0&klass=economy&lang=ru'),
    }]


def test_get_only_approved_and_non_deleted(
    User, TravelVertical, UserAuthType, approve_all_users,
    session, user_price_actor, qkey_factory
):
    TravelVertical(session).get_or_create(name='train')
    UserAuthType(session).get_or_create(name='session')
    qkey = qkey_factory(national_version='ru')

    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='WIZARD',
            travel_vertical_name='train',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )

    # Деактивируем пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=1),
        values=dict(approved_at=None)
    )
    subscriptions = run(user_price_actor.get_subscriptions_list(
        credentials=(('session', '123456789abc'),),
    ))
    assert len(subscriptions) == 0

    # Подтвердим и удалим пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=1),
        values=dict(
            deleted_at=datetime.utcnow(),
            approved_at=datetime.utcnow(),
        )
    )
    subscriptions = run(user_price_actor.get_subscriptions_list(
        credentials=(('session', '123456789abc'),),
    ))
    assert len(subscriptions) == 0


def test_delete(
    TravelVertical, UserAuthType, UserPriceChangeSubscription,
    approve_all_users, user_price_actor, session, qkey_factory,
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey_1 = qkey_factory()
    qkey_2 = qkey_factory()
    qkey_3 = qkey_factory()

    # Подписались на три разных направления
    for qkey in qkey_1, qkey_2, qkey_3:
        run(
            user_price_actor.put(
                email='mikhail@lomonosov.ru',
                credentials=(('session', '123456789abc'),),
                source='VALERA',
                travel_vertical_name='avia',
                language='ru',
                timezone='Asia/Yekaterinburg',
                name='Anatoly',
                qkey=qkey
            )
        )
    # Подтвердили всех пользователей
    approve_all_users()

    result = run(user_price_actor.delete(
        subscription_codes=[qkey_1, qkey_3],
        credentials=(('session', '123456789abc'),),
        email='mikhail@lomonosov.ru'
    ))
    assert result == {qkey_1, qkey_3}
    assert UserPriceChangeSubscription(session).get(id=1).deleted_at is not None
    # Только вторая подписка не была удалена
    assert UserPriceChangeSubscription(session).get(id=2).deleted_at is None
    assert UserPriceChangeSubscription(session).get(id=3).deleted_at is not None


def test_delete_with_no_access(
    TravelVertical, UserAuthType, approve_all_users,
    user_price_actor, qkey_factory, session
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey = qkey_factory()
    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    # Подтвердили всех пользователей
    approve_all_users()

    # не существующий тип credential
    with pytest.raises(NoAccess):
        run(user_price_actor.delete(
            subscription_codes=[qkey],
            credentials=(('unknown_auth_type', 'bad'),),
            email='mikhail@lomonosov.ru'
        ))

    # неверные аутенфикационные данные
    with pytest.raises(NoAccess):
        run(user_price_actor.delete(
            subscription_codes=[qkey],
            credentials=(('session', 'incorrect'),),
            email='mikhail@lomonosov.ru'
        ))


def test_delete_with_unknown_email(
    TravelVertical, UserAuthType, approve_all_users,
    user_price_actor, qkey_factory, session
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey = qkey_factory()
    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    # Подтвердили всех пользователей
    approve_all_users()

    with pytest.raises(EmailNotFound):
        run(user_price_actor.delete(
            subscription_codes=[qkey],
            credentials=(('session', '123456789abc'),),
            email='incorrect@mail.ru'
        ))


def test_delete_with_passport_access(
    TravelVertical, UserAuthType, approve_all_users,
    user_price_actor, qkey_factory, session, blackbox
):
    # Зададим доступы в blackbox
    blackbox.emails['uid-1'] = ['mikhail@lomonosov.ru', 'some-yet@lomonosov.ru']
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey = qkey_factory()
    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    # Подтвердили всех пользователей
    approve_all_users()
    # passportId  - зарезервированный credential
    result = run(user_price_actor.delete(
        subscription_codes=[qkey],
        credentials=(('passportId', 'uid-1'),),
        email='mikhail@lomonosov.ru'
    ))
    assert result == {qkey}


def test_put_after_delete(
    TravelVertical, UserAuthType, approve_all_users,
    user_price_actor, qkey_factory, session
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    UserAuthType(session).get_or_create(name='token')
    qkey = qkey_factory()
    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    # Подтвердили всех пользователей
    approve_all_users()
    run(user_price_actor.delete(
        subscription_codes=[qkey],
        credentials=(('session', '123456789abc'),),
        email='mikhail@lomonosov.ru'
    ))
    result = run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    assert len(result[UpsertAction.UPDATE]) == 1
    assert len(result[UpsertAction.INSERT]) == 0


def test_delete_only_approved_and_non_deleted_on_authenticate_step(
    TravelVertical, User, UserAuthType,
    user_price_actor, qkey_factory, session
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    qkey = qkey_factory()
    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            source='VALERA',
            travel_vertical_name='avia',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    # Сейчас пользователь mikhail@lomonosov.ru не подтвержден
    # Поэтому этот пользователь сейчас не доступен
    with pytest.raises(NoAccess):
        run(user_price_actor.delete(
            subscription_codes=[qkey],
            credentials=(('session', '123456789abc'),),
            email='mikhail@lomonosov.ru'
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
        run(user_price_actor.delete(
            subscription_codes=[qkey],
            credentials=(('session', '123456789abc'),),
            email='mikhail@lomonosov.ru'
        ))


def test_delete_only_approved_and_non_deleted_on_deletion_step(
    TravelVertical, UserAuthType, User, UserPriceChangeSubscription,
    user_price_actor, qkey_factory, session
):
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    UserAuthType(session).get_or_create(name='cookie')
    qkey = qkey_factory()
    run(
        user_price_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(
                ('session', '123456789abc'),
                ('cookie', 'cookieVal'),
            ),
            source='VALERA',
            travel_vertical_name='avia',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
            qkey=qkey
        )
    )
    # Подтвердили только 2-го пользователя
    User(session).upsert(
        where=dict(id=2),
        values=dict(
            approved_at=datetime.now()
        )
    )
    # Воспользуемся только подтвержденным пользователем
    run(user_price_actor.delete(
        subscription_codes=[qkey],
        credentials=(('cookie', 'cookieVal'),),
        email='mikhail@lomonosov.ru'
    ))
    assert UserPriceChangeSubscription(session).get(id=1).deleted_at is None
    assert UserPriceChangeSubscription(session).get(id=2).deleted_at is not None

    # Подтвердим и удалим 1-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=1),
        values=dict(
            deleted_at=datetime.utcnow(),
            approved_at=datetime.utcnow(),
        )
    )
    # Воспользуемся только подтвержденным пользователем
    run(user_price_actor.delete(
        subscription_codes=[qkey],
        credentials=(('cookie', 'cookieVal'),),
        email='mikhail@lomonosov.ru'
    ))
    # Все еще не получилось удалить 1-го пользователя
    assert UserPriceChangeSubscription(session).get(id=1).deleted_at is None
    assert UserPriceChangeSubscription(session).get(id=2).deleted_at is not None


@freeze_time("2020-12-13")
def test_expand_qkey_date_range_only_relevant(user_price_actor):
    date_range = 3
    qkey_struct = qkey_from_params(
        point_from_key='c1',
        point_to_key='c2',
        date_forward='2020-12-12',
        date_backward=None,
        klass='economy',
        adults=1,
        infants=0,
        children=0,
        national_version='ru'
    )
    qkeys = user_price_actor.expand_qkey_date_range(
        qkey=qkey_struct, date_range=date_range, only_relevant=True
    )

    assert list(qkeys) == [
        'c1_c2_2020-12-13_None_economy_1_0_0_ru',
        'c1_c2_2020-12-14_None_economy_1_0_0_ru',
    ]


@freeze_time("2020-12-13")
def test_expand_qkey_date_range_with_not_relevant(user_price_actor):
    date_range = 3
    qkey = qkey_from_params(
        point_from_key='c1',
        point_to_key='c2',
        date_forward='2020-12-12',
        date_backward=None,
        klass='economy',
        adults=1,
        infants=0,
        children=0,
        national_version='ru'
    )
    qkeys = user_price_actor.expand_qkey_date_range(
        qkey=qkey, date_range=date_range, only_relevant=False
    )

    assert list(qkeys) == [
        qkey,
        'c1_c2_2020-12-13_None_economy_1_0_0_ru',
        'c1_c2_2020-12-14_None_economy_1_0_0_ru',
    ]


@pytest.fixture()
def func_now_patcher():
    @contextmanager
    def patch_func_now(time_to_freeze):
        with freeze_time(time_to_freeze, tick=False) as frozen_time:
            def set_timestamp(mapper, connection, target):
                now = datetime.now()
                target.created_at = now

            event.listen(PriceChangeSubscription, 'before_insert', set_timestamp)
            yield frozen_time
            event.remove(PriceChangeSubscription, 'before_insert', set_timestamp)

    return patch_func_now


def assert_price_change_subscription_has_qkey(price_change_subscription, qkey):
    assert price_change_subscription.qkey == qkey

    qkey_struct = structure_from_qkey(qkey)
    for field, value in asdict(qkey_struct).items():
        assert getattr(price_change_subscription, field) == value
