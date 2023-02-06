from search.martylib.db_utils import prepare_db, session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.sqla.warden.model import MetricType
from search.mon.warden.src.services.model import Warden

WARDEN_CLIENT = Warden()


class TestMetricTypeCreation(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

    @classmethod
    def tearDownClass(cls):
        with session_scope() as session:
            session.query(MetricType).delete(synchronize_session=False)

    def _check_metric_type(self, metric_type_proto: metric_pb2.MetricType):
        self.assertEqual(metric_type_proto.key, 'test')
        self.assertEqual(metric_type_proto.name, 'test-name')
        self.assertEqual(len(metric_type_proto.owners.logins), 2)
        self.assertIn('test-user-1', metric_type_proto.owners.logins)
        self.assertIn('test-user', metric_type_proto.owners.logins)
        self.assertTrue(metric_type_proto.print_in_spi)
        self.assertTrue(metric_type_proto.is_additive)
        self.assertTrue(metric_type_proto.is_private)
        self.assertEqual(metric_type_proto.ydt_convert_coefficient, 0.004)
        self.assertTrue(metric_type_proto.consider_weights)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_metric_type_creation(self):
        response = WARDEN_CLIENT.create_metric_type(
            metric_pb2.MetricType(
                key='test',
                name='test-name',
                owners=metric_pb2.MetricOwners(logins=['test-user-1']),
                is_additive=True,
                is_private=True,
                print_in_spi=True,
                ydt_convert_coefficient=0.004,
                consider_weights=True,
            ),
            context=None,
        )
        self._check_metric_type(response.metric_type)
        with session_scope() as session:
            metric_type = session.query(MetricType).filter(MetricType.key == 'test').one()
            self._check_metric_type(metric_type.to_protobuf())
