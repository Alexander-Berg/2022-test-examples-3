import pytest

from crypta.lib.python.graphite import sender
from crypta.lib.python.graphite.sender import schemas
import conftest

DEFAULT_SCHEMA = schemas.ONE_SEC
DEFAULT_ROOT_PATH = "default.root.path"
DEFAULT_FQDN = "default.fqdn"
DEFAULT_NORMALIZED_FQDN = "default_fqdn"


@pytest.mark.parametrize("kwargs,metric_parts", [
    (
        {},
        (schemas.ONE_MIN, conftest.NORMALIZED_FQDN, None)
    ),
    (
        {"schema": schemas.ONE_SEC},
        (schemas.ONE_SEC, conftest.NORMALIZED_FQDN, None)
    ),
    (
        {"fqdn": "f.q.d.n"},
        (schemas.ONE_MIN, "f_q_d_n", None)
    ),
    (
        {"root_path": "root.path"},
        (schemas.ONE_MIN, conftest.NORMALIZED_FQDN, "root.path")
    ),
    (
        {"schema": schemas.ONE_SEC, "root_path": "root.path", "fqdn": "f.q.d.n"},
        (schemas.ONE_SEC, "f_q_d_n", "root.path")
    )
])
def test_get_metric_parts(kwargs, metric_parts, mock_getfqdn):
    with mock_getfqdn:
        graphite_sender = sender.BaseGraphiteSender()
        assert metric_parts == graphite_sender._get_metric_parts(**kwargs)


@pytest.mark.parametrize("kwargs,metric_parts", [
    (
        {},
        (DEFAULT_SCHEMA, DEFAULT_NORMALIZED_FQDN, DEFAULT_ROOT_PATH)
    ),
    (
        {"schema": schemas.FIVE_MIN},
        (schemas.FIVE_MIN, DEFAULT_NORMALIZED_FQDN, DEFAULT_ROOT_PATH)
    ),
    (
        {"fqdn": "f.q.d.n"},
        (DEFAULT_SCHEMA, "f_q_d_n", DEFAULT_ROOT_PATH)
    ),
    (
        {"fqdn": None},
        (DEFAULT_SCHEMA, conftest.NORMALIZED_FQDN, DEFAULT_ROOT_PATH)
    ),
    (
        {"root_path": "root.path"},
        (DEFAULT_SCHEMA, DEFAULT_NORMALIZED_FQDN, "root.path")
    ),
    (
        {"root_path": None},
        (DEFAULT_SCHEMA, DEFAULT_NORMALIZED_FQDN, None)
    ),
    (
        {"schema": schemas.FIVE_MIN, "root_path": "root.path", "fqdn": "f.q.d.n"},
        (schemas.FIVE_MIN, "f_q_d_n", "root.path")
    )
])
def test_get_metric_parts_with_preset_args(kwargs, metric_parts, mock_getfqdn):
    with mock_getfqdn:
        graphite_sender = sender.BaseGraphiteSender(DEFAULT_FQDN, DEFAULT_SCHEMA, DEFAULT_ROOT_PATH)
        assert metric_parts == graphite_sender._get_metric_parts(**kwargs)


@pytest.mark.parametrize("item,default_timestamp,ref_timestamp", [
    (("field", 1), None, conftest.TIMESTAMP_INT),
    (("field", 1, 2), None, 2),
    (("field", 1, None), None, conftest.TIMESTAMP_INT),
    (("field", 1), 3, 3),
    (("field", 1, 2), 3, 2)
])
def test_get_ts_from_tuple(item, default_timestamp, ref_timestamp, mock_time):
    with mock_time:
        assert ref_timestamp == sender.BaseGraphiteSender._get_ts_from_tuple(item, default_timestamp)
