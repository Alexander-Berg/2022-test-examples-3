# -*- coding: utf-8 -*-

import pytest

from market.idx.pylibrary.ellipticslib import Elliptics
from market.idx.pylibrary.ellipticslib.curllib import Curl, Error

import yatest.common


def test_elliptics():
    el = Elliptics(('aida.yandex.ru', 88))
    assert hasattr(el, 'put')


def test_curllib_ok():
    url = 'file://' + yatest.common.source_path('market/idx/pylibrary/ellipticslib/tests/ya.make')
    response = Curl().request_get(url=url)
    assert response.code == 0


def test_curllib_fail():
    url = 'file://' + yatest.common.source_path('market/idx/pylibrary/ellipticslib/tests/not_exists')
    with pytest.raises(Error):
        Curl().request_get(url=url)
