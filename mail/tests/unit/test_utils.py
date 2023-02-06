from itertools import chain

import pytest

from sendr_utils import enum_value

from mail.payments.payments.core.entities.enums import OrderKind
from mail.payments.payments.utils import helpers
from mail.payments.payments.utils.crypto import decrypt, encrypt
from mail.payments.payments.utils.helpers import (
    fio_str_full, fio_str_short, is_entrepreneur_by_inn, transliterate_to_eng
)


@pytest.mark.parametrize(['lst', 'size', 'chunks_num'], [
    (list(range(100)), 10, 10),
    (list(range(100)), 5, 20),
    (list(range(100)), 0, 1),
    (list(range(100)), None, 1),
    ([], None, 1)
])
def test_split_list__number_of_chunks(lst, size, chunks_num):
    """Количество чанков правильное."""
    chunks = helpers.split_list(lst, size=size)
    assert len(chunks) == chunks_num


@pytest.mark.parametrize(['lst', 'size'], [
    (list(range(100)), 10),
    (list(range(100)), 5),
    (list(range(100)), 0),
    (list(range(100)), None),
    ([], None)
])
def test_split_list__items(lst, size):
    """Все элементоы на месте в нужном порядке."""
    chunks = helpers.split_list(lst, size=size)
    assert lst == list(chain(*chunks))


class TestCrypto:
    @pytest.fixture(params=(decrypt, encrypt))
    def function(self, request):
        return request.param

    @pytest.mark.parametrize('size', (16, 24, 32))
    def test_decrypt(self, size, payments_settings, randn, rands):
        payload = rands()
        version = randn()
        payments_settings.ENCRYPTION_KEY[version] = rands(k=size)
        assert payload == decrypt(encrypt(payload, version), version)

    def test_unknown_version(self, payments_settings, rands, randn, function):
        payments_settings.ENCRYPTION_KEY = {}
        with pytest.raises(RuntimeError):
            function(rands(), randn)

    def test_none_key(self, payments_settings, rands, randn, function):
        payments_settings.ENCRYPTION_KEY = {1: None}
        with pytest.raises(AssertionError):
            function(rands(), 1)

    def test_invalid_key_length(self, payments_settings, rands, randn, function):
        payments_settings.ENCRYPTION_KEY = {1: '1'}
        with pytest.raises(AssertionError):
            function(rands(), 1)


@pytest.mark.parametrize('enum', (None, OrderKind.PAY))
def test_enum_value(enum):
    value = enum.value if enum else None
    assert enum_value(enum) == value


class TestHelpers:
    @pytest.mark.parametrize('inn,is_entrepreneur', (
        ('123456789', False),
        ('1234567890', False),
        ('12345678901', True),
        ('123456789012', True),
    ))
    def test_is_entrepreneur_by_inn(self, inn, is_entrepreneur):
        assert is_entrepreneur == is_entrepreneur_by_inn(inn)

    @pytest.mark.parametrize('name,patronymic,surname,expected', (
        pytest.param('', None, '', None, id='empty-fio'),
        pytest.param('Иван', None, 'Иванов', 'Иванов Иван', id='no-middle-name'),
        pytest.param('Иван', 'Петрович', 'Иванов', 'Иванов Иван Петрович', id='full'),
    ))
    def test_str_full(self, surname, name, patronymic, expected):
        assert expected == fio_str_full(surname, name, patronymic)

    @pytest.mark.parametrize('name,patronymic,surname,expected', (
        pytest.param('', None, '', None, id='empty-fio'),
        pytest.param('Иван', None, 'Иванов', 'Иванов И.', id='no-patronymic'),
        pytest.param('Иван', 'Петрович', 'Иванов', 'Иванов И.П.', id='full'),
    ))
    def test_str_short(self, surname, name, patronymic, expected):
        assert expected == fio_str_short(surname, name, patronymic)

    @pytest.mark.parametrize('text,expected', (
        pytest.param('', '', id='empty-str'),
        pytest.param('   ', '   ', id='spaces'),
        pytest.param('1!@#$', '1!@#$', id='non-letters'),
        pytest.param('Текст для транслитерации', 'Tekst dlja transliteratsii', id='ru'),
        pytest.param('Already English', 'Already English', id='already-en'),
    ))
    def test_transliterate_to_eng(self, text, expected):
        assert expected == transliterate_to_eng(text)
