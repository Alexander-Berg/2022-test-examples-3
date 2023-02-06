import logging
import time

import pytest

from drive.backend.api import client as api
from helpers.helpers import client_initial_state, get_cars, get_car, get_current_session, lock_car, unlock_car
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.proxied_backend_api import BackendClientAutotests
from utils.timeout import timeout
from utils.tus import unlock_tus_account


class TestRadar(BaseTestClass):

    @classmethod
    def setup_class(cls):
        logging.info("starting class: {} execution".format(cls.__name__))

    @classmethod
    def teardown_class(cls):
        logging.info("starting class: {} execution".format(cls.__name__))

    @pytest.fixture(autouse=True)
    def client(self, endpoint, secret, account):
        self.client = BackendClientAutotests(endpoint, public_token=get_drive_token(account.login, account.password),
                                        private_token=secret.get("value")["PRIVATE_DRIVE_TOKEN"])
        client_initial_state(self.client)
        lock_car(self.client)
        yield
        unlock_car(self.client)
        client_initial_state(self.client)
        unlock_tus_account(account.uid)

    # id = 242
    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_radar_auto_order(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        self.client.radar_start(car.location.lat, car.location.lon)
        current_performing = get_current_session(self.client).current_performing
        while current_performing != 'old_state_reservation':
            time.sleep(5)
            current_performing = get_current_session(self.client).current_performing
        current_session = get_current_session(self.client)
        assert current_session.offer.from_scanner is True

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_radar_start_stop(self):
        lat = 55.75322
        lon = 37.622513
        walking_time = 300
        livetime = 2100

        self.client.radar_start(lat=lat, lon=lon, walking_time=walking_time, livetime=livetime)
        time.sleep(10)
        assert get_current_session(self.client).user.scanner.lat == lat
        assert get_current_session(self.client).user.scanner.lon == lon
        assert get_current_session(self.client).user.scanner.walk_duration == walking_time
        assert get_current_session(self.client).user.scanner.deadline == (
            livetime + get_current_session(self.client).user.scanner.start)
        self.client.radar_stop()
        assert not get_current_session(self.client).user.scanner
