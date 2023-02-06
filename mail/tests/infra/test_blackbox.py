import pytest

import json

from mail.nwsmtp.tests.lib.stubs.blackbox import Blackbox, render_response
from mail.nwsmtp.tests.lib.users import to_mail_yandex_team


@pytest.fixture
async def stub(conf, users):
    async with Blackbox(conf.modules.blackbox_client.configuration, users) as stub:
        yield stub


@pytest.mark.mxbackout
async def test_find_user(stub, rcpt):
    client = stub.get_client()

    # found
    path = f"/blackbox/?method=userinfo&format=json&login={rcpt.email}"
    code, body = await client.fetch(path)
    assert code == 200

    root = json.loads(body)
    uid_node = root["users"][0]["uid"]
    assert not uid_node["hosted"]
    assert uid_node["value"] == str(rcpt.uid)

    # not found
    path = f"/blackbox/?method=userinfo&format=json&login=foo{rcpt.email}"
    code, body = await client.fetch(path)
    assert code == 200

    root = json.loads(body)
    uid_node = root["users"][0]["uid"]
    assert "value" not in uid_node


@pytest.mark.mxbackcorp
async def test_find_mail_yandex_team(stub, corp_rcpt):
    client = stub.get_client()

    path = "/blackbox/?method=userinfo&login=" + to_mail_yandex_team(corp_rcpt.email)
    code, _ = await client.fetch(path)
    assert code == 200


@pytest.mark.mxbackout
async def test_stub_could_reuse_port(conf, users):
    async with Blackbox(conf.modules.blackbox_client.configuration, users):
        pass
    async with Blackbox(conf.modules.blackbox_client.configuration, users):
        pass


@pytest.mark.parametrize("email", [
    "first@yandex.ru", "second@yandex.ru",
    "first@yandex-team.ru", "second@yandex-team.ru"
])
def test_render_users(email, users):
    user = users.get(email)
    resp = render_response("user_info.json", user=user)
    root = json.loads(resp.encode())
    uid_node = root["users"][0]["uid"]
    assert not uid_node["hosted"]

    assert "13" not in root["users"][0]["attributes"]


@pytest.mark.parametrize("email", [
    "otdel@bigmltest.yaconnect.com",
    "corp_ml@yandex-team.ru"
])
def test_render_maillist(email, users):
    user = users.get(email)
    assert user.is_ml
    resp = render_response("user_info.json", user=user)
    root = json.loads(resp.encode())
    assert root["users"][0]["attributes"]["13"] == "1"


@pytest.mark.parametrize("email", [
    "otdel@bigmltest.yaconnect.com",
    "second@bigmltest.yaconnect.com",
    "first@bigmltest.yaconnect.com"
])
def test_render_big_ml_users(email, users):
    user = users.get(email)
    assert user.is_hosted
    resp = render_response("user_info.json", user=user)
    root = json.loads(resp.encode())
    uid_node = root["users"][0]["uid"]
    assert uid_node["hosted"]
    assert uid_node["domid"] == str(user.domid)
    assert uid_node["domain"] == user.domain
