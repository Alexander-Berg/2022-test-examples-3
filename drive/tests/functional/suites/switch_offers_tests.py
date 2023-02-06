from base import TestBase
from drive.backend.api import client as api
from utils.args import get_args
from utils.endpoints import get_endpoint
from utils.helpers import *
from utils.wrappers import skip_unless_has_tags
from utils.timeout import timeout

MIN_ZONE = "37.39999771 55.87350464"
CAR_INITIAL_LOCATION = "37.64015961 55.73690033"
FIX_ZONE_IN_MIN_ZONE = "37.3885994 55.8765564"
OFFER_TYPE_FIX = "fix_point"
USER_DESTINATION = "37.64015961 55.75302887"


class SwitchOfferSuite(TestBase):
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

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
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

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_switch_to_fix_in_minimal_zone(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        offer = get_offer(self.client, car)

        reservation(self.client, offer)
        acceptance(self.client)
        riding(self.client)
        set_car_location(self.client, car, MIN_ZONE)

        user_position = f'{car.location.lon} {car.location.lat}'

        offer = get_offer(self.client, car, offer_type=OFFER_TYPE_FIX, user_position=user_position)
        assert offer.constructor_id
        constructor_id = offer.constructor_id
        fix_offer = get_offer(self.client, car, offer_type=OFFER_TYPE_FIX, user_position=user_position,
                              constructor_id=constructor_id, user_destination=FIX_ZONE_IN_MIN_ZONE)

        self.client.switch_offer(fix_offer.id)
        set_car_location(self.client, car, FIX_ZONE_IN_MIN_ZONE)
        set_car_location(self.client, car, MIN_ZONE)
        end_session_with_failure(self.client)
        end_session(self.client, user_choice="accept")
