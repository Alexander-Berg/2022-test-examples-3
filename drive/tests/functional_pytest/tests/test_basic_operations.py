import logging
import time

import pytest

from drive.backend.api import client as api
from helpers.helpers import client_initial_state, get_cars, get_car, get_current_session, \
    end_session, acceptance, riding, parking, expect_http_code, heating, get_bill, create_debt, check_debt, remove_debt, \
    unlock_car, lock_car, get_offer, reservation
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.proxied_backend_api import BackendClientAutotests
from utils.timeout import timeout
from utils.tus import unlock_tus_account


class TestBasicOperations(BaseTestClass):
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
                                             private_token=secret.get("value")["PRIVATE_DRIVE_TOKEN"]
                                             )
        client_initial_state(self.client)
        lock_car(self.client)
        yield
        unlock_car(self.client)
        client_initial_state(self.client)
        unlock_tus_account(account.uid)

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_reservation(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_reservation"
        end_session(self.client)
        is_cancelled = get_current_session(self.client).is_cancelled
        assert is_cancelled

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_acceptance(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_acceptance"
        end_session(self.client)

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
        end_session(self.client)

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
        end_session(self.client)

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_riding_from_parking(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        parking(self.client)
        riding(self.client)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_riding"
        end_session(self.client)

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_flash(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        expect_http_code(lambda: self.client.action_flash(), 200)
        end_session(self.client)

    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    @pytest.mark.prod
    def test_heating(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        heating(self.client)
        while True:
            session = get_current_session(self.client)
            print(session.is_engine_on)
            if session.is_engine_on:
                assert session.is_engine_on
                break
            time.sleep(4)
        end_session(self.client)

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_offer_prices(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)

        offer = get_offer(self.client, car)
        offer_riding = offer.riding_price
        offer_parking = offer.parking_price

        reservation(self.client, offer)
        session = get_current_session(self.client)
        assert offer_parking == session.offer.parking_price
        assert offer_riding == session.offer.riding_price

        acceptance(self.client)
        riding(self.client)

        parking(self.client)
        session = get_current_session(self.client)
        assert offer_parking == session.offer.parking_price
        assert offer_riding == session.offer.riding_price
        end_session(self.client)

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_session_history(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        time.sleep(30)
        parking(self.client)
        time.sleep(30)
        end_session(self.client)

        session = get_current_session(self.client)
        session_id = session.id
        session_history = self.client.get_session(session_id)
        assert session_history.sessions[0].session_id
        assert session_history.sessions[0].start
        assert session_history.sessions[0].finish
        bill = get_bill(self.client, session_id)
        assert len(bill) == 1
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        line_riding = [line for line in bill[0] if line.line_type in ["old_state_reservation", "old_state_riding",
                                                                      "old_state_parking", "old_state_acceptance"]]
        total_cost = sum(i.cost for i in line_total)
        riding_cost = sum(i.cost for i in line_riding)
        assert total_cost == riding_cost

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_debt(self):
        create_debt(self.client)
        time.sleep(10)
        while True:
            debt = check_debt(self.client)
            if debt:
                break
            time.sleep(10)
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        expect_http_code(lambda: self.client.accept_offer(offer), 402)
        remove_debt(self.client)
