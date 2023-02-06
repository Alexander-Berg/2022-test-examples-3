import logging

import pytest

from drive.backend.api import client as api
from helpers.helpers import client_initial_state, get_cars, get_car, get_current_session, \
    acceptance, riding, parking, end_session, set_car_location, end_session_and_get_bill, end_session_with_failure, \
    lock_car, unlock_car, get_offer, reservation
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.proxied_backend_api import BackendClientAutotests
from utils.timeout import timeout
from utils.tus import unlock_tus_account

MIN_ZONE = "37.39999771 55.87350464"
CAR_INITIAL_LOCATION = "37.64015961 55.73690033"
FIX_ZONE_IN_MIN_ZONE = "37.3885994 55.8765564"
OFFER_TYPE_FIX = "fix_point"
USER_DESTINATION = "37.64015961 55.75302887"


class TestSwitchOffer(BaseTestClass):

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

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_offer_is_switchable(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        current_session = get_current_session(self.client)
        assert current_session.offer.switchable
        acceptance(self.client)
        riding(self.client)
        current_session = get_current_session(self.client)
        assert current_session.offer.switchable
        parking(self.client)
        current_session = get_current_session(self.client)
        assert current_session.offer.switchable
        end_session(self.client)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_switch_from_min_to_fix(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)

        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)

        user_position = f'{car.location.lon} {car.location.lat}'

        offer = get_offer(self.client, car, offer_type=OFFER_TYPE_FIX, user_position=user_position)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE_FIX, user_position=user_position,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)

        self.client.switch_offer(fix_offer.id)

        current_session = get_current_session(self.client)
        assert current_session.offer.constructor_id == constructor_id
        set_car_location(self.client, car, USER_DESTINATION)
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 2

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_switch_to_fix_in_minimal_zone(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)

        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)

        user_position = f'{car.location.lon} {car.location.lat}'
        fix_offer = get_offer(self.client, car,
                              offer_type=OFFER_TYPE_FIX,
                              user_position=user_position,
                              user_destination=FIX_ZONE_IN_MIN_ZONE)

        self.client.switch_offer(fix_offer.id)
        end_session_with_failure(self.client)
        end_session(self.client, user_choice="accept")
