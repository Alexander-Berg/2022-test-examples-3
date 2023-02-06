# -*- coding: utf-8 -*-

from mpfs.core.social.publicator import Publicator


def test_replace_yadisk_cc_url():
    assert (
        Publicator.replace_yadisk_cc_url('https://yadisk.cc/d/LJAu4LX3xHt') ==
        (True, 'https://yadi.sk/d/LJAu4LX3xHt')
    )
    assert (
        Publicator.replace_yadisk_cc_url('https://yadi.sk/d/LJAu4LX3xHt') ==
        (False, 'https://yadi.sk/d/LJAu4LX3xHt')
    )
    assert (
        Publicator.replace_yadisk_cc_url('https://yadi.sk/d/LJAu4LX3xHt?first=1&second=2') ==
        (False, 'https://yadi.sk/d/LJAu4LX3xHt?first=1&second=2')
    )
    assert (
        Publicator.replace_yadisk_cc_url('https://yadisk.cc/d/LJAu4LX3xHt?first=1&second=2') ==
        (True, 'https://yadi.sk/d/LJAu4LX3xHt?first=1&second=2')
    )
