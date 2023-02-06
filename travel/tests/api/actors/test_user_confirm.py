import datetime
import pytest

from asyncio import run
from urllib.parse import parse_qs, urlparse

from travel.avia.subscriptions.app.api.consts import PASSPORT_AUTH_TYPE, TOKEN_AUTH_TYPE
from travel.avia.subscriptions.app.api.interactor.user_confirm import (
    InvalidApprovingToken, UserAlreadyApproved
)
from travel.avia.subscriptions.app.settings.app import frontend_hosts


def test_try_approve_with_passport(
    UserAuthType, user_confirm_actor, session, blackbox,
    mailbox, user_register,
):
    # Нужно добавить доступы через токен, так как токен нужен для письма 1opt-in
    UserAuthType(session).create(name=TOKEN_AUTH_TYPE)
    expected_mail = 'mikhail@lomonosov.ru'
    expected_nv = 'com'
    # Добавим в блэкбоксе пользователя с uid=uid-1
    blackbox.emails['uid-1'] = [expected_mail]

    # Добавим пользователя с доступом через пасспорт
    user, email_obj = user_register(
        mail=expected_mail,
        auth_type=PASSPORT_AUTH_TYPE,
        auth_value='uid-1'
    )
    assert user.approved_at is None

    run(user_confirm_actor.try_approve(
        session=session,
        user=user,
        email_obj=email_obj,
        auth_type=PASSPORT_AUTH_TYPE,
        auth_value='uid-1',
        national_version=expected_nv
    ))

    assert user.approved_at is not None
    assert_single_opt_in_mail(
        mailbox[-1], 'token-0', expected_mail, expected_nv
    )


def test_try_approve(
    session, user_confirm_actor, mailbox, user_register
):
    expected_mail = 'mikhail@lomonosov.ru'
    expected_nv = 'com'
    user, email_obj = user_register(
        mail=expected_mail,
        auth_type='session',
        auth_value='123'
    )
    assert user.approved_at is None

    run(user_confirm_actor.try_approve(
        session=session,
        user=user,
        email_obj=email_obj,
        auth_type='session',
        auth_value='123',
        national_version=expected_nv
    ))

    assert user.approved_at is None
    assert_double_opt_in_mail(
        user, mailbox[-1], expected_mail, expected_nv
    )


def test_confirm(
    UserAuthType, user_confirm_actor, user_register,
    session, mailbox
):
    # Нужно добавить доступы через токен, так как токен нужен для письма 1opt-in
    UserAuthType(session).create(name=TOKEN_AUTH_TYPE)
    expected_mail = 'mikhail@lomonosov.ru'
    expected_nv = 'com'
    user, email_obj = user_register(
        mail=expected_mail,
        auth_type='session',
        auth_value='123'
    )

    run(user_confirm_actor.try_approve(
        session=session,
        user=user,
        email_obj=email_obj,
        auth_type='session',
        auth_value='123',
        national_version=expected_nv
    ))
    assert user.approved_at is None

    run(user_confirm_actor.confirm(
        approving_token=user.approving_token,
        national_version=expected_nv
    ))

    assert user.approved_at is not None
    assert len(mailbox) == 2
    assert_single_opt_in_mail(
        mailbox[-1], 'token-0', expected_mail, expected_nv
    )


def test_confirm_with_wrong_approving_token(user_confirm_actor):
    with pytest.raises(InvalidApprovingToken):
        run(user_confirm_actor.confirm(
            approving_token='some-wrong-token',
            national_version='com'
        ))


def test_confirm_twice(user_register, user_confirm_actor):
    # Создаем уже подтвержденного пользователя
    user, email_obj = user_register(
        mail='some@mail.ru',
        auth_type='session',
        auth_value='123'
    )
    user.approved_at = datetime.datetime.utcnow()
    user.approving_token = 'token'

    with pytest.raises(UserAlreadyApproved):
        run(user_confirm_actor.confirm('token', 'com'))


@pytest.fixture()
def user_register(User, UserAuth, UserAuthType, Email, session):
    def register_user(mail, auth_type, auth_value):
        email_obj = Email(session).create(email=mail)
        user_auth_type = UserAuthType(session).create(name=auth_type)
        user_auth = UserAuth(session).create(
            user_auth_type_id=user_auth_type.id,
            auth_value=auth_value
        )
        user = User(session).create(email_id=email_obj.id, user_auth_id=user_auth.id)

        return user, email_obj

    return register_user


def assert_double_opt_in_mail(user, last_mail, expected_mail, expected_nv):
    assert last_mail[0] == expected_mail
    assert 'subscribe_link' in last_mail[1]
    parse_result = urlparse(last_mail[1]['subscribe_link'])
    assert frontend_hosts()[expected_nv].endswith(parse_result.hostname)
    assert parse_qs(parse_result.query)['approving_token'] == [user.approving_token]
    # TODO: добавить проверку url path, когда он точно станет известен


def assert_single_opt_in_mail(last_mail, token, expected_mail, expected_nv):
    assert last_mail[0] == expected_mail
    assert 'cabinet_link' in last_mail[1]
    parse_result = urlparse(last_mail[1]['cabinet_link'])
    assert frontend_hosts()[expected_nv].endswith(parse_result.hostname)
    assert parse_qs(parse_result.query)['token'] == [token]
    # TODO: добавить проверку url path, когда он точно станет известен
