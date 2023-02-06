# -*- coding: utf-8 -*-

import unittest
import pytest

import subprocess32 as subprocess

from market.idx.mif.mif.util import gen_fastbone_fqdn, subprocess_call


class Test(unittest.TestCase):
    def test_gen_fastbone_fqdn(self):
        cases = {
            'idx01g.market.yandex.net': 'idx01g.fb.market.yandex.net',
            'idx01h.market.yandex.net': 'idx01h.fb.market.yandex.net',
            'mi01h.market.yandex.net': 'mi01h.fb.market.yandex.net',
        }
        for src, dst in cases.items():
            self.assertEquals(gen_fastbone_fqdn(src), dst)


def test_subprocess_call():
    with pytest.raises(subprocess.TimeoutExpired):
        subprocess_call(['sleep', '10'], timeout=0.1)

    result = subprocess_call(['sleep', '0.1'], timeout=10)
    assert result.returncode == 0
