from base import TestBase
from drive.backend.api import client as api
from utils.args import get_args
from utils.endpoints import get_endpoint
from utils.helpers import *
from utils.wrappers import skip_unless_has_tags
from utils.timeout import timeout

OFFER_TYPE = "fix_point"
TEST_FINISH_AREA = 1223
FINISH_POINT = []
USER_POSITION = "37.63700867 55.74348831"
USER_DESTINATION = "37.635203363 55.745031936"
CAR_INITIAL_LOCATION = "37.64015961 55.73690033"


class FixPointOfferSuite(TestBase):
    args = get_args()

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.client = api.BackendClient(endpoint=get_endpoint(cls.args.endpoint),
                                        public_token=cls.args.client_public_token,
                                        private_token=cls.args.private_token)

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()

    def setUp(self):
        super().setUp()
        cars = get_cars(self.client)
        if cars:
            car = get_car(self.client, cars)
            set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    def tearDown(self):
        super().tearDown()
        cars = get_cars(self.client)
        if cars:
            car = get_car(self.client, cars)
            set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_offer_car(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        assert fix_offer.pack_price
        assert fix_offer.short_description
        assert fix_offer.detailed_description
        assert fix_offer.car_info
        self.assertIsInstance(fix_offer.finish_area_border, list)
        self.assertTrue(fix_offer.finish_area_border)
        self.assertRegex(fix_offer.finish, "(37.[0-9]*\s55.[0-9]*)")
        self.assertTrue(fix_offer.finish_area, "(37.[0-9]*\s55.[0-9]*)")

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_reservation(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        current_session = get_current_session(self.client)
        self.assertRegex(current_session.offer.finish, "(37.[0-9]*\s55.[0-9]*)")
        self.assertTrue(current_session.offer.finish_area, "(37.[0-9]*\s55.[0-9]*)")
        self.assertIsInstance(current_session.offer.finish_area_border, list)
        self.assertTrue(current_session.offer.finish_area_border)
        self.assertIsInstance(current_session.offer.pack_price, int)
        assert current_session.offer.short_description
        assert current_session.offer.detailed_description
        assert current_session.obj
        end_session(self.client)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_reservation_free_time(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        current_session = get_current_session(self.client)
        assert current_session.free_time > 0
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + 10)
        current_session = get_current_session(self.client)
        assert current_session.free_time == 0
        end_session(self.client)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_reservation_over_free_time(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        reservation(self.client, fix_offer)
        current_session = get_current_session(self.client)
        assert current_session.free_time > 0
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + 10)
        current_session = get_current_session(self.client)
        assert current_session.offer_state.waiting_price > 0
        assert current_session.offer_state.pack_price_round > 0
        assert current_session.total_price > 0
        self.assertRegex(current_session.total_price_hr, "\d*\u2009.")
        end_session(self.client)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_riding(self):
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
        current_session = get_current_session(self.client)
        assert not current_session.offer_state.waiting_price == 0
        assert current_session.offer_state.pack_price_round > 0
        assert current_session.offer_state.remaining_time > 0
        assert current_session.offer_state.remaining_distance > 0
        assert current_session.total_price > 0
        self.assertRegex(current_session.total_price_hr, "\d*\u2009.")
        end_session(self.client)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_finish_not_green_zone_riding(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        offer_pack_price = fix_offer.pack_price
        reservation(self.client, fix_offer)
        acceptance(self.client)
        riding(self.client)
        time.sleep(10)
        current_session = get_current_session(self.client)
        self.assertEqual(offer_pack_price, current_session.offer.pack_price)
        bill = end_session_and_get_bill(self.client)
        line_riding = [line for line in bill[0] if line.line_type == "old_state_riding"]
        assert len(line_riding) == 1
        for lp in line_riding:
            assert lp.cost > 0, "cost is {}".format(lp.cost)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_finish_not_green_zone_riding_parking(self):
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

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_finish_green_zone(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
        fix_price = fix_offer.pack_price
        reservation(self.client, fix_offer)
        acceptance(self.client)

        riding(self.client)
        set_car_location(self.client, car, USER_DESTINATION)

        bill = end_session_and_get_bill(self.client)
        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        assert len(line_pack) == 1, logging.info(len(line_pack), line_pack)
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        total_cost = sum(i.cost for i in line_total)
        self.assertAlmostEqual(line_pack[0].cost, fix_price, delta=100)
        self.assertEqual(line_pack[0].cost, total_cost)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_finish_in_riding_area(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
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
        self.assertAlmostEqual(line_pack[0].cost, fix_price, delta=100)
        self.assertEqual(line_pack[0].cost, total_cost)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_overrun(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
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
        assert len(line_pack) == 1, logging.info(len(line_pack), line_pack)
        assert len(line_overrun) == 1, logging.info(len(line_overrun), line_overrun)

        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        total_cost = sum(i.cost for i in line_total)
        self.assertAlmostEqual(line_pack[0].cost, fix_price, delta=100)
        self.assertEqual(line_pack[0].cost + line_overrun[0].cost, total_cost)
        for lp in line_pack:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lo in line_overrun:
            assert lo.cost > 0, "cost is {}".format(lo.cost)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_overtime(self):
        # step 1: car is booked and passed to the riding state
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE, user_position=USER_POSITION,
                              constructor_id=constructor_id, user_destination=USER_DESTINATION)
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
        assert len(line_pack) == 1, logging.info(len(line_pack), line_pack)
        assert len(line_overtime) == 1, logging.info(len(line_overtime), line_overtime)
        line_total = [line for line in bill[0] if line.line_type in ["total", "billing_bonus"]]
        total_cost = sum(i.cost for i in line_total)
        self.assertAlmostEqual(line_pack[0].cost, fix_price, delta=100)
        self.assertEqual(line_pack[0].cost + line_overtime[0].cost, total_cost)
        for lp in line_pack:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lo in line_overtime:
            assert lo.cost > 0, "cost is {}".format(lo.cost)
