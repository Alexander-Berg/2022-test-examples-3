# -*- coding: utf-8 -*-

def pytest_configure(config):
    config.addinivalue_line('markers', 'ticket(name): mark with startrek ticket')
    config.addinivalue_line('markers', 'soy_http(name): mark tests which need to run in http mode')
