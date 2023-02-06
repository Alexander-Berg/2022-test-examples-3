import json
import random
import string
import sys
from contextlib import contextmanager
from datetime import timezone
from decimal import Decimal
from inspect import iscoroutine
from typing import List

from aiohttp import web

from hamcrest import assert_that, contains_inanyorder, has_entries, is_

from mail.payments.payments.core.entities.enums import OrderKind, TaskType
from mail.payments.payments.core.entities.task import Task

MERCHANT_DATA_TEST_CASES = [
    {
        'name': 'test-merchant-post-name',
    },
    {
        'name': 'test-merchant-post-name',
        'persons': {
            'ceo': {
                'name': 'test-merchant-post-ceo-name',
            },
            'signer': {
                'name': 'test-merchant-post-signer-name',
                'birthDate': '2019-03-13',
            },
        }
    },
    {
        'name': 'test-merchant-post-name',
        'bank': {
            'account': 'test-merchant-post-account',
            'name': 'test-merchant-post-name',
        },
    },
    {
        'name': 'test-merchant-post-name',
        'bank': {
            'account': 'test-merchant-post-account',
            'bik': '123456789',
            'correspondentAccount': '12345678901234567890',
            'name': 'test-merchant-post-name',
        },
        'organization': {
            'type': 'ooo',
            'name': 'test-merchant-post-name',
            'englishName': 'english_name',
            'ogrn': '1234567890123',
            'scheduleText': 'test-merchant-schedule-text',
            'siteUrl': 'test-merchant-post-site_url',
            'description': 'test-merchant-post-description',
        },
    },
    {
        'name': 'test-merchant-post-name',
        'addresses': {
            'legal': {
                'city': 'test-merchant-post-city',
                'home': 'test-merchant-post-home',
                'street': 'test-merchant-post-street',
            },
        }
    },
    {
        'name': 'test-merchant-post-name',
        'addresses': {
            'legal': {
                'city': 'test-merchant-post-city',
                'country': 'RUS',
                'home': 'test-merchant-post-home',
                'street': 'test-merchant-post-street',
                'zip': '123456',
            },
        },
        'bank': {
            'account': 'test-merchant-post-account',
            'bik': '123456789',
            'correspondentAccount': '12345678901234567890',
            'name': 'test-merchant-post-name',
        },
        'organization': {
            'type': 'ooo',
            'name': 'test-merchant-post-name',
            'englishName': 'english_name',
            'fullName': 'test-merchant-post-full_name',
            'inn': '1234567890',
            'kpp': '0987654321',
            'ogrn': '1234567890123',
            'scheduleText': 'test-merchant-schedule-text',
            'siteUrl': 'test-merchant-post-site_url',
            'description': 'test-merchant-post-description',
        },
        'persons': {
            'ceo': {
                'name': 'test-merchant-post-ceo-name',
                'email': 'test-merchant-post-ceo-email@mail.ru',
                'phone': 'test-merchant-post-ceo-phone',
                'surname': 'test-merchant-post-ceo-surname',
                'patronymic': 'test-merchant-post-ceo-patronymic',
                'birthDate': '2019-03-14',
            },
            'signer': {
                'name': 'test-merchant-post-signer-name',
                'email': 'test-merchant-post-signer-email@gmail.com',
                'phone': 'test-merchant-post-signer-phone',
                'surname': 'test-merchant-post-signer-surname',
                'patronymic': 'test-merchant-post-signer-patronymic',
                'birthDate': '2019-03-13',
            },
        },
        'username': 'test-merchant-username',
    },
    {
        'name': 'test-merchant-post-name',
        'organization': {
            'type': 'ooo',
            'inn': '1234567890',
        },
        'addresses': {
            'legal': {
                'country': 'RUS',
            }
        }
    },
]


async def callback_tasks(storage) -> List[Task]:
    """Вспомогательная процедура для извлечения задачки во время выполнения тест кейсов."""
    return [t async for t in storage.task.find() if t.task_type == TaskType.API_CALLBACK]


