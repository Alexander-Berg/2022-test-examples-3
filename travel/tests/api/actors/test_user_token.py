import datetime
import pytest

from asyncio import run


@pytest.mark.usefixtures('fill_db')
def test_get(user_token_actor, user_promo_actor, approve_all_users, Email, session):
    expected = [
        ('mikhail@lomonosov.ru', 'Anatoly', 'token-0'),
        ('yet_other@yet_email.ru', 'Vitaly', 'token-1'),
    ]
    result = run(to_list(user_token_actor.get()))

    assert result == expected

    run(user_promo_actor.put(
        email='my_best@ymail.ru',
        credentials=(('cookie', 'cookie_val'),),
        promo_subscription_code='yet_other_promo',
        source='WIZARD',
        travel_vertical_name='train',
        national_version='com',
        language='en',
        timezone='Asia/Yekaterinburg',
        name='Abel',
    ))
    approve_all_users()

    result = run(to_list(user_token_actor.get()))
    expected.insert(1, ('my_best@ymail.ru', 'Abel', 'token-2'))

    assert result == expected


@pytest.mark.usefixtures('fill_db')
def test_get_only_approved_and_non_deleted(
    user_token_actor, user_promo_actor, User, session
):
    # Деактивируем всех пользователей почты mikhail@lomonosov.ru
    for i in 1, 2:
        User(session).upsert(
            where=dict(id=i),
            values=dict(approved_at=None)
        )

    expected = [
        ('yet_other@yet_email.ru', 'Vitaly', 'token-0'),
    ]
    result = run(to_list(user_token_actor.get()))
    assert result == expected

    # Деактивируем всех пользователей почты yet_other@yet_email.ru
    User(session).upsert(
        where=dict(id=3),
        values=dict(deleted_at=datetime.datetime.utcnow())
    )
    result = run(to_list(user_token_actor.get()))
    assert result == []


@pytest.mark.usefixtures('fill_db')
def test_get_token_or_create(Email, user_token_actor, session):
    email_obj = Email(session).get(email='mikhail@lomonosov.ru')
    actual = run(user_token_actor.get_token_or_create(session, email_id=email_obj.id))
    expected = ('mikhail@lomonosov.ru', 'Anatoly', 'token-0')
    assert actual == expected

    # Не должен создаться новый токен
    actual = run(user_token_actor.get_token_or_create(session, email_id=email_obj.id))
    assert actual == expected


async def to_list(async_gen):
    return sorted([e async for e in async_gen])
