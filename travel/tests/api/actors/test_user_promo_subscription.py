import datetime
import pytest

from asyncio import run

from travel.avia.subscriptions.app.model.storage import UpsertAction
from travel.avia.subscriptions.app.api.consts import PASSPORT_AUTH_TYPE, TOKEN_AUTH_TYPE
from travel.avia.subscriptions.app.api.exceptions import NoAccess, EmailNotFound


def test_put(PromoSubscription, TravelVertical, UserAuthType, User, session, user_promo_actor):
    promo = PromoSubscription(session).create(code='travel_news', national_version='com', language='en')
    travel_vertical = TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    result = run(
        user_promo_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            promo_subscription_code='travel_news',
            source='VALERA',
            travel_vertical_name='avia',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )

    created = result[UpsertAction.INSERT]
    updated = result[UpsertAction.UPDATE]
    assert len(created) == 1
    assert len(updated) == 0
    assert created[0].user_id == 1
    assert created[0].promo_subscription_id == promo.id
    assert created[0].travel_vertical_id == travel_vertical.id
    assert created[0].source == 'VALERA'

    # Нужно подтвердить пользователя в дальнейшем
    assert User(session).get(id=created[0].user_id).approved_at is None
    assert User(session).get(id=created[0].user_id).name == 'Anatoly'


def test_put_same_subscription(PromoSubscription, TravelVertical, UserAuthType, session, user_promo_actor):
    promo = PromoSubscription(session).create(code='travel_news', national_version='com', language='en')
    travel_vertical = TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    result = run(
        user_promo_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            promo_subscription_code='travel_news',
            source='VALERA',
            travel_vertical_name='avia',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )
    assert result[UpsertAction.INSERT][0].updated_at is None
    result = run(
        user_promo_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            promo_subscription_code='travel_news',
            source='VALERA2',
            travel_vertical_name='avia',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )

    created = result[UpsertAction.INSERT]
    updated = result[UpsertAction.UPDATE]
    assert len(created) == 0
    assert len(updated) == 1
    assert updated[0].updated_at is not None
    assert updated[0].user_id == 1
    assert updated[0].promo_subscription_id == promo.id
    assert updated[0].travel_vertical_id == travel_vertical.id
    assert updated[0].source == 'VALERA2'


def test_change_user_name_on_update(PromoSubscription, TravelVertical, UserAuthType, User, session, user_promo_actor):
    PromoSubscription(session).create(code='travel_news', national_version='com', language='en')
    TravelVertical(session).get_or_create(name='avia')
    UserAuthType(session).get_or_create(name='session')
    result = run(
        user_promo_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            promo_subscription_code='travel_news',
            source='VALERA',
            travel_vertical_name='avia',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )

    created = result[UpsertAction.INSERT]
    assert User(session).get(id=created[0].user_id).name == 'Anatoly'
    result = run(
        user_promo_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            promo_subscription_code='travel_news',
            source='VALERA2',
            travel_vertical_name='avia',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Vasily',
        )
    )
    updated = result[UpsertAction.UPDATE]
    assert User(session).get(id=updated[0].user_id).name == 'Vasily'


