# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from contextlib import contextmanager

from django import db
from django.db import connections


@contextmanager
def mock_django_connection(alias, db_conf):
    conn_handler = db.ConnectionHandler()
    with mock.patch.object(db, 'connections', conn_handler):
        with mock.patch.dict(connections._databases, {alias: db_conf}):
            yield conn_handler
