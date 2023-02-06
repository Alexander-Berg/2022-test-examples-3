from search.martylib.test_utils import TestCase
from search.mon.warden.sqla.warden.model import Worker
from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.mon.warden.src.services.model import Warden


WARDEN_CLIENT = Warden()


def unistat():
    try:
        return Warden().unistat()
    except Exception as e:
        raise e


class TestUnistat(TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        with session_scope() as session:
            session.add(Worker(name='worker1', last_success_launch=1))
            session.add(Worker(name='worker2', last_success_launch=2))

    @classmethod
    def tearDownClass(cls):
        clear_db()

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_unistat_case(self):
        unistat_data = unistat()
        self.assertNotEqual(unistat_data.numerical['worker-worker1-delay_attt'], 0)
        self.assertNotEqual(unistat_data.numerical['worker-worker2-delay_attt'], 0)
