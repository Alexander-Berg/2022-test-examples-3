from copy import deepcopy
import json
import pytest
import time

from google.protobuf import json_format
import requests

from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.proto.refugee_pb2 import TRefugee
from crypta.cm.services.common.test_utils import (
    crypta_cm_service,
    exec_range,
    fields,
    helpers,
    id_utils,
    upload_utils,
    yt_kv_utils,
)
from crypta.lib.python.logbroker.test_helpers import consumer_utils

EXT_ID_TYPE_WITHOUT_BACK_REFERENCE = "ext_ns_no_backref"
EXT_ID_TYPE_CUSTOM_TTL = "ext_ns_custom_ttl"
EXT_ID_TYPE_LOG_ONLY = "ext_ns_log_only"
EXT_ID_TYPE_LOG_ONLY_REGEX = ".*_log_only"
EXT_ID_TYPE_DUID = "duid"
TTL_DEFAULT = 10 * 86400
TTL_EXTENDED = 30 * 86400
TTL_CUSTOM = 100
EXTEND_TTL_TIMEOUT_SEC = 30


pytestmark = pytest.mark.mutator_template_args(
    custom_ttls=[(EXT_ID_TYPE_CUSTOM_TTL, TTL_CUSTOM)],
    extend_ttl_timeout_sec=EXTEND_TTL_TIMEOUT_SEC,
    default_ttl=TTL_DEFAULT,
    extended_ttl=TTL_EXTENDED,
)


@pytest.fixture(scope="module")
@crypta_cm_service.create(log_only_types=[EXT_ID_TYPE_LOG_ONLY_REGEX])
def log_only_cm_client():
    return


def verify_back_refs(cm_client, ext_ids, matched_ids):
    for matched_id in matched_ids:
        identify = cm_client.identify(matched_id.Id)
        assert requests.codes.ok == identify.status_code, identify.text

        back_refs = upload_utils.strip_match_ts_and_sort(json.loads(identify.text))
        reference_back_refs = upload_utils.get_reference_back_refs(ext_ids, matched_id)

        assert reference_back_refs == back_refs


def verify_not_found(cm_client, id):
    assert requests.codes.not_found == cm_client.identify(id).status_code


def assert_ids_equal(expected, actual):
    assert (expected.Type, expected.Value) == (actual.Type, actual.Value)


@pytest.fixture(scope="function", params=[None, {fields.SYNT: "0"}, {fields.SYNT: "1"}])
def yuid_attrs(request):
    yield request.param


@pytest.fixture(scope="function", params=[None, {fields.SYNT: "0"}, {fields.SYNT: "1"}])
def icookie_attrs(request):
    yield request.param


@pytest.mark.parametrize("type_order", [["yandexuid"], ["icookie"], ["yandexuid", "icookie"], ["icookie", "yandexuid"]])
def test_upload_new(cm_client, add_prefix_func, type_order, yuid_attrs, icookie_attrs):
    ids = id_utils.create_ids_for_test(add_prefix_func, yuid_attrs, icookie_attrs)
    matched_ids = [ids.matched_ids_by_type[id_type] for id_type in type_order]

    helpers.upload_and_identify(cm_client, ids.ext_id, matched_ids)
    verify_back_refs(cm_client, [ids.ext_id], matched_ids)


def test_upload_without_backreference_tracking(cm_client, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func, {fields.SYNT: "0"}, {fields.SYNT: "1"})
    ids.ext_id.Type = EXT_ID_TYPE_WITHOUT_BACK_REFERENCE

    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)

    for matched_id in ids.matched_ids:
        identify = cm_client.identify(matched_id.Id)
        assert requests.codes.not_found == identify.status_code, identify.text


def test_upload_with_explicit_track_back_reference(cm_client, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func, {fields.SYNT: "0"}, {fields.SYNT: "1"})
    ids.ext_id.Type = EXT_ID_TYPE_WITHOUT_BACK_REFERENCE

    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids, track_back_reference=True)

    for matched_id in ids.matched_ids:
        identify = cm_client.identify(matched_id.Id)
        assert requests.codes.ok == identify.status_code, identify.text


@pytest.mark.parametrize("type_order", [["yandexuid", "icookie"], ["icookie", "yandexuid"]])
def test_matches_are_independent(cm_client, add_prefix_func, type_order, yuid_attrs, icookie_attrs):
    ids = id_utils.create_ids_for_test(add_prefix_func, yuid_attrs, icookie_attrs)

    expected_count = 0
    cumulative_matched_ids = []
    for id_type in type_order:
        matched_id = ids.matched_ids_by_type[id_type]
        cumulative_matched_ids.append(matched_id)
        expected_count += 1

        responded_ids = helpers.upload_and_identify(cm_client, ids.ext_id, [matched_id], cumulative_matched_ids)
        helpers.verify_ids_are_equal(cumulative_matched_ids, responded_ids)
        verify_back_refs(cm_client, [ids.ext_id], cumulative_matched_ids)


