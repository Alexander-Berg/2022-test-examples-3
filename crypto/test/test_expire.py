import time

import pytest
import requests

from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.matched_id import TMatchedId
from crypta.cm.services.common.test_utils import helpers

EXT_ID_TYPE_WITH_SHORT_TTL = "ext_ns_short_ttl"
EXT_ID_TYPE_WITH_DEFAULT_TTL = "ext_ns"
EXT_ID_SHORT_TTL = 1
YANDEXUID_TYPE = "yandexuid"

pytestmark = pytest.mark.mutator_template_args(custom_ttls=[(EXT_ID_TYPE_WITH_SHORT_TTL, EXT_ID_SHORT_TTL)])


def test_expire(cm_client):
    client = cm_client

    expired_ext_id = TId(EXT_ID_TYPE_WITH_SHORT_TTL, "value")
    expired_int_id = TId(YANDEXUID_TYPE, "int_value")

    not_expired_ext_id = TId(EXT_ID_TYPE_WITH_DEFAULT_TTL, "value-2")
    not_expired_int_id = TId(YANDEXUID_TYPE, "int_value-2")

    helpers.upload_and_identify(client, expired_ext_id, [TMatchedId(expired_int_id, 0, 0, {})])
    helpers.upload_and_identify(client, not_expired_ext_id, [TMatchedId(not_expired_int_id, 0, 0, {})])

    time.sleep(EXT_ID_SHORT_TTL * 2)

    for ext_id in (expired_ext_id, not_expired_ext_id):
        client.expire(ext_id)

    time.sleep(10)

    for id, code in (
        (expired_ext_id, requests.codes.not_found),
        (expired_int_id, requests.codes.not_found),
        (not_expired_ext_id, requests.codes.ok),
        (not_expired_int_id, requests.codes.ok),
    ):
        identify = client.identify(id)
        assert code == identify.status_code, identify.text


def test_expire_404(cm_client, tvm_ids, tvm_api):
    ext_id = TId(EXT_ID_TYPE_WITH_DEFAULT_TTL, "non-existent")
    response = cm_client.expire(ext_id)
    assert requests.codes.not_found == response.status_code, response.text
