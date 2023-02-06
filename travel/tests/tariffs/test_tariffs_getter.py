# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from collections import defaultdict
from typing import Dict, Set, AnyStr
from datetime import datetime, timedelta

import pytest
from hamcrest import assert_that, has_entries, has_properties, contains_inanyorder

from travel.rasp.library.python.common23.date import environment
from common.models.tariffs import (
    SuburbanSellingFlow, SuburbanTariffProvider, TariffTypeCode, SuburbanSellingBarcodePreset
)
from common.models.schedule import Company
from common.tester.utils.replace_setting import replace_setting

from travel.rasp.suburban_selling.selling.tariffs.selling_companies import SuburbanCarrierCompany, SuburbanCarrierCode
from travel.rasp.suburban_selling.selling.tariffs.interfaces import (
    SellingTariff, TariffKeyData, TariffKey, TariffKeyDataStatus
)
from travel.rasp.suburban_selling.selling.tariffs.tariffs_getter import (
    TariffsGetter, TariffsProvider, TariffsConfiguration
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


class TariffsProviderMock(TariffsProvider):
    def __init__(self, tariffs_configuration, selling_flows, keys_to_results):
        # type: (TariffsConfiguration, Set[AnyStr], Dict[TariffKey, TariffKeyData]) -> None
        super(TariffsProviderMock, self).__init__(tariffs_configuration)

        self.selling_flows = selling_flows
        self.keys_to_results = keys_to_results

    def get_selling_flows(self):
        # type: () -> Set[AnyStr]
        return self.selling_flows

    def get_tariffs(self, tariffs_keys):
        # type: (Set[TariffKey]) -> Dict[TariffKey, TariffKeyData]
        return {key: tariff_key_data for key, tariff_key_data in self.keys_to_results.items() if key in tariffs_keys}


TARIFF_KEYS = [
    TariffKey(
        date=datetime(2021, 10, 7), station_from=100, station_to=200,
        company=Company.SZPPK_ID, tariff_type=TariffTypeCode.USUAL
    ),
    TariffKey(
        date=datetime(2021, 10, 7), station_from=100, station_to=200,
        company=Company.SZPPK_ID, tariff_type=TariffTypeCode.EXPRESS
    ),
    TariffKey(
        date=datetime(2021, 10, 7), station_from=100, station_to=200,
        company=Company.CPPK_ID, tariff_type=TariffTypeCode.USUAL
    ),
    TariffKey(
        date=datetime(2021, 10, 7), station_from=100, station_to=200,
        company=Company.CPPK_AEROEX_ID, tariff_type=TariffTypeCode.USUAL
    ),
    TariffKey(
        date=datetime(2021, 10, 7), station_from=100, station_to=300,
        company=Company.CPPK_AEROEX_ID, tariff_type=TariffTypeCode.USUAL
    ),
    # Ключи, которые не должны обрабатываться
    TariffKey(
        date=datetime(2021, 10, 7), station_from=100, station_to=200,
        company=9999, tariff_type=TariffTypeCode.USUAL
    ),
    TariffKey(
        date=datetime(2021, 10, 7), station_from=999, station_to=200,
        company=Company.SZPPK_ID, tariff_type=TariffTypeCode.USUAL
    ),
    TariffKey(
        date=datetime(2021, 10, 7), station_from=100, station_to=200,
        company=Company.SZPPK_ID, tariff_type='unknown'
    ),
]


def _make_tariff(partner, tariff_type, selling_flow, price):
    return SellingTariff(
        tariff_type=tariff_type,
        partner=partner,
        selling_flow=selling_flow,
        price=price,
        name='Обычный',
        description='Самый обычный',
        max_days=1,
        valid_from=environment.today(),
        valid_until=environment.today() + timedelta(days=1),
        book_data={'some': 'thing'}
    )


def _make_tariffs_getter(selling_flows=None, barcode_presets=None):
    tariffs_configuration = TariffsConfiguration(
        carrier_codes_by_company_id={
            Company.SZPPK_ID: [SuburbanCarrierCode.SZPPK],
            Company.CPPK_ID: [SuburbanCarrierCode.CPPK],
            Company.CPPK_AEROEX_ID: [SuburbanCarrierCode.CPPK, SuburbanCarrierCode.AEROEXPRESS]
        },
        carriers_by_codes={
            SuburbanCarrierCode.SZPPK: SuburbanCarrierCompany(
                code=SuburbanCarrierCode.SZPPK,
                provider=SuburbanTariffProvider.IM,
                title='СЗППК',
                selling_enabled_testing=True,
                ogrn='ogrn_szppk',
                address='address_szppk',
                work_time='time_szppk',
                barcode_preset=SuburbanSellingBarcodePreset.PDF417_SZPPK
            ),
            SuburbanCarrierCode.CPPK: SuburbanCarrierCompany(
                code=SuburbanCarrierCode.CPPK,
                provider=SuburbanTariffProvider.MOVISTA,
                title='ЦППК',
                selling_enabled_testing=True,
                ogrn='ogrn_cppk',
                address='address_cppk',
                work_time='time_cppk',
                barcode_preset=SuburbanSellingBarcodePreset.PDF417_CPPK
            ),
            SuburbanCarrierCode.AEROEXPRESS: SuburbanCarrierCompany(
                code=SuburbanCarrierCode.AEROEXPRESS,
                provider=SuburbanTariffProvider.AEROEXPRESS,
                title='Аэроэкспресс',
                selling_enabled_testing=True,
            )
        }
    )
    tariff_cppk_1 = _make_tariff(SuburbanCarrierCode.CPPK, TariffTypeCode.USUAL, SuburbanSellingFlow.VALIDATOR, 100)
    tariff_cppk_2 = _make_tariff(SuburbanCarrierCode.CPPK, TariffTypeCode.USUAL, SuburbanSellingFlow.VALIDATOR, 200)

    providers = {
        SuburbanTariffProvider.IM: TariffsProviderMock(tariffs_configuration, {SuburbanSellingFlow.SIMPLE}, {
            TARIFF_KEYS[0]: TariffKeyData(
                status=TariffKeyDataStatus.ACTUAL,
                tariffs=[
                    _make_tariff(SuburbanCarrierCode.SZPPK, TariffTypeCode.USUAL, SuburbanSellingFlow.SIMPLE, 100),
                    _make_tariff(SuburbanCarrierCode.SZPPK, TariffTypeCode.USUAL, SuburbanSellingFlow.SIMPLE, 200)
                ]
            ),
            TARIFF_KEYS[1]: TariffKeyData(
                status=TariffKeyDataStatus.OLD,
                tariffs=[
                    _make_tariff(SuburbanCarrierCode.SZPPK, TariffTypeCode.EXPRESS, SuburbanSellingFlow.SIMPLE, 300)
                ]
            ),
        }),
        SuburbanTariffProvider.MOVISTA: TariffsProviderMock(tariffs_configuration, {SuburbanSellingFlow.VALIDATOR}, {
            TARIFF_KEYS[2]: TariffKeyData(
                status=TariffKeyDataStatus.ACTUAL,
                tariffs=[tariff_cppk_1, tariff_cppk_2]
            ),
            TARIFF_KEYS[3]: TariffKeyData(
                status=TariffKeyDataStatus.ACTUAL,
                tariffs=[tariff_cppk_1, tariff_cppk_2]
            )
        }),
        SuburbanTariffProvider.AEROEXPRESS: TariffsProviderMock(
            tariffs_configuration, {SuburbanSellingFlow.AEROEXPRESS}, {
                TARIFF_KEYS[4]: TariffKeyData(
                    status=TariffKeyDataStatus.ACTUAL,
                    tariffs=[_make_tariff(
                        SuburbanCarrierCode.AEROEXPRESS, TariffTypeCode.AEROEXPRESS,
                        SuburbanSellingFlow.AEROEXPRESS, 400
                    )]
                )
            }
        )
    }
    selling_flows = selling_flows if selling_flows is not None else {
        SuburbanSellingFlow.VALIDATOR, SuburbanSellingFlow.SIMPLE, SuburbanSellingFlow.AEROEXPRESS
    }
    barcode_presets = barcode_presets if barcode_presets is not None else {
        SuburbanSellingBarcodePreset.PDF417_CPPK, SuburbanSellingBarcodePreset.PDF417_SZPPK
    }
    return TariffsGetter(providers, selling_flows, barcode_presets, tariffs_configuration)


def test_tariffs_getter_init():
    tariffs_getter = _make_tariffs_getter(
        selling_flows={SuburbanSellingFlow.VALIDATOR, SuburbanSellingFlow.SIMPLE},
        barcode_presets={SuburbanSellingBarcodePreset.PDF417_CPPK, SuburbanSellingBarcodePreset.PDF417_SZPPK}
    )

    assert_that(tariffs_getter.tariffs_configuration, has_properties({
        'carrier_codes_by_company_id': has_entries({
            Company.SZPPK_ID: [SuburbanCarrierCode.SZPPK],
            Company.CPPK_ID: [SuburbanCarrierCode.CPPK],
            Company.CPPK_AEROEX_ID: [SuburbanCarrierCode.CPPK, SuburbanCarrierCode.AEROEXPRESS],
        }),
        'carriers_by_codes': has_entries({
            SuburbanCarrierCode.SZPPK: has_properties({'provider': SuburbanTariffProvider.IM}),
            SuburbanCarrierCode.CPPK: has_properties({'provider': SuburbanTariffProvider.MOVISTA}),
            SuburbanCarrierCode.AEROEXPRESS: has_properties({'provider': SuburbanTariffProvider.AEROEXPRESS}),
        })
    }))
    assert tariffs_getter.selling_flows == {SuburbanSellingFlow.VALIDATOR, SuburbanSellingFlow.SIMPLE}
    assert tariffs_getter.barcode_presets == {
        SuburbanSellingBarcodePreset.PDF417_CPPK, SuburbanSellingBarcodePreset.PDF417_SZPPK
    }

    assert len(tariffs_getter.providers) == 2
    assert_that(tariffs_getter.providers, has_entries({
        SuburbanTariffProvider.IM: has_properties({'selling_flows': {SuburbanSellingFlow.SIMPLE}}),
        SuburbanTariffProvider.MOVISTA: has_properties({'selling_flows': {SuburbanSellingFlow.VALIDATOR}})
    }))

    assert tariffs_getter.provider_codes_by_priority == ['movista', 'im']

    assert len(tariffs_getter.priority_by_provider_codes) == 2
    assert SuburbanTariffProvider.IM in tariffs_getter.priority_by_provider_codes
    assert SuburbanTariffProvider.MOVISTA in tariffs_getter.priority_by_provider_codes
    assert tariffs_getter.priority_by_provider_codes['movista'] < tariffs_getter.priority_by_provider_codes['im']


def test_prepare_start_tariff_keys_data():
    tariffs_getter = _make_tariffs_getter(selling_flows=SuburbanSellingFlow.ALL)
    data_by_providers_and_keys = tariffs_getter.prepare_start_tariff_keys_data(TARIFF_KEYS)

    assert len(data_by_providers_and_keys) == 3
    assert_that(data_by_providers_and_keys, has_entries({
        SuburbanTariffProvider.IM: has_entries({
            TARIFF_KEYS[0]: has_properties({
                'status': TariffKeyDataStatus.NO_DATA,
                'tariffs': []
            }),
            TARIFF_KEYS[1]: has_properties({
                'status': TariffKeyDataStatus.NO_DATA,
                'tariffs': []
            })
        }),
        SuburbanTariffProvider.MOVISTA: has_entries({
            TARIFF_KEYS[2]: has_properties({
                'status': TariffKeyDataStatus.NO_DATA,
                'tariffs': []
            }),
            TARIFF_KEYS[3]: has_properties({
                'status': TariffKeyDataStatus.NO_DATA,
                'tariffs': []
            }),
            TARIFF_KEYS[4]: has_properties({
                'status': TariffKeyDataStatus.NO_DATA,
                'tariffs': []
            })
        }),
        SuburbanTariffProvider.AEROEXPRESS: has_entries({
            TARIFF_KEYS[4]: has_properties({
                'status': TariffKeyDataStatus.NO_DATA,
                'tariffs': []
            })
        })
    }))


def test_get_tariffs_from_providers():
    with replace_setting('GET_TARIFFS_TIMEOUT', 1):
        with replace_setting('GET_TARIFFS_POLL_FREQUENCY', 0.6):
            tariffs_getter = _make_tariffs_getter()

            data_by_providers_and_keys = tariffs_getter.prepare_start_tariff_keys_data(TARIFF_KEYS)
            tariffs_getter.get_tariffs_from_providers(data_by_providers_and_keys)

            assert_that(data_by_providers_and_keys, has_entries({
                'im': has_entries({
                    TARIFF_KEYS[0]: has_properties({
                        'status': TariffKeyDataStatus.ACTUAL,
                        'tariffs': contains_inanyorder(
                            has_properties({
                                'partner': SuburbanCarrierCode.SZPPK,
                                'tariff_type': TariffTypeCode.USUAL,
                                'price': 100
                            }),
                            has_properties({
                                'partner': SuburbanCarrierCode.SZPPK,
                                'tariff_type': TariffTypeCode.USUAL,
                                'price': 200
                            }),
                        )
                    }),
                    TARIFF_KEYS[1]: has_properties({
                        'status': TariffKeyDataStatus.OLD,
                        'tariffs': contains_inanyorder(
                            has_properties({
                                'partner': SuburbanCarrierCode.SZPPK,
                                'tariff_type': TariffTypeCode.EXPRESS,
                                'price': 300
                            })
                        )
                    })
                }),
                'movista': has_entries({
                    TARIFF_KEYS[2]: has_properties({
                        'status': TariffKeyDataStatus.ACTUAL,
                        'tariffs': contains_inanyorder(
                            has_properties({
                                'partner': SuburbanCarrierCode.CPPK,
                                'tariff_type': TariffTypeCode.USUAL,
                                'price': 100
                            }),
                            has_properties({
                                'partner': SuburbanCarrierCode.CPPK,
                                'tariff_type': TariffTypeCode.USUAL,
                                'price': 200
                            }),
                        )
                    }),
                    TARIFF_KEYS[3]: has_properties({
                        'status': TariffKeyDataStatus.ACTUAL,
                        'tariffs': contains_inanyorder(
                            has_properties({
                                'partner': SuburbanCarrierCode.CPPK,
                                'tariff_type': TariffTypeCode.USUAL,
                                'price': 100
                            }),
                            has_properties({
                                'partner': SuburbanCarrierCode.CPPK,
                                'tariff_type': TariffTypeCode.USUAL,
                                'price': 200
                            })
                        )
                    }),
                }),
                'aeroexpress': has_entries({
                    TARIFF_KEYS[4]: has_properties({
                        'status': TariffKeyDataStatus.ACTUAL,
                        'tariffs': contains_inanyorder(
                            has_properties({
                                'partner': SuburbanCarrierCode.AEROEXPRESS,
                                'tariff_type': TariffTypeCode.AEROEXPRESS,
                                'price': 400
                            })
                        )
                    })
                })
            }))

            tariffs_getter = _make_tariffs_getter(barcode_presets={})
            data_by_providers_and_keys = tariffs_getter.prepare_start_tariff_keys_data(TARIFF_KEYS)
            tariffs_getter.get_tariffs_from_providers(data_by_providers_and_keys)

            assert len(data_by_providers_and_keys['im'][TARIFF_KEYS[0]].tariffs) == 0
            assert len(data_by_providers_and_keys['im'][TARIFF_KEYS[1]].tariffs) == 0
            assert len(data_by_providers_and_keys['movista'][TARIFF_KEYS[2]].tariffs) == 0
            assert len(data_by_providers_and_keys['movista'][TARIFF_KEYS[3]].tariffs) == 0
            assert len(data_by_providers_and_keys['aeroexpress'][TARIFF_KEYS[4]].tariffs) == 1


def test_get_tariffs():
    tariffs_getter = _make_tariffs_getter()

    tariffs_result = tariffs_getter.get_tariffs([])
    assert len(tariffs_result['selling_tariffs']) == 0

    with replace_setting('GET_TARIFFS_TIMEOUT', 1):
        with replace_setting('GET_TARIFFS_POLL_FREQUENCY', 0.6):
            tariffs_result = tariffs_getter.get_tariffs(TARIFF_KEYS)

            tariff_ids_by_partner_and_type = defaultdict(list)
            for provider_data in tariffs_result['selling_tariffs']:
                for tariff in provider_data['tariffs']:
                    tariff_ids_by_partner_and_type[(tariff['partner'], tariff['type'])].append(tariff['id'])

            assert_that(tariffs_result, has_entries({
                'selling_tariffs': contains_inanyorder(
                    has_entries({
                        'provider': SuburbanTariffProvider.IM,
                        'tariffs': contains_inanyorder(
                            has_entries({
                                'partner': SuburbanCarrierCode.SZPPK,
                                'type': TariffTypeCode.USUAL,
                                'price': 100,
                                'name': 'Обычный',
                                'description': 'Самый обычный',
                                'max_days': 1,
                                'book_data': '{"some": "thing"}',
                                'selling_flow': SuburbanSellingFlow.SIMPLE
                            }),
                            has_entries({
                                'partner': SuburbanCarrierCode.SZPPK,
                                'type': TariffTypeCode.USUAL,
                                'price': 200,
                                'selling_flow': SuburbanSellingFlow.SIMPLE
                            }),
                            has_entries({
                                'partner': SuburbanCarrierCode.SZPPK,
                                'type': TariffTypeCode.EXPRESS,
                                'price': 300,
                                'selling_flow': SuburbanSellingFlow.SIMPLE
                            })
                        )
                    }),
                    has_entries({
                        'provider': SuburbanTariffProvider.MOVISTA,
                        'tariffs': contains_inanyorder(
                            has_entries({
                                'partner': SuburbanCarrierCode.CPPK,
                                'type': TariffTypeCode.USUAL,
                                'price': 100,
                                'selling_flow': SuburbanSellingFlow.VALIDATOR
                            }),
                            has_entries({
                                'partner': SuburbanCarrierCode.CPPK,
                                'type': TariffTypeCode.USUAL,
                                'price': 200,
                                'selling_flow': SuburbanSellingFlow.VALIDATOR
                            })
                        )
                    }),
                    has_entries({
                        'provider': SuburbanTariffProvider.AEROEXPRESS,
                        'tariffs': contains_inanyorder(
                            has_entries({
                                'partner': SuburbanCarrierCode.AEROEXPRESS,
                                'type': TariffTypeCode.AEROEXPRESS,
                                'price': 400,
                                'selling_flow': SuburbanSellingFlow.AEROEXPRESS
                            })
                        )
                    }),
                ),

                'keys': contains_inanyorder(
                    has_entries({
                        'key': has_properties({
                            'date': datetime(2021, 10, 7),
                            'station_from': 100,
                            'station_to': 200,
                            'company': Company.SZPPK_ID,
                            'tariff_type': 'etrain'
                        }),
                        'provider': SuburbanTariffProvider.IM,
                        'tariff_ids': contains_inanyorder(
                            *tariff_ids_by_partner_and_type[(SuburbanCarrierCode.SZPPK, TariffTypeCode.USUAL)]
                        )
                    }),
                    has_entries({
                        'key': has_properties({
                            'date': datetime(2021, 10, 7),
                            'station_from': 100,
                            'station_to': 200,
                            'company': Company.SZPPK_ID,
                            'tariff_type': 'express'
                        }),
                        'provider': SuburbanTariffProvider.IM,
                        'tariff_ids': contains_inanyorder(
                            *tariff_ids_by_partner_and_type[(SuburbanCarrierCode.SZPPK, TariffTypeCode.EXPRESS)]
                        )
                    }),
                    has_entries({
                        'key': has_properties({
                            'date': datetime(2021, 10, 7),
                            'station_from': 100,
                            'station_to': 200,
                            'company': Company.CPPK_ID,
                            'tariff_type': 'etrain'
                        }),
                        'provider': SuburbanTariffProvider.MOVISTA,
                        'tariff_ids': contains_inanyorder(
                            *tariff_ids_by_partner_and_type[(SuburbanCarrierCode.CPPK, TariffTypeCode.USUAL)]
                        )
                    }),
                    has_entries({
                        'key': has_properties({
                            'date': datetime(2021, 10, 7),
                            'station_from': 100,
                            'station_to': 200,
                            'company': Company.CPPK_AEROEX_ID,
                            'tariff_type': 'etrain'
                        }),
                        'provider': SuburbanTariffProvider.MOVISTA,
                        'tariff_ids': contains_inanyorder(
                            *tariff_ids_by_partner_and_type[(SuburbanCarrierCode.CPPK, TariffTypeCode.USUAL)]
                        )
                    }),
                    has_entries({
                        'key': has_properties({
                            'date': datetime(2021, 10, 7),
                            'station_from': 100,
                            'station_to': 300,
                            'company': Company.CPPK_AEROEX_ID,
                            'tariff_type': TariffTypeCode.USUAL
                        }),
                        'provider': SuburbanTariffProvider.AEROEXPRESS,
                        'tariff_ids': contains_inanyorder(
                            *tariff_ids_by_partner_and_type[
                                (SuburbanCarrierCode.AEROEXPRESS, TariffTypeCode.AEROEXPRESS)
                            ]
                        )
                    }),
                    has_entries({
                        'key': has_properties({'station_from': 999}),
                        'tariff_ids': []
                    }),
                    has_entries({
                        'key': has_properties({'tariff_type': 'unknown'}),
                        'tariff_ids': []
                    })
                ),

                'selling_partners': contains_inanyorder(
                    has_entries({
                        'code': SuburbanCarrierCode.CPPK,
                        'provider': SuburbanTariffProvider.MOVISTA,
                        'title': 'ЦППК',
                        'ogrn': 'ogrn_cppk',
                        'address': 'address_cppk',
                        'work_time': 'time_cppk'
                    }),
                    has_entries({
                        'code': SuburbanCarrierCode.SZPPK,
                        'provider': SuburbanTariffProvider.IM,
                        'title': 'СЗППК',
                        'ogrn': 'ogrn_szppk',
                        'address': 'address_szppk',
                        'work_time': 'time_szppk'
                    }),
                    has_entries({
                        'code': SuburbanCarrierCode.AEROEXPRESS,
                        'provider': SuburbanTariffProvider.AEROEXPRESS,
                        'title': 'Аэроэкспресс'
                    })
                )
            }))