def test_upload_rewrite_attributes(cm_client, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func)

    ids.matched_yuid.Attributes = {fields.SYNT: "0"}
    helpers.upload_and_identify(cm_client, ids.ext_id, [ids.matched_yuid])

    ids.matched_yuid.Attributes = {fields.SYNT: "1"}
    helpers.upload_and_identify(cm_client, ids.ext_id, [ids.matched_yuid])


def test_invalid_attribute_name(cm_client, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func, {"foo": "0"})
    response = helpers.upload(cm_client, ids.ext_id, [ids.matched_yuid])
    assert requests.codes.bad_request == response.status_code
    assert "Invalid attribute 'foo'" in response.text


@pytest.mark.parametrize("attr_value", ["", "2", "a", "true", "false"])
def test_invalid_attribute_value(cm_client, add_prefix_func, attr_value):
    ids = id_utils.create_ids_for_test(add_prefix_func, {fields.SYNT: attr_value})
    response = helpers.upload(cm_client, ids.ext_id, [ids.matched_yuid])
    assert requests.codes.bad_request == response.status_code
    assert "Invalid value '{}' for attribute '{}'".format(attr_value, fields.SYNT) in response.text


def test_two_yandexuids_in_match(cm_client, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func)
    ids2 = id_utils.create_ids_for_test(add_prefix_func)

    response = helpers.upload(cm_client, ids.ext_id, [ids.matched_yuid, ids2.matched_yuid])
    assert requests.codes.bad_request == response.status_code
    assert "Allowed only one id for each type" in response.text.strip()


def test_upload_rewrite_ids(cm_client, add_prefix_func):
    ids_1 = id_utils.create_ids_for_test(add_prefix_func)
    ids_2 = id_utils.create_ids_for_test(add_prefix_func)
    ext_id = ids_1.ext_id

    helpers.upload_and_identify(cm_client, ext_id, ids_1.matched_ids, False)
    helpers.upload_and_identify(cm_client, ext_id, ids_2.matched_ids, False)


def test_one_to_many(cm_client, add_prefix_func, yuid_attrs):
    ids = id_utils.create_ids_for_test(add_prefix_func, yuid_attrs)
    ext_id_1 = ids.ext_id
    ext_id_2 = id_utils.create_random_id(id_utils.EXT_ID_TYPE, add_prefix_func)

    responded_ids_1 = upload_utils.strip_match_ts_and_sort(helpers.upload_and_identify(cm_client, ext_id_1, [ids.matched_yuid]))
    verify_back_refs(cm_client, [ext_id_1], [ids.matched_yuid])

    responded_ids_2 = upload_utils.strip_match_ts_and_sort(helpers.upload_and_identify(cm_client, ext_id_2, [ids.matched_yuid]))

    assert responded_ids_1 == responded_ids_2

    verify_back_refs(cm_client, [ext_id_2], [ids.matched_yuid])

    matched_id_to_overwrite = id_utils.create_ids_for_test(add_prefix_func, yuid_attrs).matched_yuid
    ids_overwritted_from_cm = helpers.upload_and_identify(cm_client, ext_id_2, [matched_id_to_overwrite])
    assert upload_utils.strip_match_ts_and_sort(ids_overwritted_from_cm) != responded_ids_2
    verify_back_refs(cm_client, [ext_id_2], [matched_id_to_overwrite])
    verify_not_found(cm_client, ids.matched_yuid.Id)


def test_match_ts_assigned_right_now(cm_client, add_prefix_func, yuid_attrs, icookie_attrs):
    ids = id_utils.create_ids_for_test(add_prefix_func, yuid_attrs, icookie_attrs)

    with exec_range.ExecRange() as ts_range_1:
        responded_ids = helpers.upload_and_identify(cm_client, ids.ext_id, [ids.matched_icookie])
    assert responded_ids[0][fields.MATCH_TS] in ts_range_1

    with exec_range.ExecRange() as ts_range_2:
        responded_ids_2 = upload_utils.get_sorted_ids(helpers.upload_and_identify(cm_client, ids.ext_id, [ids.matched_yuid], ids.matched_ids))

    icookie_match_ts = responded_ids_2[0][fields.MATCH_TS]
    yuid_match_ts = responded_ids_2[1][fields.MATCH_TS]

    assert icookie_match_ts < yuid_match_ts
    assert icookie_match_ts in ts_range_1
    assert yuid_match_ts in ts_range_2


