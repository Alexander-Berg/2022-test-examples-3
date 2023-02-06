import json
import pytest

from mail.nwsmtp.tests.lib.stubs.corpml import CorpML
from mail.nwsmtp.tests.lib.stubs.blackbox import Blackbox


@pytest.fixture
async def corp_ml_stub(conf, users):
    async with CorpML(conf.nwsmtp.corp_maillist, users) as stub:
        yield stub


@pytest.fixture
async def blackbox_stub(conf, users):
    async with Blackbox(conf.modules.blackbox_client.configuration, users) as stub:
        yield stub


@pytest.mark.mxbackcorp
async def test_delivery_info_user_is_found(corp_ml_stub, corp_rcpt):
    client = corp_ml_stub.get_client()
    path = "/apiv2/delivery/info?email=" + corp_rcpt.email
    code, body = await client.fetch(path)
    assert code == 200

    resp = json.loads(body)
    assert resp["status"] == "ok"
    assert resp["type"] == "user"
    assert resp["emails"] == [corp_rcpt.email]


@pytest.mark.mxbackcorp
async def test_delivery_info_maillist_is_found(corp_ml_stub, corp_ml):
    client = corp_ml_stub.get_client()
    path = "/apiv2/delivery/info?email=" + corp_ml.email
    code, body = await client.fetch(path)
    assert code == 200

    resp = json.loads(body)
    assert resp["status"] == "ok"
    assert resp["type"] == "maillist"
    assert resp["is_open"] is True
    assert resp["is_internal"] is False
    assert sorted(resp["subscribers"]["inbox"]) == \
           sorted(list(corp_ml.subscribers.keys()))


@pytest.mark.mxbackcorp
async def test_delivery_info_user_is_not_found(corp_ml_stub, corp_rcpt):
    client = corp_ml_stub.get_client()
    path = "/apiv2/delivery/info?email=foo" + corp_rcpt.email
    code, body = await client.fetch(path)
    assert code == 410

    resp = json.loads(body)
    assert resp["status"] == "error"
    assert "no entity with" in resp["error"]


@pytest.mark.mxcorp
async def test_ml_list_not_registered_in_blackbox_is_found_in_ml(corp_ml_stub, corp_ml_not_registered_in_blackbox, blackbox_stub):
    client = corp_ml_stub.get_client()
    path = f"/apiv2/delivery/info?email={corp_ml_not_registered_in_blackbox.email}"
    code, body = await client.fetch(path)
    assert code == 200

    resp = json.loads(body)
    assert resp["status"] == "ok"
    assert resp["type"] == "maillist"
    assert resp["is_open"] is True
    assert resp["is_internal"] is False
    assert sorted(resp["subscribers"]["inbox"]) == \
           sorted(list(corp_ml_not_registered_in_blackbox.subscribers.keys()))


@pytest.mark.mxcorp
async def test_ml_list_not_registered_in_blackbox_is_not_found_in_blackbox(corp_ml_stub, corp_ml_not_registered_in_blackbox, blackbox_stub):
    bb_client = blackbox_stub.get_client()
    path = f"/blackbox/?method=userinfo&format=json&login={corp_ml_not_registered_in_blackbox.email}"
    code, body = await bb_client.fetch(path)
    assert code == 200

    root = json.loads(body)
    uid_node = root["users"][0]["uid"]
    assert "value" not in uid_node


@pytest.mark.mxcorp
async def test_ml_list_not_registered_in_blackbox_is_found_in_blackbox(corp_ml_stub, corp_ml_not_registered_in_blackbox, blackbox_stub):
    corp_ml_not_registered_in_blackbox.is_registered_in_blackbox = True

    bb_client = blackbox_stub.get_client()
    path = f"/blackbox/?method=userinfo&format=json&login={corp_ml_not_registered_in_blackbox.email}"
    code, body = await bb_client.fetch(path)
    assert code == 200

    root = json.loads(body)
    uid_node = root["users"][0]["uid"]
    assert uid_node["value"] == str(corp_ml_not_registered_in_blackbox.uid)
