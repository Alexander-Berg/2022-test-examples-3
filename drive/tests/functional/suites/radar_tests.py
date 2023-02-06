from base import TestBase
from drive.backend.api import client as api
from utils.args import get_args
from utils.endpoints import get_endpoint
from utils.helpers import *
from utils.wrappers import skip_unless_has_tags
from utils.timeout import timeout


class RadarSuite(TestBase):
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

    # id = 242
    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
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

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_radar_start_stop(self):
        remove_debt(self.client)
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