FULL_EXT_ID = {
    fields.EXT_ID: {
        fields.TYPE: id_utils.EXT_ID_TYPE,
        fields.VALUE: "XXXXXXXXXXXXXXXXXXXX"
    },
    fields.IDS: [
        {
            fields.TYPE: "yandexuid",
            fields.VALUE: "2340000001500000000",
            fields.MATCH_TS: 1560000000,
            fields.CAS: 100500,
            fields.ATTRIBUTES: {
                fields.SYNT: "0"
            }
        },
        {
            fields.TYPE: "icookie",
            fields.VALUE: "2340000001500000001",
            fields.MATCH_TS: 1560000001,
            fields.CAS: 200500,
            fields.ATTRIBUTES: {
                fields.SYNT: "1"
            }
        }
    ]
}


def test_match_ts_cas_not_accounted(cm_client, add_prefix_func):
    ext_id_match = FULL_EXT_ID
    ext_id_match[fields.EXT_ID][fields.VALUE] = id_utils.create_ids_for_test(add_prefix_func).ext_id.Value

    with exec_range.ExecRange() as upload_range:
        response = cm_client.upload(json.dumps(ext_id_match))

    helpers.check_upload(response)

    for timeout in helpers.get_timeouts():
        time.sleep(timeout)
        response = cm_client.identify(TId(ext_id_match[fields.EXT_ID][fields.TYPE], ext_id_match[fields.EXT_ID][fields.VALUE]))
        if requests.codes.ok == response.status_code:
            break

    assert requests.codes.ok == response.status_code, response.text

    responded_ids = sorted(json.loads(response.text), key=lambda x: x[fields.TYPE])

    assert len(responded_ids) == 2
    assert responded_ids[0][fields.MATCH_TS] in upload_range
    assert responded_ids[1][fields.MATCH_TS] in upload_range

    ref_ids = sorted(deepcopy(ext_id_match[fields.IDS]), key=lambda x: x[fields.TYPE])
    for responded_id, ref_id in zip(responded_ids, ref_ids):
        assert responded_id[fields.MATCH_TS] != ref_id[fields.MATCH_TS]
        assert responded_id[fields.CAS] != ref_id[fields.CAS]

        ref_id[fields.CAS] = 0
        ref_id[fields.MATCH_TS] = responded_id[fields.MATCH_TS]

    assert ref_ids == responded_ids


@pytest.mark.parametrize("data,error", [
    ["", "The document is empty"],
    ["{}", "Ext id match is not valid"],
    ["[]", "Json must be a map"]
])
def test_upload_empty(cm_client, data, error):
    response = cm_client.upload(data)
    assert requests.codes.bad_request == response.status_code
    assert error in response.text


@pytest.mark.parametrize("ext_id", [None, "", {}, []])
def test_upload_incomplete_ext_id(cm_client, ext_id):
    body = {
        fields.IDS: [
            {
                fields.TYPE: "some_type",
                fields.VALUE: "some_value"
            }
        ]
    }
    if ext_id is not None:
        body[fields.EXT_ID] = ext_id

    response = cm_client.upload(json.dumps(body))
    assert requests.codes.bad_request == response.status_code
    assert "Ext id match is not valid" in response.text


@pytest.mark.parametrize("ids", [None, "", [], {}])
def test_upload_incomplete_ids(cm_client, ids):
    body = {
        fields.EXT_ID: {
            fields.TYPE: "some_type",
            fields.VALUE: "some_value"
        }
    }
    if ids is not None:
        body[fields.IDS] = ids

    response = cm_client.upload(json.dumps(body))
    assert requests.codes.bad_request == response.status_code
    assert "Ext id match is not valid" in response.text


def test_upload_ttl(cm_client, add_prefix_func, yt_kv):
    custom_ttl_ids = id_utils.create_ids_for_test(add_prefix_func)
    custom_ttl_ids.ext_id.Type = EXT_ID_TYPE_CUSTOM_TTL
    helpers.upload_and_identify(cm_client, custom_ttl_ids.ext_id, [custom_ttl_ids.matched_yuid])
    assert TTL_CUSTOM == yt_kv_utils.read_match(yt_kv, custom_ttl_ids.ext_id).GetTtl()

    default_ttl_ids = id_utils.create_ids_for_test(add_prefix_func)
    default_ttl_ids.ext_id.Type = id_utils.EXT_ID_TYPE
    helpers.upload_and_identify(cm_client, default_ttl_ids.ext_id, [default_ttl_ids.matched_yuid])
    assert TTL_CUSTOM != yt_kv_utils.read_match(yt_kv, default_ttl_ids.ext_id).GetTtl()


