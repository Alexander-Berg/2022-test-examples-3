import json
import logging
import time

import requests

from crypta.cm.services.common.test_utils import (
    upload_utils,
)


SCHEDULED_FOR_WRITING = "Scheduled for writing"

logger = logging.getLogger("ya.test")


def verify_ids_are_equal(ref_matched_ids, responded_ids):
    ref_ids = upload_utils.get_reference_forward_refs(ref_matched_ids)
    responded_ids_sorted = upload_utils.strip_match_ts_and_sort(responded_ids)
    assert ref_ids == responded_ids_sorted, (ref_ids, responded_ids_sorted)


def check_upload(upload_response):
    assert requests.codes.ok == upload_response.status_code, upload_response.text
    assert SCHEDULED_FOR_WRITING == upload_response.text, upload_response.text


def get_timeouts(total_timeout=None):
    single_timeout = 0.1
    total_timeout = total_timeout or 30
    timeout_count = int(total_timeout / single_timeout)
    return [single_timeout] * timeout_count + [0]


def check_not_identify(cm_client, ext_id, tvm_ticket=None):
    response = cm_client.identify(ext_id, tvm_ticket=tvm_ticket)
    assert requests.codes.not_found == response.status_code, response.text


def assert_with_timeout(assert_func, timeout=None):
    mini_timeouts = get_timeouts(timeout)
    for mini_timeout in mini_timeouts:
        try:
            time.sleep(mini_timeout)
            return assert_func()
        except AssertionError:
            if mini_timeout == 0:
                raise


def check_identify(cm_client, ext_id, ref_matched_ids, total_timeout=None, tvm_ticket=None):
    def identify_with_assert():
        identify_response = cm_client.identify(ext_id, tvm_ticket=tvm_ticket)
        assert requests.codes.ok == identify_response.status_code, identify_response.text

        responded_ids = json.loads(identify_response.text)
        verify_ids_are_equal(ref_matched_ids, responded_ids)

        return responded_ids

    return assert_with_timeout(identify_with_assert, total_timeout)


def upload(cm_client, ext_id, matched_ids, track_back_reference=None):
    logger.info("Upload body: %s", upload_utils.serialize_upload_body(ext_id, matched_ids, track_back_reference))
    return cm_client.upload(upload_utils.serialize_upload_body(ext_id, matched_ids, track_back_reference))


def upload_and_identify(cm_client, ext_id, matched_ids, ref_matched_ids=None, timeout=None, track_back_reference=None):
    ref_matched_ids = ref_matched_ids or matched_ids

    upload_response = upload(cm_client, ext_id, matched_ids, track_back_reference)

    check_upload(upload_response)
    upload_ts = time.time()

    result = check_identify(cm_client, ext_id, ref_matched_ids, timeout)

    # This sleep ensures that next upload has different timestamp than the previous one,
    # otherwise next upload might not overwrite the previous one
    time.sleep(max(0, 1.1 - (time.time() - upload_ts)))

    return result


def check_upload_failed_due_to_quota(cm_client, ext_id, matched_ids):
    upload_response = upload(cm_client, ext_id, matched_ids)

    assert requests.codes.service_unavailable == upload_response.status_code, upload_response.text
    assert "Service has run out of quota: Quota is full for qa" == upload_response.text, upload_response.text
