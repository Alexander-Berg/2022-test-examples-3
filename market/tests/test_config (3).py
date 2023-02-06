from rules_config import get_promo_from_config
from blacklist.blacklists import get_blacklists
from datetime import datetime


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


def test_real_config():
    promos = get_promo_from_config('testing', datetime.now().strftime("%Y-%m-%d"))
    assert promos is not None


def test_blacklist():
    blacklists = get_blacklists(datetime.now().strftime('%Y-%m-%dT%H:%M:%S'), 'testing')
    assert len(blacklists) == 4
    assert blacklists['supplier_blacklist'] is not None
    assert blacklists['msku_blacklist'] is not None
    assert blacklists['category_blacklist'] is not None
    assert blacklists['vendor_blacklist'] is not None