def create_class_mocker(mocker, *cls_paths):  # noqa: C901
    """
    Create class mocker which will make mock for any method by its name.

    Returns context manager where:
        __enter__ returns mock of method
        __exit__ tears down mock return value (e. g. closes non-awaited coroutine)

    Direct usage example:
        >>> with mocker('method_name') as mock:
        >>>     mock.assert_called_once_with(...)

    Usage in pytest fixture definition:
        >>> @pytest.fixture
        >>> def fixture():
        >>>     with mocker('mock') as mock:
        >>>         yield mock
    """

    class MethodMocker:
        def __init__(self, method_name, result=None, exc=None, sync_result=None, multiple_calls=False):
            self.method_name = method_name
            self.result = result
            self.exc = exc
            self.sync_result = sync_result
            self.multiple_calls = multiple_calls
            self.mocks = []

        def __enter__(self):
            def create_return_value(*args, **kwargs):
                """Create return value for mocked method."""
                if self.sync_result:
                    return self.sync_result
                else:
                    if sys.version_info >= (3, 8):
                        if self.exc:
                            raise self.exc
                        else:
                            return self.result
                    else:
                        return dummy_coro(result=self.result, exc=self.exc)

            for cls_path in cls_paths:
                mock = mocker.patch(f'{cls_path}.{self.method_name}')
                if self.multiple_calls:
                    mock.side_effect = create_return_value
                else:
                    if sys.version_info >= (3, 8):
                        if self.exc:
                            mock.side_effect = self.exc
                        else:
                            mock.return_value = self.sync_result or self.result
                    else:
                        mock.return_value = create_return_value()
                self.mocks.append(mock)

            if len(self.mocks) == 1:
                return self.mocks[0]
            else:
                return self.mocks

        def __exit__(self, exc_type, exc_val, exc_tb):
            for mock in self.mocks:
                if iscoroutine(mock.return_value):
                    mock.return_value.close()

    return MethodMocker


def dummy_coro(result=None, label=None, exc=None):
    if label:
        print(f'{label}_coro')

    async def coro():
        if exc:
            raise exc
        else:
            return result

    return coro()


@contextmanager
def dummy_coro_ctx(*args, **kwargs):
    coro = dummy_coro(*args, **kwargs)
    yield coro
    coro.close()


def dummy_coro_generator(*args, **kwargs):
    while True:
        coro = dummy_coro(*args, **kwargs)
        yield coro
        coro.close()


def dummy_json_response(result=None):
    return web.Response(
        text=json.dumps(result),
        content_type='application/json',
    )


def dummy_json_handler(result=None):
    async def _handler(request):
        return dummy_json_response(result)

    return _handler


def dummy_async_context_manager(value):
    class _Inner:
        async def __aenter__(self):
            return value

        async def __aexit__(self, *args):
            pass

        async def _await_mock(self):
            return value

        def __await__(self):
            return self._await_mock().__await__()

    return _Inner()


def dummy_async_function(result=None, exc=None, calls=[]):
    async def _inner(*args, **kwargs):
        nonlocal calls
        calls.append((args, kwargs))

        if exc:
            raise exc
        return result

    return _inner


def dummy_async_generator(values):
    async def _inner(*args, **kwargs):
        for value in values:
            yield value

    return _inner


async def dummy_middleware(app, handler):
    return handler


def random_string(length: int) -> str:
    return random.choices(string.ascii_letters, k=length)