def test_extend_ttl(cm_client, add_prefix_func, yt_kv):
    ids = id_utils.create_ids_for_test(add_prefix_func)

    helpers.upload_and_identify(cm_client, ids.ext_id, [ids.matched_yuid])
    assert TTL_DEFAULT == yt_kv_utils.read_match(yt_kv, ids.ext_id).GetTtl()

    time.sleep(EXTEND_TTL_TIMEOUT_SEC)

    helpers.upload_and_identify(cm_client, ids.ext_id, [ids.matched_yuid])
    assert TTL_EXTENDED == yt_kv_utils.read_match(yt_kv, ids.ext_id).GetTtl()


def test_upload_log_only_type(log_only_cm_client, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func)
    ids.ext_id.Type = EXT_ID_TYPE_LOG_ONLY

    response = helpers.upload(log_only_cm_client, ids.ext_id, [ids.matched_yuid])
    assert requests.codes.ok == response.status_code, response.text
    assert "Logged but not actually stored" in response.text


def test_upload_duid_and_evacuate(cm_client, add_prefix_func, evacuate_log_logbroker_client):
    time.sleep(3)
    consumer_utils.read_all(evacuate_log_logbroker_client.create_consumer())

    ids = id_utils.create_ids_for_test(add_prefix_func, {fields.SYNT: "0"}, {fields.SYNT: "1"})

    def assert_no_refugee():
        time.sleep(3)
        assert [] == consumer_utils.read_all(evacuate_log_logbroker_client.create_consumer())

    def assert_refugee():
        time.sleep(3)
        messages = consumer_utils.read_all(evacuate_log_logbroker_client.create_consumer())
        assert 1 == len(messages)

        refugee = json_format.Parse(messages[0], TRefugee())
        assert "cm" == refugee.Type
        assert 1500000000 < refugee.Timestamp
        assert_ids_equal(ids.ext_id, refugee.Source)
        assert_ids_equal(ids.yuid, refugee.Destination)

    # not duid
    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)
    assert_no_refugee()

    # new duid
    ids.ext_id.Type = EXT_ID_TYPE_DUID

    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)
    assert_refugee()

    # no changes
    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)
    assert_no_refugee()

    # yandexuid changed
    ids = id_utils.IdsForTest(ids.ext_id, id_utils.create_random_id(id_utils.YANDEXUID_TYPE, add_prefix_func), ids.icookie)

    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)
    assert_refugee()


# TODO(cherenkov-p-a) Find a way to test this
# def test_upload_same_twice(cm_client, add_prefix_func):
#     ids = id_utils.create_ids_for_test(add_prefix_func)
#     ids.ext_id.Type = EXT_ID_TYPE
#
#     response = helpers.upload(cm_client, ids.ext_id, [ids.matched_yuid)])
#     assert requests.codes.ok == response.status_code, response.text
#     assert "Successfully stored" in response.text
#
#     response = helpers.upload(cm_client, ids.ext_id, [ids.matched_yuid])
#     assert requests.codes.ok == response.status_code, response.text
#     assert "Already in database" in response.text


def test_rt_and_offline_upload(cm_client, add_prefix_func, tvm_api, tvm_ids):
    ids = id_utils.create_ids_for_test(add_prefix_func)

    ids.matched_yuid.Attributes = {fields.RT: "1"}
    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)

    tvm_ticket = tvm_api.get_service_ticket(tvm_ids.identify_offline_only, tvm_ids.api)
    helpers.check_identify(cm_client, ids.ext_id, [ids.matched_icookie], tvm_ticket=tvm_ticket)


def test_rt_upload(cm_client, add_prefix_func, tvm_api, tvm_ids):
    ids = id_utils.create_ids_for_test(add_prefix_func)

    ids.matched_yuid.Attributes = {fields.RT: "1"}
    helpers.upload_and_identify(cm_client, ids.ext_id, [ids.matched_yuid])

    tvm_ticket = tvm_api.get_service_ticket(tvm_ids.identify_offline_only, tvm_ids.api)
    helpers.check_not_identify(cm_client, ids.ext_id, tvm_ticket=tvm_ticket)


def test_rt_upload_does_not_overwrite(cm_client, add_prefix_func, tvm_api, tvm_ids):
    ids = id_utils.create_ids_for_test(add_prefix_func)
    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)

    rt_ids = id_utils.create_ids_for_test(add_prefix_func)
    rt_ids.matched_yuid.Attributes = {fields.RT: "1"}
    helpers.upload_and_identify(cm_client, ids.ext_id, [rt_ids.matched_yuid], ref_matched_ids=ids.matched_ids)

    tvm_ticket = tvm_api.get_service_ticket(tvm_ids.identify_offline_only, tvm_ids.api)
    helpers.check_identify(cm_client, ids.ext_id, ids.matched_ids, tvm_ticket=tvm_ticket)
