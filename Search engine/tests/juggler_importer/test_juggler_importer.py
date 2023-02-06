from search.martylib.db_utils import prepare_db, session_scope, clear_db, generate_field_name as F
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, functionality_pb2, metric_pb2, history_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.sqla.warden.model import Alert, Metric, History
from search.mon.warden.src.workers.juggler_sync import JugglerChecksLoader, JugglerSyncProcessor
from search.mon.warden.tests.utils.clients import Clients
from search.mon.warden.tests.utils.creators import create_components, create_functionalities, create_metric_types, create_metrics


clients = Clients()


TEST_DATA = {
    'no_functionality_tag': {
        'juggler': {
            'host': {
                'service_no_functionality': {
                    'tags': [
                        'warden_functionlity_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_no_functionality',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_no_functionality',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'simple': {
        'juggler': {
            'host': {
                'service_simple': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_simple',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_simple',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'no_funct': {
        'juggler': {
            'host': {
                'service_no_funct': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_func',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_func',
            alert=alert_pb2.Alert(
                name='host:service_no_funct',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_no_funct',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'no_metric': {
        'juggler': {
            'host': {
                'service_no_metric': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_metric_key_no',
                        'warden_alert_metric_type_no'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_no_metric',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_no_metric',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='no', key='no'),
                external=True
            )
        )
    },
    'nonpublic_metric': {
        'juggler': {
            'host': {
                'service_nonpublic_metric': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_metric_type_money',
                        'warden_alert_metric_key_loss'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_nonpublic_metric',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_nonpublic_metric',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='money', key='loss'),
                external=True
            )
        )
    },
    'complex': {
        'juggler': {
            'host': {
                'service_complex': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_create_infra_event',
                        'warden_alert_account_metric',
                        'warden_alert_account_background_metric',
                        'warden_alert_use_weight_function',
                        'warden_alert_normalize_by_rps',
                        'warden_alert_downtime_calculation_offset_10',
                        'warden_alert_crit_threshold_5.6',
                        'warden_alert_mean_day_rps_1000',
                        'warden_alert_normalize_percentile_80',
                        'warden_alert_category_ERROR_ABS',
                        'warden_alert_metric_key_availability',
                        'warden_alert_metric_type_SLA'
                    ],
                    'meta': {
                        'sla_target_value': 300,
                        'absolute_url': 'http://ya.ru'
                    }
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_complex',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_complex',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True,
                    create_infra_event=True,
                    calculate_background_metric=True,
                    calculate_metric=True,
                    use_weight_function=True,
                    normalize_by_rps=True,
                    downtime_calculation_offset=10,
                    crit_threshold=5.6,
                    mean_day_rps=1000,
                    normalize_percentile=80
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_ABS],
                metric=metric_pb2.Metric(type='SLA', key='availability'),
                target_value=300,
                url_abs='http://ya.ru',
                external=True
            )
        )
    },
    'change_metric': {
        'juggler': {
            'host': {
                'service_change_metric': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_metric_key_availability',
                        'warden_alert_metric_type_SLA'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_metric',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_metric',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='SLA', key='availability'),
                external=True
            )
        ),
        'prepare_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_metric',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_metric',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'change_unexistent_metric': {
        'juggler': {
            'host': {
                'service_change_nometric': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_metric_key_no',
                        'warden_alert_metric_type_no'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_nometric',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_nometric',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='no', key='no'),
                external=True
            )
        ),
        'prepare_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_nometric',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_nometric',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'change_to_nonpublic_metric': {
        'juggler': {
            'host': {
                'service_change_nonpublic_metric': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_metric_key_loss',
                        'warden_alert_metric_type_money'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_nonpublic_metric',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_nonpublic_metric',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='money', key='loss'),
                external=True
            )
        ),
        'prepare_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_nonpublic_metric',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_nonpublic_metric',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'change_functionality': {
        'juggler': {
            'host': {
                'service_change_functionality': {
                    'tags': [
                        'warden_functionality_test-check-importer_test-check-importer-service_test_funct-second',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_metric_key_',
                        'warden_alert_metric_type_YDT'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer_test-check-importer-service_test_funct-second',
            alert=alert_pb2.Alert(
                name='host:service_change_functionality',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_functionality',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        ),
        'prepare_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_functionality',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_functionality',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'change_unexistent_functionality': {
        'juggler': {
            'host': {
                'service_change_unexistent_functionality': {
                    'tags': [
                        'warden_functionality_test-check-importer_test-check-importer-service_test_funct-',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_metric_key_',
                        'warden_alert_metric_type_YDT'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer_test-check-importer-service_test_funct-',
            alert=alert_pb2.Alert(
                name='host:service_change_unexistent_functionality',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_unexistent_functionality',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        ),
        'prepare_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_unexistent_functionality',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_unexistent_functionality',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'change_alert_complex': {
        'juggler': {
            'host': {
                'service_change_complex': {
                    'tags': [
                        'warden_functionality_test-check-importer_test-check-importer-service_test_funct-second',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow',
                        'warden_alert_create_infra_event',
                        'warden_alert_account_metric',
                        'warden_alert_account_background_metric',
                        'warden_alert_use_weight_function',
                        'warden_alert_normalize_by_rps',
                        'warden_alert_downtime_calculation_offset_10',
                        'warden_alert_crit_threshold_5.6',
                        'warden_alert_mean_day_rps_1000',
                        'warden_alert_normalize_percentile_80',
                        'warden_alert_category_ERROR_ABS',
                        'warden_alert_metric_key_availability',
                        'warden_alert_metric_type_SLA'
                    ],
                    'meta': {
                        'sla_target_value': 300,
                        'absolute_url': 'http://ya.ru'
                    }
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer_test-check-importer-service_test_funct-second',
            alert=alert_pb2.Alert(
                name='host:service_change_complex',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_complex',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True,
                    create_infra_event=True,
                    calculate_background_metric=True,
                    calculate_metric=True,
                    use_weight_function=True,
                    normalize_by_rps=True,
                    downtime_calculation_offset=10,
                    crit_threshold=5.6,
                    mean_day_rps=1000,
                    normalize_percentile=80
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_ABS],
                metric=metric_pb2.Metric(type='SLA', key='availability'),
                target_value=300,
                url_abs='http://ya.ru',
                external=True
            )
        ),
        'prepare_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_change_complex',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_change_complex',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
    'nochange_alert': {
        'juggler': {
            'host': {
                'service_nochange': {
                    'tags': [
                        'warden_functionality_test-check-importer__test_funct',
                        'warden_auto_source',
                        'warden_alert_create_spi',
                        'warden_alert_start_flow'
                    ]
                }
            }
        },
        'expected_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_nochange',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_nochange',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        ),
        'prepare_request': alert_message_pb2.AddAlertWithSlugRequest(
            functionality_slug='test-check-importer__test_funct',
            alert=alert_pb2.Alert(
                name='host:service_nochange',
                type=alert_pb2.Alert.Type.JUGGLER,
                url='https://juggler.yandex-team.ru/check_details/?host=host&service=service_nochange',
                beholder_settings=alert_pb2.BeholderAlertSettings(
                    create_spi=True,
                    start_flow=True
                ),
                category=alert_pb2.Alert.Category[alert_pb2.Alert.Category.ERROR_PERC],
                metric=metric_pb2.Metric(type='YDT', key=''),
                external=True
            )
        )
    },
}


class TestJugglerImporter(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        with session_scope() as session:
            session.execute('alter table "warden__Functionality" ALTER COLUMN  "slug" set DEFAULT NULL;')
        cls.load_to_db()

    @classmethod
    def load_to_db(cls):
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-check-importer', abc_service_slug='test-check-importer-abc')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='zen', abc_service_slug='test-zen')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-check-importer__test-check-importer-service')),
        )
        create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test-check-functionality',
                    description='test funct',
                    weight=0.2,
                    slug='test-check-importer__test_funct'
                ),
                component_name='test-check-importer'
            ),
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test-check-functionality-second',
                    description='second test funct',
                    weight=0.2,
                    slug='test-check-importer_test-check-importer-service_test_funct-second'
                ),
                component_name='test-check-importer-service',
                parent_component_name='test-check-importer'
            ),
        )
        create_metric_types((
            metric_pb2.MetricType(
                key='YDT',
                name='YDT',
                owners=metric_pb2.MetricOwners(logins=['test-user-1', 'test-user-2']),
                is_private=False,
            ),
            metric_pb2.MetricType(
                key='SLA',
                name='SLA',
                owners=metric_pb2.MetricOwners(logins=['test-user-1', 'test-user-2']),
                is_private=False
            ),
            metric_pb2.MetricType(
                key='money',
                name='money',
                owners=metric_pb2.MetricOwners(logins=['test-user-2']),
                is_private=True
            )
        ))
        create_metrics((
            metric_pb2.Metric(
                key='',
                type='YDT',
                name=''
            ),
            metric_pb2.Metric(
                key='availability',
                type='SLA',
                name=''
            ),
            metric_pb2.Metric(
                key='loss',
                type='money',
                name=''
            )
        ))

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def tearDown(self) -> None:
        with session_scope() as session:
            alerts = session.query(Alert).filter(Alert.external is True).all()
            for alert in alerts:
                session.delete(alert)

    def check_creation(
        self,
        key: str,
        expect_no_check: bool = False,
        expect_none: bool = False,
        expect_no_change: bool = False,
        expect_no_funct_change: bool = False
    ):
        test_alert = TEST_DATA[key]['juggler']
        expected = TEST_DATA[key]['expected_request']
        clients.juggler.set_data(test_alert)
        checks_loader = JugglerChecksLoader(clients=clients)
        checks_loader.load_alerts_from_juggler()
        if expect_no_check:
            self.assertEqual(len(checks_loader.loaded_alerts), 0)
        else:
            self.assertEqual(len(checks_loader.loaded_alerts), 1)
            self.assertEqual(checks_loader.loaded_alerts[0], expected)
        checks_processor = JugglerSyncProcessor(checks=checks_loader)
        with session_scope() as session:
            checks_processor.sync_alerts_from_juggler_with_db(session)
            loaded_alert = session.query(Alert).filter(Alert.url == expected.alert.url).one_or_none()
            if expect_none:
                self.assertIsNone(loaded_alert)
                # TODO: test that there are no addition log
                # self.check_log(session, alert_id, entry_type=history_pb2.History.ActionType.add)
            else:
                loaded_alert_funct = loaded_alert.functionality.slug
                self.assertIsNotNone(loaded_alert)
                self.assertEqual(loaded_alert.state, alert_pb2.Alert.State[alert_pb2.Alert.State.SYNCED])
                alert_id = loaded_alert.id
                alert_proto = loaded_alert.to_protobuf(
                    exclude=(F(Alert.id), F(Alert.state),
                             F(Alert.metric, Metric.owners))
                )

                compare_to = expected.alert
                if expect_no_change:
                    compare_to = TEST_DATA[key]['prepare_request'].alert
                self.assertEqual(alert_proto, compare_to)
                if expect_no_funct_change:
                    self.assertNotEqual(loaded_alert_funct, TEST_DATA[key]['expected_request'].functionality_slug)
                else:
                    self.assertEqual(loaded_alert_funct, TEST_DATA[key]['expected_request'].functionality_slug)
                if expect_no_change and expect_no_funct_change:
                    self.check_log(
                        session,
                        alert_id=str(alert_id),
                        entry_type=history_pb2.History.ActionType[history_pb2.History.ActionType.modify],
                        expect_no_entry=True
                    )

    def check_change(self, key: str, expect_no_change: bool = False, expect_no_funct_change: bool = False):
        checks_loader = JugglerChecksLoader(clients=clients)
        checks_loader.loaded_alerts = [TEST_DATA[key]['prepare_request']]
        checks_processor = JugglerSyncProcessor(checks=checks_loader)
        expected = TEST_DATA[key]['expected_request']
        with session_scope() as session:
            checks_processor.sync_alerts_from_juggler_with_db(session)
        with session_scope() as session:
            existed_alert = session.query(Alert).filter(Alert.url == expected.alert.url).one_or_none()
            self.assertIsNotNone(existed_alert)
        self.check_creation(key, expect_no_change=expect_no_change, expect_no_funct_change=expect_no_funct_change)

    def check_log(self, session, alert_id: str, entry_type: history_pb2.History.ActionType, expect_no_entry: bool = False):
        entry = session.query(History).filter(History.alert_id == alert_id, History.action == entry_type).one_or_none()
        if expect_no_entry:
            self.assertIsNone(entry)
        else:
            self.assertIsNotNone(entry)

    def test_no_functionality_tag(self):
        self.check_creation('no_functionality_tag', expect_no_check=True, expect_none=True)

    def test_create_simple_alert(self):
        self.check_creation('simple')

    def test_create_alert_with_unexistent_functionality(self):
        self.check_creation('no_funct', expect_none=True)

    def test_create_alert_with_unexistent_metric(self):
        self.check_creation('no_metric', expect_none=True)

    def test_create_alert_with_nonpublic_metric(self):
        self.check_creation('nonpublic_metric', expect_none=True)

    def test_create_complex_alert(self):
        self.check_creation('complex')

    def test_change_metric(self):
        self.check_change('change_metric')

    def test_change_unexistent_metric(self):
        self.check_change('change_unexistent_metric', expect_no_change=True)

    def test_change_to_nonpublic_metric(self):
        self.check_change('change_to_nonpublic_metric', expect_no_change=True)

    def test_change_functionality(self):
        self.check_change('change_functionality')

    def test_change_unexistent_functionality(self):
        self.check_change('change_unexistent_functionality', expect_no_change=True, expect_no_funct_change=True)

    def test_change_alert_complex(self):
        self.check_change('change_alert_complex')

    def test_nochange_alert(self):
        self.check_change('nochange_alert', expect_no_change=True)