def check_merchant(merchant, merchant_dict):
    assert_that(
        merchant_dict,
        has_entries({
            'uid': merchant.uid,
            'name': merchant.name,
            'revision': merchant.revision,
            'status': merchant.status.value,
            'created': merchant.created.astimezone(timezone.utc).isoformat(),
            'updated': merchant.updated.astimezone(timezone.utc).isoformat(),
            'addresses': has_entries({
                address.type: {
                    'city': address.city,
                    'country': address.country,
                    'home': address.home,
                    'street': address.street,
                    'zip': address.zip,
                }
                for address in merchant.addresses
            }),
            'bank': has_entries({
                'account': merchant.bank.account,
                'bik': merchant.bank.bik,
            }),
            'organization': has_entries({
                'type': merchant.organization.type.value,
                'name': merchant.organization.name,
                'englishName': merchant.organization.english_name,
                'fullName': merchant.organization.full_name,
                'inn': merchant.organization.inn,
                'kpp': merchant.organization.kpp,
                'ogrn': merchant.organization.ogrn,
                'scheduleText': merchant.organization.schedule_text,
                'siteUrl': merchant.organization.site_url,
            }),
            'persons': has_entries({
                'ceo': has_entries({
                    'name': merchant.ceo.name,
                    'email': merchant.ceo.email,
                    'phone': merchant.ceo.phone,
                    'surname': merchant.ceo.surname,
                    'patronymic': merchant.ceo.patronymic,
                    'birthDate': merchant.ceo.birth_date.isoformat(),
                }),
            }),
            'billing': has_entries({
                'client_id': merchant.client_id,
                'contract_id': merchant.contract_id,
                'person_id': merchant.person_id,
                'trust_partner_id': merchant.client_id,
                'trust_submerchant_id': merchant.submerchant_id,
            }),
            'username': merchant.username,
        })
    )


def check_merchant_from_person(merchant, person, merchant_dict):
    legal_address = {
        'city': person.legal_address_city,
        'country': 'RUS',
        'home': person.legal_address_home,
        'street': person.legal_address_street,
        'zip': person.legal_address_postcode,
    }
    post_address = {
        'city': person.address_city,
        'country': 'RUS',
        'home': person.address_home,
        'street': person.address_street,
        'zip': person.address_postcode,
    }
    addresses = {'legal': legal_address}
    if person.address_city:
        addresses['post'] = post_address

    assert_that(
        merchant_dict,
        has_entries({
            'addresses': has_entries(addresses),
            'bank': has_entries({
                'account': person.account,
                'bik': person.bik,
            }),
            'organization': has_entries({
                'type': merchant.organization.type.value,
                'name': person.name,
                'englishName': merchant.organization.english_name,
                'fullName': person.longname,
                'inn': person.inn,
                'kpp': person.kpp,
                'ogrn': person.ogrn,
                'siteUrl': merchant.organization.site_url,
                'description': merchant.organization.description,
            }),
            'persons': has_entries({
                'ceo': has_entries({
                    'name': person.fname,
                    'email': person.email,
                    'phone': person.phone,
                    'surname': person.lname,
                    'patronymic': person.mname,
                    'birthDate': merchant.ceo.birth_date.isoformat(),
                }),
            }),
            'billing': has_entries({
                'client_id': merchant.client_id,
                'contract_id': merchant.contract_id,
                'person_id': merchant.person_id,
                'trust_partner_id': merchant.client_id,
                'trust_submerchant_id': merchant.submerchant_id,
            })
        }),
    )