def test_get(
    PromoSubscription, TravelVertical, UserAuthType, approve_all_users,
    session, user_promo_actor,
):
    PromoSubscription(session).create(code='travel_news', national_version='com', language='en')
    PromoSubscription(session).create(code='travel_news', national_version='ru', language='ru')
    TravelVertical(session).get_or_create(name='avia')
    TravelVertical(session).get_or_create(name='train')
    UserAuthType(session).get_or_create(name='session')

    run(
        user_promo_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            promo_subscription_code='travel_news',
            source='VALERA',
            travel_vertical_name='avia',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',

        )
    )
    run(
        user_promo_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('session', '123456789abc'),),
            promo_subscription_code='travel_news',
            source='WIZARD',
            travel_vertical_name='train',
            national_version='ru',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )
    run(
        user_promo_actor.put(
            email='vasya@pupkin.ru',
            credentials=(('session', '123456789abc'),),
            promo_subscription_code='travel_news',
            source='WIZARD',
            travel_vertical_name='train',
            national_version='ru',
            language='ru',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',

        )
    )
    approve_all_users()

    subscriptions = run(
        user_promo_actor.get_subscriptions_list(
            credentials=(('session', '123456789abc'),),
        )
    )

    assert subscriptions == [
        {
            'email': 'vasya@pupkin.ru', 'subscription_code': 'travel_news',
            'national_version': 'com', 'language': 'en', 'source': 'VALERA',
            'name': '', 'url': '',
        }, {
            'email': 'vasya@pupkin.ru', 'subscription_code': 'travel_news',
            'national_version': 'ru', 'language': 'ru', 'source': 'WIZARD',
            'name': '', 'url': '',
        }, {
            'email': 'mikhail@lomonosov.ru', 'subscription_code': 'travel_news',
            'national_version': 'ru', 'language': 'ru', 'source': 'WIZARD',
            'name': '', 'url': '',
        }
    ]

    subscriptions = run(
        user_promo_actor.get_subscriptions_list(
            credentials=(('session', '123456789abc'),),
            email='mikhail@lomonosov.ru'
        )
    )
    assert len(subscriptions) == 1
    assert subscriptions == [
        {
            'email': 'mikhail@lomonosov.ru', 'subscription_code': 'travel_news',
            'national_version': 'ru', 'language': 'ru', 'source': 'WIZARD',
            'name': '', 'url': '',
        }
    ]

    subscriptions = run(
        user_promo_actor.get_subscriptions_list(
            credentials=(('session', '123456789abc'),),
            email='unknown_email@kremlin.ru'
        )
    )

    assert len(subscriptions) == 0


@pytest.mark.usefixtures('fill_db')
def test_get_with_many_credentials(user_promo_actor):
    # К почте mikhail@lomonosov.ru для доступов ниже привязано
    # 6 промо-подписок, две из которых - тоже самое, поэтому
    # всего на два доступа ниже должно приходится 5 уникальных
    # подписок
    actual = run(user_promo_actor.get_subscriptions_list(
        credentials=(
            ('session', '123456789abc'),
            ('cookie', 'cookie_val_1'),
        ),
        email='mikhail@lomonosov.ru'
    ))
    assert len(actual) == 5


@pytest.mark.usefixtures('fill_db')
def test_get_only_approved_and_non_deleted(user_promo_actor, User, session):
    subscriptions = run(user_promo_actor.get_subscriptions_list(
        credentials=(('session', '123456789abc'),),
    ))
    assert len(subscriptions) == 2

    # Деактивируем 1-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=1),
        values=dict(approved_at=None)
    )
    subscriptions = run(user_promo_actor.get_subscriptions_list(
        credentials=(('session', '123456789abc'),),
    ))
    assert len(subscriptions) == 0

    # Подтвердим и удалим 1-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=1),
        values=dict(
            deleted_at=datetime.datetime.utcnow(),
            approved_at=datetime.datetime.utcnow(),
        )
    )
    subscriptions = run(user_promo_actor.get_subscriptions_list(
        credentials=(('session', '123456789abc'),),
    ))
    assert len(subscriptions) == 0


@pytest.mark.usefixtures('fill_db')
def test_delete(UserPromoSubscription, session, user_promo_actor):
    result = run(user_promo_actor.delete(
        subscription_codes=('travel_news',),
        credentials=(('cookie', 'cookie_val_1'),),
        email='mikhail@lomonosov.ru'
    ))

    assert result == {'travel_news'}
    assert UserPromoSubscription(session).get(id=1).deleted_at is not None
    assert UserPromoSubscription(session).get(id=2).deleted_at is not None
    assert UserPromoSubscription(session).get(id=3).deleted_at is not None
    assert UserPromoSubscription(session).get(id=4).deleted_at is None
    assert UserPromoSubscription(session).get(id=5).deleted_at is None


