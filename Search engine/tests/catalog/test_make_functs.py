from search.mon.workplace.protoc.structures import catalog_pb2

from search.martylib.core.exceptions import ValidationError
from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.test_utils import TestCase
from search.mon.workplace.src.libs.catalog.catalog_functs import _make_functs
from search.mon.workplace.src.sqla.workplace.model import CatalogFunctionals


@TestCase.require_postgres
class TestWorkplaceMakeFuncts(TestCase):
    @classmethod
    def setUpClass(cls):
        clear_db()
        prepare_db()

    @classmethod
    def tearDownClass(cls):
        with session_scope() as session:
            session.query(CatalogFunctionals).delete(synchronize_session=False)

    def test_typical_request(self):
        request = catalog_pb2.Functionality(
            name='Test',
            weight=0.1,
            comment='No',
            vertical='mail',
            id=1,
            disaster_url='yasm.yandex-team.ru/',
            absolute_chart_url='yasm.yandex-team.ru/',
            coefficient=0,
            type=catalog_pb2.ValueType['UNSET']
        )

        response = _make_functs(request, None)

        self.assertEqual(response.disaster_url, '')
        self.assertEqual(response.absolute_chart_url, '')
        self.assertEqual(response.coefficient, 0.0)
        self.assertEqual(response.weight, 0.1)

    def test_bad_weight_request(self):
        request = catalog_pb2.Functionality(
            name='Test1',
            weight=-1,
            comment='No',
            vertical='mail',
            id=2,
            disaster_url='yasm.yandex-team.ru/',
            absolute_chart_url='yasm.yandex-team.ru/',
            coefficient=0,
            type=catalog_pb2.ValueType[catalog_pb2.ValueType.UNSET]
        )

        with self.assertRaisesWithMessage(ValidationError, message='weight should be between [0;1]'):
            _make_functs(request, None)

    def test_err_http_perc_type_request(self):
        request = catalog_pb2.Functionality(
            name='Test2',
            weight=0,
            comment='No',
            vertical='mail',
            id=3,
            disaster_url='yasm.yandex-team.ru/',
            absolute_chart_url='yasm.yandex-team.ru/',
            coefficient=0,
            type=catalog_pb2.ValueType[catalog_pb2.ValueType.HTTP_ERR_PERC]
        )

        response = _make_functs(request, None)

        self.assertEqual(response.disaster_url, request.disaster_url)
        self.assertEqual(response.absolute_chart_url, '')

    def test_err_http_abs_request(self):
        request = catalog_pb2.Functionality(
            name='Test3',
            weight=0,
            comment='No',
            vertical='mail',
            id=4,
            disaster_url='yasm.yandex-team.ru/',
            absolute_chart_url='yasm.yandex-team.ru/',
            coefficient=0,
            type=catalog_pb2.ValueType[catalog_pb2.ValueType.HTTP_ERR_ABS]
        )

        response = _make_functs(request, None)

        self.assertEqual(response.disaster_url, request.disaster_url)
        self.assertEqual(response.absolute_chart_url, request.absolute_chart_url)

    def test_make_with_description(self):
        request = catalog_pb2.Functionality(
            name='Test4',
            weight=0,
            comment='No',
            vertical='mail',
            id=5,
            description='    name  '
        )

        response = _make_functs(request, None)

        self.assertEqual(response.description, 'name')

    def test_make_with_empty_description(self):
        request = catalog_pb2.Functionality(
            name='Test5',
            weight=0,
            comment='No',
            vertical='mail',
            id=6,
        )

        response = _make_functs(request, None)

        self.assertEqual(response.description, '')
