from search.martylib.core.logging_utils import configure_binlog
from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.test_utils import TestCase

from search.mon.warden.src import const
from search.mon.warden.tests.utils.setup import setup_metrics

configure_binlog(
    'warden',
    loggers=const.LOGGERS,
    stdout=True,
)


class BaseTestCase(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        with session_scope() as session:
            session.execute('alter table "warden__Functionality" ALTER COLUMN  "slug" set DEFAULT NULL;')
            session.execute('alter table "warden__Component" ALTER COLUMN "value_stream_id" set DEFAULT NULL;')

        setup_metrics()
        cls.load_to_db()

    @classmethod
    def tearDownClass(cls):
        clear_db()

    @staticmethod
    def load_to_db():
        pass
