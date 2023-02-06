# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.apps.train.models import ServiceClass


@pytest.mark.dbuser
def test_single_code():
    assert ServiceClass(code='2Э').codes == ['2Э']


@pytest.mark.dbuser
def test_multiple_codes():
    assert ServiceClass(code='2Э , 2Ж,2Д').codes == ['2Э', '2Ж', '2Д']
