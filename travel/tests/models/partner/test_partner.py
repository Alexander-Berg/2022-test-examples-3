# -*- coding=utf-8 -*-

from datetime import datetime
from mock import patch

from travel.avia.library.python.common.models.partner import Partner

from travel.avia.library.python.tester.testcase import TestCase

import travel.avia.library.python.common.utils.environment as environment


class TestUpdateUnavailabilityRule(TestCase):
    def test_enabled(self):
        partner = Partner(start_unavailability_datetime=datetime(2017, 1, 10),
                          end_unavailability_datetime=datetime(2017, 1, 15),
                          disabled=False)

        with patch.object(environment, 'now', return_value=datetime(2017, 1, 9)):
            assert partner.enabled is True

        with patch.object(environment, 'now', return_value=datetime(2017, 1, 10)):
            assert partner.enabled is False

        with patch.object(environment, 'now', return_value=datetime(2017, 1, 13)):
            assert partner.enabled is False

        with patch.object(environment, 'now', return_value=datetime(2017, 1, 15)):
            assert partner.enabled is False

        with patch.object(environment, 'now', return_value=datetime(2017, 1, 16)):
            assert partner.enabled is True

        partner.disabled = True
        with patch.object(environment, 'now', return_value=datetime(2017, 1, 16)):
            assert partner.enabled is False

        partner.disabled = True
        partner.code = 'dohop'
        with patch.object(environment, 'now', return_value=datetime(2017, 1, 16)):
            assert partner.enabled is True

        partner.disabled = True
        partner.code = 'dohop'
        with patch.object(environment, 'now', return_value=datetime(2017, 1, 13)):
            assert partner.enabled is True
