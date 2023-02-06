# coding: utf8
import json
import requests
import time
import uuid
from urllib.parse import urlencode

from travel.library.python.rasp_vault.api import get_secret
from travel.library.python.tvm_ticket_provider import provider_fabric

from travel.rasp.smoke_tests.smoke_tests.settings import SMOKE_TESTS_TVM_ID
from travel.rasp.smoke_tests.smoke_tests.config.suburban_selling.env import env
from travel.rasp.smoke_tests.smoke_tests.config.utils import msk_tomorrow_str
from travel.rasp.smoke_tests.smoke_tests.stableness import StablenessVariants


class TestContextType(object):
    NONE = 'none'
    WITHOUT_RETRIES = 'without_retries'
    WITH_RETRIES = 'with_retries'


def _make_travel_api_headers(ya_uuid):
    """Формирование заголовков для запроса в Travel API, включая tvm-тикет"""
    provider = provider_fabric.create(
        source_id=SMOKE_TESTS_TVM_ID,
        destinations=[env.travel_api_tvm_service_id],
        secret=get_secret('rasp-smoke-tests.TVM_SECRET')
    )
    tvm_ticket = provider.get_ticket(env.travel_api_tvm_service_id)

    return {
        'Content-Type': 'application/json',
        'X-Ya-YandexUid': ya_uuid,
        'X-Ya-Session-Key': ya_uuid,
        'X-Ya-Service-Ticket': tvm_ticket,
    }


def _get_test_context_token(ya_uuid, test_context_type):
    """Получение из Travel API токена тестового контекста"""
    params = {
        'ticketBody': '333',
        'ticketNumber': '222',
        # 'actualPrice': '78',
        'validForSeconds': '1800',
    }
    if test_context_type == TestContextType.WITH_RETRIES:
        params.update({
            'bookHandlerErrorType': 'ET_RETRYABLE',
            'bookHandlerErrorCount': 1,
            'confirmHandlerErrorType': 'ET_RETRYABLE',
            'confirmHandlerErrorCount': 1,
            'ticketBarcodeHandlerErrorType': 'ET_RETRYABLE',
            'ticketBarcodeHandlerErrorCount': 1,
            'blankPdfHandlerErrorType': 'ET_RETRYABLE',
            'blankPdfHandlerErrorCount': 1
        })

    response = requests.get(
        f'{env.travel_api_host}/test_context/v1/suburban_token',
        params=params,
        headers=_make_travel_api_headers(ya_uuid),
        timeout=env.travel_api_timeout
    )
    return response.json()['test_context_token']


def _get_payment_test_context_token(ya_uuid):
    """Получение из Travel API токена тестового контекста продажи"""
    response = requests.get(
        f'{env.travel_api_host}/test_context/v1/payment_token',
        params={'paymentOutcome': 'PO_SUCCESS'},
        headers=_make_travel_api_headers(ya_uuid),
        timeout=env.travel_api_timeout
    )
    return response.json()['token']


def _make_base_headers():
    """Создание заголовков, общих для всех ручек"""
    ya_uuid = str(uuid.uuid4())
    ya_device_id = str(uuid.uuid4())

    return {
        'Content-Type': 'application/json',
        'X-Ya-Uuid': ya_uuid,
        'X-Ya-Device-Id': ya_device_id,
        'X-Real-IP': '1.2.3.4',
        'User-Agent': 'App: SmokeTests 1.0(1.0); OS: SmokeTests 1.0'
    }


class OrderInfoPoll(object):
    """
    Проверка ответа ручки order_info
    И повторный вызов ручки, если не получен нужный статус и не сформировались нужные поля
    """
    def __init__(self, waiting_statuses, waiting_fields=None, sleep_time=1, max_retries=10):
        self.waiting_statuses = waiting_statuses
        self.waiting_fields = waiting_fields
        self.sleep_time = sleep_time
        self.max_retries = max_retries
        self.current_retry_number = 0

    def __call__(self, checker, response):
        order_data = json.loads(response.content)

        if order_data['status'] == 'CANCELLED':
            raise Exception('Order is cancelled')

        if order_data['status'] in self.waiting_statuses:
            has_waiting_fields = True
            if self.waiting_fields:
                for field in self.waiting_fields:
                    if field not in order_data:
                        has_waiting_fields = False
            if has_waiting_fields:
                return

        if self.current_retry_number >= self.max_retries:
            raise Exception(f'Timeout in order_info during waiting status {", ".join(self.waiting_statuses)}')

        time.sleep(self.sleep_time)
        self.current_retry_number += 1
        checker()


