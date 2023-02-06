import logging
import time
import unittest

import pytest

from drive.backend.api import client as api
from helpers.helpers import client_initial_state, get_cars, get_car, set_car_location, get_offer, \
    acceptance, \
    riding, wait_for_future_car, end_session, get_current_session, unlock_car, \
    lock_car, reservation
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.proxied_backend_api import BackendClientAutotests
from utils.timeout import timeout
from utils.tus import unlock_tus_account

OFFER_TYPE = "fix_point"
USER_POSITION = "37.63700867 55.74348831"
USER_DESTINATION = "37.635203363 55.745031936"
CAR_INITIAL_LOCATION = "37.64015961 55.73690033"
KREMLIN = "37.62242126 55.75117493"


class TestFutureCar(BaseTestClass):

    @classmethod
    def setup_class(cls):
        logging.info("starting class: {} execution".format(cls.__name__))

    @classmethod
    def teardown_class(cls):
        logging.info("starting class: {} execution".format(cls.__name__))

    @pytest.fixture(autouse=True)
    def client(self, endpoint, secret, account):
        self.client = BackendClientAutotests(endpoint, public_token=get_drive_token(account.login, account.password),
                                        private_token=secret.get("value")["PRIVATE_DRIVE_TOKEN"], login=account.login)
        client_initial_state(self.client)
        lock_car(self.client)
        yield
        unlock_car(self.client)
        client_initial_state(self.client)
        unlock_tus_account(account.uid)

    @pytest.fixture(autouse=True)
    def client_helper(self, endpoint, secret, helper_account):
        self.client_helper = BackendClientAutotests(endpoint, public_token=get_drive_token(helper_account.login,
                                                                                      helper_account.password),
                                               private_token=secret.get("value")["PRIVATE_DRIVE_TOKEN"],
                                               login=helper_account.login)
        client_initial_state(self.client)
        yield
        unlock_tus_account(helper_account.uid)

    @pytest.fixture
    def set_initial_car_location(self):
        cars = get_cars(self.client)
        if cars:
            car = get_car(self.client, cars)
            set_car_location(self.client, car, CAR_INITIAL_LOCATION)
        yield
        cars = get_cars(self.client)
        if cars:
            car = get_car(self.client, cars)
            set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_future_car(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, USER_DESTINATION)
        unlock_car(self.client)

        car = wait_for_future_car(self.client_helper)
        assert car.futures_location["lat"]
        assert car.futures_location["lon"]
        assert car.futures_location["duration"]
        assert car.futures_location["duration_hr"]
        end_session(self.client)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_future_car_offer(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, USER_DESTINATION)
        unlock_car(self.client)

        car = wait_for_future_car(self.client_helper)
        offer = get_offer(self.client_helper, car, user_position=USER_POSITION)
        assert offer.car_info.futures_location["lat"]
        assert offer.car_info.futures_location["lon"]
        assert offer.car_info.futures_location["duration"]
        assert offer.car_info.futures_location["duration_hr"]
        unittest.TestCase().assertRegex(offer.car_info.futures_location["area"], "(37.[0-9]*\s55.[0-9]*)")

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_future_car_book(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, USER_DESTINATION)
        unlock_car(self.client)
        car = wait_for_future_car(self.client_helper)
        offer = get_offer(self.client_helper, car, user_position=USER_POSITION)
        reservation(self.client_helper, offer)
        time.sleep(5)

        session = get_current_session(self.client_helper)
        assert session.futures_offer.car_waiting_duration_hr
        assert session.futures_offer.car_waiting_duration > 0
        assert session.obj.futures_location["lat"]
        assert session.obj.futures_location["lon"]
        assert session.obj.futures_location["duration"]
        assert session.obj.futures_location["duration_hr"]
        unittest.TestCase().assertRegex(session.obj.futures_location["area"], "(37.[0-9]*\s55.[0-9]*)")

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_future_car_green_zone_finish(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, USER_DESTINATION)
        unlock_car(self.client)
        car = wait_for_future_car(self.client_helper)
        offer = get_offer(self.client_helper, car, user_position=USER_POSITION)
        reservation(self.client_helper, offer)
        set_car_location(self.client, car, USER_DESTINATION)
        end_session(self.client)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_future_car_not_green_zone_finish(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)

        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, USER_DESTINATION)
        unlock_car(self.client)
        car = wait_for_future_car(self.client_helper)
        offer = get_offer(self.client_helper, car, user_position=USER_POSITION)
        reservation(self.client_helper, offer)

        set_car_location(self.client, car, KREMLIN)
        end_session(self.client)
        time.sleep(30)

        session_client_helper = get_current_session(self.client_helper)

        assert session_client_helper.futures_offer_failed
        tag_id = session_client_helper.futures_offer_tag_id
        self.client_helper.drop_future_car(tag_id)

        session_client_helper = get_current_session(self.client_helper)
        assert not session_client_helper.futures_offer_failed
