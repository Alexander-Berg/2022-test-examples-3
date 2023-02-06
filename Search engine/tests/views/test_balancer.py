# -*- coding: utf-8 -*-
from collections import OrderedDict

import pytest

from extensions.configurations.balancer import BalancerGenerator


@pytest.mark.long
def test_balancer():
    url = "svn+ssh://arcadia.yandex.ru/arc/gencfgmain/trunk/custom_generators/balancer_gencfg/"
    repository = BalancerGenerator(url)
    repository.local_path = "balancer-gencfg"
    repository.clone()
    repository.install()
    view = repository.eval({u'search_backends': OrderedDict(
            [(u'sas', ['ALL_WEB_NMETA_PRIEMKA_NOAPACHE']), (u'man', ['ALL_WEB_NMETA_PRIEMKA_NOAPACHE']),
             (u'msk', ['ALL_WEB_NMETA_PRIEMKA_NOAPACHE'])])})
    assert view
