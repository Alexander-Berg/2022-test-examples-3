from datetime import datetime
from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, lsr_pb2
from search.mon.warden.sqla.warden.model import LSR, Component
from search.mon.warden.src.services.reducers.report.report_reducer import LSRsFilters, ReportReducer


class TestGetLSRsByFilters(TestCase):
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
                LSR(
                    key='k1',
                    component=[c_1],
                    created=datetime(year=2011, month=1, day=1).timestamp(),
                )
            )

            session.add(
                LSR(
                    key='k2',
                    component=[c_2],
                    created=datetime(year=2012, month=1, day=1).timestamp(),
                )
            )

            session.add(
                LSR(
                    key='k3',
                    component=[c_3],
                    created=datetime(year=2013, month=1, day=1).timestamp(),
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
                'filters': LSRsFilters(
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
                    lsr_pb2.LSR(
                        key='k1',
                        created=int(datetime(year=2011, month=1, day=1).timestamp()),
                    ),
                    lsr_pb2.LSR(
                        key='k2',
                        created=int(datetime(year=2012, month=1, day=1).timestamp()),
                    ),
                ],
            },
            {
                'name': 'Filter by start_time',
                'filters': LSRsFilters(
                    components=None,
                    period_start=datetime(year=2012, month=1, day=1).timestamp(),
                    period_end=None,
                ),
                'expected_res': [
                    lsr_pb2.LSR(
                        key='k2',
                        created=int(datetime(year=2012, month=1, day=1).timestamp()),
                    ),
                    lsr_pb2.LSR(
                        key='k3',
                        created=int(datetime(year=2013, month=1, day=1).timestamp()),
                    ),
                ],
            },
            {
                'name': 'Filter by end_time',
                'filters': LSRsFilters(
                    period_start=None,
                    components=None,
                    period_end=datetime(year=2012, month=1, day=1).timestamp(),
                ),
                'expected_res': [
                    lsr_pb2.LSR(
                        key='k1',
                        created=int(datetime(year=2011, month=1, day=1).timestamp()),
                    ),
                    lsr_pb2.LSR(
                        key='k2',
                        created=int(datetime(year=2012, month=1, day=1).timestamp()),
                    ),
                ],
            },
            {
                'name': 'Filter by start and end time',
                'filters': LSRsFilters(
                    components=None,
                    period_end=datetime(year=2013, month=1, day=1).timestamp(),
                    period_start=datetime(year=2013, month=1, day=1).timestamp(),
                ),
                'expected_res': [
                    lsr_pb2.LSR(
                        key='k3',
                        created=int(datetime(year=2013, month=1, day=1).timestamp()),
                    ),
                ],
            },
        ]

        report_reducer = ReportReducer()
        with session_scope() as session:
            for test_case in test_cases:
                res = report_reducer._get_lsrs_by_filters(session, test_case['filters'])
                self.assertEqual(sorted(res, key=lambda i: i.key), test_case['expected_res'], msg=test_case['name'])
