from base import TestBase
from drive.backend.api import client as api
from utils.args import get_args
from utils.endpoints import get_endpoint
from utils.helpers import *
from utils.wrappers import skip_unless_has_tags
from utils.timeout import timeout

OFFER_TYPE = "fix_point"
USER_POSITION = "37.63700867 55.74348831"
USER_DESTINATION = "37.635203363 55.745031936"
CAR_INITIAL_LOCATION = "37.64015961 55.73690033"
KREMLIN = "37.62242126 55.75117493"


class FutureCarSuite(TestBase):
    args = get_args()

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.client = api.BackendClient(endpoint=get_endpoint(cls.args.endpoint),
                                        public_token=cls.args.client_public_token,
                                        private_token=cls.args.private_token)
        cls.client_helper = api.BackendClient(endpoint=get_endpoint(cls.args.endpoint),
                                               public_token=cls.args.client_helper_public_token,
                                               private_token=cls.args.private_token)

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()

    def setUp(self):
        super().setUp()
        self.client.set_location("55.735596", "37.642453")
        self.client_helper.set_location("55.735596", "37.642453")
        client_initial_state(self.client)
        client_initial_state(self.client_helper)
        cars = get_cars(self.client)
        if cars:
            car = get_car(self.client, cars)
            set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    def tearDown(self):
        super().tearDown()
        client_initial_state(self.client)
        client_initial_state(self.client_helper)
        cars = get_cars(self.client)
        if cars:
            car = get_car(self.client, cars)
            set_car_location(self.client, car, CAR_INITIAL_LOCATION)
        cars = get_cars(self.client)
        if cars:
            car = get_car(self.client, cars)
            set_car_location(self.client, car, CAR_INITIAL_LOCATION)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_future_car(self):
        print(self.client._lat)
        print(self.client._lon)
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

        car = wait_for_future_car(self.client_helper)
        assert car.futures_location["lat"]
        assert car.futures_location["lon"]
        assert car.futures_location["duration"]
        assert car.futures_location["duration_hr"]
        assert not car.location

        end_session(self.client)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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

        car = wait_for_future_car(self.client_helper)
        offer = get_offer(self.client_helper, car, user_position=USER_POSITION)
        assert offer.car_info.futures_location["lat"]
        assert offer.car_info.futures_location["lon"]
        assert offer.car_info.futures_location["duration"]
        assert offer.car_info.futures_location["duration_hr"]
        self.assertRegex(offer.car_info.futures_location["area"], "(37.[0-9]*\s55.[0-9]*)")
        assert not offer.car_info.location

        end_session(self.client)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
        self.assertRegex(session.obj.futures_location["area"], "(37.[0-9]*\s55.[0-9]*)")
        assert not session.obj.location

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
        car = wait_for_future_car(self.client_helper)
        offer = get_offer(self.client_helper, car, user_position=USER_POSITION)
        reservation(self.client_helper, offer)
        set_car_location(self.client, car, USER_DESTINATION)
        end_session(self.client)

        time.sleep(5)
        session_client_helper = get_current_session(self.client_helper)

        assert session_client_helper.current_performing == "old_state_reservation"

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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
