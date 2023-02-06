# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
from django.test import Client

from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import create_country, create_settlement, create_station, create_transport_type
from common.tester.testcase import TestCase

from travel.rasp.morda_backend.morda_backend.transport.serialization import TransportSchema


TRAIN_RU_RESULT = {
    'mainCity': {
        'slug': 'moscow_slug',
        'title': 'Москва',
        'titleGenitive': 'Москвы',
        'stations': [
            {
                'pageType': 'train',
                'mainSubtype': 'train',
                'title': 'Питерский вокзал',
                'id': 1011
            },
            {
                'pageType': 'train',
                'mainSubtype': 'train',
                'title': 'Свердловский вокзал',
                'id': 1012
            },
            {
                'pageType': 'train',
                'mainSubtype': 'train',
                'title': 'Урюпинский вокзал',
                'id': 1014
            },
            {
                'pageType': 'train',
                'mainSubtype': 'train',
                'title': 'Васюковский вокзал',
                'id': 1013
            }
        ]
    },
    'secondaryCity': {
        'slug': 'piter_slug',
        'title': 'Питер',
        'titleGenitive': 'Питера',
        'stations': [
            {
                'pageType': 'train',
                'mainSubtype': 'train',
                'title': 'Московский вокзал',
                'id': 1021
            },
            {
                'pageType': 'train',
                'mainSubtype': 'train',
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
            'pageType': 'train',
            'mainSubtype': 'train',
            'stationId': 1041,
            'title': 'Васюки'
        },
        {
            'pageType': 'train',
            'mainSubtype': 'train',
            'stationId': 1051,
            'title': 'Урюпинск'
        }
    ],
    'countries': [
        {
            'code': 'KZ',
            'title': 'Казахстан',
            'titleGenitive': 'Казахстана',
            'titlePrepositional': 'в Казахстане'
        },
        {
            'code': 'RU_',
            'title': 'Россия',
            'titleGenitive': 'России',
            'titlePrepositional': 'в России'
        }
    ]
}


class TestTransportSchema(TestCase):
    def test_transport_schema(self):
        data = {
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
                    },
                    {
                        'page_type': 'train',
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
                    }
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
            'countries': [
                {
                    'code': 'KZ',
                    'title': 'Казахстан',
                    'title_genitive': 'Казахстана',
                    'title_prepositional': 'в Казахстане'
                },
                {
                    'code': 'RU_',
                    'title': 'Россия',
                    'title_genitive': 'России',
                    'title_prepositional': 'в России'
                }
            ]
        }
        result, _ = TransportSchema().dump(data)
        assert result == TRAIN_RU_RESULT


FULL_TRANSPORT_DATA = {
    'train': {
        'RU_': {
            'main_city': 'moscow_slug',
            'secondary_city': 'piter_slug',
            'cities': [
                'eburg_slug',
                'vasuki_slug',
                'urupinsk_slug'
            ]
        },
        'KZ': {
            'main_city': 'nur-sultan_slug',
            'cities': [
                'alma-ata_slug'
            ]
        }
    }
}


def _make_init_data():
    train = create_transport_type(TransportType.TRAIN_ID)
    bus = create_transport_type(TransportType.BUS_ID)

    russia = create_country(
        code='RU_', title='Россия', id=Country.BELARUS_ID,
        title_ru_genitive='России', title_ru_preposition_v_vo_na='в', title_ru_locative='России'
    )
    create_country(
        code='KZ', title='Казахстан', id=Country.KAZAKHSTAN_ID,
        title_ru_genitive='Казахстана', title_ru_preposition_v_vo_na='в', title_ru_locative='Казахстане'
    )
    create_country(
        code='UA', title='Украина', id=Country.UKRAINE_ID,
        title_ru_genitive='Украины', title_ru_preposition_v_vo_na='в', title_ru_locative='Украине'
    )

    moscow = create_settlement(
        id=101, title='Москва', title_ru_genitive='Москвы', slug='moscow_slug', country=russia
    )
    create_station(
        settlement=moscow, id=1011, title='Питерский вокзал', t_type=train, majority=1, type_choices='train'
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
        settlement=moscow, id=1015, title='Левая станция', t_type=train, majority=1, hidden=True
    )
    create_station(
        settlement=moscow, id=1016, title='Автобусный вокзал', t_type=bus, majority=1, type_choices='schedule'
    )

    piter = create_settlement(id=102, title='Питер', title_ru_genitive='Питера', slug='piter_slug', country=russia)
    create_station(settlement=piter, id=1021, title='Московский вокзал', t_type=train, majority=1, type_choices='train')
    create_station(settlement=piter, id=1022, title='Финский вокзал', t_type=train, majority=1, type_choices='train')
    create_station(settlement=piter, id=1023, title='Старая деревня', t_type=train, majority=3, type_choices='train')
    create_station(settlement=piter, id=1024, title='Автостанция', t_type=bus, majority=1, type_choices='schedule')

    eburg = create_settlement(id=103, title='Екатеринбург', slug='eburg_slug', country=russia)
    create_station(settlement=eburg, id=1031, t_type=train, type_choices='train')
    create_station(settlement=eburg, id=1032, t_type=train, type_choices='train')

    vasuki = create_settlement(id=104, title='Васюки', slug='vasuki_slug', country=russia)
    create_station(settlement=vasuki, id=1041, t_type=train, title='ст.Васюки', type_choices='train')
    create_station(settlement=vasuki, id=1042, t_type=bus, title='ост.Васюки', type_choices='schedule')

    urupinsk = create_settlement(id=105, title='Урюпинск', slug='urupinsk_slug', country=russia)
    create_station(settlement=urupinsk, id=1051, t_type=train, title='Урюп', type_choices='train')

    create_settlement(id=106, title='Маленький город', slug='small_city_slug', country=russia)


class TestTransportResponse(TestCase):
    def test_transport_response(self):
        _make_init_data()
        client = Client()

        with mock.patch(
                'travel.rasp.morda_backend.morda_backend.transport.views.get_full_transport_data',
                return_value=FULL_TRANSPORT_DATA
        ):
            response = client.get('/ru/transport/train/?country=RU_')
            assert response.status_code == 200
            data = json.loads(response.content)
            assert data == TRAIN_RU_RESULT

            response = client.get('/ru/transport/train/?country=USSR')
            assert response.status_code == 404

            response = client.get('/ru/transport/train/')
            assert response.status_code == 404

            response = client.get('/ru/transport/plane/?country=RU_')
            assert response.status_code == 404

            response = client.get('/ru/transport/train/?country=UA')
            assert response.status_code == 404
