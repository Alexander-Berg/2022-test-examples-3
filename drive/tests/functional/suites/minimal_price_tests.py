from base import TestBase
from drive.backend.api import client as api
from utils.args import get_args
from utils.endpoints import get_endpoint
from utils.helpers import *
from utils.wrappers import skip_unless_has_tags
from utils.timeout import timeout

MIN_ZONE = "37.39381409 55.87476349"
CAR_INITIAL_LOCATION = "37.64015961 55.73690033"
FIX_ZONE_IN_MIN_ZONE = "37.3885994 55.8765564"
MIN_ZONE_PRICE = 110


class MinimalPriceSuite(TestBase):
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
    def test_minimal_zone_shows_in_operation_area(self):
        pass

    # @case(661)
    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_minimal_zone_showes_landing(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)
        response = end_session_with_failure(self.client)
        assert len(response.landings) == 1

    # @case(681)
    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
            time.sleep(5)
        bill = end_session_and_get_bill(self.client)
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert not len(line_fee)
        set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
        assert sum(i.cost for i in line_total) < MIN_ZONE_PRICE

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
            time.sleep(5)
        bill = end_session_and_get_bill(self.client)
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert not len(line_fee)
        set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
        time.sleep(300)

        # Взять машину в зоне с минимальной ценой
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)
        reservation(self.client, offer)
        bill = end_session_and_get_bill(self.client)

        # Проверить что в чеке нет поля про минимальную цену
        line_fee = [line for line in bill[0] if line.line_type in ["fee_drop_zone_max"]]
        assert len(line_fee) == 0, f'lines with "fee_drop_zone_max" {len(line_fee)}'
