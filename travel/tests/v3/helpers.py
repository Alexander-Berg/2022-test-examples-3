# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import json


def check_response_invalid_date(response, date, field_name=u'date'):
    assert response.status_code == 400
    result = json.loads(response.content)
    assert result['error']['text'] == (u'{}: Указана недопустимая дата - {}. '
                                       u'Доступен выбор даты на 30 дней назад и 11 месяцев вперед от текущей даты'
                                       ).format(field_name, date)
