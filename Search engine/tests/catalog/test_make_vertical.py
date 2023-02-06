from search.mon.workplace.protoc.structures import vertical_spec_pb2

from search.martylib.db_utils import clear_db, prepare_db, session_scope
from search.martylib.test_utils import TestCase
from search.mon.workplace.src.libs.catalog.catalog_vertical import make_vertical
from search.mon.workplace.src.sqla.workplace.model import CatalogVertical, CatalogProduct
from search.mon.workplace.src.libs.catalog.constants import DEFAULT_KPI_VALUE


@TestCase.require_postgres
class TestWorkplaceMakeFuncts(TestCase):
    @classmethod
    def setUpClass(cls):
        clear_db()
        prepare_db()

    @classmethod
    def tearDownClass(cls):
        with session_scope() as session:
            session.query(CatalogVertical).delete(synchronize_session=False)
            session.query(CatalogProduct).delete(synchronize_session=False)

    def test_typical_request(self):
        spi_chat = vertical_spec_pb2.UrlRecord(url='https://t.me/joinchat/AAAAAEATqpxoIyjvlu_ESw', name='<>')

        request = vertical_spec_pb2.CatalogVertical(
            name='New vertical!',
            environments=['prod'],
            weight=0.1,
            users=['harond1'],
            comment='My first vertical    ',
            spi_chat=spi_chat,
            target=1000,
        )

        response = make_vertical(request)

        self.assertEqual(response.id, 1)
        self.assertEqual(response.environments, ['prod'])
        self.assertEqual(response.weight, 0.1)
        self.assertEqual(response.comment, 'My first vertical')
        self.assertEqual(response.spi_chat, spi_chat)
        self.assertEqual(response.kpi, DEFAULT_KPI_VALUE)

    def test_with_profiles_and_envs(self):
        profiles = vertical_spec_pb2.ProfileList(objects=[vertical_spec_pb2.Profile(
            env_type=vertical_spec_pb2.Environment[vertical_spec_pb2.Environment.CANARY],
            infra_id=999
        )])

        environments = ['prod', 'hamster']
        spi_chat = vertical_spec_pb2.UrlRecord(url='https://t.me/joinchat/AAAAAEATqpxoIyjvlu_ESw', name='<>')

        request = vertical_spec_pb2.CatalogVertical(
            name='Test!',
            environments=environments,
            profiles=profiles,
            weight=0.1,
            users=['harond1'],
            comment='cmnt',
            spi_chat=spi_chat,
        )

        response = make_vertical(request)

        self.assertEqual(response.environments, environments)
        self.assertEqual(response.profiles, profiles)

    def test_product_exists(self):
        vname = 'v1'
        spi_chat = vertical_spec_pb2.UrlRecord(url='https://t.me/joinchat/AAAAAEATqpxoIyjvlu_ESw', name='<>')

        request = vertical_spec_pb2.CatalogVertical(
            name=vname,
            environments=['prod'],
            weight=0.1,
            users=['harond1'],
            comment='Check',
            spi_chat=spi_chat,
            target=90,
        )

        response = make_vertical(request)
        product_name = f'vertical:{vname}'

        with session_scope() as session:
            product_model = session.query(CatalogProduct == product_name).first_or_none()

            assert product_model is not None
            assert product_model.workplace__CatalogVertical_id == response.id