def _make_order_info_calls(provider, suburban_order, base_headers, use_payment_test_context):
    """Поллинг созданного заказа через order_info"""

    headers = base_headers.copy()
    headers['Content-Type'] = 'application/json'

    if provider == 'aeroexpress':
        # Для аэроэкспресса вызываем order_info всего один раз
        return [
            lambda: f'order_info?version=3&uid={suburban_order.uid}&command=start_payment&is_aeroexpress=true',
            {'headers': headers}
        ]

    # Формирование билета вызываем только если задан тестовый контекст оплаты,
    # в этом случае пропускаем получение урла оплаты
    if use_payment_test_context:
        waiting_fields = ['ticket_body', 'price', 'ticket_number']
        if provider == 'movista':
            waiting_fields.append('wicket')
        second_order_info_call = [[
            lambda: f'order_info?version=3&uid={suburban_order.uid}&command=get_ticket',
            {
                'headers': headers,
                'processes': [OrderInfoPoll(
                    ['CONFIRMED'],
                    waiting_fields=waiting_fields,
                    sleep_time=env.order_info_retries_delay, max_retries=env.order_info_retries_number
                )]
            }
        ]]
    # Если не задан тестовый контекст оплаты, то заканчиваем после получения урла оплаты
    else:
        second_order_info_call = [[
            lambda: f'order_info?version=3&uid={suburban_order.uid}&command=get_payment_data',
            {
                'headers': headers,
                'processes': [OrderInfoPoll(
                    ['WAITING_PAYMENT'],
                    waiting_fields=['payment_url'],
                    sleep_time=env.order_info_retries_delay, max_retries=env.order_info_retries_number
                )]
            }
        ]]

    return [
        lambda: f'order_info?version=3&uid={suburban_order.uid}&command=start_payment',
        {
            'headers': headers,
            'processes': [OrderInfoPoll(
                ['RESERVED', 'WAITING_PAYMENT', 'CONFIRMED'],
                sleep_time=env.order_info_retries_delay, max_retries=env.order_info_retries_number
            )]
        },
        second_order_info_call
    ]


def _make_orders_list_info_call(suburban_order, provider, headers):
    """Запуск ручки order_list_info"""
    return [
        'orders_list_info',
        lambda: {
            'headers': headers,
            'data': json.dumps({
                'orders': [
                    {
                        'uid': suburban_order.uid,
                        'provider': provider
                    }
                ]
            })
        }
    ]


def _make_user_action_event_call(suburban_order, base_headers):
    """Активация билета в Мовисте"""
    headers = base_headers.copy()
    headers['Content-Type'] = 'application/x-www-form-urlencoded'
    return [
        'user_action_event',
        lambda: {
            'headers': headers,
            'data': urlencode({
                'uid': suburban_order.uid,
                'is_success': True,
                'qr_body': 'QR'
            })
        }
    ]


class SuburbanOrder(object):
    """Заказ, созданный в create_order и используемый далее в order_info"""
    def set_params(self, uid):
        self.uid = uid


class SetSuburbanOrder(object):
    """Запоминание созданного заказа"""
    def __init__(self, suburban_order):
        self.suburban_order = suburban_order

    def __call__(self, checker, response):
        order_data = response.json()
        self.suburban_order.set_params(
            uid=order_data['uid']
        )
        time.sleep(1)


def _make_create_order_headers(base_headers, test_context_type, use_payment_test_context):
    headers = base_headers.copy()
    ya_uuid = headers['X-Ya-Uuid']
    headers['Content-Type'] = 'application/x-www-form-urlencoded'
    if test_context_type != TestContextType.NONE:
        headers['X-Test-Context-Token'] = _get_test_context_token(ya_uuid, test_context_type)
    if use_payment_test_context:
        headers['X-Payment-Test-Context-Token'] = _get_payment_test_context_token(ya_uuid)
    return headers


