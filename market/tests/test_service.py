import pytest

from market.front.tools.service_updater.lib import Service, NotPreparedService, AnonimousUpdate, \
    NotUpdatedService, ServiceStatus, SpecialServiceType, NotIdempotentMutation
from market.sre.tools.rtc.nanny.models.service import Service as RTCService
from .mutators import mutate_idempotent, mutate_none, mutate_innacurate


def test_special_type():
    assert Service('some-service').special_type is None
    assert Service('some-canary-service').special_type == SpecialServiceType.canary
    assert Service('some-kraken-service').special_type == SpecialServiceType.kraken
    assert Service('some-sample-service').special_type == SpecialServiceType.sample
    assert Service('some-preview-service').special_type == SpecialServiceType.preview
    assert Service('some-load-service').special_type == SpecialServiceType.load
    assert Service('some-market_test-service').special_type == SpecialServiceType.market_test


def test_link():
    service = Service('some_service_id')
    assert service.link == 'https://nanny.yandex-team.ru/ui/#/services/catalog/some_service_id'


def test_hostnames():
    service = Service('production_service_iva')
    assert service.hostnames == [
        'iva_host_1',
        'iva_host_2',
        'iva_host_3',
        'iva_host_4',
    ]


def test_mutate_good():
    service = Service('production_service_iva')

    assert service.prepare(mutate_idempotent) is True
    assert '{}'.format(service.diff) == \
           "{'runtime_attrs': {update: {u'resources': {update: {u'static_files': {0: {update: {u'content': 'updated'}}}}}}}, 'info_attrs': {}}"
    assert isinstance(service.prepared, RTCService)


def test_mutate_noedit():
    service = Service('production_service_iva')

    assert service.prepare(mutate_none) is False
    assert service.prepared is None


def test_mutate_bad():
    service = Service('production_service_iva')

    with pytest.raises(NotIdempotentMutation):
        service.prepare(mutate_innacurate)


def test_update_not_prepared():
    service = Service('production_service_iva')

    with pytest.raises(NotPreparedService):
        service.update('some description')


def test_update_no_description():
    service = Service('production_service_iva')

    with pytest.raises(AnonimousUpdate):
        service.update('')


def test_update_dry(dry_run_fixture):
    service = Service('production_service_iva')
    service.prepare(mutate_idempotent)
    service.update('some desc')
    assert service.updated == service.prepared


def test_update_no_dry(no_dry_run_fixture):
    service = Service('production_service_iva')
    service.prepare(mutate_idempotent)
    service.update('some desc')
    assert isinstance(service.updated, RTCService)
    assert service.updated == service.prepared


def test_activate_not_updated():
    service = Service('production_service_iva')

    with pytest.raises(NotUpdatedService):
        service.activate()


def test_activate_dry(dry_run_fixture):
    service = Service('production_service_iva')
    service.prepare(mutate_idempotent)
    service.update('some desc')
    service.activate()

    assert service.activated == service.updated


def test_activate_no_dry(no_dry_run_fixture):
    service = Service('production_service_iva')
    service.prepare(mutate_idempotent)
    service.update('some desc')
    service.activate()

    assert isinstance(service.activated, RTCService)
    assert service.activated == service.updated


def test_service_status(service_status_fixture):
    service = Service('production_service_iva')

    service_status_fixture(lambda _: 'ONLINE')
    assert service.status == ServiceStatus.ONLINE

    service_status_fixture(lambda _: 'UPDATING')
    assert service.status == ServiceStatus.UPDATING


def test_is_banned():
    assert Service.is_banned('some-service') is False
    assert Service.is_banned('some-xscript-service') is True


def test_special_type_static():
    assert Service.special_type('some-service') is None
    assert Service.special_type('some-canary-service') == SpecialServiceType.canary
    assert Service.special_type('some-sample-service') == SpecialServiceType.sample
    assert Service.special_type('some-kraken-service') == SpecialServiceType.kraken
    assert Service.special_type('some-preview-service') == SpecialServiceType.preview
