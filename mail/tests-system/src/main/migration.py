import base64
import time
import pytest
from hamcrest import (
    assert_that,
    contains,
    has_entry,
    has_entries,
    has_length,
    has_property,
    equal_to,
)
from helpers.api import call_service_api, InternalApiException
from helpers.passport import get_access_token
from helpers import retry


pytest.skip(msg="Tests are broken. Skipped as migrations should be removed.", allow_module_level=True)


@pytest.fixture(autouse=True)
def clean_collectors(rpop_api, collectors_api):
    for src in [rpop_api, collectors_api]:
        collectors = src.list()["rpops"]
        for c in collectors:
            src.delete(c["popid"])
    time.sleep(10)  # time for iteration take step and remove collector completly


@retry()
def wait_for_migration_complete(rpop_api, popid):
    collectors = rpop_api.list()["rpops"]
    assert_that(
        collectors,
        contains(has_entries({"popid": popid, "is_on": "0", "error_status": "migrated"})),
    )


@retry()
def wait_for_unmigration_complete(rpop_api, popid):
    collectors = rpop_api.list()["rpops"]
    assert_that(collectors, contains(has_entries({"popid": popid, "error_status": "ok"})))


def migrate_collector(rpop_api, internal_api_host, dst_user, login, password, popid, enabled):
    call_service_api(
        internal_api_host + "/migrate",
        {
            "dst_suid": dst_user["suid"],
            "dst_uid": dst_user["uid"],
            "login": login,
            "password": password,
            "last_mid": 0,
            "old_popid": popid,
            "creation_ts": int(time.time()),
            "ignore_folders_struct": "1",
            "original_server": "pop3.qwe.ru",
            "collector_ip": "127.0.0.1",
            "enabled": enabled,
        },
    )
    wait_for_migration_complete(rpop_api, popid)


def migrate_collector_by_token(
    rpop_api, internal_api_host, dst_user, login, auth_token, popid, enabled
):
    call_service_api(
        internal_api_host + "/migrate",
        {
            "dst_suid": dst_user["suid"],
            "dst_uid": dst_user["uid"],
            "login": login,
            "auth_token": auth_token,
            "last_mid": 0,
            "old_popid": popid,
            "creation_ts": int(time.time()),
            "ignore_folders_struct": "1",
            "original_server": "pop3.qwe.ru",
            "collector_ip": "127.0.0.1",
            "enabled": enabled,
        },
    )
    wait_for_migration_complete(rpop_api, popid)


@pytest.fixture
def migrated_collector(
    collectors_service_url, rpop_api, dst_user, src_user_login, test_users_password
):
    create_res = rpop_api.create(src_user_login, test_users_password)
    migrate_collector(
        rpop_api,
        collectors_service_url,
        dst_user,
        src_user_login,
        test_users_password,
        create_res["popid"],
        1,
    )
    return create_res["popid"]


def test_collectors_migration(migrated_collector, collectors_api, dst_user, src_user_login):
    collectors = collectors_api.list()["rpops"]
    assert_that(collectors, has_length(1))
    assert_that(
        collectors[0],
        has_entries(
            {
                "email": src_user_login,
                "login": src_user_login,
                "error_status": "ok",
                "is_on": "1",
                "label_id": "0",
                "server": "pop3.qwe.ru",
                "imap": False,
            }
        ),
    )
    popid = collectors[0]["popid"]
    while len(popid) % 4 != 0:
        popid += "="
    decoded_popid = base64.b64decode(popid).decode()
    assert_that(decoded_popid.startswith(str(dst_user["uid"]) + ":"))


def test_collectors_unmigration(
    migrated_collector, collectors_service_url, rpop_api, collectors_api, dst_user, src_user_login
):
    collectors = collectors_api.list()["rpops"]
    popid = collectors[0]["popid"]
    call_service_api(
        collectors_service_url + "/unmigrate",
        {"suid": dst_user["suid"], "uid": dst_user["uid"], "popid": popid},
    )

    wait_for_unmigration_complete(collectors_api, migrated_collector)
    collectors = collectors_api.list()["rpops"]
    assert_that(collectors, has_length(1))
    assert_that(
        collectors[0],
        has_entries(
            {
                "email": src_user_login,
                "login": src_user_login,
                "error_status": "ok",
                "is_on": "1",
                "popid": str(migrated_collector),
            }
        ),
    )


