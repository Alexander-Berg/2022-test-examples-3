import logging
import time
import unittest

import pytest

from drive.backend.api import client as api
from helpers.helpers import client_initial_state, get_cars, get_car, set_car_location, get_current_session, \
    end_session, acceptance, riding, end_session_and_get_bill, parking, get_mileage, set_mileage, \
    unlock_car, lock_car, get_offer, reservation
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.proxied_backend_api import BackendClientAutotests
from utils.timeout import timeout
from utils.tus import unlock_tus_account

OFFER_TYPE = "fix_point"
TEST_FINISH_AREA = 1223
FINISH_POINT = []
USER_POSITION = "37.63700867 55.74348831"
USER_DESTINATION = "37.635203363 55.745031936"
CAR_INITIAL_LOCATION = "37.64015961 55.73690033"


class TestFixPointOffer(BaseTestClass):

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

    @pytest.fixture(autouse=True)
    def setup_car(self, client):
        cars = get_cars(self.client)
        if cars:
            car = get_car(self.client, cars)
            set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_offer_car(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        assert fix_offer.pack_price
        assert fix_offer.short_description
        assert fix_offer.detailed_description
        assert fix_offer.car_info
        unittest.TestCase().assertIsInstance(fix_offer.finish_area_border, list)
        unittest.TestCase().assertTrue(fix_offer.finish_area_border)
        unittest.TestCase().assertRegex(fix_offer.finish, "(37.[0-9]*\s55.[0-9]*)")
        unittest.TestCase().assertTrue(fix_offer.finish_area, "(37.[0-9]*\s55.[0-9]*)")

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_reservation(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        current_session = get_current_session(self.client)
        unittest.TestCase().assertRegex(current_session.offer.finish, "(37.[0-9]*\s55.[0-9]*)")
        unittest.TestCase().assertTrue(current_session.offer.finish_area, "(37.[0-9]*\s55.[0-9]*)")
        unittest.TestCase().assertIsInstance(current_session.offer.finish_area_border, list)
        unittest.TestCase().assertTrue(current_session.offer.finish_area_border)
        unittest.TestCase().assertIsInstance(current_session.offer.pack_price, int)
        assert current_session.offer.short_description
        assert current_session.offer.detailed_description
        assert current_session.obj
        end_session(self.client)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_reservation_free_time(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        current_session = get_current_session(self.client)
        assert current_session.free_time > 0
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + 10)
        current_session = get_current_session(self.client)
        assert current_session.free_time == 0
        end_session(self.client)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_reservation_over_free_time(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        current_session = get_current_session(self.client)
        assert current_session.free_time > 0
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + 10)
        current_session = get_current_session(self.client)
        assert current_session.offer_state.waiting_price > 0
        assert current_session.offer_state.pack_price_round > 0
        assert current_session.total_price > 0
        unittest.TestCase().assertRegex(current_session.total_price_hr, "\d*\u2009.")
        end_session(self.client)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_riding(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        current_session = get_current_session(self.client)
        assert not current_session.offer_state.waiting_price == 0
        assert current_session.offer_state.pack_price_round > 0
        assert current_session.offer_state.remaining_time > 0
        assert current_session.offer_state.remaining_distance > 0
        assert current_session.total_price > 0
        unittest.TestCase().assertRegex(current_session.total_price_hr, "\d*\u2009.")
        end_session(self.client)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_finish_not_green_zone_riding(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        offer_pack_price = fix_offer.pack_price
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        time.sleep(10)
        current_session = get_current_session(self.client)
        unittest.TestCase().assertEqual(offer_pack_price, current_session.offer.pack_price)
        bill = end_session_and_get_bill(self.client)
        line_riding = [line for line in bill[0] if line.line_type == "old_state_riding"]
        assert len(line_riding) == 1
        for lp in line_riding:
            assert lp.cost > 0, "cost is {}".format(lp.cost)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_finish_not_green_zone_riding_parking(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        time.sleep(10)
        parking(self.client)
        time.sleep(10)
        bill = end_session_and_get_bill(self.client)
        line_riding = [line for line in bill[0] if line.line_type == "old_state_riding"]
        line_parking = [line for line in bill[0] if line.line_type == "old_state_parking"]
        assert len(line_riding) == 1, len(line_riding)
        assert len(line_parking) == 1, len(line_parking)
        for lp in line_riding:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lp in line_parking:
            assert lp.cost > 0, "cost is {}".format(lp.cost)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_finish_green_zone(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        fix_price = fix_offer.pack_price
        reservation(self.client, fix_offer)
        acceptance(self.client)

        riding(self.client)
        set_car_location(self.client, car, USER_DESTINATION)

        bill = end_session_and_get_bill(self.client)
        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        assert len(line_pack) == 1
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        total_cost = sum(i.cost for i in line_total)
        unittest.TestCase().assertAlmostEqual(line_pack[0].cost, fix_price, delta=100)
        unittest.TestCase().assertEqual(line_pack[0].cost, total_cost)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_finish_in_riding_area(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        fix_price = fix_offer.pack_price
        reservation(self.client, fix_offer)
        acceptance(self.client)

        riding(self.client)
        set_car_location(self.client, car, USER_DESTINATION)

        bill = end_session_and_get_bill(self.client)

        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        assert len(line_pack) == 1, f'{len(line_pack)} {",".join(line_pack)}'
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        total_cost = sum(i.cost for i in line_total)
        unittest.TestCase().assertAlmostEqual(line_pack[0].cost, fix_price, delta=100)
        unittest.TestCase().assertEqual(line_pack[0].cost, total_cost)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_overrun(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        fix_price = fix_offer.pack_price
        reservation(self.client, fix_offer)
        acceptance(self.client)

        riding(self.client)
        current_session = get_current_session(self.client)
        remaining_distance = current_session.offer_state.remaining_distance
        assert remaining_distance > 0, "remaining distance is {}".format(remaining_distance)
        logging.info("remaining distance is {}".format(remaining_distance))

        current_mileage = get_mileage(self.client, car)
        set_mileage(self.client, car, current_mileage + remaining_distance + 10)
        current_session = get_current_session(self.client)
        remaining_distance = current_session.offer_state.remaining_distance
        overrun_distance = current_session.offer_state.overrun_distance
        assert overrun_distance > 0, "overrun distance is {}".format(overrun_distance)
        assert remaining_distance == 0, "remaining distance is {}".format(remaining_distance)
        set_car_location(self.client, car, USER_DESTINATION)

        bill = end_session_and_get_bill(self.client)
        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        line_overrun = [line for line in bill[0] if line.line_type == "overrun"]
        assert len(line_pack) == 1
        assert len(line_overrun) == 1

        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        total_cost = sum(i.cost for i in line_total)
        unittest.TestCase().assertAlmostEqual(line_pack[0].cost, fix_price, delta=100)
        unittest.TestCase().assertEqual(line_pack[0].cost + line_overrun[0].cost, total_cost)
        for lp in line_pack:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lo in line_overrun:
            assert lo.cost > 0, "cost is {}".format(lo.cost)

    @pytest.mark.testing
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_overtime(self):
        # step 1: car is booked and passed to the riding state
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              user_destination=USER_DESTINATION)
        fix_price = fix_offer.pack_price
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        # step 2
        current_session = get_current_session(self.client)
        remaining_time = current_session.offer_state.remaining_time
        assert remaining_time > 0, "remaining time is {}".format(remaining_time)
        logging.info("remaining time is {}".format(remaining_time))
        time.sleep(remaining_time + 60)
        # step 3
        current_session = get_current_session(self.client)
        remaining_time = current_session.offer_state.remaining_time
        overrun_time = current_session.offer_state.overrun_time
        assert remaining_time == 0, "remaining time is {}".format(remaining_time)
        assert overrun_time > 0, "overrun time is {}".format(overrun_time)
        # step 4
        set_car_location(self.client, car, USER_DESTINATION)
        bill = end_session_and_get_bill(self.client)
        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        line_overtime = [line for line in bill[0] if line.line_type == "overtime"]
        assert len(line_pack) == 1
        assert len(line_overtime) == 1
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        total_cost = sum(i.cost for i in line_total)
        unittest.TestCase().assertAlmostEqual(line_pack[0].cost, fix_price, delta=100)
        unittest.TestCase().assertEqual(line_pack[0].cost + line_overtime[0].cost, total_cost)
        for lp in line_pack:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lo in line_overtime:
            assert lo.cost > 0, "cost is {}".format(lo.cost)
