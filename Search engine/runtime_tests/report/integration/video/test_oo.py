#!/usr/bin/env python2
# -*- coding: utf-8 -*-

"""
Тест для проверки ручки объектного ответа: таск VIDEOUI-3049.
Делаем запрос к report.priemka к ветке wizplaces.entity.0
и смотрим на предмет соответствия ответа описанным в таске условиям.
"""

import pytest
import base
import config

queries = [q.rstrip() for q in open("report/integration/video/queries_oo.txt")]
domain = "ru"

@pytest.mark.parametrize("query", queries)
def test_oo(query):
    url = base.make_url(config.beta, domain, path="/search")
    if len(query) > 1:   # защита от пустых строк в файле.
        new_params = {"json_dump": "wizplaces.entity.0", "text": query}
        new_params.update(config.additional_parameters)
        content = base.get_content(url, new_params)
        assert check(content)

def check(content):
    if "parent_collection" in content["wizplaces.entity.0"]:
        return True
    elif "type" in content["wizplaces.entity.0"]:
        if content["wizplaces.entity.0"]["data"]["related_object"][0]["type"] in ("assoc", "proj"):
            return True
    return False
