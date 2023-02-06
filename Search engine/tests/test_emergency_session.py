# -*- coding: utf-8 -*-
from contextlib import contextmanager

import pytest

from rtcc.core.datatable import (
    DataTable,
    DataTableWithFallback,
    CacheMissError,
)
from rtcc.core.session import (
    Session,
    SessionWithEmergencyFallback,
)
from rtcc.core.dataprovider import (
    DataProvider,
    NoFallbacksException
)


@pytest.fixture
def prefab_session():
    fbs = Session()
    fbs.get('Foo').set(100, foo='bar')
    fbs.get('Baz').set(1000, bar='baz')
    fbs.get('Baz').set(1000, lol='kek')
    return fbs


@pytest.fixture
def prefab_fallback_session(prefab_session):
    fbs = SessionWithEmergencyFallback(emergency_session=prefab_session)
    fbs.get('Baz').set(200, a='boo')
    return fbs


class SomeObscureError(Exception):
    pass


class TestDataTable(object):

    def test_normal_behaviour_on_empty_constructor(self):
        dt = DataTableWithFallback()
        dt.set(100, foo='bar')
        assert dt.get(foo='bar') == 100
        with pytest.raises(CacheMissError):
            dt.get(foo='baz')

    def test_correct_fallbacks(self):
        fallback_dt = DataTable()
        fallback_dt.set(100, foo='bar')
        dt = DataTableWithFallback(fallback_table=fallback_dt)
        assert dt.get(foo='bar') == 100
        with pytest.raises(CacheMissError):
            assert dt.get(bar='baz')


class TestSession(object):

    def normal_impl(self, s):
        with pytest.raises(CacheMissError):
            s.get('Foo').get(bar='baz')
        s.get('Foo').set(100, bar='baz')
        assert s.get('Foo').get(bar='baz') == 100

    def fallback_impl(self, s):
        assert s.get('Foo').get(foo='bar') == 100
        with pytest.raises(CacheMissError):
            s.get('Bar').get(foo='bar')
        with pytest.raises(CacheMissError):
            s.get('Foo').get(bar='baz')

    def test_normal_behaviour_on_empty_constructor(self):
        s = SessionWithEmergencyFallback()
        self.normal_impl(s)

    def test_normal_behaviour_on_empty_constructor_store_fallbacks(self):
        s = SessionWithEmergencyFallback(store_fallbacks=True)
        self.normal_impl(s)

    def test_correct_fallbacks(self, prefab_session):
        s = SessionWithEmergencyFallback(emergency_session=prefab_session)
        self.fallback_impl(s)

    def test_correct_fallbacks_store_fallbacks(self, prefab_session):
        s = SessionWithEmergencyFallback(emergency_session=prefab_session, store_fallbacks=True)
        self.fallback_impl(s)


class TestDataProvider(object):

    @pytest.fixture
    def dataprovider(self):

        def _raise():
            raise SomeObscureError

        _Dp = type('_Dataprovider', (DataProvider,), {
            'do_raise': False,
            'result': None,
            'get': lambda me, *a, **kwa: _raise() if getattr(me, 'do_raise') else me.result,
            'set_result': lambda me, result: setattr(me, 'result', result),
            'is_get_failed': lambda me, result: result is None,
            '_table': property(lambda me: me._session.get('Baz'))
        })
        return _Dp

    @contextmanager
    def toggle_raise(self, dp):
        dp.do_raise = not dp.do_raise
        yield
        dp.do_raise = not dp.do_raise

    @contextmanager
    def toggle_result(self, dp, result):
        dp.result, old_result = result, dp.result
        yield
        dp.result = old_result

    def test_simple_storage(self, dataprovider, prefab_session):
        dp = dataprovider(session=prefab_session)
        with self.toggle_raise(dp):
            with pytest.raises(SomeObscureError):
                 dp.get(a=1, b=2, c=3)
        assert dp.get(lol='kek') == 1000
        sentinel = {'foo': 'bar'}
        with self.toggle_result(dp, sentinel):
            assert dp.get(a='b') == sentinel

        key = dp._table._build_key(c='d')
        assert key not in dp._table.data
        assert dp.get(c='d') is None
        assert key in dp._table.data and dp._table.data[key] is None

    def test_with_fallback_storage(self, dataprovider, prefab_fallback_session):
        dp = dataprovider(session=prefab_fallback_session)
        with self.toggle_raise(dp):
            assert dp.get(a='boo') == 200
            assert dp.get(lol='kek') == 1000
            with pytest.raises(SomeObscureError):
                dp.get(kek='pek')

        with pytest.raises(NoFallbacksException):
            dp.get(tututu='lalala')
