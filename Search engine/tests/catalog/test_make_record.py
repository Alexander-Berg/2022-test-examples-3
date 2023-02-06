# import grpc
# from search.mon.workplace.protoc.structures import catalog_pb2, vertical_spec_pb2
#
# # from search.martylib.core.exceptions import ValidationError
# from search.martylib.db_utils import prepare_db, session_scope
# from search.martylib.test_utils import TestCase
# from search.mon.workplace.src.libs.catalog.service import Service
# from search.mon.workplace.src.sqla.workplace.model import CatalogService
#
#
# @TestCase.require_postgres
# class TestWorkplaceMakeFuncts(TestCase):
#
#     @classmethod
#     def setUpClass(cls):
#         prepare_db()
#
#     @classmethod
#     def tearDownClass(cls):
#         with session_scope() as session:
#             session.query(CatalogService).delete(synchronize_session=False)
#
#     def test_typical_request(self):
#
#         request = catalog_pb2.RecordForm(
#             id=0,
#             service='My service',
#             vertical='web',
#             owner='talion',
#             description='No descr',
#             priority=0,
#             downtime_impact=0,
#             product='vertical:web',
#             abc=[catalog_pb2.AbcServiceFilter(slug='web')],
#             wiki=[vertical_spec_pb2.UrlRecord(url='https://wiki.yandex-team.ru',
#                                               name='wiki page')],
#             chats=[vertical_spec_pb2.UrlRecord(
#                 url='https://wiki.yandex-team.ru/search-interfaces/infra/report-renderer/duty/',
#                 name='new chat')],
#             charts=[],
#             calendars=[],
#             downtime_alerts=[],
#             extra=catalog_pb2.RecordExtra(),
#             extra_patch=catalog_pb2.RecordExtra(),
#             spi_chat=vertical_spec_pb2.UrlRecord(url='https://t.me/joinchat/AAAAAEATqpxoIyjvlu_ESw', name='marty chat'),
#         )
#
#         with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
#             Service.make_record(request=request, context=ctx)
#
#         # self.assertEqual(response.disaster_url, '')
#         # self.assertEqual(response.absolute_chart_url, '')
#         # self.assertEqual(response.coefficient, 0.0)
#         # self.assertEqual(response.weight, 0.1)