def check_order(order, order_dict, extra=None, items=False):
    extra = extra or {}
    if order.kind == OrderKind.PAY:
        extra['pay_status'] = order.pay_status.value
    else:
        pass

    if items:
        extra['items'] = contains_inanyorder(*[
            {
                'amount': float(round(item.amount, 2)),  # type: ignore
                'currency': item.currency,
                'name': item.name,
                'nds': item.nds.value,
                'price': float(round(item.price, 2)),  # type: ignore
                'product_id': item.product_id,
                'image': {
                    'url': item.image.url,
                    'stored': {
                        'original': item.image.stored.orig,
                    } if item.image.stored is not None else None
                } if item.image is not None else None,
                'markup': item.markup,
            }
            for item in order.items
        ])
        price_decimal = sum([item.amount * item.price for item in order.items], Decimal(0))
        extra['price'] = float(round(price_decimal, 2))  # type: ignore
        extra['currency'] = 'RUB'

    assert_that(
        order_dict,
        has_entries({
            'active': order.active,
            'caption': order.caption,
            'created': order.created.astimezone(timezone.utc).isoformat(),
            'description': order.description,
            'kind': order.kind.value,
            'order_id': order.order_id,
            'revision': order.revision,
            'uid': order.uid,
            'updated': order.updated.astimezone(timezone.utc).isoformat(),
            'verified': order.verified,
            'paymethod_id': order.paymethod_id,
            'pay_status_updated_at': (
                order.pay_status_updated_at.astimezone(timezone.utc).isoformat()
                if order.pay_status_updated_at else None
            ),
            'fast_moderation': order.fast_moderation,
            **extra,
        })
    )


def check_order_hashes(crypto, order_dict):
    for hash_ in [
        order_dict['order_url'].split('/')[-1],
        order_dict['order_hash'],
    ]:
        with crypto.decrypt_order(hash_) as order:
            assert order == {
                'uid': order_dict['uid'],
                'order_id': order_dict['order_id'],
                'url_kind': 'order',
                'version': 1,
            }

    for hash_ in [
        order_dict['payment_url'].split('/')[-1],
        order_dict['payment_hash'],
    ]:
        with crypto.decrypt_payment(hash_) as payment:
            assert payment == {
                'uid': order_dict['uid'],
                'order_id': order_dict['order_id'],
                'url_kind': 'payment',
                'version': 1,
            }


def check_transaction(transaction, transaction_dict):
    assert_that(
        transaction_dict,
        has_entries({
            'uid': transaction.uid,
            'order_id': transaction.order_id,
            'tx_id': transaction.tx_id,
            'revision': transaction.revision,
            'status': transaction.status.value,
            'created': transaction.created.astimezone(timezone.utc).isoformat(),
            'updated': transaction.updated.astimezone(timezone.utc).isoformat(),
            'trust_failed_result': transaction.trust_failed_result,
            'trust_resp_code': transaction.trust_resp_code,
            'trust_payment_id': transaction.trust_payment_id,
            'trust_payment_url': transaction.trust_payment_url,
            'trust_purchase_token': transaction.trust_purchase_token,
        })
    )


def items_price(items):
    return round(sum([
        item['price'] * item['amount']
        for item in items
    ]), 2)  # type: ignore


def items_with_product_id(items):
    return [
        has_entries({
            'name': item['name'],
            'amount': item['amount'],
            'price': item['price'],
            'nds': item['nds'],
            'currency': item['currency'],
            'product_id': is_(int),
            'image': {
                'url': item['image']['url'],
                'stored': None,
            }
        })
        for item in items
    ]


def print_fixtures():
    def pytest_runtestloop(session):
        """
        Хук, через который можно посмотреть какие фикстуры были собраны во время инициализации
        тестовой сессии.

        Чтобы посмотреть фикстуры, нужно разместить данный хук в каком-нибудь conftest.py,
        рядом с модулем, в котором расположены тесты, для которых хочется посмотреть
        доступные фикстуры.

        pytest_runtestloop = print_fixtures()
        """

        import inspect
        defs = session._fixturemanager._arg2fixturedefs
        names = sorted(defs)
        print("TEST SESSION COLLECTED FIXTURES:")
        for n in names:
            print(f"{n}")
            for fd in defs[n]:
                print(
                    f"\tfixture_name={fd.argname}, baseid={fd.baseid}, "
                    f"object_name={fd.func.__name__}, file={inspect.getfile(fd.func)}")

    return pytest_runtestloop


def strip_fields(data):
    if isinstance(data, dict):
        return {k: strip_fields(v) for k, v in data.items()}
    if isinstance(data, list):
        return [strip_fields(v) for v in data]
    return data.strip() if hasattr(data, 'strip') else data
