# coding: utf8
"""
Это точка входа настроек тестов, прописывается в DJANGO_SETTINGS_MODULE в bin/tests/ya.make
"""

from __future__ import unicode_literals, absolute_import, division, print_function

MYSQL_RZD_DB_ALIAS = 'default'
RZD_DATABASE_NAME = '__rasp-tests__'
LOG_PATH = 'log'
ENABLE_GET_RZD_HOSTS_FROM_YP = False
RZD_HOSTS = [('test.host.rzd', 4242)]
