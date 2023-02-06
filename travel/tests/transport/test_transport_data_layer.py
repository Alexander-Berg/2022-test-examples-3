# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import (
    create_country, create_settlement, create_station, create_transport_type, create_thread
)
from common.tester.testcase import TestCase

from travel.rasp.morda_backend.morda_backend.transport.views import make_transport_result


create_thread = create_thread.mutate(__={'calculate_noderoute': True})

FULL_TRANSPORT_DATA = {
    'train': {
        'RU_': {
            'main_city': 'moscow_slug',
            'secondary_city': 'piter_slug',
            'cities': [
                'eburg_slug',
                'vasuki_slug',
                'urupinsk_slug',
                'anadir_slug'
            ]
        },
        'KZ': {
            'main_city': 'nur-sultan_slug',
            'cities': [
                'karaganda_slug',
                'alma-ata_slug'
            ]
        }
    },
    'suburban': {
        'RU_': {
            'main_city': 'moscow_slug',
            'secondary_city': 'piter_slug',
            'cities': [
                'eburg_slug',
                'vasuki_slug',
                'urupinsk_slug'
            ]
        }
    },
    'bus': {
        'RU_': {
            'main_city': 'moscow_slug',
            'secondary_city': 'piter_slug',
            'cities': [
                'eburg_slug'
            ]
        },
        'KZ': {
            'main_city': 'nur-sultan_slug',
            'cities': []
        },
        'UA': {
            'main_city': 'kiev_slug'
        }
    }
}


def _make_init_data():
    train = create_transport_type(TransportType.TRAIN_ID)
    bus = create_transport_type(TransportType.BUS_ID)

    russia = create_country(
        id=Country.BELARUS_ID, code='RU_', title='Россия', title_ru_genitive='России',
        title_ru_preposition_v_vo_na='в', title_ru_locative='России'
    )
    kazakhstan = create_country(
        id=Country.KAZAKHSTAN_ID, code='KZ', title='Казахстан', title_ru_genitive='Казахстана',
        title_ru_preposition_v_vo_na='в', title_ru_locative='Казахстане'
    )
    ukraine = create_country(
        id=Country.UKRAINE_ID, code='UA', title='Украина', title_ru_genitive='Украины',
        title_ru_preposition_v_vo_na='в', title_ru_locative='Украине'
    )

    moscow = create_settlement(
        id=101, title='Москва', title_ru_genitive='Москвы',
        slug='moscow_slug', country=russia
    )
    create_station(
        settlement=moscow, id=1011, title='Питерский вокзал', t_type=train, majority=1, type_choices='train,suburban'
    )
    create_station(
        settlement=moscow, id=1012, title='Свердловский вокзал', t_type=train, majority=1, type_choices='train'
    )
    create_station(
        settlement=moscow, id=1013, title='Васюковский вокзал', t_type=train, majority=2, type_choices='train'
    )
    create_station(
        settlement=moscow, id=1014, title='Урюпинский вокзал', t_type=train, majority=1, type_choices='train'
    )
    create_station(
        settlement=moscow, id=1015, title='Левая станция', t_type=train, majority=1, hidden=True, type_choices=''
    )
    create_station(
        settlement=moscow, id=1016, title='Автобусный вокзал', t_type=bus, majority=1, type_choices='schedule'
    )
    create_station(settlement=moscow, id=1017, title='Автовокзал', t_type=bus, majority=1, type_choices='schedule')
    create_station(settlement=moscow, id=1018, title='Остановочка', t_type=bus, majority=3, type_choices='schedule')

    piter = create_settlement(
        id=102, title='Питер', title_ru_genitive='Питера',
        slug='piter_slug', country=russia
    )
    create_station(settlement=piter, id=1021, title='Московский вокзал', t_type=train, majority=1, type_choices='train')
    create_station(settlement=piter, id=1022, title='Финский вокзал', t_type=train, majority=1, type_choices='train')
    create_station(settlement=piter, id=1023, title='Старая деревня', t_type=train, majority=3, type_choices='train')
    create_station(settlement=piter, id=1024, title='Автостанция', t_type=bus, majority=1, type_choices='schedule')

    eburg = create_settlement(id=103, title='Екатеринбург', slug='eburg_slug', country=russia)
    create_station(settlement=eburg, id=1031, t_type=train, type_choices='train,suburban')
    create_station(settlement=eburg, id=1032, t_type=train, type_choices='train,suburban')
    create_station(settlement=eburg, id=1033, t_type=bus, type_choices='schedule')
    create_station(settlement=eburg, id=1034, t_type=bus, type_choices='schedule')

    vasuki = create_settlement(id=104, title='Васюки', slug='vasuki_slug', country=russia)
    create_station(settlement=vasuki, id=1041, t_type=train, title='ст.Васюки', type_choices='train')
    create_station(settlement=vasuki, id=1042, t_type=bus, title='ост.Васюки', type_choices='schedule')
    create_station(settlement=vasuki, id=1043, t_type=bus, title='мал.Васюки', majority=3, type_choices='schedule')

    urupinsk = create_settlement(id=105, title='Урюпинск', slug='urupinsk_slug', country=russia)
    create_station(settlement=urupinsk, id=1051, t_type=train, title='Урюп', type_choices='train,suburban')
    create_station(settlement=urupinsk, id=1052, t_type=train, title='Урюп', majority=4, type_choices='train')

    create_settlement(id=106, title='Анадырь', slug='anadir_slug', country=russia)

    create_settlement(id=107, title='Маленький город', slug='small_city_slug', country=russia)

    nur_sultan = create_settlement(
        id=201, title='Нур-Султан', title_ru_genitive='Нур-Султана',
        slug='nur-sultan_slug', country=kazakhstan
    )
    create_station(
        settlement=nur_sultan, id=2011, title='Вокзал им. Назарбаева', t_type=train, majority=1, type_choices='train'
    )
    create_station(
        settlement=nur_sultan, id=2012, title='Нурсултан главный', t_type=train, majority=1, type_choices='train'
    )
    create_station(
        settlement=nur_sultan, id=2013, title='Назарбаевская', t_type=train, majority=3, type_choices='train'
    )
    create_station(
        settlement=nur_sultan, id=2014, title='Автовокзал Назарбаева', t_type=bus, majority=1, type_choices='schedule'
    )
    create_station(
        settlement=nur_sultan, id=2015, title='ост. ул. Назарбаева', t_type=bus, majority=3, type_choices='schedule'
    )

    karaganda = create_settlement(id=202, title='Караганда', slug='karaganda_slug', country=kazakhstan)
    create_station(settlement=karaganda, id=2021, t_type=train, title='Караганда-Пасс', type_choices='train')
    create_station(settlement=karaganda, id=2022, t_type=bus, title='Караганда-Бас', type_choices='schedule')

    alma_ata = create_settlement(id=203, title='Алма-Ата', slug='alma-ata_slug', country=kazakhstan)
    create_station(settlement=alma_ata, id=2031, t_type=train, type_choices='train')
    create_station(settlement=alma_ata, id=2032, t_type=train, type_choices='train')

    create_settlement(id=204, title='Маленький казахский город', slug='small_kz_city_slug', country=kazakhstan)

    create_settlement(id=301, title='Киев', title_ru_genitive='Киева', slug='kiev_slug', country=ukraine)

    create_settlement(id=302, title='Донецк', slug='donetsk_slug', country=ukraine)


