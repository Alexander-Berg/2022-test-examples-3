# coding: utf8
import json

from travel.rasp.smoke_tests.smoke_tests.stableness import StablenessVariants
from travel.rasp.smoke_tests.smoke_tests.config.suburban_selling.suburban_order import (
    suburban_order_check, suburban_order_with_test_context_check_list, TestContextType
)
from travel.rasp.smoke_tests.smoke_tests.config.utils import msk_tomorrow_str
from travel.rasp.smoke_tests.smoke_tests.config.suburban_selling.helpers import (
    GetTariffsCheck, GetTariffsEmptinessCheck
)
from travel.rasp.smoke_tests.smoke_tests.config.suburban_selling.env import env


urls = [
    'ping',

    # bad auth
    ['get_user_data', {'headers': {'Authorization': 'OAuth: aaaaaaaaaaaa'}, 'code': 403}],

    # Перевозчик, для которого пока нет продаж
    [
        'get_tariffs',
        {
            'data': json.dumps({
                'keys': [{
                    'date': msk_tomorrow_str,
                    'station_from': 9607404,  # Екатеринбург
                    'station_to': 9607483,  # Нижний Тагил
                    'company': 1331,  # СВППК
                    'tariff_type': 'etrain',
                }]
            }),
            'processes': [GetTariffsEmptinessCheck()]
        }
    ],

    # Аэроэкспресс
    [
        'get_tariffs',
        {
            'data': json.dumps({
                'keys': [{
                    'date': msk_tomorrow_str,
                    'station_from': 2000006,  # Белорусский
                    'station_to': 9881841,  # Шереметьево, ЮГ
                    'company': 162,  # Аэроэкспресс
                    'tariff_type': 'aeroexpress',
                }]
            }),
            'processes': [GetTariffsCheck('aeroexpress', 'menu_id')]
        }
    ],

    # Мовиста
    [
        'get_tariffs',
        {
            'data': json.dumps({
                'keys': [{
                    'date': msk_tomorrow_str,
                    'station_from': 2000005,  # Павелецкий
                    'station_to': 9600216,  # Домодедово
                    'company': 153,  # ЦППК
                    'tariff_type': 'etrain',
                }]
            }),
            'processes': [GetTariffsCheck('movista', 'book_data')],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],
    [
        'get_tariffs',
        {
            'data': json.dumps({
                'keys': [{
                    'date': msk_tomorrow_str,
                    'station_from': 9600721,  # Одинцово
                    'station_to': 2000006,  # Белорусский
                    'company': 63448,  # ЦППК + АЭ
                    'tariff_type': 'etrain',
                }]
            }),
            'processes': [GetTariffsCheck('movista', 'book_data')],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Аэроэкспресс. Павелецкий вокзал - Домодедово, создание заказа и один вызов order_info
    suburban_order_check(
        test_name='Aeroexpress order. Create and start payment',
        provider='aeroexpress',
        tariff_type='aeroexpress',
        st_from_esr='193519', st_from_id=2000005,
        st_to_esr='193114', st_to_id=9600216
    ),

    # Мовиста. ЦППК. Курский вокзал - Подольск, создание заказа, отслеживание состояния до появления ссылки на оплату
    suburban_order_check(
        test_name='Movista order. Create and start payment',
        provider='movista',
        tariff_type='etrain',
        st_from_esr='191602', st_from_id=2000001,
        st_to_esr='190900', st_to_id=9600731
    ),

    # ИМ. СЗППК. Финдляндский вокзал - Удельная, создание заказа, отслеживание состояния до появления ссылки на оплату
    suburban_order_check(
        test_name='IM SZPPK order. Create and start payment',
        provider='im',
        tariff_type='etrain',
        st_from_esr='038205', st_from_id=9602497,
        st_to_esr='038525', st_to_id=9603463
    ),

    # ИМ. МТППК. Тверь - Ленинградский вокзал, создание заказа, отслеживание состояния до появления ссылки на оплату
    suburban_order_check(
        test_name='IM MTPPK order. Create and start payment',
        provider='im',
        tariff_type='etrain',
        st_from_esr='061502', st_from_id=9603093,
        st_to_esr='060073', st_to_id=2006004
    )
]

if env.allow_test_context:
    # Аэроэкспресс. Павелецкий вокзал - Домодедово. Оплата через тестовый контекст
    urls.extend([
        suburban_order_check(
            test_name='Аэроэкспресс. Payment with test context',
            provider='aeroexpress',
            tariff_type='aeroexpress',
            st_from_esr='193519', st_from_id=2000005,
            st_to_esr='193114', st_to_id=9600216,
            test_context_type=TestContextType.NONE,
            use_payment_test_context=True,
            stableness=StablenessVariants.UNSTABLE
        )
    ])
    urls.extend(
        # Мовиста. ЦППК. Курский вокзал - Подольск, работа с заказом через тестовый контекст
        suburban_order_with_test_context_check_list(
            tests_set_name='Movista order',
            provider='movista',
            tariff_type='etrain',
            st_from_esr=191602, st_from_id=2000001,
            st_to_esr=190900, st_to_id=9600731
        )
    )
    urls.extend(
        # ИМ. СЗППК. Финдляндский вокзал - Удельная, работа с заказом через тестовый контекст
        suburban_order_with_test_context_check_list(
            tests_set_name='IM SZPPK order',
            provider='im',
            tariff_type='etrain',
            st_from_esr='038205', st_from_id=9602497,
            st_to_esr='038525', st_to_id=9603463
        )
    )
    urls.extend(
        # ИМ. МТППК. Тверь - Ленинградский вокзал, работа с заказом через тестовый контекст
        suburban_order_with_test_context_check_list(
            tests_set_name='IM MTPPK order',
            provider='im',
            tariff_type='etrain',
            st_from_esr='061502', st_from_id=9603093,
            st_to_esr='060073', st_to_id=2006004
        )
    )

checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout},
        'urls': urls
    }
]
