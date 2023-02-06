import pytest
import json

from mail.nwsmtp.tests.lib.stubs.fouras import Fouras, load_keys
from mail.nwsmtp.tests.fixtures.dkim_domains import *  # noqa


@pytest.fixture
async def stub(conf, users):
    async with Fouras(conf.modules.dkim.configuration.fouras, users) as stub:
        yield stub


@pytest.mark.mxbackout
async def test_get_dkim_domain(stub, disabled_dkim_domain, wrong_dkim_domain, external_dkim_domain):
    dkim_public_key = load_keys("dkim_public_key")
    dkim_private_key = load_keys("dkim_private_key")
    for dkim_domain in (external_dkim_domain, disabled_dkim_domain, wrong_dkim_domain):
        stub.add_dkim_domains(dkim_domain)
        path = f"/smtp/key?domain={dkim_domain.domain}"
        code, body = await stub.get_client().fetch(path)

        assert code == 200

        resp = json.loads(body)

        assert resp["status"] == "ok"
        assert resp["response"]["domain"] == dkim_domain.domain
        assert resp["response"]["selector"] == dkim_domain.selector
        assert resp["response"]["enabled"] == dkim_domain.is_enabled

        assert resp["response"]["public_key"] == dkim_public_key
        assert resp["response"]["private_key"] == (dkim_private_key if not dkim_domain.is_incorrect else "")


@pytest.mark.mxbackout
async def test_get_non_existen_dkim_domain(stub):
    code, body = await stub.get_client().fetch("/smtp/key?domain=non_existen_domain")

    assert code == 404

    resp = json.loads(body)
    assert resp["status"] == "error"
    assert resp["response"]["code"] == "not_found"
    assert resp["response"]["message"] == "Object not found"
