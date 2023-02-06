import contextlib

from search.martylib.test_utils import TestCase
from search.martylib.db_utils import prepare_db, clear_db
from search.martylib.protobuf_utils.patch import patch_enums
from search.martylib.core.logging_utils import configure_binlog

from search.morty.src.common import const
from search.morty.src.common.clients.clients import Clients
from search.morty.src.common.lock import create_locks


patch_enums()


class MortyTestCase(TestCase):
    clients = Clients()

    @staticmethod
    @contextlib.contextmanager
    def mock_function(obj: object, function: str, mock: callable):
        tmp = getattr(obj, function)
        setattr(obj, function, mock)
        try:
            yield
        finally:
            setattr(obj, function, tmp)

    @classmethod
    def setUpClass(cls):
        configure_binlog(
            'morty-test',
            loggers=const.LOGGERS,
            indexed_fields=const.INDEX_FIELDS,
        )

        super().setUpClass()
        prepare_db()

    def setUp(self) -> None:
        super().setUp()
        prepare_db()
        create_locks()

    def tearDown(self) -> None:
        super().tearDown()
        clear_db()
        self.clients.unload()


class ControllerTestCase(MortyTestCase):
    def tearDown(self) -> None:
        super().tearDown()
