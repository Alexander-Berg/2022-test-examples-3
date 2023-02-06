# coding: utf8

from __future__ import absolute_import, division, print_function, unicode_literals

import ydb
from mock import Mock

from travel.rasp.library.python.ydb import Cache
from travel.rasp.library.python.ydb.util import YDBSessonContext


def create_cache_with_mock():
    driver_config = ydb.DriverConfig(
        endpoint='ru-ydb.yandex.ru',
        database='/test/db/name',
        auth_token='token'
    )
    driver, session_pool = Mock(), Mock()
    context = YDBSessonContext(
        driver_config=driver_config,
        driver=driver,
        session_pool=session_pool
    )
    cache = Cache(context)

    assert cache.full_path == '/test/db/name/cache'
    assert cache.full_name == '/test/db/name/cache/cache'

    return cache, driver, session_pool
