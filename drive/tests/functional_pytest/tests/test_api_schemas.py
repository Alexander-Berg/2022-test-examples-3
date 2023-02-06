import logging

import allure
import jsonschema
import pytest

import schemas
from utils.proxied_backend_api import BackendClientAutotests
from helpers.helpers import client_initial_state, get_cars, get_car, set_car_location, lock_car, unlock_car
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.timeout import timeout
from utils.tus import unlock_tus_account


class TestValidateSchemas(BaseTestClass):
    CAR_INITIAL_LOCATION = "37.64015961 55.73690033"

    @classmethod
    def setup_class(cls):
        logging.info("starting class: {} execution".format(cls.__name__))

    @classmethod
    def teardown_class(cls):
        logging.info("starting class: {} execution".format(cls.__name__))

    @pytest.fixture(autouse=True)
    def client(self, endpoint, secret, account):
        self.client = BackendClientAutotests(endpoint,
                                             public_token=get_drive_token(account.login, account.password),
                                             private_token=secret.get("value")["PRIVATE_DRIVE_TOKEN"])
        client_initial_state(self.client)
        lock_car(self.client)
        yield
        unlock_car(self.client)
        client_initial_state(self.client)
        unlock_tus_account(account.uid)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    @allure.feature("Validate")
    def test_validate_car_list(self):
        cars = self.client.raw_list_cars_user()
        self._validate(cars, schemas.user_app_car_list)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_validate_offers_create(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        self._validate(self.client.raw_create_offers(car), schemas.user_app_offers_create)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_validate_user_app_user_session(self):
        user_session = self.client.raw_current_session()
        self._validate(user_session, schemas.user_app_user_session)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_validate_radar_areas(self):
        radar_areas = self.client.radar_areas(lat=55.734, lon=37.5895)
        self._validate(radar_areas, schemas.user_app_radar_areas)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_sessions_history(self):
        sessions = self.client.get_sessions_raw()
        self._validate(sessions, schemas.user_app_sessions_history)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_operation_areas(self):
        areas = self.client.operation_areas_raw()
        self._validate(areas, schemas.user_app_areas_operation_info)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_drop_areas_info(self):
        drop_areas = self.client.get_drop_areas_info_raw()
        self._validate(drop_areas, schemas.user_app_drop_areas)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_chat_history_unread(self):
        chats = self.client.get_chat_history_unread_raw()
        self._validate(chats, schemas.support_api_chat_history_unread)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_chats_list(self):
        chats = self.client.get_chats_list_raw()
        self._validate(chats, schemas.support_api_chats_list)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.testing
    def test_fixpoint_offer(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)

        self.client.set_location("55.73690033", "37.64015961")
        set_car_location(self.client, car, self.CAR_INITIAL_LOCATION)
        data = {
            "work": {
                "name": "Работа",
                "coordinate": [
                    37.64084895021607,
                    55.73678588807751
                ]
            },
            "home": {
                "name": "Дом",
                "coordinate": [
                    37.398815000000006,
                    55.876096999999994
                ]
            }
        }
        fix_point_offers = self.client.get_offers_fixpoint(data=data)
        self._validate(fix_point_offers, schemas.user_app_offers_create)

    @staticmethod
    def _validate(instance, schema):
        v = jsonschema.Draft4Validator(schema)
        errors = sorted(v.iter_errors(instance), key=lambda e: e.path)
        if errors:
            messages = []
            for error in errors:
                messages.append(', '.join(str(n) for n in error.schema_path) + str(error.message))
            raise jsonschema.ValidationError("\n".join(messages))
