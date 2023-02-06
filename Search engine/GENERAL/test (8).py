from search.martylib.core.logging_utils import configure_binlog
from search.martylib.db_utils import clear_db, prepare_db
from search.martylib.test_utils import TestCase

from search.stoker.sqla.stoker import model  # noqa
from search.stoker.src.stoker_model_lib import Model


class ModelTestCase(TestCase):
    ADDITIONAL_LOGGERS = {'stoker'}

    @classmethod
    def setUpClass(cls):
        super(ModelTestCase, cls).setUpClass()

        configure_binlog(
            'stoker',
            loggers={'stoker'},
            stdout=True,
        )

        cls.model = Model()

    def setUp(self):
        clear_db()
        prepare_db()
        self.model.create_necessary_rows()
