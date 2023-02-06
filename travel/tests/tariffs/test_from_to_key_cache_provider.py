# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime

import mock
import pytest
from hamcrest import assert_that, has_entries, has_properties, contains_inanyorder
from freezegun import freeze_time

from common.tester.factories import create_station
from common.models.schedule import Company
from common.models.tariffs import TariffTypeCode

from travel.rasp.suburban_selling.selling.tariffs.tariffs_configuration import TariffsConfiguration
from travel.rasp.suburban_selling.selling.tariffs.selling_companies import SuburbanCarrierCode
from travel.rasp.suburban_selling.selling.tariffs.interfaces import TariffKey, TariffKeyData, TariffKeyDataStatus
from travel.rasp.suburban_selling.selling.tariffs.from_to_key_cache_provider import (
    FromToKeyCacheTariffsProvider, FromToKeyCacheAsynchTariffsProvider, TariffFromToKey, get_stations_express_codes,
    BaseFromToKeyTariffsData
)


pytestmark = [pytest.mark.dbuser]

PROVIDER_PATH = 'travel.rasp.suburban_selling.selling.tariffs.from_to_key_cache_provider.FromToKeyCacheTariffsProvider'
ASYNC_PROVIDER_PATH = 'travel.rasp.suburban_selling.selling.tariffs.from_to_key_cache_provider.FromToKeyCacheAsynchTariffsProvider'


def test_get_stations_express_codes():
    create_station(id=11, express_id='111')
    create_station(id=22, express_id='222')
    create_station(id=33)
    today = date(2021, 7, 23)

    station_from_express_id, station_to_express_id = get_stations_express_codes(
        TariffFromToKey(date=today, station_from=11, station_to=22)
    )
    assert station_from_express_id == '111'
    assert station_to_express_id == '222'

    assert get_stations_express_codes(
        TariffFromToKey(date=today, station_from=11, station_to=33)
    ) is None

    assert get_stations_express_codes(
        TariffFromToKey(date=today, station_from=11, station_to=44)
    ) is None


class TariffStub(object):
    def __init__(self, tariff_type, partner):
        self.tariff_type = tariff_type
        self.partner = partner


class TariffsBaseDataStub(object):
    def __init__(self, need_update):
        self.need_update = need_update


def test_base_from_to_key_tariffs_data():
    with freeze_time(datetime(2021, 10, 29, 16, 30)):
        tariff = TariffStub(TariffTypeCode.USUAL, SuburbanCarrierCode.SZPPK)
        base_data = BaseFromToKeyTariffsData(
            tariffs_ttl=3600,
            updated=datetime(2021, 10, 29, 16, 25),
            update_started=datetime(2021, 10, 29, 16, 20)
        )
        assert base_data.tariffs_ttl == 3600
        assert base_data.empty_tariffs_ttl == 3600
        assert base_data.updated == datetime(2021, 10, 29, 16, 25)
        assert base_data.update_started == datetime(2021, 10, 29, 16, 20)
        assert base_data.status == TariffKeyDataStatus.ACTUAL
        assert base_data.need_update is False

        base_data.tariffs = [tariff]
        assert base_data.need_update is False

        base_data = BaseFromToKeyTariffsData(
            tariffs_ttl=3600,
            empty_tariffs_ttl=60,
            updated=datetime(2021, 10, 29, 16, 25),
            update_started=datetime(2021, 10, 29, 16, 24, 30)
        )
        assert base_data.empty_tariffs_ttl == 60
        assert base_data.status == TariffKeyDataStatus.ACTUAL
        assert base_data.need_update is True

        base_data.tariffs = [tariff]
        assert base_data.need_update is False

        base_data = BaseFromToKeyTariffsData(3600, updated=datetime(2021, 10, 29, 16, 25), update_started=None)
        assert base_data.need_update is True
        assert base_data.status == TariffKeyDataStatus.ACTUAL

        base_data = BaseFromToKeyTariffsData(3600, updated=None, update_started=None)
        assert base_data.need_update is True
        assert base_data.status == TariffKeyDataStatus.NO_DATA

        base_data = BaseFromToKeyTariffsData(3600, updated=None, update_started=datetime(2021, 10, 29, 16, 25))
        assert base_data.need_update is False
        assert base_data.status == TariffKeyDataStatus.NO_DATA

        base_data = BaseFromToKeyTariffsData(3600, updated=None, update_started=datetime(2021, 10, 29, 16, 19))
        assert base_data.need_update is True
        assert base_data.status == TariffKeyDataStatus.NO_DATA

        base_data = BaseFromToKeyTariffsData(
            3600, updated=datetime(2021, 10, 29, 15), update_started=datetime(2021, 10, 29, 16, 19)
        )
        assert base_data.need_update is True
        assert base_data.status == TariffKeyDataStatus.OLD

        base_data = BaseFromToKeyTariffsData(
            tariffs_ttl=3600,
            updated=datetime(2021, 10, 29, 15),
            update_started=None
        )
        assert base_data.need_update is True
        assert base_data.status == TariffKeyDataStatus.OLD

        base_data = BaseFromToKeyTariffsData(
            tariffs_ttl=3600,
            updated=datetime(2021, 10, 29, 15),
            update_started=datetime(2021, 10, 29, 14, 55)
        )
        assert base_data.need_update is True
        assert base_data.status == TariffKeyDataStatus.OLD

        base_data = BaseFromToKeyTariffsData(
            tariffs_ttl=3600,
            updated=datetime(2021, 10, 29, 15),
            update_started=datetime(2021, 10, 29, 16, 25)
        )
        base_data.tariffs = [tariff]
        assert base_data.need_update is False
        assert base_data.status == TariffKeyDataStatus.OLD


