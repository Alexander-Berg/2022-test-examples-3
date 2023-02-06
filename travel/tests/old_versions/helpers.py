# -*- coding: utf-8 -*-
import json


def check_response_invalid_date(response, date):
    assert response.status_code == 400
    result = json.loads(response.content)
    assert result['error']['text'] == (u'Указана недопустимая дата - {}. '
                                       u'Доступен выбор даты на 30 дней назад и 11 месяцев вперед от текущей даты'
                                       ).format(date)
