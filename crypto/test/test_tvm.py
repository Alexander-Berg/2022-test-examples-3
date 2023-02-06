import functools

import requests

from crypta.cm.services.common.test_utils import (
    helpers,
    id_utils,
    upload_utils,
)


def get_ids_for_test(cm_client, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func)
    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)
    return ids


def test_tvm_unknown_service(cm_client, add_prefix_func, tvm_ids, tvm_api):
    ids = id_utils.create_ids_for_test(add_prefix_func)

    unknown_tvm_id = tvm_api.issue_id()
    tvm_ticket = tvm_api.get_service_ticket(unknown_tvm_id, tvm_ids.api)

    for method in [
        cm_client.ping,
        cm_client.version,
        functools.partial(cm_client.delete, ids.ext_id),
        functools.partial(cm_client.expire, ids.ext_id),
        functools.partial(cm_client.identify, ids.ext_id),
        functools.partial(cm_client.upload, upload_utils.serialize_upload_body(ids.ext_id, ids.matched_ids)),
    ]:
        response = method(ids.ext_id, tvm_ticket=tvm_ticket)
        assert requests.codes.forbidden == response.status_code
        assert "Permission denied" in response.text.strip()


def test_handles(cm_client, add_prefix_func, tvm_ids, tvm_api):
    ids = get_ids_for_test(cm_client, add_prefix_func)
    ids_to_delete = get_ids_for_test(cm_client, add_prefix_func)

    for tvm_id_allowed, tvm_id_not_allowed, method in [
        (tvm_ids.delete_only, tvm_ids.full_except_delete, functools.partial(cm_client.delete, ids_to_delete.ext_id)),
        (tvm_ids.expire_only, tvm_ids.full_except_expire, functools.partial(cm_client.expire, ids.ext_id)),
        (tvm_ids.identify_only, tvm_ids.full_except_identify, functools.partial(cm_client.identify, ids.ext_id)),
        (tvm_ids.ping_only, tvm_ids.full_except_ping, cm_client.ping),
        (tvm_ids.upload_only, tvm_ids.full_except_upload, functools.partial(cm_client.upload, upload_utils.serialize_upload_body(ids.ext_id, ids.matched_ids))),
        (tvm_ids.version_only, tvm_ids.full_except_version, cm_client.version),
    ]:
        response = method(tvm_ticket=tvm_api.get_service_ticket(tvm_id_allowed, tvm_ids.api))
        assert requests.codes.ok == response.status_code, response.text

        response = method(tvm_ticket=tvm_api.get_service_ticket(tvm_id_not_allowed, tvm_ids.api))
        assert requests.codes.forbidden == response.status_code
        assert "Permission denied" in response.text.strip()

        response = method(tvm_ticket="INVALID_TVM_TICKET")
        assert requests.codes.unauthorized == response.status_code
        assert "Invalid TVM ticket" in response.text.strip()