class TestFromToKeyCacheAsynchTariffsProvider(object):
    def test_get_selling_tariffs_by_from_to_keys(self):
        tariffs_configuration = TariffsConfiguration({}, {})
        provider = FromToKeyCacheAsynchTariffsProvider(tariffs_configuration)
        today = date(2021, 7, 23)

        from_to_keys = [
            TariffFromToKey(date=today, station_from=11, station_to=22),
            TariffFromToKey(date=today, station_from=11, station_to=33),
            TariffFromToKey(date=today, station_from=11, station_to=44)
        ]

        cache_tariffs_data = {
            from_to_keys[0]: TariffsBaseDataStub(need_update=False),
            from_to_keys[1]: TariffsBaseDataStub(need_update=True),
            from_to_keys[2]: TariffsBaseDataStub(need_update=True)
        }

        with mock.patch(
            '{}.get_tariffs_data_from_cache'.format(ASYNC_PROVIDER_PATH), return_value=cache_tariffs_data
        ) as m_get_tariffs_data_from_cache:
            with mock.patch(
                '{}.run_get_tariffs_data_from_provider'.format(ASYNC_PROVIDER_PATH),
            ) as m_run_get_tariffs_data_from_provider:
                with mock.patch(
                    '{}.make_selling_tariffs_from_tariffs_data'.format(ASYNC_PROVIDER_PATH), return_value='result'
                ) as m_make_selling_tariffs_from_tariffs_data:

                    assert provider.get_selling_tariffs_by_from_to_keys(from_to_keys) == 'result'

                    assert m_get_tariffs_data_from_cache.call_count == 1
                    assert m_get_tariffs_data_from_cache.call_args_list[0][0][0] == from_to_keys

                    assert m_run_get_tariffs_data_from_provider.call_count == 1
                    assert len(m_run_get_tariffs_data_from_provider.call_args_list[0][0][0]) == 2
                    assert_that(m_run_get_tariffs_data_from_provider.call_args_list[0][0][0], contains_inanyorder(
                        from_to_keys[1], from_to_keys[2]
                    ))

                    assert m_make_selling_tariffs_from_tariffs_data.call_count == 1
                    assert m_make_selling_tariffs_from_tariffs_data.call_args_list[0][0][0] == cache_tariffs_data

    def test_get_tariffs(self):
        tariffs_configuration = TariffsConfiguration({}, {})
        provider = FromToKeyCacheAsynchTariffsProvider(tariffs_configuration)
        today = date(2021, 7, 23)
        tariff_keys = [
            TariffKey(
                date=today, station_from=11, station_to=22,
                company=Company.SZPPK_MTPPK_ID, tariff_type=TariffTypeCode.USUAL
            ),
            TariffKey(
                date=today, station_from=11, station_to=22,
                company=Company.MTPPK_ID, tariff_type=TariffTypeCode.EXPRESS
            ),
            TariffKey(
                date=today, station_from=11, station_to=33,
                company=Company.SZPPK_ID, tariff_type=TariffTypeCode.USUAL
            )
        ]

        from_to_keys = [
            TariffFromToKey(date=today, station_from=11, station_to=22),
            TariffFromToKey(date=today, station_from=11, station_to=33)
        ]

        tariffs_by_from_to_keys = {
            from_to_keys[0]: TariffKeyData(
                status=TariffKeyDataStatus.ACTUAL,
                tariffs=[
                    TariffStub(TariffTypeCode.USUAL, SuburbanCarrierCode.SZPPK),
                    TariffStub(TariffTypeCode.USUAL, SuburbanCarrierCode.MTPPK),
                    TariffStub(TariffTypeCode.EXPRESS, SuburbanCarrierCode.MTPPK)
                ]
            ),
            from_to_keys[1]: TariffKeyData(
                status=TariffKeyDataStatus.ACTUAL,
                tariffs=[TariffStub(TariffTypeCode.USUAL, SuburbanCarrierCode.SZPPK)]
            )
        }

        with mock.patch('{}.check_get_tariffs_enabled'.format(ASYNC_PROVIDER_PATH), return_value=False):
            assert provider.get_tariffs(tariff_keys) == {}

        with mock.patch('{}.check_get_tariffs_enabled'.format(ASYNC_PROVIDER_PATH), return_value=True):
            with mock.patch(
                '{}.get_selling_tariffs_by_from_to_keys'.format(ASYNC_PROVIDER_PATH),
                return_value=tariffs_by_from_to_keys
            ) as m_get_selling_tariffs_by_from_to_keys:
                tariffs_by_tariff_keys = provider.get_tariffs(tariff_keys)

                assert m_get_selling_tariffs_by_from_to_keys.call_args_list[0][0][0] == set(from_to_keys)

                assert_that(tariffs_by_tariff_keys, has_entries({
                    tariff_keys[0]: has_properties({
                        'tariffs': contains_inanyorder(
                            has_properties({
                                'tariff_type': TariffTypeCode.USUAL,
                                'partner': SuburbanCarrierCode.SZPPK
                            }),
                            has_properties({
                                'tariff_type': TariffTypeCode.USUAL,
                                'partner': SuburbanCarrierCode.MTPPK
                            })
                        )
                    }),
                    tariff_keys[1]: has_properties({
                        'tariffs': contains_inanyorder(
                            has_properties({
                                'tariff_type': TariffTypeCode.EXPRESS,
                                'partner': SuburbanCarrierCode.MTPPK
                            })
                        )
                    }),
                    tariff_keys[2]: has_properties({
                        'tariffs': contains_inanyorder(
                            has_properties({
                                'tariff_type': TariffTypeCode.USUAL,
                                'partner': SuburbanCarrierCode.SZPPK
                            })
                        )
                    })
                }))


