# -*- coding: utf-8 -*-
from __future__ import unicode_literals
from datetime import datetime, timedelta

import mock
import pytest

from travel.avia.library.python.tester.factories import create_company, create_settlement
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.avia_api.avia.v1.model.user import User
from travel.avia.avia_api.avia.v1.model.device import Device
from travel.avia.avia_api.avia.v1.model.flight import Flight
from travel.avia.avia_api.avia.v1.model.flight_status import FlightStatusCheckinStarted
from travel.avia.avia_api.avia.lib.pusher import Pusher
from travel.avia.avia_api.avia.lib.push_notifications import CheckinStartPushNotification


class TestCheckinStartedStatus(TestCase):
    @pytest.fixture()
    def clear_users(self):
        Device.objects.delete()
        User.objects.delete()

    @pytest.fixture()
    def clear_flights(self):
        Flight.objects.delete()

    @pytest.fixture()
    def create_good_company(self, request):
        company = create_company(title=u'AAAA', sirena_id=u'AAA')
        company.registration_url = 'http://check-in.airlines.net'
        company.registration_phone = '+9 111(4334)9'
        company.save()
        request.addfinalizer(company.delete)
        self.company = company

    @pytest.fixture()
    def create_bad_company(self, request):
        company = create_company(title=u'AAAA', sirena_id=u'AAA')
        company.save()
        request.addfinalizer(company.delete)
        self.company = company

    @pytest.fixture()
    def create_flight(self, request):
        settlement = create_settlement()
        self.flight = Flight(
            number='SU 11123',
            departure_date=datetime.now() + timedelta(hours=3),
            departure_datetime=datetime.now() + timedelta(hours=3),
            departure=settlement.point_key,
            company_id=self.company.id,
        )
        self.flight.save()
        request.addfinalizer(self.flight.delete)

    @pytest.fixture()
    def create_flight_with_user(self, request):
        settlement = create_settlement()
        self.user = User(uuid='test')
        self.user.save()
        self.device = Device(user=self.user, lang='ru', platform='android', country='RU')
        self.device.save()
        self.flight = Flight(
            number='SU 11123',
            departure_date=datetime.now() + timedelta(hours=3),
            departure_datetime=datetime.now() + timedelta(hours=3),
            departure=settlement.point_key,
            company_id=self.company.id,
        )
        self.flight.save()

        self.user.flights.append(self.flight)
        self.user.save()

        request.addfinalizer(self.flight.delete)
        request.addfinalizer(self.user.delete)

    @pytest.mark.usefixtures('clear_flights',
                             'clear_users',
                             'create_good_company',
                             'create_flight_with_user')
    @mock.patch.object(Pusher, 'push_many')
    def test_should_generate_a_full_notification(self, push_many_mock):
        status = FlightStatusCheckinStarted()
        status.apply_to_flight(self.flight)

        assert push_many_mock.call_count == 1
        args, kwargs = push_many_mock.call_args

        assert not args

        message = 'Зарегистрируйтесь на рейс {}'.format(self.flight.number)
        assert kwargs['users'] == [self.user]
        assert kwargs['devices'] == []
        assert kwargs['push_tag'] == CheckinStartPushNotification.tag
        assert kwargs['message'] == message
        assert kwargs['data'] == {
            'title': message,
            'u': {
                'e': 're',
                'ph': self.company.registration_phone,
                'url': self.company.registration_url,
                'fk': str(self.flight.id),
            }
        }

    @pytest.mark.usefixtures(
        'clear_flights',
        'clear_users',
        'create_bad_company',
        'create_flight_with_user',
    )
    @mock.patch.object(Pusher, 'push_many')
    def test_should_generate_a_small_notification(self, push_many_mock):
        assert len(self.user.flights) == 1

        status = FlightStatusCheckinStarted()
        status.apply_to_flight(self.flight)

        assert push_many_mock.call_count == 1
        args, kwargs = push_many_mock.call_args

        assert not args

        message = 'Зарегистрируйтесь на рейс {}'.format(self.flight.number)
        assert kwargs['users'] == [self.user]
        assert kwargs['devices'] == []
        assert kwargs['push_tag'] == CheckinStartPushNotification.tag
        assert kwargs['message'] == message
        assert kwargs['data'] == {
            'title': message,
            'u': {
                'e': 're',
                'fk': str(self.flight.id),
            }
        }
