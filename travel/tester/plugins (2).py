# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from mongoengine.connection import get_db, get_connection

from travel.rasp.library.python.common23.db.mongo.base import ensure_indexes_in_installed_apps


def pytest_configure(config):
    # register markers
    config.addinivalue_line('markers',
                            'mongouser: Очищает коллекции перед тестами')


@pytest.mark.hookwrapper(tryfirst=True)
def pytest_runtest_setup(item):
    """
    Очищает коллекции монги перед тестом при наличии метки mongouser.
    Монгу можно заполнять в setup_method и setup_function.
    На уровне класса и модуля, этого делать нельзя, если используется эта метка!

    @pytest.mark.mongouser
    @pytest.mark.parametrize("param", [1,2,3])
    def test_something(param):
        pass

    """
    from django.conf import settings

    if 'mongouser' in item.keywords:
        for alias in settings.MONGO_DATABASES.keys():
            db = get_db(alias=alias)
            for collection_name in db.list_collection_names():
                if not collection_name.startswith('system.'):
                    c = db.get_collection(collection_name)
                    c.delete_many({})
    yield


def pytest_sessionstart(session):
    from django.conf import settings

    for alias in settings.MONGO_DATABASES.keys():
        mongo_client = get_connection(alias)
        db = get_db(alias)
        mongo_client.drop_database(db.name)

    ensure_indexes_in_installed_apps()
