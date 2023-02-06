# -*- coding: utf-8 -*-
from test.unit.base import NoDBTestCase

from mpfs.core.office.logic.only_office_utils import check_country_for_only_office


class CheckCountryForOOTestCase(NoDBTestCase):
    def test_none(self):
        check_country_for_only_office('123', None, None)
