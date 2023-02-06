from search.martylib.db_utils import session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import functionality_pb2, monitoring_pb2, owner_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.proto.structures.component import common_pb2
from search.mon.warden.proto.structures import component_pb2
from search.mon.warden.sqla.warden.model import Component, Functionality
from search.mon.warden.src.config import Config
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components, create_functionalities, create_alerts, create_panels


class TestWardenGetFunctionality(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test_get_functionality',
                    weight=0.1,
                    abc_service_slug='test_get_functionalit',
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            )
        )
        create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test_functionality',
                    description='test functionality',
                    weight=0.2,
                ),
                component_name='test_get_functionality'
            ),
        )
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_get_functionality_component', abc_service_slug='test-1')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_get_functionality_component_2', abc_service_slug='test-2')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_get_functionality_component_3', abc_service_slug='test-3')),
        )
        create_panels(
            (
                component_pb2.AddPanelRequest(
                    monitoring=monitoring_pb2.Monitoring(panel=monitoring_pb2.Panel(name='test', url='https://yasm.yandex-team.ru/alert/test', iconostas_allocated=True)),
                    component_name='test_get_functionality_component_3',
                ),
            )
        )

    @TestCase.mock_auth(login='test-user')
    def test_get_functionality(self):
        # Setup test data
        created_functionality_list = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test_get_functionality_component',
                functionality=functionality_pb2.Functionality(
                    name='test_functionality',
                )
            )
        )
        create_alerts(
            alert_message_pb2.AddAlertRequest(
                functionality_id=created_functionality_list[0],
                alert=alert_pb2.Alert(
                    name='test_alert',
                )
            )
        )

        response = Warden().get_functionality(functionality_pb2.GetFunctionalityRequest(functionality_id=created_functionality_list[0]), context=None)
        self.assertEqual(response.functionality.name, 'test_functionality')
        self.assertEqual(len(response.functionality.alerts), 1)
        for alert in response.functionality.alerts:
            self.assertEqual(alert.name, 'test_alert')

    @TestCase.mock_auth(login='test-user')
    def test_get_functionality_components(self):
        # Setup test data
        created_functionality_list = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test_get_functionality_component_3',
                functionality=functionality_pb2.Functionality(
                    name='test_functionality_3',
                )
            )
        )
        expected_panels = 1 + len(Config().config.service.root_component_panels)

        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test_get_functionality_component_3').one()
            component_proto = component.to_protobuf()
            self.assertEqual(len(component_proto.monitoring_panels), expected_panels)

        response = Warden().get_functionality_components(component_pb2.GetFunctionalityComponentsRequest(functionality_id=created_functionality_list[0]), context=None)
        self.assertEqual(response.component.name, 'test_get_functionality_component_3')
        self.assertEqual(len(response.component.monitoring_panels), expected_panels)
        panels = [(m.panel.name, m.panel.url) for m in response.component.monitoring_panels]
        self.assertIn(('test', 'https://yasm.yandex-team.ru/alert/test'), panels)

    def test_get_component_functionality_list(self):
        response = Warden().get_component_functionality_list(common_pb2.ComponentFilter(component_name='test_get_functionality'), context=None)
        self.assertEqual(len(response.objects), 1)
        for functionality in response.objects:
            self.assertNotEqual(functionality.functionality.id, '')

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_get_deleted_service_component_functionality_list(self):
        create_components(component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_get_functionality_component_1', abc_service_slug='test-get-1')))
        create_components(component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_get_functionality_component_1__test')))
        create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                parent_component_name='test_get_functionality_component_1',
                component_name='test',
                functionality=functionality_pb2.Functionality(
                    name='test_functionality_3',
                )
            )
        )

        WARDEN_CLIENT = Warden()

        response = WARDEN_CLIENT.get_component_functionality_list(common_pb2.ComponentFilter(component_name='test_get_functionality_component_1'), context=None)
        self.assertEqual(len(response.objects), 1)
        WARDEN_CLIENT.delete_component(common_pb2.ComponentFilter(component_name='test', parent_component_name='test_get_functionality_component_1'), context=None)

        response = WARDEN_CLIENT.get_component_functionality_list(common_pb2.ComponentFilter(component_name='test_get_functionality_component_1'), context=None)
        self.assertEqual(len(response.objects), 0)

    @TestCase.mock_auth(login='test-user')
    def test_get_functionality_list(self):
        WARDEN_CLIENT = Warden()

        with session_scope() as session:
            model_functs = session.query(Functionality).order_by(Functionality.name).all()

            # with empty DB
            for f in model_functs:
                session.delete(f)
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=[],
                total_objects=0,
            )
            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)

            # add 5 functionalities
            create_functionalities(
                *[
                    functionality_pb2.AddFunctionalityRequest(
                        functionality=functionality_pb2.Functionality(
                            name=f'test_functionality_list_{n}',
                            description='test functionality',
                            weight=0.2,
                        ),
                        component_name='test_get_functionality',
                    ) for n in range(5)
                ]
            )

            model_functs = session.query(Functionality).order_by(Functionality.name).all()
            functs = [f.to_protobuf() for f in model_functs]
            total_functs = len(functs)

            # all functs
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(page_size=10000000),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=functs,
                total_objects=total_functs,
            )
            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)

            # 1st page
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(page_size=2),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=functs[:2],
                total_objects=total_functs,
            )
            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)

            # last page
            page_size = int(total_functs / 2) + 1
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(page=2, page_size=page_size),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=functs[page_size:],
                total_objects=total_functs,
            )
            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)

            # too far page
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(page=2, page_size=10000000),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=[],
                total_objects=total_functs,
            )
            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)

            session.rollback()

    @TestCase.mock_auth(login='test-user')
    def test_get_functionality_list_with_filter_by_name(self):
        WARDEN_CLIENT = Warden()

        with session_scope() as session:
            model_functs = session.query(Functionality).order_by(Functionality.name).all()

            # with empty DB
            for f in model_functs:
                session.delete(f)

            # add 5 functionalities
            create_functionalities(
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_1_functionality',
                        description='test functionality 1',
                        weight=0.1,
                    ),
                    component_name='test_get_functionality',
                ),
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_2_functionality',
                        description='test functionality 2',
                        weight=0.3,
                    ),
                    component_name='test_get_functionality',
                ),
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_3_func_with_short_name',
                        description='test functionality 3',
                        weight=0.5,
                    ),
                    component_name='test_get_functionality',
                ),
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_4_unnamed_f',
                        description='test functionality 4',
                        weight=0.1,
                    ),
                    component_name='test_get_functionality',
                ),
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_5_functionality',
                        description='test functionality 5',
                        weight=0.3,
                    ),
                    component_name='test_get_functionality',
                )
            )

            model_functs = session.query(Functionality).order_by(Functionality.name).all()
            functs = [f.to_protobuf() for f in model_functs]
            total_functs = len(functs)

            # get first 3 functionalities
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(page_size=3, functionality_name='test'),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=functs[:3],
                total_objects=total_functs,
            )

            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)

            # get functionalities which contain substring "func": 1, 2, 3, 5
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(page_size=10000, functionality_name='func'),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=functs[:3] + [functs[4]],
                total_objects=4,
            )

            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)

            # get functionalities which contain substring "func", page_size=2, page=2: 3, 5
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(page=2, page_size=2, functionality_name='func'),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=[functs[2]] + [functs[4]],
                total_objects=4,
            )

            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)
            session.rollback()

    @TestCase.mock_auth(login='test-user')
    def test_get_functionality_list_with_filter_by_slug(self):
        WARDEN_CLIENT = Warden()

        with session_scope() as session:
            model_functs = session.query(Functionality).order_by(Functionality.name).all()

            # with empty DB
            for f in model_functs:
                session.delete(f)

            # add 5 functionalities
            create_functionalities(
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_1_functionality',
                        description='test functionality 1',
                        weight=0.1,
                    ),
                    component_name='test_get_functionality',
                ),
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_2_substr_functionality',
                        description='test functionality 2',
                        weight=0.3,
                    ),
                    component_name='test_get_functionality',
                ),
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_3_func_with_short_name',
                        description='test functionality 3',
                        weight=0.5,
                        slug='test_get_functionality_pref_substr_suffix'
                    ),
                    component_name='test_get_functionality',
                ),
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_4_substr_f',
                        description='test functionality 4',
                        weight=0.1,
                        slug='test_get_functionality_pref_substr'
                    ),
                    component_name='test_get_functionality',
                ),
                functionality_pb2.AddFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        name='test_5_functionality',
                        description='test functionality 5',
                        weight=0.3,
                        slug='test_get_functionality_substr_suffix'
                    ),
                    component_name='test_get_functionality',
                )
            )

            model_functs = session.query(Functionality).order_by(Functionality.name).all()
            functs = [f.to_protobuf() for f in model_functs]

            # get functionalities which contain substring "substr" in name and in slug: 2, 3, 4, 5
            response = WARDEN_CLIENT.get_functionality_list(
                functionality_pb2.GetFunctionalityListRequest(page_size=10000, functionality_name='substr'),
                context=None,
            )
            expected = functionality_pb2.GetFunctionalityListResponse(
                objects=functs[1:],
                total_objects=4,
            )

            self.assertEqual(response.objects[:], expected.objects[:])
            self.assertEqual(response.total_objects, expected.total_objects)
            session.rollback()
