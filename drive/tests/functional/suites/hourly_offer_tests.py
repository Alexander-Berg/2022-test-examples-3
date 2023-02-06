from base import TestBase
from drive.backend.api import client as api
from utils.args import get_args
from utils.endpoints import get_endpoint
from utils.helpers import *
from utils.wrappers import skip_unless_has_tags
from utils.timeout import timeout

OFFER_TYPE = "pack_offer_builder"


class HourlyOfferSuite(TestBase):
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

    def tearDown(self):
        super().tearDown()

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_offer(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, OFFER_TYPE)

        assert offer.riding_discounted > 0
        assert offer.short_description
        assert offer.name
        assert offer.group_name
        assert offer.short_name
        assert offer.pack_price
        assert offer.detailed_description

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_reservation(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, OFFER_TYPE)
        reservation(self.client, offer)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_reservation"
        current_session = get_current_session(self.client)
        remaining_time = current_session.offer_state.remaining_time
        remaining_distance = current_session.offer_state.remaining_distance
        assert remaining_time > 0
        assert remaining_distance > 0
        assert current_session.offer.name
        assert current_session.offer.detailed_description
        assert current_session.offer.short_description
        assert current_session.free_time > 0
        logging.info("waiting for {}".format(current_session.free_time))
        time.sleep(current_session.free_time + remaining_time + 10)
        current_session = get_current_session(self.client)
        remaining_time = current_session.offer_state.remaining_time
        assert remaining_time == 0
        assert current_session.free_time == 0
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1
        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        line_overtime = [line for line in bill[0] if line.line_type == "overtime"]
        assert len(line_pack) == 1
        assert len(line_overtime) == 1
        for lp in line_pack:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lo in line_overtime:
            assert lo.cost > 0, "cost is {}".format(lo.cost)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_acceptance(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, OFFER_TYPE)
        reservation(self.client, offer)
        acceptance(self.client)
        session = get_current_session(self.client)
        assert session
        assert session.current_performing == "old_state_acceptance"
        assert session.free_time > 0
        logging.info("waiting for {}".format(session.free_time))
        end_session(self.client)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_riding(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, OFFER_TYPE)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        current_session = get_current_session(self.client)
        assert current_session.current_performing == "old_state_riding"
        remaining_time = current_session.offer_state.remaining_time
        remaining_distance = current_session.offer_state.remaining_distance
        assert remaining_time > 0
        assert remaining_distance > 0
        assert current_session.offer.name
        assert current_session.offer.detailed_description
        assert current_session.offer.short_description
        time.sleep(remaining_time + 10)

        current_session = get_current_session(self.client)
        remaining_time = current_session.offer_state.remaining_time
        assert remaining_time == 0
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1
        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        line_overtime = [line for line in bill[0] if line.line_type == "overtime"]
        assert len(line_pack) == 1
        assert len(line_overtime) == 1
        for lp in line_pack:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lo in line_overtime:
            assert lo.cost > 0, "cost is {}".format(lo.cost)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_parking(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, OFFER_TYPE)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        parking(self.client)
        current_session = get_current_session(self.client)
        assert current_session.current_performing == "old_state_parking"
        remaining_time = current_session.offer_state.remaining_time
        remaining_distance = current_session.offer_state.remaining_distance
        assert remaining_time > 0, "remaining time is {}".format(remaining_time)
        assert remaining_distance > 0, "remaining distance is {}".format(remaining_distance)
        assert current_session.offer.name
        assert current_session.offer.detailed_description
        assert current_session.offer.short_description
        time.sleep(remaining_time + 10)

        current_session = get_current_session(self.client)
        remaining_time = current_session.offer_state.remaining_time
        assert remaining_time == 0
        time.sleep(60)
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1
        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        line_overtime = [line for line in bill[0] if line.line_type == "overtime"]
        assert len(line_pack) == 1
        assert len(line_overtime) == 1
        for lp in line_pack:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lo in line_overtime:
            assert lo.cost > 0, "cost is {}".format(lo.cost)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_overrun(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, OFFER_TYPE)
        reservation(self.client, offer)
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
        bill = end_session_and_get_bill(self.client)
        assert len(bill) == 1
        line_pack = [line for line in bill[0] if line.line_type == "pack"]
        line_overrun = [line for line in bill[0] if line.line_type == "overrun"]
        assert len(line_pack) == 1
        assert len(line_overrun) == 1
        for lp in line_pack:
            assert lp.cost > 0, "cost is {}".format(lp.cost)
        for lo in line_overrun:
            assert lo.cost > 0, "cost is {}".format(lo.cost)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_debt(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car, offer_type=OFFER_TYPE)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client
               )
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