@pytest.mark.usefixtures('fill_db')
def test_delete_with_no_access(user_promo_actor):
    # не существующий тип credential
    with pytest.raises(NoAccess):
        run(user_promo_actor.delete(
            subscription_codes=('travel_news',),
            credentials=(('unknown_auth_type', 'bad'),),
            email='mikhail@lomonosov.ru'
        ))

    # неверные аутенфикационные данные
    with pytest.raises(NoAccess):
        run(user_promo_actor.delete(
            subscription_codes=('travel_news',),
            credentials=(('session', 'incorrect'),),
            email='mikhail@lomonosov.ru'
        ))


def test_delete_with_unknown_email(user_promo_actor):
    with pytest.raises(EmailNotFound):
        run(user_promo_actor.delete(
            subscription_codes=('travel_news',),
            credentials=(('cookie', 'cookie_val'),),
            email='unknown@email.ru'
        ))


@pytest.mark.usefixtures('fill_db')
def test_delete_with_passport_access(
    PromoSubscription, TravelVertical, UserAuthType,
    session, user_promo_actor, blackbox
):
    blackbox.emails['uid-1'] = ['mikhail@lomonosov.ru', 'some-yet@lomonosov.ru']

    result = run(user_promo_actor.delete(
        subscription_codes=('travel_news',),
        credentials=((PASSPORT_AUTH_TYPE, 'uid-1'),),
        email='mikhail@lomonosov.ru'
    ))

    assert result == {'travel_news'}


@pytest.mark.usefixtures('fill_db')
def test_put_after_delete(UserPromoSubscription, session, user_promo_actor):
    run(user_promo_actor.delete(
        subscription_codes=('yet_other_promo',),
        credentials=(('cookie', 'cookie_val_1'),),
        email='mikhail@lomonosov.ru'
    ))

    assert UserPromoSubscription(session).get(id=5).deleted_at is not None

    result = run(
        user_promo_actor.put(
            email='mikhail@lomonosov.ru',
            credentials=(('cookie', 'cookie_val_1'),),
            promo_subscription_code='yet_other_promo',
            source='WIZARD',
            travel_vertical_name='train',
            national_version='com',
            language='en',
            timezone='Asia/Yekaterinburg',
            name='Anatoly',
        )
    )
    assert len(result[UpsertAction.UPDATE]) == 1
    assert len(result[UpsertAction.INSERT]) == 0
    assert UserPromoSubscription(session).get(id=5).deleted_at is None


@pytest.mark.usefixtures('fill_db')
def test_delete_only_approved_and_non_deleted_on_authenticate_step(User, user_promo_actor, session):
    # Деактивируем 2-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=2),
        values=dict(approved_at=None)
    )
    with pytest.raises(NoAccess):
        run(user_promo_actor.delete(
            subscription_codes=('travel_news',),
            credentials=(('cookie', 'cookie_val_1'),),
            email='mikhail@lomonosov.ru'
        ))

    # Подтвердим и удалим 2-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=2),
        values=dict(
            deleted_at=datetime.datetime.utcnow(),
            approved_at=datetime.datetime.utcnow(),
        )
    )
    with pytest.raises(NoAccess):
        run(user_promo_actor.delete(
            subscription_codes=('travel_news',),
            credentials=(('cookie', 'cookie_val_1'),),
            email='mikhail@lomonosov.ru'
        ))


@pytest.mark.usefixtures('fill_db')
def test_delete_only_approved_and_non_deleted_on_deletion_step(
    User, UserPromoSubscription, user_promo_actor, session
):
    def assert_result():
        promo_subscriptions = run(user_promo_actor.delete(
            subscription_codes=('travel_news',),
            credentials=(('session', '123456789abc'),),
            email='mikhail@lomonosov.ru'
        ))

        assert promo_subscriptions == {'travel_news'}
        assert UserPromoSubscription(session).get(id=1).deleted_at is not None
        assert UserPromoSubscription(session).get(id=2).deleted_at is not None
        assert UserPromoSubscription(session).get(id=3).deleted_at is None

    # Деактивируем 2-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=2),
        values=dict(approved_at=None)
    )

    # Но получим доступ через 1-го пользователя почты mikhail@lomonosov.ru
    # и убедимся, что подписки удаленного или неподтвержденного пользователя
    # недоступны
    assert_result()

    # Подтвердим и удалим 2-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=2),
        values=dict(
            deleted_at=datetime.datetime.utcnow(),
            approved_at=datetime.datetime.utcnow(),
        )
    )
    # То же самое, но для удаленного пользователя
    assert_result()


