from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, spproblem_pb2
from search.mon.warden.sqla.warden.model import Component, SPProblem
from search.mon.warden.src.services.reducers.report.report_reducer import SPProblemsFilters, ReportReducer


class TestGetSPProblemsByFilters(TestCase):
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
                SPProblem(
                    key='k1',
                    component=[c_1],
                )
            )

            session.add(
                SPProblem(
                    key='k2',
                    component=[c_2],
                )
            )

            session.add(
                SPProblem(
                    key='k3',
                    component=[c_3],
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
                'filters': SPProblemsFilters(
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
                ),
                'expected_res': {
                    't_c_1:t_c_p_1': [
                        spproblem_pb2.SPProblem(
                            key='k1',
                        ),
                    ],
                    't_c_2:t_c_p_1': [
                        spproblem_pb2.SPProblem(
                            key='k2',
                        ),
                    ]
                }
            },
        ]

        for test_case in test_cases:
            res = ReportReducer._get_spproblems_by_filters(test_case['filters'])
            self.assertEqual(res, test_case['expected_res'], msg=test_case['name'])
