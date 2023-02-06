# -*- coding: utf-8 -*-
from datetime import datetime

import pytest
from faker import Factory
from mock import Mock

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.common.utils import environment
from travel.avia.ticket_daemon_api.jsonrpc.lib.enabled_partner_provider import EnabledPartnerProvider
from travel.avia.library.python.tester.factories import (
    create_partner, create_dohop_vendor, create_dohop_vendor_index_info, create_dohop_index
)


def setup_params():
    create_partner(
        code='bus_partner',
        t_type_id=TransportType.BUS_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_rasp_ru=True,
        enabled_in_rasp_ru=True
    )
    create_partner(
        code='train_partner',
        t_type_id=3,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_rasp_ru=True,
        enabled_in_rasp_ru=True,
    )
    create_partner(
        code='permanent_disabled',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=True,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_rasp_ru=True,
        enabled_in_rasp_ru=True,
        start_unavailability_datetime=datetime(2016, 1, 1),
        end_unavailability_datetime=datetime(2017, 1, 1)
    )
    create_partner(
        code='permanent_disabled_in_past',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_rasp_ru=True,
        enabled_in_rasp_ru=True,
        start_unavailability_datetime=datetime(2015, 1, 1),
        end_unavailability_datetime=datetime(2016, 1, 1)
    )
    create_partner(
        code='cant_fetch_by_daemon',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=False,
        disabled=False,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_rasp_ru=True,
        enabled_in_rasp_ru=True
    )
    create_partner(
        code='disabled',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=True,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_rasp_ru=True,
        enabled_in_rasp_ru=True
    )
    create_partner(
        code='disabled_for_rasp',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_rasp_ru=False,
        enabled_in_rasp_ru=False
    )
    create_partner(
        code='enabled_only_for_rasp_desktop',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=False,
        enabled_in_ticket_ru=False,
        enabled_in_mobile_rasp_ru=False,
        enabled_in_rasp_ru=True
    )
    create_partner(
        code='enabled_only_for_rasp_mobile',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=False,
        enabled_in_ticket_ru=False,
        enabled_in_mobile_rasp_ru=True,
        enabled_in_rasp_ru=False
    )
    create_partner(
        code='enabled_only_for_ticket_desktop',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=False,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_rasp_ru=False,
        enabled_in_rasp_ru=False
    )
    create_partner(
        code='enabled_only_for_ticket_mobile',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=False,
        enabled_in_mobile_rasp_ru=False,
        enabled_in_rasp_ru=False
    )
    create_partner(
        code='disabled_for_ticket',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ru=False,
        enabled_in_ticket_ru=False,
        enabled_in_mobile_rasp_ru=True,
        enabled_in_rasp_ru=True
    )
    create_partner(
        code='enabled_in_another_national_version',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=False,
        enabled_in_mobile_ticket_ua=True,
        enabled_in_ticket_ua=True,
        enabled_in_mobile_rasp_ua=True,
        enabled_in_rasp_ua=True
    )

    create_partner(
        code='dohop',
        t_type_id=TransportType.PLANE_ID,
        can_fetch_by_daemon=True,
        disabled=True,
        enabled_in_mobile_ticket_ru=True,
        enabled_in_ticket_ru=True,
        enabled_in_mobile_ticket_com=True,
        enabled_in_ticket_com=True
    )

    common_vendors = [
        create_dohop_vendor(
            dohop_id=1,
            enabled=True,
            enabled_in_mobile_ticket_ru=True,
            enabled_in_ticket_ru=True,
        ),

        create_dohop_vendor(
            dohop_id=2,
            enabled=True,

            enabled_in_mobile_ticket_ru=False,
            enabled_in_ticket_ru=True,
        ),

        create_dohop_vendor(
            dohop_id=3,
            enabled=True,
            enabled_in_mobile_ticket_ru=True,
            enabled_in_ticket_ru=True,
        ),

        create_dohop_vendor(
            dohop_id=4,
            enabled=False,
            enabled_in_mobile_ticket_ru=False,
            enabled_in_ticket_ru=False,
        )
    ]

    com_vendor = create_dohop_vendor(
        dohop_id=5,
        enabled=True,
        enabled_in_mobile_ticket_ru=False,
        enabled_in_ticket_ru=False,
        enabled_in_mobile_ticket_com=True,
        enabled_in_ticket_com=True,
    )

    common_index = create_dohop_index(code='yandex')
    com_index = create_dohop_index(code='yandexcom')

    for v in common_vendors:
        create_dohop_vendor_index_info(index=common_index, vendor=v)
    create_dohop_vendor_index_info(index=com_index, vendor=com_vendor)

    fake_environment = Mock(environment)
    fake_environment.now = Mock(return_value=datetime(2016, 6, 24))

    provider = EnabledPartnerProvider(
        environment=fake_environment
    )
    provider._get_actual_partners.reset()
    return provider


