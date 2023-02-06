import csv

import pytest

from hamcrest import assert_that, has_properties

from mail.ipa.ipa.core.csvops.sniffer import sniff


@pytest.mark.parametrize('data, expected', (
    (
        'hello,world\na,b\nc,d',
        {
            'delimiter': ',',
            'quotechar': '"',
            'skipinitialspace': False,
        }
    ),
    (
        'hello;world\na;b\nc;d',
        {
            'delimiter': ';',
            'quotechar': '"',
            'skipinitialspace': False,
        }
    ),
    (
        'hello\t world\na\t b\nc\t d',
        {
            'delimiter': '\t',
            'quotechar': '"',
            'skipinitialspace': True,
        }
    ),
    (
        'hello\tworld\na\tb\nc\td\n',
        {
            'delimiter': '\t',
            'quotechar': '"',
            'skipinitialspace': False,
        }
    ),
))
def test_dialect(data, expected):
    # https://docs.python.org/3/library/csv.html#dialects-and-formatting-parameters
    assert_that(
        sniff(data),
        has_properties({
            # Разделитель столбцов
            'delimiter': expected['delimiter'],
            # Этим символом обрамляют поле, в значении которого используются специальные символы, например delimiter.
            # Пример, в котором delimiter = , и quotechar = ": "abc,def"
            'quotechar': expected['quotechar'],
            # When True, whitespace immediately following the delimiter is ignored. The default is False.
            'skipinitialspace': expected['skipinitialspace'],

            # Про lineterminator сказано: https://docs.python.org/3/library/csv.html#csv.Dialect.lineterminator
            # Сейчас csv.Sniffer всегда возвращает \r\n.
            # А csv reader'у всё равно на lineterminator - он осуществляет uniformed EOL recognition.
            'lineterminator': '\r\n',

            # Sniffer не умеет распознавать escapechar.
            'escapechar': None,

            # Как нужно экранировать qoutechar? Если True - quotechar заиспользуется чтобы обрамить qoutechar.
            # Например, qoutechar - двойные кавычки ("), а delimiter - запятая (,).
            # Тогда поле со значением abc,"def в csv будет выглядить вот так: "abc,"""def"
            # Поскольку escapechar распознавать мы не умеем, если посмотреть на логику нашего Sniffer,
            # doubleqoute всегда будет True
            'doublequote': True,
            'quoting': csv.QUOTE_MINIMAL,  # Это так для всех sniffed. И этого достаточно.
        })
    )
