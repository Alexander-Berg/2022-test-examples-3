from .common.api import Api
from .common.passport import userinfo_by_login
from hamcrest import assert_that, is_not, has_entries, equal_to, has_item
import pytest
import time


@pytest.fixture(scope="session")
def outlook_login():
    return "devops@kontrtest.onmicrosoft.com"


@pytest.fixture(scope="module")
def import_dst_user():
    return userinfo_by_login("devops@omenname.auto.connect-tk.tk")


@pytest.fixture(scope="module")
def _api(api_url, import_dst_user):
    return Api(api_url, import_dst_user)


@pytest.fixture(scope="module")
def password_ph():
    return "outlook-oauth2"


@pytest.fixture(scope="module")
def outlook_server_info():
    return {"server": "outlook.office365.com", "port": 993, "ssl": 1, "imap": 1}


@pytest.fixture(autouse=True)
def clean_collectors(_api):
    collectors = _api.list()["rpops"]
    for c in collectors:
        _api.delete(c["popid"])


@pytest.fixture
def outlook_collector(outlook_login, password_ph, outlook_server_info, _api):
    create_res = _api.create(outlook_login, password_ph, **outlook_server_info)
    return create_res["popid"]


@pytest.fixture
def bad_outlook_collector(outlook_login, outlook_server_info, _api):
    create_res = _api.create(outlook_login, "bad password", **outlook_server_info)
    return create_res["popid"]


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_create(outlook_login, password_ph, outlook_server_info, _api):
    popid = _api.create(outlook_login, password_ph, **outlook_server_info)["popid"]
    assert_that(popid, is_not(""))
    collector = _list(_api, popid)
    assert_that(collector.get("is_oauth"))


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_edit(bad_outlook_collector, password_ph, _api):
    old_collector = _list(_api, bad_outlook_collector)
    assert_that(is_not(old_collector.get("is_oauth")))
    _api.edit(bad_outlook_collector, password=password_ph)
    new_collector = _list(_api, bad_outlook_collector)
    assert_that(new_collector.get("is_oauth"))


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_token_loaded(outlook_collector, _api):
    time.sleep(20)
    collector = _list(_api, outlook_collector)
    assert_that(collector.get("error_status") == "ok")


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_token_loaded_after_edit(bad_outlook_collector, password_ph, _api):
    _api.edit(bad_outlook_collector, password=password_ph)
    time.sleep(10)
    new_collector = _list(_api, bad_outlook_collector)
    assert_that(new_collector.get("error_status") == "ok")


@pytest.mark.xfail(reason="failed if oauth hack disabled")
def test_status(outlook_collector, _api):
    time.sleep(20)
    status = _api.status(outlook_collector)
    assert_that(
        status,
        has_entries(
            {
                "folders": has_item(
                    has_entries(
                        {
                            "name": "Входящие",
                            "messages": is_not(equal_to("0")),
                            "collected": is_not(equal_to(0)),
                        }
                    )
                )
            }
        ),
    )


def _list(api, popid):
    listed = api.list(popid=popid)["rpops"]
    assert_that(len(listed) == 1)
    return listed[0]
