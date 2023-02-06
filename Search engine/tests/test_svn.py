# -*- coding: utf-8 -*-
import pytest

from extensions.configurations.balancer import BalancerGenerator


@pytest.mark.long
def test_eval_balancer(tmpdir):
    url = "svn+ssh://arcadia.yandex.ru/arc/gencfgmain/trunk/custom_generators/balancer_gencfg/"
    repo = BalancerGenerator(url)
    repo.clone()
    repo.install()
    config = repo.eval()
    assert config
