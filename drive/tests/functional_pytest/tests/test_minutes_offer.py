import logging
import time

import pytest

from drive.backend.api import client as api
from helpers.helpers import client_initial_state, get_cars, get_car, get_current_session, \
    end_session, end_session_and_get_bill, acceptance, riding, parking, create_debt, check_debt, expect_http_code, \
    remove_debt, lock_car, unlock_car, get_offer, reservation
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.proxied_backend_api import BackendClientAutotests
from utils.timeout import timeout
from utils.tus import unlock_tus_account


class TestMinutesOffer(BaseTestClass):

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

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_offer_car(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)

        assert offer.riding_discounted > 0
        assert offer.short_description
        assert offer.name

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_reservation_free_time(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        current_session = get_current_session(self.client)
        assert current_session.free_time > 0
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + 10)
        current_session = get_current_session(self.client)
        assert current_session.free_time == 0
        end_session(self.client)

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_reservation_over_free_time(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        current_session = get_current_session(self.client)
        assert current_session.free_time > 0
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + 10)
        current_session = get_current_session(self.client)
        assert current_session.free_time == 0
        total_price = current_session.total_price
        time.sleep(60)
        session = get_current_session(self.client)
        total_price_new = session.total_price
        assert total_price_new > total_price
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1
        total_cost = 0
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        assert (i for i in line_total)
        for bill_line in bill[0]:
            if bill_line.line_type in ["old_state_reservation", "old_state_riding", "old_state_parking",
                                       "old_state_acceptance"]:
                assert bill_line.cost > 0, "cost is {}".format(bill_line.cost)
                assert bill_line.duration > 0, "duration is {}".format(bill_line.duration)
            if bill_line.line_type in ["total", "billing_bonus"]:
                total_cost += bill_line.cost
                assert bill_line.cost >= 0

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_acceptance_free_time(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        session = get_current_session(self.client)
        assert session, ("{}".format(session))
        assert session.current_performing == "old_state_acceptance", ("{}".format(session.current_performing))
        current_session = get_current_session(self.client)
        assert current_session.free_time > 0
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + 10)
        current_session = get_current_session(self.client)
        assert current_session.free_time == 0
        end_session(self.client)

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_acceptance_over_free_time(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_acceptance", session.current_performing
        current_session = get_current_session(self.client)
        assert current_session.free_time > 0, current_session.free_time
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + 10)
        current_session = get_current_session(self.client)
        assert current_session.free_time == 0, current_session.free_time
        session = get_current_session(self.client)
        total_price = session.total_price
        time.sleep(60)
        session = get_current_session(self.client)
        total_price_new = session.total_price
        assert total_price_new > total_price, f'{total_price_new} > {total_price}'
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1, bill
        total_cost = 0
        for bill_line in bill[0]:
            if bill_line.line_type in ["old_state_reservation", "old_state_riding", "old_state_parking",
                                       "old_state_acceptance"]:
                assert bill_line.cost > 0, "cost is {}".format(bill_line.cost)
                assert bill_line.duration > 0, "duration is {}".format(bill_line.duration)
            if bill_line.line_type in ["total", "billing_bonus"]:
                total_cost += bill_line.cost
                assert bill_line.cost >= 0

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_riding(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_riding"
        total_price = session.total_price
        time.sleep(60)
        session = get_current_session(self.client)
        assert total_price < session.total_price
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1
        total_cost = 0
        for bill_line in bill[0]:
            if bill_line.line_type in ["old_state_reservation",
                                       "old_state_riding",
                                       "old_state_parking",
                                       "old_state_acceptance"]:
                assert bill_line.cost > 0, "cost is {}".format(bill_line.cost)
                assert bill_line.duration > 0, "duration is {}".format(bill_line.duration)
            if bill_line.line_type in ["total", "billing_bonus"]:
                total_cost += bill_line.cost
                assert bill_line.cost >= 0

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_parking(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        parking(self.client)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_parking"
        total_price = session.total_price
        time.sleep(60)
        session = get_current_session(self.client)
        assert total_price < session.total_price
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1
        total_cost = 0
        for bill_line in bill[0]:
            if bill_line.line_type in ["old_state_reservation", "old_state_riding", "old_state_parking",
                                       "old_state_acceptance"]:
                assert bill_line.cost > 0, "cost is {}".format(bill_line.cost)
                assert bill_line.duration > 0, "duration is {}".format(bill_line.duration)
            if bill_line.line_type in ["total", "billing_bonus"]:
                total_cost += bill_line.cost
                assert bill_line.cost >= 0

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_cost_price(self):
        parking_time = 120
        riding_time = 120
        delta = 1000

        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        offer_riding = offer.riding_price
        offer_parking = offer.parking_price
        reservation(self.client, offer)
        acceptance(self.client)

        start_riding_time = time.time()
        riding(self.client)
        time.sleep(riding_time)

        start_parking_time = time.time()
        parking(self.client)
        stop_riding_time = time.time()
        time.sleep(parking_time)

        bill = end_session_and_get_bill(self.client)
        stop_parking_time = time.time()
        assert len(bill) == 1

        expected_riding_cost = round(offer_riding * ((stop_riding_time - start_riding_time) / 60)) + delta
        expected_parking_cost = round(offer_parking * ((stop_parking_time - start_parking_time) / 60)) + delta
        expected_total_cost = expected_parking_cost + expected_riding_cost + delta

        last_session_bonuses = 0
        last_session_total_cost = 0
        last_session_riding_cost = 0
        last_session_parking_cost = 0
        for bill_line in bill[0]:
            if bill_line.line_type == "old_state_parking":
                last_session_parking_cost = bill_line.cost
            if bill_line.line_type == "old_state_riding":
                last_session_riding_cost = bill_line.cost
            if bill_line.line_type == "total":
                last_session_total_cost = bill_line.cost
            if bill_line.line_type == "billing_bonus":
                last_session_bonuses = bill_line.cost

        assert expected_riding_cost >= last_session_riding_cost >= offer_riding * (riding_time / 60) > 0, \
            ("expected riding should be between: {} and {} actual riding cost is: {}"
             .format(expected_riding_cost, offer_riding * (riding_time / 60), last_session_riding_cost))

        assert expected_parking_cost >= last_session_parking_cost >= offer_parking * (parking_time / 60) > 0, \
            ("expected parking cost should be between: {} and {} actual parking cost is: {}".
             format(expected_parking_cost, offer_parking * (parking_time / 60), last_session_parking_cost))

        assert expected_total_cost >= (last_session_total_cost + last_session_bonuses) > 0, \
            ("expected total cost is:{} actual total cost is: {}"
             .format(expected_total_cost, (last_session_total_cost + last_session_bonuses)))

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_debt(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        parking(self.client)
        create_debt(self.client)
        while True:
            debt = check_debt(self.client)
            if debt:
                break
            time.sleep(10)
        expect_http_code(lambda: self.client.evolve_to_riding(), 402)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_parking"
        end_session(self.client)
        remove_debt(self.client)
