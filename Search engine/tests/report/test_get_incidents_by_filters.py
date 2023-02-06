from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, incident_pb2
from search.mon.warden.sqla.warden.model import Component, Incident
from search.mon.warden.src.services.reducers.report.report_reducer import IncidentsFilters, ReportReducer


class TestGetIncidentsByFilters(TestCase):
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

        with session_scope() as session:
            c_1 = Component(
                name='t_c_1',
                parent_component_name="t_c_p_1",
                tier="tier_1",
                abc_service_slug='abc1',
                slug='t_c_p_1__t_c_1',
            )
            session.add(c_1)
            session.commit()

            c_2 = Component(
                name='t_c_2',
                parent_component_name="t_c_p_1",
                tier="tier_2",
                abc_service_slug='abc2',
                slug='t_c_p_1__t_c_2',
            )
            session.add(c_2)
            session.commit()

            c_3 = Component(
                name='t_c_3',
                parent_component_name="t_c_p_1",
                tier="tier_1",
                abc_service_slug='abc3',
                slug='t_c_p_1__t_c_3',
            )
            session.add(c_3)

            session.add(
                Incident(
                    key='k1',
                    component=[c_1],
                    created=1293840000,  # 01.01.2011
                )
            )

            session.add(
                Incident(
                    key='k2',
                    component=[c_2],
                    created=1325376000,  # 01.01.2012
                )
            )

            session.add(
                Incident(
                    key='k3',
                    component=[c_3],
                    created=1356998400,  # 01.01.2013
                )
            )

            session.commit()

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def test_get_incidents_by_filters(self):
        test_cases = [
            {
                'name': 'Filter by components',
                'filters': IncidentsFilters(
                    components=[
                        component_pb2.Component(
                            name='t_c_1',
                            parent_component_name="t_c_p_1",
                        ),
                        component_pb2.Component(
                            name='t_c_2',
                            parent_component_name="t_c_p_1",
                        )
                    ],
                    period_end=None,
                    period_start=None
                ),
                'expected_res': [
                    incident_pb2.Incident(
                        key='k1',
                        created=1293840000,
                    ),
                    incident_pb2.Incident(
                        key='k2',
                        created=1325376000,
                    ),
                ]
            },
            {
                'name': 'Filter by start_time',
                'filters': IncidentsFilters(
                    components=None,
                    period_start=1325376000,  # 01.01.2012
                    period_end=None,
                ),
                'expected_res': [
                    incident_pb2.Incident(
                        key='k2',
                        created=1325376000,
                    ),
                    incident_pb2.Incident(
                        key='k3',
                        created=1356998400,
                    ),
                ]
            },
            {
                'name': 'Filter by end_time',
                'filters': IncidentsFilters(
                    period_start=None,
                    components=None,
                    period_end=1325376000,  # 01.01.2012
                ),
                'expected_res': [
                    incident_pb2.Incident(
                        key='k1',
                        created=1293840000,
                    ),
                    incident_pb2.Incident(
                        key='k2',
                        created=1325376000,
                    ),
                ]
            },
            {
                'name': 'Filter by start and end time',
                'filters': IncidentsFilters(
                    components=None,
                    period_end=1356998400,  # 01.01.2013
                    period_start=1356998400,  # 01.01.2013
                ),
                'expected_res': [
                    incident_pb2.Incident(
                        key='k3',
                        created=1356998400,
                    ),
                ]
            },
        ]

        report_reducer = ReportReducer()
        with session_scope() as session:
            for test_case in test_cases:
                res = report_reducer._get_incidents_by_filters(session, test_case['filters'])
                proto_res = sorted([i.to_protobuf() for i in res], key=lambda i: i.key)
                self.assertEqual(proto_res, test_case['expected_res'], msg=test_case['name'])
