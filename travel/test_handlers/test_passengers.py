# -*- encoding: utf-8 -*-
import json

import pytest

from travel.avia.travelers.application.schemas import Gender
from travel.avia.travelers.tests.conftest import USER_UID
from travel.avia.travelers.tests.custom_faker import faker
from travel.avia.travelers.tests.utils import random_enum, update_params_for_method


@pytest.mark.gen_test
def test_get(http_client, base_url, header):
    path = '/travelers/{}/passengers'.format(USER_UID)
    response = yield http_client.fetch(base_url + path, headers=header)

    assert response.code == 200, response.body


class RequestTester:
    def __init__(self, fixture_store):
        self._fixture_store = fixture_store
        self.faker = self.get_fixture('custom_faker')
        self.http_client = self.get_fixture('http_client')
        self.base_url = self.get_fixture('base_url')
        self.header = self.get_fixture('header')
        self.feature_flag_client = self.get_fixture('feature_flag_client')
        self.fixture_flag_storage = self.get_fixture('fixture_flag_storage')
        self.passenger_param = {}

    def get_fixture(self, name):
        return self._fixture_store.getfixturevalue(name)

    def get(self):
        path = '/travelers/{}/passengers/{}'.format(USER_UID, faker.uuid4())
        return self.http_client.fetch(
            self.base_url + path,
            headers=self.header,
            raise_error=False,
        )

    def post(self):
        path = '/travelers/{}/passengers'.format(USER_UID)
        return self.http_client.fetch(
            self.base_url + path,
            method='POST',
            headers=self.header,
            raise_error=False,
            body=json.dumps(self.passenger_param),
        )

    def setup_test_for_post(self, **kwargs):
        self.passenger_param = self.get_passenger_param(kwargs)

    def enable_itn(self):
        self.feature_flag_client.update_flags({'TR_ENABLE_ITN_DOCUMENT'})
        self.fixture_flag_storage.reset_context()

    @update_params_for_method
    def get_passenger_param(self):
        return {
            'title': self.faker.pystr(),
            'gender': random_enum(Gender).value,
            'birth_date': self.faker.date(),
            'phone': self.faker.phone_number(),
            'email': self.faker.email(),
            'created_at': self.faker.date(),
            'updated_at': self.faker.date(),
            'itn': str(faker.random_number(digits=12, fix_len=True)),
        }


@pytest.mark.gen_test
def test_get_one(request):
    tester = RequestTester(request)
    response = yield tester.get()

    assert response.code == 200, response.body


@pytest.mark.gen_test
def test_post(request):
    tester = RequestTester(request)
    tester.setup_test_for_post()
    response = yield tester.post()

    assert response.code == 200, response.body


@pytest.mark.gen_test
def test_post_with_itn(request, data_sync_client):
    tester = RequestTester(request)

    itn = str(faker.random_number(digits=12, fix_len=True))
    tester.setup_test_for_post(itn=itn)

    response = yield tester.post()

    assert response.code == 200, response.body
    assert data_sync_client.saved['passenger'].itn == itn


@pytest.mark.gen_test
def test_itn_is_too_long(request, data_sync_client):
    perform_post_with_itn_validation(
        str(faker.random_number(digits=13, fix_len=True)),
        'ITN must contain 12 digits.', request
    )


@pytest.mark.gen_test
def test_itn_is_too_short(request, data_sync_client):
    perform_post_with_itn_validation(
        str(faker.random_number(digits=11)),
        'ITN must contain 12 digits.', request
    )


@pytest.mark.gen_test
def test_itn_is_string(request, data_sync_client):
    perform_post_with_itn_validation(
        faker.random_number(),
        'Not a valid string.', request
    )


def perform_post_with_itn_validation(itn, error_message, request):
    tester = RequestTester(request)

    tester.setup_test_for_post(itn=itn)
    response = yield tester.post()

    assert response.code == 400, 'error for itn %s: %s' % (itn, response.body)
    assert json.loads(response.body.decode('utf8')) == {'itn': [error_message]}, 'error for itn %s' % itn


@pytest.mark.gen_test
def test_post_without_itn(request, data_sync_client):
    tester = RequestTester(request)

    tester.setup_test_for_post()
    del tester.passenger_param['itn']
    response = yield tester.post()

    assert response.code == 200, response.body
    assert data_sync_client.saved['passenger'].itn is None


@pytest.mark.gen_test
def test_post_with_train_notifications_enabled(request, data_sync_client):
    tester = RequestTester(request)

    tester.setup_test_for_post(train_notifications_enabled=True)

    response = yield tester.post()

    assert response.code == 200, response.body
    assert data_sync_client.saved['passenger'].attributes['train_notifications_enabled'] is True


@pytest.mark.gen_test
def test_post_with_train_notifications_not_enabled(request, data_sync_client):
    tester = RequestTester(request)

    tester.setup_test_for_post(train_notifications_enabled=False)

    response = yield tester.post()

    assert response.code == 200, response.body
    assert data_sync_client.saved['passenger'].attributes['train_notifications_enabled'] is False
