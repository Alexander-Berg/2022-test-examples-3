# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import copy

import mock

from travel.rasp.library.python.api_clients.baris import BarisClient


def mock_baris_response(baris_raw_data=None, side_effect=None):
    """
    Мок для вызова функций из клиента БАРиС. Перед вызовом выполняется глубокое копирование исходных данных
    """
    if side_effect is not None:
        return mock.patch.object(
            BarisClient,
            '_call_and_parse',
            side_effect=side_effect
        )

    return mock.patch.object(
        BarisClient,
        '_call_and_parse',
        return_value=copy.deepcopy(baris_raw_data or {})
    )
