#!/usr/bin/env python2
# -*- coding: utf-8 -*-

"""
Тест для проверки ручки связанных запросов: таск VIDEOUI-3049.
Делаем запрос к report.priemka к ветке wizplaces.related.0
и смотрим на предмет соответствия ответа описанным в таске условиям.
"""

import pytest
import base
import config

queries = [q.rstrip() for q in open("report/integration/video/queries_rel.txt")]
domain = "ru"

@pytest.mark.parametrize("query", queries)
def test_relqueries(query):
    url = base.make_url(config.beta, domain, path="/search")
    if len(query) > 1: # защита от пустых строк в файле.
        new_params = {"json_dump": "wizplaces.related.0", "text": query, "exp_flags": "video_relqueries"}
        new_params.update(config.additional_parameters)
        content = base.get_content(url, new_params)
        assert content["wizplaces.related.0"]["items"][0]["text"]     # Проверяем, что есть список json["items"][0]["text"] и что он не пуст.
