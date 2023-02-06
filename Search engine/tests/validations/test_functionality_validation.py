import time

from search.martylib.db_utils import session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, component_check_pb2
from search.mon.warden.proto.structures.alert.alert_pb2 import Alert, BeholderAlertSettings
from search.mon.warden.proto.structures.functionality_pb2 import Functionality
from search.mon.warden.src import const
from search.mon.warden.src.services.reducers.validators import functionality as v_functionality


class TestWardenValidateFunctionality(TestCase):
    maxDiff = None

    def test_fill_functionalities_check_map_for_component(self):
        test_cases = [
            {
                'name': 'Functionalities with autocreate alerts (one alert deleted)',
                'component': component_pb2.Component(
                    functionality_list=[
                        Functionality(
                            id='1',
                            alerts=[
                                Alert(
                                    id='11',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                    )
                                ),
                                Alert(
                                    id='12',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                    )
                                ),
                                Alert(
                                    id='13',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='2',
                            alerts=[
                                Alert(
                                    id='21',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                    )
                                ),
                                Alert(
                                    id='22',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='3',
                            alerts=[
                                Alert(
                                    id='31',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=False,
                                    )
                                ),
                                Alert(
                                    id='32',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                    ),
                                    state=Alert.State.DELETED,
                                ),
                            ]
                        ),
                    ]
                ),
                'expected':
                    {
                        const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME: {
                            'functionalities': {'3', '2', '1'},
                            'alerts': {'13', '22', '31'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                            'functionalities': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                        },
                }
            },
            {
                'name': 'Functionalities with autocreate and flow alerts',
                'component': component_pb2.Component(
                    functionality_list=[
                        Functionality(
                            id='1',
                            alerts=[
                                Alert(
                                    id='11',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                        start_flow=True,
                                    )
                                ),
                                Alert(
                                    id='12',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                    )
                                ),
                                Alert(
                                    id='13',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='2',
                            alerts=[
                                Alert(
                                    id='21',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                        start_flow=True,
                                    )
                                ),
                                Alert(
                                    id='22',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='3',
                            alerts=[
                                Alert(
                                    id='31',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=False,
                                    )
                                ),
                            ]
                        ),
                    ]
                ),
                'expected':
                    {
                        const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '21'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME: {
                            'functionalities': {'3', '2', '1'},
                            'alerts': {'13', '22', '31'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                            'functionalities': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                        },
                }
            },
            {
                'name': 'Functionalities with turn off autocreate and downtime',
                'component': component_pb2.Component(
                    functionality_list=[
                        Functionality(
                            id='1',
                            alerts=[
                                Alert(
                                    id='11',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                        calculate_metric=False
                                    )
                                ),
                                Alert(
                                    id='12',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                        calculate_metric=True,
                                    )
                                ),
                                Alert(
                                    id='13',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                        create_spi=False
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='2',
                            alerts=[
                                Alert(
                                    id='21',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                        calculate_metric=True,
                                    )
                                ),
                                Alert(
                                    id='22',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=False,
                                        calculate_metric=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='3',
                            alerts=[
                                Alert(
                                    id='31',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=False,
                                        calculate_metric=False,
                                    )
                                ),
                            ]
                        ),
                    ]
                ),
                'expected':
                    {
                        const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME: {
                            'functionalities': {'3', '2', '1'},
                            'alerts': {'13', '22', '31'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'13', '12', '21'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                            'functionalities': {'1', '2'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2'},
                            'alerts': {'21', '12', '13'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                        },
                }
            },
            {
                'name': 'Functionalities with turned on background downtime',
                'component': component_pb2.Component(
                    functionality_list=[
                        Functionality(
                            id='1',
                            alerts=[
                                Alert(
                                    id='11',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_background_metric=True,
                                    )
                                ),
                                Alert(
                                    id='12',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_background_metric=True,
                                    )
                                ),
                                Alert(
                                    id='13',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_background_metric=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='2',
                            alerts=[
                                Alert(
                                    id='21',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_background_metric=True,
                                    )
                                ),
                                Alert(
                                    id='22',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_background_metric=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='3',
                            alerts=[
                                Alert(
                                    id='31',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_background_metric=False,
                                    )
                                ),
                            ]
                        ),
                    ]
                ),
                'expected':
                    {
                        const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2', '3'},
                            'alerts': {'13', '22', '31'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                            'functionalities': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                        },
                }
            },
            {
                'name': 'Functionalities with turned on downtime',
                'component': component_pb2.Component(
                    functionality_list=[
                        Functionality(
                            id='1',
                            alerts=[
                                Alert(
                                    id='11',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                    )
                                ),
                                Alert(
                                    id='12',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                    )
                                ),
                                Alert(
                                    id='13',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='2',
                            alerts=[
                                Alert(
                                    id='21',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                    )
                                ),
                                Alert(
                                    id='22',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='3',
                            alerts=[
                                Alert(
                                    id='31',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=False,
                                    )
                                ),
                            ]
                        ),
                    ]
                ),
                'expected':
                    {
                        const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2', '3'},
                            'alerts': {'11', '12', '13', '21', '22', '31'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                            'functionalities': {'1', '2'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT: {
                            'functionalities': {'1', '2'},
                        },
                }
            },
            {
                'name': 'Functionalities with turned on downtime and turned off flow',
                'component': component_pb2.Component(
                    functionality_list=[
                        Functionality(
                            id='1',
                            alerts=[
                                Alert(
                                    id='11',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                        start_flow=True,
                                    )
                                ),
                                Alert(
                                    id='12',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                    )
                                ),
                                Alert(
                                    id='13',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=False,
                                        start_flow=True,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='2',
                            alerts=[
                                Alert(
                                    id='21',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                        start_flow=False,
                                    )
                                ),
                                Alert(
                                    id='22',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=False,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='3',
                            alerts=[
                                Alert(
                                    id='31',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=False,
                                    )
                                ),
                            ]
                        ),
                    ]
                ),
                'expected':
                    {
                        const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2', '3'},
                            'alerts': {'11', '12', '13', '21', '22', '31'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                            'functionalities': {'2'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT: {
                            'functionalities': {'1', '2'},
                        },
                }
            },
            {
                'name': 'Functionalities with alert with turned on downtime and turned off background downtime and not boolean category',
                'component': component_pb2.Component(
                    functionality_list=[
                        Functionality(
                            id='1',
                            alerts=[
                                Alert(
                                    id='11',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                        calculate_background_metric=False,
                                    )
                                ),
                                Alert(
                                    id='12',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                        calculate_background_metric=False,
                                    ),
                                    category=Alert.Category.BOOLEAN
                                ),
                                Alert(
                                    id='13',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                        calculate_background_metric=True,
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='2',
                            alerts=[
                                Alert(
                                    id='21',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                        calculate_background_metric=False,
                                    )
                                ),
                            ]
                        ),
                    ]
                ),
                'expected':
                    {
                        const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '21'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT: {
                            'functionalities': {'1'},
                            'alerts': {'13'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '12', '13', '21'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                            'functionalities': {'2', '1'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '21'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT: {
                            'functionalities': {'1', '2'},
                        },
                }
            },
            {
                'name': 'Functionalities with downtime alert and without autocreate alert',
                'component': component_pb2.Component(
                    functionality_list=[
                        Functionality(
                            id='1',
                            alerts=[
                                Alert(
                                    id='11',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                    )
                                ),
                                Alert(
                                    id='12',
                                    beholder_settings=BeholderAlertSettings(
                                        create_spi=True,
                                    ),
                                ),
                                Alert(
                                    id='13',
                                    beholder_settings=BeholderAlertSettings(
                                    )
                                ),
                            ]
                        ),
                        Functionality(
                            id='2',
                            alerts=[
                                Alert(
                                    id='21',
                                    beholder_settings=BeholderAlertSettings(
                                        calculate_metric=True,
                                        create_spi=True,
                                    )
                                ),
                            ]
                        ),
                    ]
                ),
                'expected':
                    {
                        const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'21', '12'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW: {
                            'functionalities': set(),
                            'alerts': set(),
                        },

                        const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1'},
                            'alerts': {'11', '13'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT: {
                            'functionalities': set(),
                            'alerts': set(),
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '21'},
                        },

                        const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                            'functionalities': {'2', '1'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME: {
                            'functionalities': {'1', '2'},
                            'alerts': {'11', '21'},
                        },
                        const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT: {
                            'functionalities': set(),
                        },
                }
            },
        ]
        with session_scope() as session:
            for test_case in test_cases:
                self.assertEqual(
                    v_functionality.Functionality().get_check_map_for_component(session, test_case['component']),
                    test_case['expected'],
                    msg=test_case['name']
                )

    def test_check_get_critical_functionality_checks(self):
        test_cases = [
            {
                'name': 'No tier (horizontal)',
                'input_tier': '',
                'input_is_vertical': False,
                'expected_res': {const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT},
            },
            {
                'name': 'No tier (vertical)',
                'input_tier': '',
                'input_is_vertical': True,
                'expected_res': {const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT},
            },
            {
                'name': 'Tier D (horizontal)',
                'input_tier': 'D',
                'input_is_vertical': False,
                'expected_res': {
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME
                },
            },
            {
                'name': 'Tier D (vertical)',
                'input_tier': 'D',
                'input_is_vertical': True,
                'expected_res': {
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME
                },
            },
            {
                'name': 'Tier C (horizontal)',
                'input_tier': 'C',
                'input_is_vertical': False,
                'expected_res': {
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME
                },
            },
            {
                'name': 'Tier C (vertical)',
                'input_tier': 'C',
                'input_is_vertical': True,
                'expected_res': {
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME,
                    const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT
                },
            },
            {
                'name': 'Tier B (vertical)',
                'input_tier': 'B',
                'input_is_vertical': True,
                'expected_res': {
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME,
                    const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW,
                },
            },
            {
                'name': 'Tier B (horizontal)',
                'input_tier': 'B',
                'input_is_vertical': False,
                'expected_res': {
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME
                },
            },
            {
                'name': 'Tier A (horizontal)',
                'input_tier': 'A',
                'input_is_vertical': False,
                'expected_res': {
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME
                },
            },
            {
                'name': 'Tier A (vertical)',
                'input_tier': 'A',
                'input_is_vertical': True,
                'expected_res': {
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW,
                    const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME,
                    const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT,
                    const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW,
                    const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME
                },
            },
        ]
        for test_case in test_cases:
            res = v_functionality.Functionality().get_critical_checks(test_case['input_tier'], test_case['input_is_vertical'])
            self.assertEqual(
                res,
                test_case['expected_res'],
                msg=test_case['name']
            )

    def test_get_functionality_check_status(self):
        ok = component_check_pb2.ComponentCheck.Status.OK
        error = component_check_pb2.ComponentCheck.Status.ERROR

        test_cases = [
            {
                'name': 'Check has functionality with autocreate alert',
                'check_name': const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT,
                'check_details': {'functionalities': ['1']},
                'expected_res': ok,
            },
            {
                'name': 'Check has not functionality with autocreate alert',
                'check_name': const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT,
                'check_details': {'functionalities': []},
                'expected_res': error,
            },
            {
                'name': 'Check has not functionality with alerts where createSpi==False and calculateBackgroundMetric==False',
                'check_name': const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME,
                'check_details': {'functionalities': []},
                'expected_res': ok,
            },
            {
                'name': 'Check has functionality with alerts where createSpi==False and calculateBackgroundMetric==False',
                'check_name': const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_OFF_AUTOCREATE_AND_BACKGROUND_DOWNTIME,
                'check_details': {'functionalities': ['1']},
                'expected_res': error,
            },
            {
                'name': 'Check has functionality with alert with turned on autocreate and flow',
                'check_name': const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW,
                'check_details': {'functionalities': ['1']},
                'expected_res': ok,
            },
            {
                'name': 'Check has not functionality with alert with turned on autocreate and flow',
                'check_name': const.HAS_FUNCTIONALITY_WITH_ALERT_WITH_TURNED_ON_AUTOCREATE_AND_FLOW,
                'check_details': {'functionalities': []},
                'expected_res': error,
            },
            {
                'name': 'Check has functionality with alert with turned on background downtime calculation',
                'check_name': const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT,
                'check_details': {'functionalities': ['1']},
                'expected_res': ok,
            },
            {
                'name': 'Check has not functionality with alert with turned on background downtime calculation',
                'check_name': const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT,
                'check_details': {'functionalities': []},
                'expected_res': error,
            },
            {
                'name': 'Check has functionality with alert with turned on downtime calculation',
                'check_name': const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT,
                'check_details': {'functionalities': ['1']},
                'expected_res': ok,
            },
            {
                'name': 'Check has not functionality with alert with turned on downtime calculation',
                'check_name': const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT,
                'check_details': {'functionalities': []},
                'expected_res': error,
            },
            {
                'name': 'Check has functionality with alert with turned on downtime calculation and turned off flow',
                'check_name': const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW,
                'check_details': {'functionalities': ['1']},
                'expected_res': error,
            },
            {
                'name': 'Check has not functionality with alert with turned on downtime calculation and turned off flow',
                'check_name': const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW,
                'check_details': {'functionalities': []},
                'expected_res': ok,
            },
            {
                'name': 'Check has functionality with nonboolean alert with turned on downtime calculation and turned off background downtime calculation',
                'check_name': const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME,
                'check_details': {'functionalities': ['1']},
                'expected_res': error,
            },
            {
                'name': 'Check has not functionality with nonboolean alert with turned on downtime calculation and turned off background downtime calculation',
                'check_name': const.HAS_FUNCTIONALITY_WITH_NONBOOLEAN_ALERT_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_BACKGROUND_DOWNTIME,
                'check_details': {'functionalities': []},
                'expected_res': ok,
            },
            {
                'name': 'Check has functionality with nonboolean alert with turned on downtime calculation and turned off autocreation',
                'check_name': const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                'check_details': {'functionalities': ['1']},
                'expected_res': error,
            },
            {
                'name': 'Check has not functionality with nonboolean alert with turned on downtime calculation and turned off autocreation',
                'check_name': const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT_AND_WITHOUT_AUTOCREATE_ALERT,
                'check_details': {'functionalities': []},
                'expected_res': ok,
            },
        ]
        for test_case in test_cases:
            res = v_functionality.Functionality().get_check_status(test_case['check_name'], test_case['check_details'])
            self.assertEqual(
                res,
                test_case['expected_res'],
                msg=test_case['name']
            )

    def test_merge_old_checks_and_new_checks(self):
        old_checks = [
            component_check_pb2.ComponentCheck(
                id='1',
                check_type=component_check_pb2.ComponentCheck.CheckType.FUNCTIONALITIES,
                status=component_check_pb2.ComponentCheck.Status.OK,
                name=const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT,
                description=const.CHECK_DESCRIPTION_MAP[const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT],
                priority=component_check_pb2.ComponentCheck.Priority.WARNING,
                causes=component_check_pb2.ComponentCheckCauses(
                    functional_ids=['1', '2'],
                    alert_ids=['3', '4']
                ),
                created=int(time.time()),
            ),
            component_check_pb2.ComponentCheck(
                id='2',
                check_type=component_check_pb2.ComponentCheck.CheckType.FUNCTIONALITIES,
                status=component_check_pb2.ComponentCheck.Status.ERROR,
                name=const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT,
                description=const.CHECK_DESCRIPTION_MAP[const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT],
                priority=component_check_pb2.ComponentCheck.Priority.CRITICAL,
                causes=component_check_pb2.ComponentCheckCauses(),
                created=int(time.time()),
            ),
        ]

        current_check_map = {
            const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT: {
                'functionalities': {'8', '9'},
                'alerts': {'10', '11'},
            },
            const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW: {
                'functionalities': {'12', '13'},
            },
        }

        critical_checks = {const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW}

        expected_res = [
            component_check_pb2.ComponentCheck(
                id='1',
                check_type=component_check_pb2.ComponentCheck.CheckType.FUNCTIONALITIES,
                status=component_check_pb2.ComponentCheck.Status.OK,
                name=const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT,
                description=const.CHECK_DESCRIPTION_MAP[const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT],
                priority=component_check_pb2.ComponentCheck.Priority.WARNING,
                causes=component_check_pb2.ComponentCheckCauses(
                    functional_ids=['1', '2'],
                    alert_ids=['3', '4']
                ),
                created=int(time.time()),
            ),
            component_check_pb2.ComponentCheck(
                id='2',
                check_type=component_check_pb2.ComponentCheck.CheckType.FUNCTIONALITIES,
                status=component_check_pb2.ComponentCheck.Status.OK,
                name=const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT,
                description=const.CHECK_DESCRIPTION_MAP[const.HAS_FUNCTIONALITY_WITH_DOWNTIME_ALERT],
                priority=component_check_pb2.ComponentCheck.Priority.CRITICAL,
                causes=component_check_pb2.ComponentCheckCauses(
                    functional_ids=['8', '9'],
                    alert_ids=['10', '11']
                ),
                created=int(time.time()),
            ),
            component_check_pb2.ComponentCheck(
                check_type=component_check_pb2.ComponentCheck.CheckType.FUNCTIONALITIES,
                status=component_check_pb2.ComponentCheck.Status.ERROR,
                name=const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW,
                description=const.CHECK_DESCRIPTION_MAP[const.HAS_FUNCTIONALITY_WITH_TURNED_ON_DOWNTIME_AND_TURNED_OFF_FLOW],
                priority=component_check_pb2.ComponentCheck.Priority.CRITICAL,
                causes=component_check_pb2.ComponentCheckCauses(
                    functional_ids=['12', '13']
                ),
                created=int(time.time()),
            ),
        ]

        res = v_functionality.Functionality().merge_old_checks_and_new_checks(old_checks, current_check_map, critical_checks)

        for i in range(0, 3):
            self.assertEqual(res[i].check_type, expected_res[i].check_type, msg=i)
            self.assertEqual(res[i].status, expected_res[i].status, msg=i)
            self.assertEqual(res[i].name, expected_res[i].name, msg=i)
            self.assertEqual(res[i].description, expected_res[i].description, msg=i)
            self.assertEqual(res[i].priority, expected_res[i].priority, msg=i)
            self.assertCountEqual(res[i].causes.functional_ids, expected_res[i].causes.functional_ids, msg=i)
            self.assertCountEqual(res[i].causes.alert_ids, expected_res[i].causes.alert_ids, msg=i)
