# coding=utf-8
from __future__ import unicode_literals, absolute_import, print_function

import pytest

from travel.avia.admin.feature_flag_app.models import FeatureFlag
from travel.avia.admin.lib.feature_flags import extended_report_flag_by_partner_code, new_pricing_flag_by_partner_code

pytestmark = [pytest.mark.dbuser]


@pytest.mark.parametrize('enabled, expected', (
    (FeatureFlag.DISABLED, False),
    (FeatureFlag.ENABLED, True),
    (FeatureFlag.USE_AB, False),
))
def test_feature_flag(enabled, expected):
    FeatureFlag.objects.create(code='AVIA_PRICING_2019_TEST-PARTNER', enabled=enabled)
    flag_value = new_pricing_flag_by_partner_code('test-partner')
    assert flag_value == expected


def test_not_existing_feature_flag():
    flag_value = extended_report_flag_by_partner_code('not-existing-partner')
    assert flag_value is False
