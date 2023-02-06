from search.mon.workplace.protoc.structures import shift_pb2
from search.mon.workplace.protoc.structures import user_pb2

from search.martylib.db_utils import prepare_db, to_model
from search.martylib.test_utils import TestCase


class TestShifts(TestCase):

    @classmethod
    def setUpClass(cls):
        prepare_db()
        cls.load_to_db()

    @staticmethod
    def load_to_db():
        user = user_pb2.User(login='karasique')
        to_model(
            shift_pb2.Shift(
                user=user,
                shift_time=1,
            )
        )
