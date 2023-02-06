# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.dev_tools.swagger import get_swagger_view


def test_get_swagger_view():
    spec = mock.Mock()
    spec.to_dict.return_value = {'foo': 1}
    view = get_swagger_view(spec)
    assert view.view_class().get(mock.Mock()).data == {'foo': 1}
