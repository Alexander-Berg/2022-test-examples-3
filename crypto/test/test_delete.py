import requests
import time

from crypta.cm.services.common.test_utils import (
    helpers,
    id_utils,
)


def test_delete(cm_client, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func)
    helpers.upload_and_identify(cm_client, ids.ext_id, ids.matched_ids)

    delete_response = cm_client.delete(ids.ext_id)
    assert requests.codes.ok == delete_response.status_code

    time.sleep(10)

    identify_response = cm_client.identify(ids.ext_id)
    assert requests.codes.not_found == identify_response.status_code
