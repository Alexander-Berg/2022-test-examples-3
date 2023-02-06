# -*- coding: utf-8 -*-

#from report.const import *
RU='ru'
COMTR='com.tr'

from tests_autogen import TESTS_AUTOGEN

TESTS = [
    #колдунщик маршрутов
    {'params': {'text': 'как добраться от и до проложить маршрут общественным транспортом', 'lr': '213'}, 'tld': RU, 'noapache_request_schema': 'test_wizards_transport_request.json', 'wizards_mock': None, 'response_schema': 'test_wizards_transport_response.json'},

] + TESTS_AUTOGEN
