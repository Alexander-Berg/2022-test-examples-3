# -*- coding: utf-8 -*-
from __future__ import absolute_import

from mock import patch

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.transport_models import PlaneTransportModelRepository


class PlaneViewTest(TestCase):
    def setUp(self):
        with patch.object(PlaneTransportModelRepository, '_load_models') as load_models_mock:
            load_models_mock.return_value = self._get_models()
            self._repository = PlaneTransportModelRepository()
            self._repository.pre_cache()

    def _get_models(self):
        # type: () -> list
        t1 = {
            'code': 'CR1',
            'code_en': 'CE1',
            'title': 'Заголовок1',
            'title_en': 'title1',
            'is_cargo': False,
            'is_propeller_flight': False,
            'plane_body_type': 'standard',
            'producer__title': 'Airbus',
        }
        t2 = {
            'code': 'CR2',
            'code_en': 'CE2',
            'title': 'Заголовок2',
            'title_en': 'title2',
            'is_cargo': True,
            'is_propeller_flight': True,
            'plane_body_type': 'standard',
            'producer__title': 'Airbus',
        }

        return [t1, t2]
