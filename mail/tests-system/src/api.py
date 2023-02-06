from .common.api import ApiException
from .common.utils import wait_collector_iteration
from hamcrest import (
    assert_that,
    has_properties,
    has_entries,
    has_item,
    starts_with,
    contains,
    empty,
    equal_to,
    is_not,
)
import pytest
import socket


@pytest.fixture(autouse=True)
def clean_collectors(api):
    collectors = api.list()["rpops"]
    for c in collectors:
        api.delete(c["popid"])


@pytest.fixture
def mailru_collector(api, mailru_login, mailru_password):
    create_res = api.create(mailru_login, mailru_password)
    return create_res["popid"]


def test_bad_domain_in_check_server(api):
    with pytest.raises(ApiException) as e:
        api.check_server_full("bad.domain.not.resolvable", 993, True, "imap", "login", "password")
    assert_that(
        e.value,
        has_properties({"reason": "internal error", "description": starts_with("resolve error")}),
    )


def test_forbidden_pop3_in_check_server(api):
    with pytest.raises(ApiException) as e:
        api.check_server_full("pop.yandex.ru", 995, True, "pop3", "login", "password")
    assert_that(e.value, has_properties({"reason": "forbidden server error"}))


def test_forbidden_imap_in_check_server(api):
    with pytest.raises(ApiException) as e:
        api.check_server_full("imap.yandex.ru", 993, True, "imap", "login", "password")
    assert_that(e.value, has_properties({"reason": "forbidden server error"}))


def test_bad_port_in_check_server(api):
    with pytest.raises(ApiException) as e:
        api.check_server_full("imap.mail.ru", 1993, True, "imap", "login", "password")
    assert_that(e.value, has_properties({"reason": "connect error"}))


@pytest.mark.xfail(reason="forbidden if gmail oauth hack enabled")
def test_oauth_with_bad_token_in_check_server(api):
    with pytest.raises(ApiException) as e:
        api.check_server_with_oauth("bad_token")
    assert_that(
        e.value,
        has_properties(
            {
                "reason": "syntax error",
                "description": starts_with("can`t process request: incorrect social_task_id"),
            }
        ),
    )


@pytest.mark.xfail(reason="forbidden if gmail oauth hack enabled")
def test_oauth_in_check_server(api, mailru_social_task_id):
    api.check_server_with_oauth(mailru_social_task_id)


def test_imap_in_check_server(api, mailru_login, mailru_password):
    api.check_server_with_password(mailru_login, mailru_password)


def test_pop3_in_check_server(api, mailru_login, mailru_password):
    api.check_server_full("pop.mail.ru", 995, True, "pop3", mailru_login, mailru_password)


def test_imap_login_error_in_check_server(api, mailru_login):
    with pytest.raises(ApiException) as e:
        api.check_server_with_password(mailru_login, "bad_password")
    assert_that(
        e.value,
        has_properties(
            {"reason": "login error", "description": starts_with("login error: Server returns NO")}
        ),
    )


def test_pop3_login_error_in_check_server(api, mailru_login):
    with pytest.raises(ApiException) as e:
        api.check_server_full("pop.mail.ru", 995, True, "pop3", mailru_login, "bad_password")
    assert_that(
        e.value,
        has_properties(
            {
                "reason": "login error",
                "description": starts_with("login error: "),
            }
        ),
    )


def test_create(api, mailru_login, mailru_password):
    popid = api.create(mailru_login, mailru_password)["popid"]
    assert_that(popid, is_not(""))


def test_empty_list(api):
    listed = api.list()["rpops"]
    assert_that(listed, empty())


def test_list(api, mailru_collector):
    listed = api.list()["rpops"]
    assert_that(listed, contains(has_entries({"popid": mailru_collector})))


def test_list_with_id(api, mailru_collector):
    listed = api.list(mailru_collector)["rpops"]
    assert_that(listed, contains(has_entries({"popid": mailru_collector})))


def test_delete(api, mailru_collector):
    api.delete(mailru_collector)
    listed = api.list()["rpops"]
    assert_that(listed, empty())


def test_enable(api, mailru_collector):
    api.enable(mailru_collector, False)
    listed = api.list()["rpops"]
    assert_that(listed, contains(has_entries({"is_on": "0"})))


def test_edit_not_protected_fields(api, mailru_collector):
    api.edit(mailru_collector, label_id=100)
    listed = api.list()["rpops"]
    assert_that(listed, contains(has_entries({"label_id": "100"})))


def test_edit_protected_fields_without_password(api, mailru_collector):
    with pytest.raises(ApiException) as e:
        api.edit(mailru_collector, port="1993")
    assert_that(e.value, has_properties({"description": "can`t process request: password missing"}))


def test_edit_protected_fields_with_password(api, mailru_collector, mailru_password):
    NEW_PORT = "1993"
    api.edit(mailru_collector, port=NEW_PORT, password=mailru_password)
    listed = api.list()["rpops"]
    assert_that(listed, contains(has_entries({"port": NEW_PORT})))


def test_status(api, mailru_collector):
    wait_collector_iteration(api, mailru_collector)
    status = api.status(mailru_collector)
    assert_that(
        status,
        has_entries(
            {"folders": has_item(has_entries({"name": "INBOX", "messages": is_not(equal_to("0"))}))}
        ),
    )


def test_info(api, mailru_collector):
    wait_collector_iteration(api, mailru_collector)
    info = api.info(mailru_collector)
    assert_that(info, has_entries({"owner": socket.gethostname()}))
