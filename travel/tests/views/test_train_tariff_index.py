# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
from datetime import datetime
from mock import Mock
from urllib import urlencode

from django.conf.urls import url
from django.test import override_settings
from django.test.client import Client

from common.tester.testcase import TestCase
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.train_info_saver import TrainInfoSaver
from travel.rasp.wizards.train_wizard_api.lib.train_facility_fabric import TrainFacilityFabric
from travel.rasp.wizards.train_wizard_api.lib.train_info_parser import TrainInfoParser
from travel.rasp.wizards.train_wizard_api.lib.train_tariff_fabric import TrainTariffFabric
from travel.rasp.wizards.train_wizard_api.views.index_train import IndexTrainView
from travel.rasp.wizards.wizard_lib.tests.helpers.fake_url_conf import FakeUrlConf


class IndexTrainViewTests(TestCase):
    def setUp(self):
        super(TestCase, self).setUp()
        self._fake_parser = Mock(spec=TrainInfoParser)
        self._fake_fabric = Mock(spec=TrainTariffFabric)
        self._fake_saver = Mock(spec=TrainInfoSaver)
        self._fake_facility_fabric = Mock(spec=TrainFacilityFabric)

        self._fake_url_conf = FakeUrlConf([
            url(
                r'^testing_views/train/index$',
                IndexTrainView.as_view(
                    train_info_parser=self._fake_parser,
                    train_tariff_fabric=self._fake_fabric,
                    train_info_saver=self._fake_saver,
                    train_facility_fabric=self._fake_facility_fabric,
                )
            )
        ])

    def _run(self, query_params, json_data=None):
        default_params = {
            'departure_point_express_id': '1',
            'arrival_point_express_id': '10',
            'departure_dt': '2017-09-01T10:20',
            'number': 'some_number',
        }
        default_params.update(query_params)
        if json_data is None:
            json_data = {'coaches': 'my_coaches', 'electronic_ticket': True}

        with override_settings(ROOT_URLCONF=self._fake_url_conf):
            response = Client().post(
                '/testing_views/train/index?' + urlencode(default_params),
                json.dumps(json_data),
                content_type='application/json'
            )

            return response.json(), response.status_code

    def test_broken_departure_point(self):
        response, status_code = self._run({
            'departure_point_express_id': 'cool_point'
        })

        assert status_code == 400
        assert response == {u'departure_point_express_id': [u'Not a valid integer.']}

    def test_broken_arrival_point(self):
        response, status_code = self._run({
            'arrival_point_express_id': 'cool_point'
        })

        assert status_code == 400
        assert response == {u'arrival_point_express_id': [u'Not a valid integer.']}

    def test_broken_departure_dt(self):
        response, status_code = self._run({
            'departure_dt': 'not_valid_date'
        })

        assert status_code == 400
        assert response == {u'departure_dt': [u'Not a valid datetime.']}

    def test_store(self):
        self._fake_parser.parse = Mock(
            return_value='places_info'
        )
        self._fake_fabric.make_tariffs_info = Mock(
            return_value='tariffs_info'
        )
        self._fake_facility_fabric.get_facilities_ids = Mock(
            return_value='facilities_ids'
        )

        response, status_code = self._run({})
        assert status_code == 200
        assert response == 'ok'

        self._fake_parser.parse.assert_called_once_with(
            'my_coaches'
        )
        self._fake_fabric.make_tariffs_info.assert_called_once_with(
            'places_info'
        )
        self._fake_facility_fabric.get_facilities_ids.assert_called_once_with(
            'places_info'
        )

        self._fake_saver.save.assert_called_once_with(
            query={
                'arrival_point_express_id': 10,
                'departure_point_express_id': 1,
                'number': u'some_number',
                'departure_at': datetime(2017, 9, 1, 10, 20)
            },
            tariffs_info='tariffs_info',
            places_info='places_info',
            electronic_ticket=True,
            facilities_ids='facilities_ids'
        )
