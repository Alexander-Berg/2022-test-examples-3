import json
import pytest

from operator import itemgetter

from mail.nwsmtp.tests.lib.stubs.bigml import BigML, to_subscriptions


@pytest.fixture
async def stub(conf, users):
    async with BigML(conf.modules.big_ml_client.configuration, users) as stub:
        yield stub


@pytest.mark.mxbackout
async def test_recipients(stub, big_ml):
    path = f"/api/v1/recipients?email_to={big_ml.email}&email_from=bar@example.com"
    code, body = await stub.get_client().fetch(path)

    assert code == 200

    resp = json.loads(body)
    assert resp["status"] == "ok"
    assert sorted(resp["response"]["subscriptions"], key=itemgetter("email")) == \
        sorted(to_subscriptions(big_ml), key=itemgetter("email"))


@pytest.mark.mxbackout
async def test_recipients_maillist_is_not_found(stub, rcpt):
    path = f"/api/v1/recipients?email_to=foo{rcpt.email}&email_from=bar@example.com"
    code, body = await stub.get_client().fetch(path)

    assert code == 404

    resp = json.loads(body)
    assert resp["status"] == "error"
    assert resp["response"]["code"] == "not_found"