@pytest.mark.usefixtures('fill_db')
def test_get_with_token_and_email(user_promo_actor, user_token_actor, all_credentials):
    # Доступ по токену становится возможен только после выгрузки
    _run_async_generator(user_token_actor.get())

    actual = run(user_promo_actor.get_subscriptions_list(
        credentials=((TOKEN_AUTH_TYPE, 'token-0'),),
        email='mikhail@lomonosov.ru'
    ))
    expected = run(user_promo_actor.get_subscriptions_list(
        credentials=all_credentials,
        email='mikhail@lomonosov.ru',
    ))
    assert_dict_lists_equal(actual, expected)


@pytest.mark.usefixtures('fill_db')
def test_get_with_token_without_email(user_promo_actor, user_token_actor, all_credentials):
    # Доступ по токену становится возможен только после выгрузки
    _run_async_generator(user_token_actor.get())

    actual = run(user_promo_actor.get_subscriptions_list(
        credentials=((TOKEN_AUTH_TYPE, 'token-0'),),
    ))
    expected = run(user_promo_actor.get_subscriptions_list(
        credentials=all_credentials,
        email='mikhail@lomonosov.ru',
    ))
    assert_dict_lists_equal(actual, expected)


@pytest.mark.usefixtures('fill_db')
def test_get_with_token_only_approved_and_non_deleted(
    user_promo_actor, user_token_actor, User,
    session, all_credentials
):
    # Доступ по токену становится возможен только после выгрузки
    _run_async_generator(user_token_actor.get())
    # Деактивируем 1-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=1),
        values=dict(approved_at=None)
    )
    actual = run(user_promo_actor.get_subscriptions_list(
        credentials=((TOKEN_AUTH_TYPE, 'token-0'),),
    ))
    expected = run(user_promo_actor.get_subscriptions_list(
        credentials=all_credentials,
        email='mikhail@lomonosov.ru',
    ))
    assert_dict_lists_equal(actual, expected)

    # Подтвердим и удалим 1-го пользователя почты mikhail@lomonosov.ru
    User(session).upsert(
        where=dict(id=1),
        values=dict(
            deleted_at=datetime.datetime.utcnow(),
            approved_at=datetime.datetime.utcnow(),
        )
    )
    actual = run(user_promo_actor.get_subscriptions_list(
        credentials=((TOKEN_AUTH_TYPE, 'token-0'),),
    ))
    expected = run(user_promo_actor.get_subscriptions_list(
        credentials=all_credentials,
        email='mikhail@lomonosov.ru',
    ))
    assert_dict_lists_equal(actual, expected)


@pytest.mark.usefixtures('fill_db')
def test_get_with_wrong_token(user_promo_actor, user_token_actor):
    # Доступ по токену становится возможен только после выгрузки
    _run_async_generator(user_token_actor.get())

    actual = run(user_promo_actor.get_subscriptions_list(
        credentials=((TOKEN_AUTH_TYPE, 'token-1'),),
        email='mikhail@lomonosov.ru'
    ))

    assert actual == []


def assert_dict_lists_equal(dict_list_1, dict_list_2, check_non_empty=True):
    def transform(dict_list):
        return sorted(tuple(
            tuple(sorted(d.items())) for d in dict_list
        ))

    if check_non_empty:
        assert len(dict_list_1) != 0
        assert len(dict_list_2) != 0

    assert transform(dict_list_1) == transform(dict_list_2)


def _run_async_generator(async_gen):
    async def _iterate():
        async for _ in async_gen:  # noqa
            pass

    run(_iterate())