COUNTRIES_1 = [
    {
        'code': 'RU_',
        'title': 'Россия',
        'title_genitive': 'России',
        'title_prepositional': 'в России',
    }
]


COUNTRIES_2 = [
    {
        'code': 'KZ',
        'title': 'Казахстан',
        'title_genitive': 'Казахстана',
        'title_prepositional': 'в Казахстане',
    }
] + COUNTRIES_1


COUNTRIES_3 = COUNTRIES_2 + [
    {
        'code': 'UA',
        'title': 'Украина',
        'title_genitive': 'Украины',
        'title_prepositional': 'в Украине'
    }
]


class TestTransport(TestCase):
    def test_get_transport_data(self):
        _make_init_data()

        result = make_transport_result(FULL_TRANSPORT_DATA, 'train', 'RU_', 'ru')

        assert result == {
            'main_city': {
                'slug': 'moscow_slug',
                'title': 'Москва',
                'title_genitive': 'Москвы',
                'stations': [
                    {
                        'page_type' : 'train',
                        'main_subtype': 'train',
                        'title': 'Питерский вокзал',
                        'id': 1011
                    },
                    {
                        'page_type' : 'train',
                        'main_subtype': 'train',
                        'title': 'Свердловский вокзал',
                        'id': 1012
                    },
                    {
                        'page_type': 'train',
                        'main_subtype': 'train',
                        'title': 'Урюпинский вокзал',
                        'id': 1014
                    },
                    {
                        'page_type': 'train',
                        'main_subtype': 'train',
                        'title': 'Васюковский вокзал',
                        'id': 1013
                    },
                ]
            },
            'secondary_city': {
                'slug': 'piter_slug',
                'title': 'Питер',
                'title_genitive': 'Питера',
                'stations': [
                    {
                        'page_type': 'train',
                        'main_subtype': 'train',
                        'title': 'Московский вокзал',
                        'id': 1021
                    },
                    {
                        'page_type': 'train',
                        'main_subtype': 'train',
                        'title': 'Финский вокзал',
                        'id': 1022
                    }
                ]
            },
            'cities': [
                {
                    'slug': 'eburg_slug',
                    'title': 'Екатеринбург'
                },
                {
                    'page_type': 'train',
                    'main_subtype': 'train',
                    'station_id': 1041,
                    'title': 'Васюки'
                },
                {
                    'page_type': 'train',
                    'main_subtype': 'train',
                    'station_id': 1051,
                    'title': 'Урюпинск'
                }
            ],
            'countries': COUNTRIES_2
        }

        result = make_transport_result(FULL_TRANSPORT_DATA, 'train', 'KZ', 'ru')

        assert result == {
            'main_city': {
                'slug': 'nur-sultan_slug',
                'title': 'Нур-Султан',
                'title_genitive': 'Нур-Султана',
                'stations': [
                    {
                        'page_type': 'train',
                        'main_subtype': 'train',
                        'title': 'Вокзал им. Назарбаева',
                        'id': 2011
                    },
                    {
                        'page_type': 'train',
                        'main_subtype': 'train',
                        'title': 'Нурсултан главный',
                        'id': 2012
                    }
                ]
            },
            'cities': [
                {
                    'page_type': 'train',
                    'main_subtype': 'train',
                    'station_id': 2021,
                    'title': 'Караганда'
                },
                {
                    'slug': 'alma-ata_slug',
                    'title': 'Алма-Ата'

                }
            ],
            'countries': COUNTRIES_2
        }

        result = make_transport_result(FULL_TRANSPORT_DATA, 'suburban', 'RU_', 'ru')

        assert result == {
            'main_city': {
                'slug': 'moscow_slug',
                'title': 'Москва',
                'title_genitive': 'Москвы',
                'stations': [
                    {
                        'page_type': 'train',
                        'main_subtype': 'train',
                        'title': 'Питерский вокзал',
                        'id': 1011
                    }
                ]
            },
            'secondary_city': {
                'slug': 'piter_slug',
                'title': 'Питер',
                'title_genitive': 'Питера',
                'stations': []
            },
            'cities': [
                {
                    'slug': 'eburg_slug',
                    'title': 'Екатеринбург'
                },
                {
                    'page_type': 'train',
                    'main_subtype': 'train',
                    'station_id': 1051,
                    'title': 'Урюпинск'
                }
            ],
            'countries': COUNTRIES_1
        }

        result = make_transport_result(FULL_TRANSPORT_DATA, 'bus', 'RU_', 'ru')

        assert result == {
            'main_city': {
                'slug': 'moscow_slug',
                'title': 'Москва',
                'title_genitive': 'Москвы',
                'stations': [
                    {
                        'page_type': 'bus',
                        'main_subtype': 'schedule',
                        'title': 'Автобусный вокзал',
                        'id': 1016
                    },
                    {
                        'page_type': 'bus',
                        'main_subtype': 'schedule',
                        'title': 'Автовокзал',
                        'id': 1017
                    }
                ]
            },
            'secondary_city': {
                'slug': 'piter_slug',
                'title': 'Питер',
                'title_genitive': 'Питера',
                'stations': [
                    {
                        'page_type': 'bus',
                        'main_subtype': 'schedule',
                        'title': 'Автостанция',
                        'id': 1024
                    }
                ]
            },
            'cities': [
                {
                    'slug': 'eburg_slug',
                    'title': 'Екатеринбург'

                }
            ],
            'countries': COUNTRIES_3
        }

        result = make_transport_result(FULL_TRANSPORT_DATA, 'bus', 'KZ', 'ru')

        assert result == {
            'main_city': {
                'slug': 'nur-sultan_slug',
                'title': 'Нур-Султан',
                'title_genitive': 'Нур-Султана',
                'stations': [
                    {
                        'page_type': 'bus',
                        'main_subtype': 'schedule',
                        'title': 'Автовокзал Назарбаева',
                        'id': 2014
                    }
                ]
            },
            'countries': COUNTRIES_3
        }

        result = make_transport_result(FULL_TRANSPORT_DATA, 'bus', 'UA', 'ru')

        assert result == {
            'main_city': {
                'slug': 'kiev_slug',
                'title': 'Киев',
                'title_genitive': 'Киева',
                'stations': []
            },
            'countries': COUNTRIES_3
        }
