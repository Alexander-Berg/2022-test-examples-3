# coding: utf-8

from market.idx.admin.system_offers.lib.offer_update_worker import register_offer_updaters
from market.idx.admin.system_offers.lib.config import load_config

import yatest.common


def test_updaters():
    config = load_config(
        'unittests',
        yatest.common.source_path('market/idx/admin/system_offers/tests/data'),
        yatest.common.source_path('market/idx/admin/system_offers/etc')
    )
    config.tvm.secret_path = None
    updaters = register_offer_updaters(config)
    assert updaters
