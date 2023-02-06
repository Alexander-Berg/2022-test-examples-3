from base64 import b64decode
from copy import deepcopy
from dataclasses import asdict
from tractor.crypto.versioned import VersionedKeyStorage
from tractor_api.methods.mail.impl.create_migration import (
    create_migration_impl,
    _convert_csv_to_users_info_list,
    SkipReason,
    make_response,
)
from tractor.crypto.fernet import Fernet
from tractor.mail.models import (
    TaskType,
    UserInfo,
    UserMigration,
    UserMigrationStatus,
    MailServerConnectionInfo,
    MailServerInfo,
)
from tractor.tests.fixtures.common import (
    DOMAIN,
    EMAIL,
    GOOGLE_PROVIDER,
    KEY_VERSIONS,
    LOGIN,
    ORG_ID,
    UID as USER_UID,
)
from tractor.util.dataclasses import construct_from_dict
from unittest.mock import MagicMock, ANY
import json
import pytest


USER_INFO_FROM_CSV = {
    "login": "test_login",
    "yandex_password": "yandex_password",
    "external_password": "external_password",
    "first_name": "name",
    "middle_name": "middle_name",
    "last_name": "surname",
    "gender": "male",
    "birthday": "11.11.1111",
    "language": "en",
}

ADMIN_UID = "admin_test_uid"


@pytest.fixture
def user_info_from_csv():
    return construct_from_dict(UserInfo, USER_INFO_FROM_CSV)


@pytest.fixture
def google_provider():
    return MailServerInfo(
        provider=GOOGLE_PROVIDER, conn_info=None
    )


@pytest.fixture
def custom_provider():
    return MailServerInfo(
        provider="custom", conn_info=MailServerConnectionInfo("http://custom.com", 665, True)
    )


@pytest.fixture
def env():
    env = {
        "db": MagicMock(),
        "yandex_directory": MagicMock(),
        "fernet": Fernet(VersionedKeyStorage(KEY_VERSIONS)),
    }
    env["db"].get_user_migration.return_value = None
    env["yandex_directory"].get_users.return_value = []
    env["yandex_directory"].get_domain.return_value = DOMAIN
    return env


def test_create_new_migration(env, user_info_from_csv, google_provider):
    skipped_logins = create_migration_impl(
        ORG_ID, ADMIN_UID, google_provider, [deepcopy(user_info_from_csv)], env
    )

    assert skipped_logins == {}
    env["db"].create_task.assert_called_once()
    check_create_task_args(
        env["db"].create_task.call_args.kwargs,
        TaskType.PREPARE,
        ORG_ID,
        DOMAIN,
        env["fernet"],
        user_info_asdict(user_info_from_csv),
        mail_server_info_asdict(google_provider),
        ADMIN_UID
    )
    env["db"].create_user_migration.assert_called_with(
        ORG_ID, DOMAIN, user_info_from_csv.login, ANY, ANY
    )


def test_create_migration_for_custom_provider(custom_provider, user_info_from_csv, env):
    create_migration_impl(ORG_ID, ADMIN_UID, custom_provider, [user_info_from_csv], env)

    check_create_task_args(
        env["db"].create_task.call_args.kwargs,
        TaskType.PREPARE,
        ORG_ID,
        DOMAIN,
        env["fernet"],
        ANY,
        mail_server_info_asdict(custom_provider),
        ANY,
    )


def test_create_migration_for_existing_user(google_provider, user_info_from_csv, env):
    env["yandex_directory"].get_users.return_value = [{"uid": USER_UID, "email": EMAIL}]
    all_class_fields_except_login = list(
        filter(lambda attr: attr != "login", list(user_info_from_csv.__dict__.keys()))
    )
    for attr in all_class_fields_except_login:
        user_info_from_csv.__dict__[attr] = None

    create_migration_impl(ORG_ID, ADMIN_UID, google_provider, [deepcopy(user_info_from_csv)], env)

    user_info_from_csv.uid = USER_UID

    check_create_task_args(
        env["db"].create_task.call_args.kwargs,
        TaskType.PREPARE,
        ORG_ID,
        DOMAIN,
        env["fernet"],
        user_info_asdict(user_info_from_csv),
        ANY,
        ANY,
    )


def test_create_migration_with_domain_in_logins(env, google_provider, user_info_from_csv):
    formatted_login = user_info_from_csv.login
    user_info_from_csv.login = f"{user_info_from_csv.login}@gmail.com"

    create_migration_impl(ORG_ID, ADMIN_UID, google_provider, [user_info_from_csv], env)

    env["db"].create_user_migration.assert_called_with(ORG_ID, DOMAIN, formatted_login, ANY, ANY)


