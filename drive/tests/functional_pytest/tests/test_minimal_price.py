import logging
import time

import pytest

from drive.backend.api import client as api
from helpers.helpers import client_initial_state, get_cars, get_car, acceptance, \
    riding, \
    set_car_location, end_session_with_failure, get_current_session, end_session, end_session_and_get_bill, \
    lock_car, unlock_car, get_offer, reservation
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.proxied_backend_api import BackendClientAutotests
from utils.timeout import timeout
from utils.tus import unlock_tus_account

MIN_ZONE = "37.39381409 55.87476349"
CAR_INITIAL_LOCATION = "37.64015961 55.73690033"
FIX_ZONE_IN_MIN_ZONE = "37.3885994 55.8765564"
MIN_ZONE_PRICE = 110


class TestMinimalPrice(BaseTestClass):

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
        car = get_car(self.client)
        set_car_location(self.client, car, CAR_INITIAL_LOCATION)
        yield
        unlock_car(self.client)
        client_initial_state(self.client)
        unlock_tus_account(account.uid)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_minimal_zone_shows_in_operation_area(self):
        pass

    # @case(661)
    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_minimal_zone_showes_landing(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        session = get_current_session(self.client)
        total_price = session.total_price
        car_location = session.obj.location
        logging.info(f'car location is:{car_location.lat} {car_location.lon}')
        logging.info(f'total_price is:{total_price}')
        response = end_session_with_failure(self.client)
        assert len(response.landings) == 1

    # @case(681)
    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_minimal_zone_price_more_then_min(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        session = get_current_session(self.client)

        total_price = session.total_price
        while total_price < MIN_ZONE_PRICE:
            session = get_current_session(self.client)
            total_price = session.total_price
            logging.info(f'total_price is:{total_price}')
            time.sleep(5)
        end_session(self.client)

        set_car_location(self.client, car, CAR_INITIAL_LOCATION)

        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        bill = end_session_and_get_bill(self.client, user_choice="accept")
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert not len(line_fee)
        set_car_location(self.client, car, CAR_INITIAL_LOCATION)

        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        session = get_current_session(self.client)
        total_price = session.total_price
        while total_price < MIN_ZONE_PRICE:
            session = get_current_session(self.client)
            total_price = session.total_price
            logging.info(f'total_price is:{total_price}')
            time.sleep(5)
        bill = end_session_and_get_bill(self.client)
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert not len(line_fee)
        set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_drop_zone_max_without_fee(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        response = end_session_with_failure(self.client)
        assert len(response.landings) == 1
        bill = end_session_and_get_bill(self.client, user_choice="accept")
        assert len(bill) == 1
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]

        assert len(line_fee) == 0
        assert sum(i.cost for i in line_total) < 100000

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_riding_in_fee_zone(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        set_car_location(self.client, car, MIN_ZONE)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, FIX_ZONE_IN_MIN_ZONE)
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert len(line_fee) == 0

    # @case(662)
    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_minimal_zone_with_fee(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        response = end_session_with_failure(self.client)
        assert len(response.landings) == 1
        end_session(self.client, user_choice="accept")

        set_car_location(self.client, car, CAR_INITIAL_LOCATION)
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        response = end_session_with_failure(self.client)
        assert len(response.landings) == 1
        bill = end_session_and_get_bill(self.client, user_choice="accept")
        assert len(bill) == 1
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        line_riding = [line for line in bill[0] if line.line_type in ["old_state_reservation", "old_state_riding",
                                                                      "old_state_parking", "old_state_acceptance"]]
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]

        total_cost = sum(i.cost for i in line_total)
        riding_cost = sum(i.cost for i in line_riding)
        fee_cost = sum(i.cost for i in line_fee)

        assert total_cost == MIN_ZONE_PRICE
        assert fee_cost > 0
        assert total_cost == (riding_cost + fee_cost)

    # @case(678)
    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_minimal_zone_second_riding_without_fee(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        response = end_session_with_failure(self.client)
        assert len(response.landings) == 1
        end_session(self.client, user_choice="accept")

        set_car_location(self.client, car, CAR_INITIAL_LOCATION)
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        session = get_current_session(self.client)
        total_price = session.total_price
        while total_price < MIN_ZONE_PRICE:
            session = get_current_session(self.client)
            total_price = session.total_price
            time.sleep(10)
        bill = end_session_and_get_bill(self.client)
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert not len(line_fee)
        set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_minimal_zone_with_fee_after_rent_with_fee(self):

        # Взять машину и завершить в минимальной зоне
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        response = end_session_with_failure(self.client)
        assert len(response.landings) == 1
        end_session(self.client, user_choice="accept")

        # Переместить машину на нанчальную позицию
        set_car_location(self.client, car, CAR_INITIAL_LOCATION)

        # Взять машину и завершить в зоне с минимальной ценой
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        response = end_session_with_failure(self.client)
        assert len(response.landings) == 1
        bill = end_session_and_get_bill(self.client, user_choice="accept")

        # Проверить, что в чеке есть поле с информацией о минимальной цене
        assert len(bill) == 1
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert len(line_fee) == 1, f'lines with "fee_drop_zone_max" {len(line_fee)}'

        # Взять машину в зоне с минимальной ценой
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        bill = end_session_and_get_bill(self.client)

        # Проверить что в чеке нет поля про минимальную цену
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert len(line_fee) == 0, f'lines with "fee_drop_zone_max" {len(line_fee)}'
