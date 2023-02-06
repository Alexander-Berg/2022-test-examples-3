import pytest
import time

from crypta.cm.services.common.test_utils import (
    crypta_cm_service,
    helpers,
    id_utils,
    yt_kv_utils,
)

TOUCH_TIMEOUT_SEC = 10
EXTEND_TTL_TIMEOUT_SEC = 20
TTL_DEFAULT = 10 * 86400
TTL_EXTENDED = 30 * 86400
MUTATION_TIMEOUT = 15


pytestmark = pytest.mark.mutator_template_args(
    touch_timeout_sec=TOUCH_TIMEOUT_SEC,
    extend_ttl_timeout_sec=EXTEND_TTL_TIMEOUT_SEC,
)


@pytest.fixture(scope="module")
@crypta_cm_service.create(touch_timeout_sec=TOUCH_TIMEOUT_SEC)
def custom_touch_timeout_cm_client():
    return


def test_touch_is_updated(custom_touch_timeout_cm_client, add_prefix_func, yt_kv):
    matches = [
        id_utils.create_ids_for_test(add_prefix_func)
        for _ in range(3)
    ]

    for match in matches:
        responded_ids = helpers.upload_and_identify(custom_touch_timeout_cm_client, match.ext_id, match.matched_ids, timeout=MUTATION_TIMEOUT)
        match_ts = responded_ids[0]["match_ts"]

        match_from_db = yt_kv_utils.read_match(yt_kv, match.ext_id)
        assert match_ts == match_from_db.GetTouch()
        assert TTL_DEFAULT == match_from_db.GetTtl()

    touch_ts = int(time.time() + TOUCH_TIMEOUT_SEC + 1)
    custom_touch_timeout_cm_client.touch([match.ext_id for match in matches], touch_ts)

    for match in matches:
        def assert_touch_changed():
            match_from_db_2 = yt_kv_utils.read_match(yt_kv, match.ext_id)
            assert match_from_db_2.GetTouch() == touch_ts
            assert TTL_DEFAULT == match_from_db_2.GetTtl()

        helpers.assert_with_timeout(assert_touch_changed, MUTATION_TIMEOUT)


def test_ttl_is_extended(custom_touch_timeout_cm_client, add_prefix_func, yt_kv):
    ids = id_utils.create_ids_for_test(add_prefix_func)
    helpers.upload_and_identify(custom_touch_timeout_cm_client, ids.ext_id, ids.matched_ids, timeout=MUTATION_TIMEOUT)

    touch_ts = int(time.time() + EXTEND_TTL_TIMEOUT_SEC + 1)
    custom_touch_timeout_cm_client.touch([ids.ext_id], touch_ts)

    def assert_ttl_changed():
        match_from_db = yt_kv_utils.read_match(yt_kv, ids.ext_id)
        assert TTL_EXTENDED == match_from_db.GetTtl()

    helpers.assert_with_timeout(assert_ttl_changed, MUTATION_TIMEOUT)