def _make_create_order_call(
    selling_tariff, provider, st_from_id, st_to_id, base_headers, test_context_type, use_payment_test_context
):
    """Создание заказа"""
    suburban_order = SuburbanOrder()

    next_calls = [_make_order_info_calls(provider, suburban_order, base_headers, use_payment_test_context)]
    if provider != 'aeroexpress':
        next_calls.append(_make_orders_list_info_call(suburban_order, provider, base_headers))
        if use_payment_test_context and test_context_type == TestContextType.NONE:
            next_calls.append(_make_user_action_event_call(suburban_order, base_headers))

    create_order_headers = _make_create_order_headers(base_headers, test_context_type, use_payment_test_context)

    return [
        'create_order',
        lambda: {
            'data': urlencode({
                'version': 3,
                'provider': provider,
                'station_from': st_from_id,
                'station_to': st_to_id,
                'user_info': json.dumps({
                    'email': 'test@testtest.ru'
                }),
                'departure_date': msk_tomorrow_str,
                'price': selling_tariff.price,
                'partner': selling_tariff.partner,
                'book_data': selling_tariff.book_data
            }),
            'headers': create_order_headers,
            'processes': [SetSuburbanOrder(suburban_order)]
        },
        next_calls
    ]


class SellingTariff(object):
    """Тариф продажи билетов на электричку, полученный из поиска в экспорте, используемый в create_order"""
    def set_params(self, book_data, price, partner):
        self.book_data = book_data
        self.price = price
        self.partner = partner


class SetSellingTariff(object):
    """Поиск нужного тарифа в ответе ручки поиска экспорта"""
    def __init__(self, provider, tariff_type, selling_tariff):
        self.provider = provider
        self.tariff_type = tariff_type
        self.selling_tariff = selling_tariff

    def __call__(self, checker, response):
        for tariff in response.json()['selling_tariffs']:
            if tariff['provider'] == self.provider and tariff['type'] == self.tariff_type:
                self.selling_tariff.set_params(
                    book_data=tariff['book_data'],
                    price=tariff['price'],
                    partner=tariff['partner']
                )
                break
        if not hasattr(self.selling_tariff, 'price'):
            raise Exception(f'Tariffs for provider {self.provider} of type {self.tariff_type} are not found')


def suburban_order_check(
    test_name, provider, tariff_type,
    st_from_esr, st_from_id, st_to_esr, st_to_id,
    test_context_type=TestContextType.NONE, use_payment_test_context=False,
    stableness=StablenessVariants.UNSTABLE
):
    """
    Проверка флоу одного заказа билета на электричку
    """
    base_headers = _make_base_headers()
    selling_tariff = SellingTariff()

    return [
        # Вызываем поиск на экспорте, среди ниток находим нитку нужного провадера и типа тарифа
        f'v3/suburban/search_on_date?station_from={st_from_esr}&station_to={st_to_esr}'
        f'&date={msk_tomorrow_str}&days_ahead=2&lang=ru_RU&selling=true&selling_version=3'
        '&selling_flows=simple&selling_flows=validator&selling_flows=aeroexpress'
        '&selling_barcode_presets=PDF417_cppk&selling_barcode_presets=PDF417_szppk&selling_barcode_presets=Aztec_mtppk',
        {
            'host': env.export_host,
            'timeout': env.export_timeout,
            'processes': [SetSellingTariff(provider, tariff_type, selling_tariff)],
            'name': test_name,
            'stableness': stableness,
        },
        # Для найденной нитки создаем заказ
        [
            _make_create_order_call(
                selling_tariff, provider, st_from_id, st_to_id,
                base_headers, test_context_type, use_payment_test_context
            )
        ]
    ]


def suburban_order_with_test_context_check_list(
    tests_set_name, provider, tariff_type, st_from_esr, st_from_id, st_to_esr, st_to_id
):
    """
    Проверка флоу заказа билета на электричку c разными вариантами тестового контекста
    """
    return [
        suburban_order_check(
            f'{tests_set_name}. Payment with test context',
            provider, tariff_type, st_from_esr, st_from_id, st_to_esr, st_to_id,
            test_context_type=TestContextType.NONE, use_payment_test_context=True,
            stableness=StablenessVariants.UNSTABLE
        ),

        suburban_order_check(
            f'{tests_set_name}. Payment and confirmation with test context',
            provider, tariff_type, st_from_esr, st_from_id, st_to_esr, st_to_id,
            test_context_type=TestContextType.WITHOUT_RETRIES, use_payment_test_context=True,
            stableness=StablenessVariants.TESTING_UNSTABLE
        ),

        suburban_order_check(
            f'{tests_set_name}. Payment and confirmation with test context having retries',
            provider, tariff_type, st_from_esr, st_from_id, st_to_esr, st_to_id,
            test_context_type=TestContextType.WITH_RETRIES, use_payment_test_context=True,
            stableness=StablenessVariants.TESTING_UNSTABLE
        )
    ]
