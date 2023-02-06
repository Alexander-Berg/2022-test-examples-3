import pytest

from .mutators import mutate_idempotent, mutate_none, mutate_innacurate

from market.front.tools.service_updater.lib import Runner, DashDeployment, Dashboard, DashboardEnv, NotIdempotentMutation


def test_runner_list_services(capture_output_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing)
    Runner._list_services(depl)

    assert capture_output_fixture() == [
        '---------------------------------------------------------------------------------------------------------------',
        "Services in https://nanny.yandex-team.ru/ui/#/services/dashboards/catalog/market_front_desktop ['testing']",
        '---------------------------------------------------------------------------------------------------------------',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_sas',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_canary',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_kraken',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_sample',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_preview',
        '',
        '',
    ]


def test_wait_for_ready(capture_output_fixture, service_status_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing)
    service_status_fixture(lambda _: 'ONLINE')
    assert Runner._wait_for_ready(depl) is True

    assert capture_output_fixture() == [
        'Waiting for ONLINE state...',
        ''
    ]


def test_wait_for_ready_timeout(capture_output_fixture, cli_read_fixture, service_status_fixture, clock_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing)

    state = {
        'clock': 0
    }

    def clock():
        state['clock'] += 1000
        return state['clock']

    clock_fixture(lambda: clock())
    cli_read_fixture('no')
    service_status_fixture(lambda _: 'UPDATING')

    assert Runner._wait_for_ready(depl) is False

    assert capture_output_fixture() == [
        'Waiting for ONLINE state...',
        'Services ONLINE state timeout. Updating right now:',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_sas',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_sample',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_preview',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_kraken',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_canary',
        '\a\a',
        'Continue waiting? [yes/no]:',
        '\aAborting deploy',
        ''
    ]


def test_wait_for_ready_timeout_ci(
        capture_output_fixture, service_status_fixture, clock_fixture, non_interactive_input_fixture
):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing)

    state = {
        'clock': 0
    }

    def clock():
        state['clock'] += 1000
        return state['clock']

    clock_fixture(lambda: clock())
    service_status_fixture(lambda _: 'UPDATING')

    assert Runner._wait_for_ready(depl) is False

    assert capture_output_fixture() == [
        'Waiting for ONLINE state...',
        'Services ONLINE state timeout. Updating right now:',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_sas',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_sample',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_preview',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_kraken',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/testing_service_iva_canary',
        'Aborting deploy',
        '',
    ]


def test_prepare_services_good(capture_output_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    assert Runner._prepare_services(mutate_idempotent, depl) is True

    assert capture_output_fixture() == [
        'Preparing...',
        'Will deploy:',
        '',
        '  Service: https://nanny.yandex-team.ru/ui/#/services/catalog/production_service_iva',
        "  Update : {'runtime_attrs': {update: {u'resources': {update: {u'static_files': {0: {update: {u'content': 'updated'}}}}}}}, 'info_attrs': {}}",
        '',
        '  Service: https://nanny.yandex-team.ru/ui/#/services/catalog/production_service_sas',
        "  Update : {'runtime_attrs': {update: {u'resources': {update: {u'static_files': [{'is_dynamic': False, 'content': 'updated', 'local_path': 'experiments.json'}]}}}}, 'info_attrs': {}}",
        '',
        ''
    ]


def test_prepare_services_none(capture_output_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    assert Runner._prepare_services(mutate_none, depl) is False

    assert capture_output_fixture() == [
        'Preparing...',
        'All services already up to date',
        ''
    ]


def test_prepare_services_bad():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)

    with pytest.raises(NotIdempotentMutation):
        Runner._prepare_services(mutate_innacurate, depl)


def test_deploy_services_good(capture_output_fixture, service_status_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    Runner._prepare_services(mutate_idempotent, depl)
    capture_output_fixture()

    service_status_fixture(lambda _: 'ONLINE')
    assert Runner._deploy_services('update', depl) is True
    assert capture_output_fixture() == [
        'Deploying 2 services',
        '[1 / 2] Deployed https://nanny.yandex-team.ru/ui/#/services/catalog/production_service_iva',
        '[2 / 2] Deployed https://nanny.yandex-team.ru/ui/#/services/catalog/production_service_sas',
        ''
    ]


def test_deploy_services_bad(capture_output_fixture, cli_read_fixture, service_status_fixture, clock_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    Runner._prepare_services(mutate_idempotent, depl)
    capture_output_fixture()

    state = {
        'clock': 0
    }

    def clock():
        state['clock'] += 1000
        return state['clock']

    clock_fixture(lambda: clock())
    service_status_fixture(lambda _: 'UPDATING')
    cli_read_fixture('no')

    assert Runner._deploy_services('update', depl) is False
    assert capture_output_fixture() == [
        'Deploying 2 services',
        'Deploy timeout. Services in queue:',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/production_service_iva',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/production_service_sas',
        '\a\a',
        'Continue waiting? [yes/no]:',
        '\aAborting deploy (you should manually revert deployed services if needed)',
        ''
    ]


def test_deploy_services_bad_ci(
        capture_output_fixture, non_interactive_input_fixture, service_status_fixture, clock_fixture
):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    Runner._prepare_services(mutate_idempotent, depl)
    capture_output_fixture()

    state = {
        'clock': 0
    }

    def clock():
        state['clock'] += 1000
        return state['clock']

    clock_fixture(lambda: clock())
    service_status_fixture(lambda _: 'UPDATING')

    assert Runner._deploy_services('update', depl) is False
    assert capture_output_fixture() == [
        'Deploying 2 services',
        'Deploy timeout. Services in queue:',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/production_service_iva',
        '  https://nanny.yandex-team.ru/ui/#/services/catalog/production_service_sas',
        'Aborting deploy (you should manually revert deployed services if needed)',
        ''
    ]
