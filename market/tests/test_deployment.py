import pytest


from market.front.tools.service_updater.lib import ServiceStatus, SpecialServiceType, \
    DashDeployment, Dashboard, DashboardEnv, NotIdempotentMutation, DeployTimeoutException

from .mutators import mutate_idempotent, mutate_none, mutate_innacurate


def test_init_all():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing)

    assert depl.services.keys() == [
        'testing_service_sas',
        'testing_service_iva',
        'testing_service_iva_canary',
        'testing_service_iva_kraken',
        'testing_service_iva_sample',
        'testing_service_iva_preview',
    ]


def test_init_special_only():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing, only_special=True)

    assert depl.services.keys() == [
        'testing_service_iva_canary',
        'testing_service_iva_kraken',
        'testing_service_iva_sample',
        'testing_service_iva_preview',
    ]


def test_init_no_special():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing, special_types=[])

    assert depl.services.keys() == [
        'testing_service_sas',
        'testing_service_iva',
    ]


def test_init_selected():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing, special_types=[
        SpecialServiceType.preview, SpecialServiceType.sample
    ])

    assert depl.services.keys() == [
        'testing_service_sas',
        'testing_service_iva',
        'testing_service_iva_sample',
        'testing_service_iva_preview',
    ]


def test_link():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing)
    assert depl.link == 'https://nanny.yandex-team.ru/ui/#/services/dashboards/catalog/market_front_desktop'


def test_description():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.testing)
    assert depl.description == 'https://nanny.yandex-team.ru/ui/#/services/dashboards/catalog/market_front_desktop [\'testing\']'


def test_hostnames():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    assert depl.hostnames == {
        'production_service_iva': [
            'iva_host_1',
            'iva_host_2',
            'iva_host_3',
            'iva_host_4'
        ],
        'production_service_sas': [
            'sas_host_1',
            'sas_host_2',
            'sas_host_3',
            'sas_host_4'
        ]
    }


def test_prepare_good():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    depl.prepare(mutate_idempotent)

    assert depl.pending.keys() == [
        'production_service_iva',
        'production_service_sas'
    ]

    assert '{}'.format(depl.pending['production_service_iva'].diff) == \
           "{'runtime_attrs': {update: {u'resources': {update: {u'static_files': {0: {update: {u'content': 'updated'}}}}}}}, 'info_attrs': {}}"

    assert '{}'.format(depl.pending['production_service_sas'].diff) == \
           "{'runtime_attrs': {update: {u'resources': {update: {u'static_files': [{'is_dynamic': False, 'content': 'updated', 'local_path': 'experiments.json'}]}}}}, 'info_attrs': {}}"


def test_prepare_none():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    depl.prepare(mutate_none)

    assert len(depl.pending) == 0


def test_prepare_bad():
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    with pytest.raises(NotIdempotentMutation):
        depl.prepare(mutate_innacurate)


def test_status(service_status_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)

    service_status_fixture(
        lambda service_id: 'ONLINE' if service_id == 'production_service_sas' else 'UPDATING'
    )

    assert depl.status() == {}

    assert depl.status(pending=False) == {
        'production_service_iva': ServiceStatus.UPDATING,
        'production_service_sas': ServiceStatus.ONLINE,
    }


def test_wait_for_ready(service_status_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)

    state = {'called': 0}

    def status(s):
        s['called'] += 1
        return 'ONLINE' if s['called'] > 5 else 'UPDATING'

    service_status_fixture(lambda service_id: status(state))

    assert depl.wait_for_ready() is None


def test_wait_for_ready_timeout(clock_fixture, service_status_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)

    state = {'called': 0}

    def mocked_clock(s):
        s['called'] += 1
        return s['called'] * 100

    service_status_fixture(lambda service_id: 'UPDATING')
    clock_fixture(lambda: mocked_clock(state))

    waiting = depl.wait_for_ready()
    assert [service.service_id for service in waiting] == [
        'production_service_iva',
        'production_service_sas',
    ]


def test_wait_for_release(service_status_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    depl.prepare(mutate_idempotent)

    state = {'called': 0}

    def status(s):
        s['called'] += 1
        return 'ONLINE' if s['called'] > 2 else 'UPDATING'

    service_status_fixture(lambda service_id: status(state))

    result = []
    for service in depl.wait_for_release():
        result.append(service)

    assert depl.pending == {}
    assert [service.service_id for service in result] == [
        'production_service_iva',
        'production_service_sas',
    ]


def test_wait_for_release_timeout(clock_fixture, service_status_fixture):
    depl = DashDeployment(Dashboard.white_desktop, DashboardEnv.production)
    depl.prepare(mutate_idempotent)

    state = {'called': 0}

    def mocked_clock(s):
        s['called'] += 1
        return s['called'] * 100

    service_status_fixture(lambda service_id: 'UPDATING')
    clock_fixture(lambda: mocked_clock(state))

    try:
        for service in depl.wait_for_release():
            assert service is None  # should never happen

    except DeployTimeoutException:
        assert depl.pending.keys() == [
            'production_service_iva',
            'production_service_sas',
        ]
