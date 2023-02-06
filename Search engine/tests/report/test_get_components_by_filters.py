from search.martylib.db_utils import prepare_db, session_scope, to_model, clear_db
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2
from search.mon.warden.proto.structures.component import common_pb2
from search.mon.warden.sqla.warden.model import Component, ComponentTag
from search.mon.warden.src.services.reducers.report.report_reducer import ComponentsFilters, ReportReducer


class TestGetComponentsByFilters(TestCase):
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

        with session_scope() as session:
            tag1 = ComponentTag(tag="t_1")
            session.add(tag1)

            tag2 = ComponentTag(tag="t_2")
            session.add(tag2)

            tag3 = to_model(component_pb2.ComponentTag(tag="t_3"))
            session.add(tag3)

            tag4 = to_model(component_pb2.ComponentTag(tag="t_4"))
            session.add(tag4)

            session.add(
                Component(
                    name='t_c_1',
                    parent_component_name="t_c_p_1",
                    tier="tier_1",
                    abc_service_slug='abc1',
                    tags=[tag1, tag2],
                    slug='t_c_p_1__t_c_1',
                )
            )
            session.commit()
            session.add(
                Component(
                    name='t_c_2',
                    parent_component_name="t_c_p_1",
                    tier="tier_2",
                    tags=[tag1, tag2],
                    abc_service_slug='abc2',
                    slug='t_c_p_1__t_c_2',
                )
            )

            session.commit()
            session.add(
                Component(
                    name='t_c_3',
                    parent_component_name="t_c_p_1",
                    tier="tier_1",
                    tags=[tag1, tag3],
                    abc_service_slug='abc3',
                    slug='t_c_p_1__t_c_3',
                )
            )

            session.add(
                Component(
                    name='t_c_4',
                    parent_component_name="t_c_p_1",
                    tier="tier_3",
                    tags=[tag4],
                    abc_service_slug='abc4',
                    slug='t_c_p_1__t_c_4',
                )
            )

            session.commit()

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def test_get_components_by_filters(self):
        test_cases = [
            {
                'name': 'Select by name',
                'filters': ComponentsFilters(
                    name=[
                        common_pb2.ComponentFilter(parent_component_name="t_c_p_1", component_name='t_c_1'),
                        common_pb2.ComponentFilter(parent_component_name="t_c_p_1", component_name='t_c_3')
                    ],
                    tier=None,
                    tags=[],
                    parent_component='',
                ),
                'expected_res': [
                    component_pb2.Component(
                        name='t_c_1',
                        parent_component_name="t_c_p_1",
                        tier="tier_1",
                        abc_service_slug='abc1',
                        tags=[
                            component_pb2.ComponentTag(tag="t_1"),
                            component_pb2.ComponentTag(tag="t_2")
                        ]
                    ),
                    component_pb2.Component(
                        name='t_c_3',
                        parent_component_name="t_c_p_1",
                        tier="tier_1",
                        tags=[
                            component_pb2.ComponentTag(tag="t_1"),
                            component_pb2.ComponentTag(tag="t_3")
                        ],
                        abc_service_slug='abc3'
                    )
                ],
            },
            {
                'name': 'Select by tier',
                'filters': ComponentsFilters(
                    name=[],
                    tier=component_pb2.Tier(name="tier_1"),
                    tags=[],
                    parent_component='',
                ),
                'expected_res': [
                    component_pb2.Component(
                        name='t_c_1',
                        parent_component_name="t_c_p_1",
                        tier="tier_1",
                        abc_service_slug='abc1',
                        tags=[
                            component_pb2.ComponentTag(tag="t_1"),
                            component_pb2.ComponentTag(tag="t_2")
                        ]
                    ),
                    component_pb2.Component(
                        name='t_c_3',
                        parent_component_name="t_c_p_1",
                        tier="tier_1",
                        tags=[
                            component_pb2.ComponentTag(tag="t_1"),
                            component_pb2.ComponentTag(tag="t_3")
                        ],
                        abc_service_slug='abc3'
                    )
                ],
            },
            {
                'name': 'Select by tags',
                'filters': ComponentsFilters(
                    name=[],
                    tier=None,
                    tags=["t_3", "t_4"],
                    parent_component='',
                ),
                'expected_res': [
                    component_pb2.Component(
                        name='t_c_3',
                        parent_component_name="t_c_p_1",
                        tier="tier_1",
                        abc_service_slug='abc3',
                        tags=[
                            component_pb2.ComponentTag(tag="t_1"),
                            component_pb2.ComponentTag(tag="t_3")
                        ]
                    ),
                    component_pb2.Component(
                        name='t_c_4',
                        parent_component_name="t_c_p_1",
                        tier="tier_3",
                        abc_service_slug='abc4',
                        tags=[
                            component_pb2.ComponentTag(tag="t_4"),
                        ]
                    ),
                ],
            },
        ]

        with session_scope() as session:
            for test_case in test_cases:
                res = ReportReducer._get_components_by_filters(session, test_case['filters'])

                res_names = []
                res_parent_component_names = []
                res_tiers = []
                res_tags = []
                res_abc_service_slug = []

                for c in res:
                    res_names.append(c.name)
                    res_parent_component_names.append(c.parent_component_name)
                    res_tiers.append(c.tier)
                    res_tags.extend([t.tag for t in c.tags])
                    res_abc_service_slug.append(c.abc_service_slug)

                res_names.sort()
                res_parent_component_names.sort()
                res_tiers.sort()
                res_tags.sort()
                res_abc_service_slug.sort()

                exp_names = []
                exp_parent_component_names = []
                exp_tiers = []
                exp_tags = []
                exp_abc_service_slug = []

                for c in test_case['expected_res']:
                    exp_names.append(c.name)
                    exp_parent_component_names.append(c.parent_component_name)
                    exp_tiers.append(c.tier)
                    exp_tags.extend([t.tag for t in c.tags])
                    exp_abc_service_slug.append(c.abc_service_slug)

                exp_names.sort()
                exp_parent_component_names.sort()
                exp_tiers.sort()
                exp_tags.sort()
                exp_abc_service_slug.sort()

                self.assertEqual(
                    res_names,
                    exp_names,
                    msg=test_case['name'] + ' names'
                )

                self.assertEqual(
                    res_parent_component_names,
                    exp_parent_component_names,
                    msg=test_case['name'] + ' parent_names'
                )

                self.assertEqual(
                    res_tiers,
                    exp_tiers,
                    msg=test_case['name'] + ' tiers'
                )

                self.assertEqual(
                    res_tags,
                    exp_tags,
                    msg=test_case['name'] + ' tags'
                )

                self.assertEqual(
                    res_abc_service_slug,
                    exp_abc_service_slug,
                    msg=test_case['name'] + ' abc_service_slug'
                )
