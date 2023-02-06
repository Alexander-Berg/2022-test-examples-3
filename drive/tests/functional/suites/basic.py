import jsonschema

import schemas
from base import TestBase
from drive.backend.api import client as api
from utils.args import get_args
from utils.endpoints import get_endpoint
from utils.helpers import *
from utils.wrappers import skip_unless_has_tags
from utils.timeout import timeout


class ValidateSchemas(TestBase):
    CAR_INITIAL_LOCATION = "37.64015961 55.73690033"
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
    def test_validate_car_list(self):
        cars = self.client.raw_list_cars_user()
        self._validate(cars, schemas.user_app_car_list)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_validate_offers_create(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)
        self._validate(self.client.raw_create_offers(car), schemas.user_app_offers_create)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_validate_user_app_user_session(self):
        user_session = self.client.raw_current_session()
        self._validate(user_session, schemas.user_app_user_session)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_validate_radar_areas(self):
        radar_areas = self.client.radar_areas(lat=55.734, lon=37.5895)
        self._validate(radar_areas, schemas.user_app_radar_areas)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_sessions_history(self):
        sessions = self.client.get_sessions_raw()
        self._validate(sessions, schemas.user_app_sessions_history)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_operation_areas(self):
        areas = self.client.operation_areas_raw()
        self._validate(areas, schemas.user_app_areas_operation_info)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_drop_areas_info(self):
        drop_areas = self.client.get_drop_areas_info_raw()
        self._validate(drop_areas, schemas.user_app_drop_areas)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_chat_history_unread(self):
        chats = self.client.get_chat_history_unread_raw()
        self._validate(chats, schemas.support_api_chat_history_unread)

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_chats_list(self):
        chats = self.client.get_chats_list_raw()
        self._validate(chats, schemas.support_api_chats_list)

    @skip_unless_has_tags(args, ["testing"])
    @timeout(args.timeout_per_test)
    def test_fixpoint_offer(self):
        cars = get_cars(self.client)
        car = get_car(self.client, cars)

        self.client.set_location("55.73690033", "37.64015961")
        set_car_location(self.client, car, self.CAR_INITIAL_LOCATION)
        data = {
            "work": {
                "name": "Работа",
                "coordinate": [
                    37.64084895021607,
                    55.73678588807751
                ]
            },
            "home": {
                "name": "Дом",
                "coordinate": [
                    37.398815000000006,
                    55.876096999999994
                ]
            }
        }
        fix_point_offers = self.client.get_offers_fixpoint(data=data)
        self._validate(fix_point_offers, schemas.user_app_offers_create)

    @staticmethod
    def _validate(instance, schema):
        v = jsonschema.Draft4Validator(schema)
        errors = sorted(v.iter_errors(instance), key=lambda e: e.path)
        if errors:
            messages = []
            for error in errors:
                messages.append(', '.join(str(n) for n in error.schema_path) + str(error.message))
            raise jsonschema.ValidationError("\n".join(messages))
