from .common.api import Api
from .common.passport import userinfo_by_login
from hamcrest import (
    assert_that,
    is_not,
)
import pytest
import time


@pytest.fixture(scope="session")
def gmail_login():
    return "devops@xn--90aru.com"


@pytest.fixture(scope="module")
def import_dst_user():
    return userinfo_by_login("devops@omenname.auto.connect-tk.tk")


@pytest.fixture(scope="module")
def _api(api_url, import_dst_user):
    return Api(api_url, import_dst_user)


@pytest.fixture(scope="module")
def password_ph():
    return "gmail-oauth2"


@pytest.fixture(scope="module")
def gmail_server_info():
    return {"server": "imap.gmail.com", "port": 993, "ssl": 1, "imap": 1}


@pytest.fixture(autouse=True)
def clean_collectors(_api):
    collectors = _api.list()["rpops"]
    for c in collectors:
        _api.delete(c["popid"])


@pytest.fixture
def gmail_collector(gmail_login, password_ph, gmail_server_info, _api):
    create_res = _api.create(gmail_login, password_ph, **gmail_server_info)
    return create_res["popid"]


@pytest.fixture
def bad_gmail_collector(gmail_login, gmail_server_info, _api):
    create_res = _api.create(gmail_login, "bad password", **gmail_server_info)
    return create_res["popid"]


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_create(gmail_login, password_ph, gmail_server_info, _api):
    popid = _api.create(gmail_login, password_ph, **gmail_server_info)["popid"]
    assert_that(popid, is_not(""))
    collector = _list(_api, popid)
    assert_that(collector.get("is_oauth"))


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_edit(bad_gmail_collector, password_ph, _api):
    old_collector = _list(_api, bad_gmail_collector)
    assert_that(is_not(old_collector.get("is_oauth")))
    _api.edit(bad_gmail_collector, password=password_ph)
    new_collector = _list(_api, bad_gmail_collector)
    assert_that(new_collector.get("is_oauth"))


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_token_loaded(gmail_collector, _api):
    time.sleep(5)
    collector = _list(_api, gmail_collector)
    assert_that(collector.get("error_status") == "ok")


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_token_loaded_after_edit(bad_gmail_collector, password_ph, _api):
    _api.edit(bad_gmail_collector, password=password_ph)
    time.sleep(5)
    new_collector = _list(_api, bad_gmail_collector)
    assert_that(new_collector.get("error_status") == "ok")


def _list(api, popid):
    listed = api.list(popid=popid)["rpops"]
    assert_that(len(listed) == 1)
    return listed[0]
