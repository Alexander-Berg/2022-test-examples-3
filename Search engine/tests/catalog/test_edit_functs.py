from search.mon.workplace.protoc.structures import catalog_pb2

from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.test_utils import TestCase
from search.mon.workplace.src.libs.catalog.catalog_functs import _edit_functs, _make_functs
from search.mon.workplace.src.sqla.workplace.model import CatalogFunctionals


@TestCase.require_postgres
class TestWorkplaceEditFuncts(TestCase):
    @classmethod
    def setUpClass(cls):
        clear_db()
        prepare_db()
        cls.load_to_db()

    @classmethod
    def tearDownClass(cls):
        with session_scope() as session:
            session.query(CatalogFunctionals).delete(synchronize_session=False)

    @staticmethod
    def load_to_db():
        fuctionality_list = catalog_pb2.FunctionalityList(objects=[
            catalog_pb2.Functionality(
                name='Test1',
                weight=0.1,
                comment='No',
                vertical='mail',
            ),
            catalog_pb2.Functionality(
                name='Test2',
                weight=0.2,
                comment='No',
                vertical='mail',
                description=' hello  ',
            ),
            catalog_pb2.Functionality(
                name='Test3',
                weight=0.3,
                comment='No',
                vertical='web',
                description='ok',
            )
        ])

        for funct in fuctionality_list.objects:
            _make_functs(funct, None)

    def test_empty_request(self):
        request = catalog_pb2.FunctionalityList(
            objects=[],
        )

        with self.assertRaises(ValueError):
            _edit_functs(request, None)

    def test_typical_edit(self):
        request = catalog_pb2.FunctionalityList(
            objects=[
                catalog_pb2.Functionality(
                    name='Test1',
                    weight=0.28,
                    comment='My first comment',
                    vertical='mail',
                    id=1,
                    description='   hello   '
                )
            ]
        )

        response = _edit_functs(request, None).objects[0]

        output = catalog_pb2.Functionality(
            name='Test1',
            weight=0.28,
            comment='My first comment',
            vertical='mail',
            id=1,
            description='hello'
        )

        self.assertEqual(response, output)
