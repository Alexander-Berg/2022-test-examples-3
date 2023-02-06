# -*- coding: utf-8 -*-

"""
Модуль для заглушек глобальных состояний и переменных.
"""

import mock

from test.helpers.stubs import base


class UWSGIStub(base.ChainedPatchBaseStub):
    """Заглушка для определения доступно ли uWSGI или нет.
    """
    mock_is_uwsgi_prcess = mock.patch(
        'mpfs.engine.process.is_uwsgi_process',
        return_value=True
    )
