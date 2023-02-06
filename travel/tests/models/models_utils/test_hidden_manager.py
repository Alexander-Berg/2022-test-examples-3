# -*- coding: utf-8 -*-
from contextlib import closing

import django.db
import mock
import pytest
from django.db import connection, models
from django.db.backends.sqlite3.base import DatabaseWrapper
from django.test.utils import CaptureQueriesContext

from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.library.python.common.models_utils import HiddenManagerWrapper
from travel.avia.library.python.tester.factories import create_settlement


@pytest.mark.dbignore
def test_hidden_manager():
    connect = DatabaseWrapper({
        'NAME': ':memory:',
        'AUTOCOMMIT': True,
        'CONN_MAX_AGE': 0,
        'OPTIONS': {},
        'TIME_ZONE': None
    })

    with closing(connect), mock.patch.object(django.db.connections._connections, 'default', connect, create=True):
        class TestModel(models.Model):
            hidden = models.BooleanField(default=1)
            objects = models.Manager()
            hidden_manager = HiddenManagerWrapper('objects')

            class Meta:
                app_label = 'test_label'

        with connection.schema_editor() as editor:
            editor.create_model(TestModel)

        TestModel.objects.create(id=1, hidden=False)
        TestModel.objects.create(id=2, hidden=True)

        assert TestModel.hidden_manager.get(pk=1)
        assert TestModel.objects.get(pk=2)
        with pytest.raises(TestModel.DoesNotExist):
            TestModel.hidden_manager.get(pk=2)

        assert TestModel.objects.count() == 2
        assert TestModel.hidden_manager.count() == 1
        assert len(TestModel.hidden_manager.filter(id__gte=-1)) == 1
        assert len(TestModel.hidden_manager.all()) == 1


@pytest.mark.dbuser
def test_hidden_manager_get():
    st = create_settlement(hidden=False)
    hidden_st = create_settlement(hidden=True)

    with CaptureQueriesContext(connection) as captured_queries:
        assert Settlement.hidden_manager.get(pk=st.pk)
        with pytest.raises(Settlement.DoesNotExist):
            Settlement.hidden_manager.get(pk=hidden_st.pk)

        assert len(captured_queries) == 2

    with Settlement.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            assert Settlement.hidden_manager.get(pk=st.pk)
            with pytest.raises(Settlement.DoesNotExist):
                Settlement.hidden_manager.get(pk=hidden_st.pk)

            assert len(captured_queries) == 0


@pytest.mark.dbuser
def test_hidden_manager_get_list():
    st1 = create_settlement(hidden=False, iata=u'AAA')
    create_settlement(hidden=True, iata=u'BBB')

    with CaptureQueriesContext(connection) as captured_queries:
        setts = Settlement.hidden_manager.get_list(iata__iexact=u'AAA')
        assert len(setts) == 1
        assert setts[0] == st1

        assert len(Settlement.hidden_manager.get_list(iata__iexact=u'BBB')) == 0

        assert len(captured_queries) == 2

    with Settlement.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            setts = Settlement.hidden_manager.get_list(iata__iexact=u'AAA')
            assert len(setts) == 1
            assert setts[0] == st1

            assert len(Settlement.hidden_manager.get_list(iata__iexact=u'BBB')) == 0

            assert len(captured_queries) == 0