def test_disabled_collector_unmigration(
    migrated_collector, collectors_service_url, rpop_api, collectors_api, dst_user, src_user_login
):
    collectors = collectors_api.list()["rpops"]
    popid = collectors[0]["popid"]
    collectors_api.enable(popid, False)
    call_service_api(
        collectors_service_url + "/unmigrate",
        {"suid": dst_user["suid"], "uid": dst_user["uid"], "popid": popid},
    )

    wait_for_unmigration_complete(collectors_api, migrated_collector)
    collectors = collectors_api.list()["rpops"]
    assert_that(collectors, has_length(1))
    assert_that(
        collectors[0],
        has_entries(
            {
                "email": src_user_login,
                "login": src_user_login,
                "error_status": "ok",
                "is_on": "0",
                "popid": str(migrated_collector),
            }
        ),
    )


def test_delete_migrated(migrated_collector, collectors_api):
    collectors = collectors_api.list()["rpops"]
    popid = collectors[0]["popid"]
    collectors_api.delete(popid)

    assert_that(collectors_api.list()["rpops"], has_length(0))


def test_migration_of_disabled_collector(
    collectors_service_url, collectors_api, rpop_api, dst_user, src_user_login, test_users_password
):
    create_res = rpop_api.create(src_user_login, test_users_password)
    migrate_collector(
        rpop_api,
        collectors_service_url,
        dst_user,
        src_user_login,
        test_users_password,
        create_res["popid"],
        0,
    )

    collectors = collectors_api.list()["rpops"]
    assert_that(collectors, has_length(1))
    assert_that(collectors[0], has_entries({"error_status": "ok", "is_on": "0"}))


def test_migration_with_auth_token(
    collectors_service_url, collectors_api, rpop_api, dst_user, src_user_login, test_users_password
):
    token = get_access_token(src_user_login, test_users_password)
    create_res = rpop_api.create(src_user_login, test_users_password)
    migrate_collector_by_token(
        rpop_api, collectors_service_url, dst_user, src_user_login, token, create_res["popid"], 0
    )

    collectors = collectors_api.list()["rpops"]
    assert_that(collectors, has_length(1))
    assert_that(collectors[0], has_entries({"error_status": "ok", "is_on": "0"}))


def test_migration_with_wrong_token(
    collectors_service_url, collectors_api, rpop_api, dst_user, src_user_login, test_users_password
):
    with pytest.raises(InternalApiException) as e:
        token = get_access_token(dst_user["login"], test_users_password)
        create_res = rpop_api.create(src_user_login, test_users_password)
        migrate_collector_by_token(
            rpop_api,
            collectors_service_url,
            dst_user,
            src_user_login,
            token,
            create_res["popid"],
            0,
        )
    assert_that(e.value, has_property("content", equal_to("invalid_auth_token")))


def test_migration_collector_from_non_existing_account(
    collectors_service_url, collectors_api, rpop_api, dst_user, test_users_password
):
    login = "non-existing-login-for-test@yandex.ru"
    create_res = rpop_api.create(login, test_users_password)
    call_service_api(
        collectors_service_url + "/migrate",
        {
            "dst_suid": dst_user["suid"],
            "dst_uid": dst_user["uid"],
            "login": login,
            "password": "",
            "last_mid": 0,
            "old_popid": create_res["popid"],
            "creation_ts": int(time.time()),
            "ignore_folders_struct": "0",
            "original_server": "imap.qwe.ru",
            "collector_ip": "127.0.0.1",
            "enabled": 1,
            "ignore_invalid_credentials": 1,
        },
    )
    wait_for_migration_complete(rpop_api, create_res["popid"])

    collectors = collectors_api.list()["rpops"]
    assert_that(collectors, has_length(1))
    assert_that(collectors[0], has_entry("error_status", "login error"))


def test_migration_duplicate_collector(
    collectors_service_url, rpop_api, dst_user, src_user_login, test_users_password
):
    create_res = rpop_api.create(src_user_login, test_users_password)
    migrate_collector(
        rpop_api,
        collectors_service_url,
        dst_user,
        src_user_login,
        test_users_password,
        create_res["popid"],
        0,
    )
    with pytest.raises(InternalApiException) as e:
        migrate_collector(
            rpop_api,
            collectors_service_url,
            dst_user,
            src_user_login,
            test_users_password,
            create_res["popid"],
            0,
        )
    assert_that(e.value, has_property("content", equal_to("duplicate_collector")))


def test_migration_collector_from_himself(
    collectors_service_url, rpop_api, dst_user, test_users_password
):
    UNEXISING_POPID = 100500
    with pytest.raises(InternalApiException) as e:
        migrate_collector(
            rpop_api,
            collectors_service_url,
            dst_user,
            dst_user["login"],
            test_users_password,
            UNEXISING_POPID,
            0,
        )
    assert_that(e.value, has_property("content", equal_to("collector_from_himself")))