class TestFromToKeyCacheTariffsProvider(object):
    def test_get_selling_tariffs_by_from_to_keys(self):
        tariffs_configuration = TariffsConfiguration({}, {})
        provider = FromToKeyCacheTariffsProvider(tariffs_configuration)
        today = date(2021, 7, 23)

        tariff_0 = TariffStub(Company.SZPPK_ID, TariffTypeCode.USUAL)
        tariff_1 = TariffStub(Company.MTPPK_ID, TariffTypeCode.EXPRESS)

        from_to_keys = [
            TariffFromToKey(date=today, station_from=11, station_to=22),
            TariffFromToKey(date=today, station_from=11, station_to=33)
        ]

        with mock.patch(
            '{}.get_tariffs_data_from_cache'.format(PROVIDER_PATH), return_value={from_to_keys[0]: [tariff_0]}
        ) as m_get_tariffs_data_from_cache:
            with mock.patch(
                '{}.get_tariffs_data_from_provider'.format(PROVIDER_PATH), return_value=[tariff_1]
            ) as m_get_tariffs_data_from_provider:
                with mock.patch(
                    '{}.save_tariffs_data_to_cache'.format(PROVIDER_PATH)
                ) as m_save_tariffs_data_to_cache:
                    with mock.patch(
                        '{}.make_selling_tariffs_from_tariffs_data'.format(PROVIDER_PATH), return_value='result'
                    ) as m_make_selling_tariffs_from_tariffs_data:

                        assert provider.get_selling_tariffs_by_from_to_keys(from_to_keys) == 'result'

                        assert m_get_tariffs_data_from_cache.call_count == 1
                        assert m_get_tariffs_data_from_cache.call_args_list[0][0][0] == from_to_keys

                        assert m_get_tariffs_data_from_provider.call_count == 1
                        assert m_get_tariffs_data_from_provider.call_args_list[0][0][0] == from_to_keys[1]

                        assert m_save_tariffs_data_to_cache.call_count == 1
                        assert m_save_tariffs_data_to_cache.call_args_list[0][0][0] == from_to_keys[1]
                        assert m_save_tariffs_data_to_cache.call_args_list[0][0][1] == [tariff_1]

                        assert m_make_selling_tariffs_from_tariffs_data.call_count == 1
                        assert m_make_selling_tariffs_from_tariffs_data.call_args_list[0][0][0] == {
                            from_to_keys[0]: [tariff_0],
                            from_to_keys[1]: [tariff_1]
                        }

    def test_get_tariffs(self):
        tariffs_configuration = TariffsConfiguration({}, {})
        provider = FromToKeyCacheTariffsProvider(tariffs_configuration)
        today = date(2021, 7, 23)
        tariff_keys = [
            TariffKey(
                date=today, station_from=11, station_to=22,
                company=Company.SZPPK_MTPPK_ID, tariff_type=TariffTypeCode.USUAL
            ),
            TariffKey(
                date=today, station_from=11, station_to=22,
                company=Company.MTPPK_ID, tariff_type=TariffTypeCode.EXPRESS
            ),
            TariffKey(
                date=today, station_from=11, station_to=33,
                company=Company.SZPPK_ID, tariff_type=TariffTypeCode.USUAL
            )
        ]

        from_to_keys = [
            TariffFromToKey(date=today, station_from=11, station_to=22),
            TariffFromToKey(date=today, station_from=11, station_to=33)
        ]

        tariffs_by_from_to_keys = {
            from_to_keys[0]: TariffKeyData(
                status=TariffKeyDataStatus.ACTUAL,
                tariffs=[
                    TariffStub(TariffTypeCode.USUAL, SuburbanCarrierCode.SZPPK),
                    TariffStub(TariffTypeCode.USUAL, SuburbanCarrierCode.MTPPK),
                    TariffStub(TariffTypeCode.EXPRESS, SuburbanCarrierCode.MTPPK)
                ]
            ),
            from_to_keys[1]: TariffKeyData(
                status=TariffKeyDataStatus.ACTUAL,
                tariffs=[TariffStub(TariffTypeCode.USUAL, SuburbanCarrierCode.SZPPK)]
            )
        }

        with mock.patch('{}.check_get_tariffs_enabled'.format(PROVIDER_PATH), return_value=False):
            assert provider.get_tariffs(tariff_keys) == {}

        with mock.patch('{}.check_get_tariffs_enabled'.format(PROVIDER_PATH), return_value=True):
            with mock.patch(
                '{}.get_selling_tariffs_by_from_to_keys'.format(PROVIDER_PATH), return_value=tariffs_by_from_to_keys
            ) as m_get_selling_tariffs_by_from_to_keys:
                tariffs_by_tariff_keys = provider.get_tariffs(tariff_keys)

                assert m_get_selling_tariffs_by_from_to_keys.call_args_list[0][0][0] == set(from_to_keys)

                assert_that(tariffs_by_tariff_keys, has_entries({
                    tariff_keys[0]: has_properties({
                        'tariffs': contains_inanyorder(
                            has_properties({'tariff_type': TariffTypeCode.USUAL, 'partner': SuburbanCarrierCode.SZPPK}),
                            has_properties({'tariff_type': TariffTypeCode.USUAL, 'partner': SuburbanCarrierCode.MTPPK})
                        )
                    }),
                    tariff_keys[1]: has_properties({
                        'tariffs': contains_inanyorder(
                            has_properties({'tariff_type': TariffTypeCode.EXPRESS, 'partner': SuburbanCarrierCode.MTPPK}),
                        )
                    }),
                    tariff_keys[2]: has_properties({
                        'tariffs': contains_inanyorder(
                            has_properties({'tariff_type': TariffTypeCode.USUAL, 'partner': SuburbanCarrierCode.SZPPK}),
                        )
                    })
                }))