def test_update_finished_migration(env, user_info_from_csv, google_provider):
    env["db"].get_user_migration.return_value = UserMigration(
        org_id=ORG_ID,
        domain=DOMAIN,
        login=LOGIN,
        status=UserMigrationStatus.STOPPED,
        error_reason="",
        prepare_task_id=None,
        stop_task_id=None,
        stats=None,
    )

    create_migration_impl(ORG_ID, ADMIN_UID, google_provider, [user_info_from_csv], env)

    env["db"].create_task.assert_called_once()
    env["db"].reset_user_migration.assert_called_with(ORG_ID, LOGIN, ANY, ANY)


def test_do_nothing_with_running_migration(env, user_info_from_csv, google_provider):
    env["db"].get_user_migration.return_value = UserMigration(
        org_id=ORG_ID,
        domain=DOMAIN,
        login=LOGIN,
        status=UserMigrationStatus.SYNC_NEWEST,
        error_reason="",
        prepare_task_id=None,
        stop_task_id=None,
        stats=None,
    )

    skipped_logins = create_migration_impl(
        ORG_ID, ADMIN_UID, google_provider, [user_info_from_csv], env
    )

    assert skipped_logins == {"test_login": SkipReason.MIGRATION_ALREADY_IN_PROGRESS}
    env["db"].create_task.assert_not_called()
    env["db"].create_user_migration.assert_not_called()
    env["db"].reset_user_migration.assert_not_called()


def test_parse_request_data():
    data = csv_data_from_line_list(
        [",".join(USER_INFO_FROM_CSV.keys()), ",".join(USER_INFO_FROM_CSV.values())]
    )
    logins = [construct_from_dict(UserInfo, USER_INFO_FROM_CSV)]

    assert _convert_csv_to_users_info_list(data) == logins


def test_parse_rempty_request_data():
    data = csv_data_from_line_list([",".join(USER_INFO_FROM_CSV.keys())])
    logins = []

    assert _convert_csv_to_users_info_list(data) == logins


def test_make_empty_response():
    skipped_logins = {}
    response = {"skipped_logins": []}

    assert response == make_response(skipped_logins)


def test_make_response():
    skipped_logins = {LOGIN: SkipReason.MIGRATION_ALREADY_IN_PROGRESS}
    response = {
        "skipped_logins": [{"login": LOGIN, "reason": SkipReason.MIGRATION_ALREADY_IN_PROGRESS}]
    }

    assert response == make_response(skipped_logins)

def test_incorrect_data_in_scv(google_provider, user_info_from_csv, env):
    user_info_from_csv.birthday = "incorrect"
    skipped_logins = create_migration_impl(ORG_ID, ADMIN_UID, google_provider, [user_info_from_csv], env)
    assert skipped_logins == {LOGIN: SkipReason.WRONG_DATA_FORMAT_IN_CSV}



def mail_server_info_asdict(mail_server_info: MailServerInfo):
    mail_server_info_dct = {
        "provider": mail_server_info.provider,
    }
    if mail_server_info.provider == "custom":
        mail_server_info_dct.update(asdict(mail_server_info.conn_info))
    return mail_server_info_dct


def user_info_asdict(user_info: UserInfo):
    user_info_dct = {}
    for field in user_info.__dict__.keys():
        if getattr(user_info, field) is not None:
            user_info_dct[field] = getattr(user_info, field)
    return user_info_dct


def check_create_task_args(
    call_args,
    task_type,
    org_id,
    domain,
    fernet: Fernet,
    user_info_dct,
    proivider_dct,
    admin_uid,
):
    worker_input_from_call = json.loads(call_args["worker_input"])
    user_info_from_call = worker_input_from_call["user"]

    assert call_args["type"] == task_type
    assert call_args["org_id"] == org_id
    assert call_args["domain"] == domain

    assert worker_input_from_call["admin_uid"] == admin_uid

    if type(proivider_dct) != type(ANY):
        assert worker_input_from_call["mail_server_info"] == proivider_dct

    if type(user_info_dct) == type(ANY):
        return

    if user_info_dct.get("yandex_password") is not None:
        encrypted_yandex_password_from_call = user_info_from_call["encrypted_yandex_password"]
        decrypted_yandex_password_from_call = decript_password(
            fernet, encrypted_yandex_password_from_call
        )
        assert user_info_dct.get("yandex_password") == decrypted_yandex_password_from_call
        del user_info_dct["yandex_password"]
        del user_info_from_call["encrypted_yandex_password"]

    if user_info_dct.get("external_password") is not None:
        encrypted_external_password_from_call = user_info_from_call["encrypted_external_password"]
        decrypted_external_password_from_call = decript_password(
            fernet, encrypted_external_password_from_call
        )
        assert user_info_dct.get("external_password") == decrypted_external_password_from_call
        del user_info_dct["external_password"]
        del user_info_from_call["encrypted_external_password"]

    assert user_info_from_call == user_info_dct


def csv_data_from_line_list(lines):
    return "\n".join(lines)


def decript_password(fernet: Fernet, encrepted_password: str) -> str:
    return fernet.decrypt_text(b64decode(encrepted_password))
