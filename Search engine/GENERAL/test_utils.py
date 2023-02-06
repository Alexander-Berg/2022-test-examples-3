import os

from yatest.common import source_path
from search.wizard.entitysearch.tools.es_hook_notifier.lib.utils import (
    get_service_port,
    get_update_fresh_url,
    get_update_realtime_url,
    untar_all,
)

from search.wizard.entitysearch.tools.es_hook_notifier.lib.consts import REALTIME_RESOURCE_TAR

TEST_DATA_PATH = source_path("search/wizard/entitysearch/tools/es_hook_notifier/lib/ut/test_data")
DUMP_JSON = os.path.join(TEST_DATA_PATH, "dump.json")

EXPECTED_FIRST_LEVEL_NODES = {"sticky", "fixlist.txt", 'version.info'}
EXPECTE_STICKY_DIR_NODES = {"realtime.gzt", "realtime.gzt.bin"}


def test_get_service_port():
    assert get_service_port(DUMP_JSON) == 1234


def test_get_fresh_update_url():
    assert get_update_fresh_url(DUMP_JSON) == "http://localhost:1234/admin?action=updatees"


def test_get_update_realtime_url():
    assert get_update_realtime_url(DUMP_JSON) == "http://localhost:1234/admin?action=updatert"


def test_untar():
    untar_all(REALTIME_RESOURCE_TAR)
    test_dir = "./realtime"
    assert os.path.isdir(test_dir)
    assert EXPECTED_FIRST_LEVEL_NODES == set(os.listdir(test_dir))
    assert EXPECTE_STICKY_DIR_NODES == set(os.listdir(os.path.join(test_dir, "sticky")))