@pytest.mark.dbuser
@pytest.mark.parametrize("national_version,mobile,for_init_search,is_from_rasp, expected", [
    ('ru', False, False, False, [
        u'disabled_for_rasp',
        u'dohop_1',
        u'dohop_2',
        u'dohop_3',
        u'enabled_only_for_ticket_desktop',
        u'permanent_disabled_in_past'
    ]),
    # Если мы определяем список партнеров, для доставания их из кэша, то всегда берем список партнеров для ticket
    ('ru', False, False, True, [
        u'disabled_for_rasp',
        u'dohop_1',
        u'dohop_2',
        u'dohop_3',
        u'enabled_only_for_ticket_desktop',
        u'permanent_disabled_in_past'
    ]),
    # При инициализации/доставании из кэша список партнеров не меняется
    ('ru', False, True, False, [
        u'disabled_for_rasp',
        u'dohop_1',
        u'dohop_2',
        u'dohop_3',
        u'enabled_only_for_ticket_desktop',
        u'permanent_disabled_in_past'
    ]),
    # Для расписаний свой списко партнеров при инициализации поиска
    ('ru', False, True, True, [
        u'disabled_for_ticket',
        u'enabled_only_for_rasp_desktop',
        u'permanent_disabled_in_past'
    ]),
    # Для мобильной версии свой набор партнеров
    ('ru', True, False, False, [
        u'disabled_for_rasp',
        u'dohop_1',
        u'dohop_3',
        u'enabled_only_for_ticket_mobile',
        u'permanent_disabled_in_past'
    ]),
    # Если мы определяем список партнеров, для доставания их из кэша, то всегда берем список партнеров для ticket
    ('ru', True, False, True, [
        u'disabled_for_rasp',
        u'dohop_1',
        u'dohop_3',
        u'enabled_only_for_ticket_mobile',
        u'permanent_disabled_in_past'
    ]),
    # При инициализации/доставании из кэша список партнеров не меняется
    ('ru', True, True, False, [
        u'disabled_for_rasp',
        u'dohop_1',
        u'dohop_3',
        u'enabled_only_for_ticket_mobile',
        u'permanent_disabled_in_past'
    ]),
    # Для расписаний свой списко партнеров при инициализации поиска
    ('ru', True, True, True, [
        u'disabled_for_ticket',
        u'enabled_only_for_rasp_mobile',
        u'permanent_disabled_in_past'
    ])
])
def test_common(national_version, mobile, for_init_search, is_from_rasp, expected):
    """
    Сценарий:
    1) Тестируем все общие кейсы на ru версии
    """

    provider = setup_params()
    actual = provider.get_codes(national_version, mobile, for_init_search, is_from_rasp)

    assert sorted(expected) == sorted(actual)


@pytest.mark.dbuser
@pytest.mark.parametrize("national_version,mobile,for_init_search,is_from_rasp, expected", [
    ('com', False, False, False, [
        u'dohop_5'
    ])
])
def test_for_com(national_version, mobile, for_init_search, is_from_rasp, expected):
    """
    Сценарий:
    1) Тестируем, что в com версии используется свой индекс партнеров дохопа
    """

    provider = setup_params()
    actual = provider.get_codes(national_version, mobile, for_init_search, is_from_rasp)
    actual.sort()

    assert expected == actual


@pytest.mark.dbuser
def test__check_exist_attribute_in_partner():
    faker = Factory.create()
    provider = setup_params()
    assert provider._check_exist_attribute_in_partner("code")
    assert not provider._check_exist_attribute_in_partner(faker.pystr())


@pytest.mark.dbuser
@pytest.mark.parametrize("args, expected", [
    (('ru', True, True, True), True),
    (('by', True, True, True), False),
])
def test__validate_enabled_field_name(args, expected):
    provider = setup_params()
    assert provider.validate_enabled_field_name(*args) == expected
