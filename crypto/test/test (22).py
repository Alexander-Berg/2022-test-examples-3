import pytest

from crypta.lib.python import duid_upload


@pytest.mark.parametrize("row,reference", [
    ({"duid": "duid_1", "field": "value"}, []),
    ({"yuid": "yandexuid", "fpc": "fpc_1", "field": "value"}, []),
    ({"yuid": "yandexuid", "duid": "duid_1", "field": "value"}, [duid_upload.Match(yandexuid="yandexuid", type="duid", ext_id="duid_1")]),
    ({"yuid": "yandexuid", "fpc": "fpc_1", "duid": "duid_1", "field": "value"}, [duid_upload.Match(yandexuid="yandexuid", type="duid", ext_id="duid_1")]),
])
def test_get_duid_upload_requests(row, reference):
    assert reference == sorted(duid_upload.get_matches(row))
