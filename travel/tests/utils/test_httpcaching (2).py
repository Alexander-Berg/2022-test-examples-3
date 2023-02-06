# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import mock
import pytest
from django.http import HttpResponse
from django.test import RequestFactory

from common.models.timestamp import Timestamp
from common.tester.utils.datetime import replace_now
from common.tester.utils.django_cache import replace_django_cache
from common.utils.date import MSK_TZ
from common.utils.httpcaching import modified_by_timestamp_cached, format_http_datetime, get_timestamp, call_hash


def test_format_http_datetime():
    dt = MSK_TZ.localize(datetime(2015, 11, 30, 12, 20, 42))
    assert format_http_datetime(dt) == 'Mon, 30 Nov 2015 09:20:42 GMT'


def test_request_hash():
    factory = RequestFactory()

    def foo():
        pass

    def boo():
        pass

    def rhash(*args, **kwargs):
        return call_hash(foo, factory.get(*args, **kwargs))

    assert rhash('/abc') == rhash('/abc')
    assert rhash('/abc') != rhash('/abc/')
    assert rhash('/abc?') == rhash('/abc?')
    assert rhash('/abc?a=1') == rhash('/abc?a=1')
    assert rhash('/abc?a=1') != rhash('/abc?a=2')
    assert rhash('/abc?a=1') != rhash('/abc?')

    assert call_hash(foo, factory.get('/a')) != call_hash(boo, factory.get('/a'))


@pytest.mark.dbuser
class TestGetTimestamp(object):
    NOW = datetime(2015, 10, 9, 8, 7, 6)

    @replace_now(NOW)
    def test_valid(self):
        assert get_timestamp('some') == MSK_TZ.localize(TestGetTimestamp.NOW)

        Timestamp.objects.create(code='some', value=None)
        assert get_timestamp('some') == MSK_TZ.localize(TestGetTimestamp.NOW)

        new_dt = TestGetTimestamp.NOW + timedelta(days=1)
        Timestamp.set(code='some', value=new_dt)
        assert get_timestamp('some') == MSK_TZ.localize(new_dt)


@pytest.mark.dbuser
class TestModifiedByTimestampCached(object):
    code = 'timestamp_code'
    request_factory = RequestFactory()

    def set_db_timestamp(self, dt):
        try:
            Timestamp.set(self.code, dt)
        except Timestamp.DoesNotExist:
            Timestamp.objects.create(code=self.code, value=dt)

    def check_response(self, response, body, last_modified):
        assert response.status_code == 200
        assert response.content == body

        last_modified_str = format_http_datetime(MSK_TZ.localize(last_modified))
        assert response['Last-Modified'] == last_modified_str

    @pytest.mark.parametrize('if_modified_dt', (
        None,  # no dt
        '10/12/2018 04:58:47',  # bad dt format
    ))
    def test_no_if_modified_since(self, if_modified_dt):
        with replace_django_cache('default') as cache:
            def func(*args, **kwargs):
                return HttpResponse(func.result)

            request = self.request_factory.get('/something')

            if if_modified_dt:
                request.META['HTTP_IF_MODIFIED_SINCE'] = if_modified_dt
            wrapper = modified_by_timestamp_cached(self.code)(func)

            now = datetime(2015, 1, 1)
            with replace_now(now):
                # таймстемпа нет - всегда отдаем новый ответ функции
                func.result = '1'
                self.check_response(wrapper(request), '1', now)

            with replace_now(now + timedelta(days=1)) as now:
                # проверяем, что при отсутствии таймстемпа ничего не закэшировалось
                func.result = '12'
                self.check_response(wrapper(request), '12', now)

            cache.clear()
            wrapper = modified_by_timestamp_cached(self.code)(func)
            with replace_now(now):
                db_timestamp = now + timedelta(days=-1)
                self.set_db_timestamp(db_timestamp)
                # таймстеп есть, но нет кэша
                func.result = '123'
                self.check_response(wrapper(request), '123', db_timestamp)

                # в кэше лежат данные за текущую дату, старый таймстемп не должен влиять
                func.result = '1234'
                self.check_response(wrapper(request), '123', db_timestamp)

                # таймстемп новее даты кэша
                db_timestamp = now + timedelta(days=1)
                self.set_db_timestamp(db_timestamp)
                func.result = '1'
                self.check_response(wrapper(request), '1', db_timestamp)

    def test_if_modified_since(self):
        def func(*args, **kwargs):
            return HttpResponse(func.result)

        base_dt = datetime(2015, 1, 1)

        def days(d):
            """ Принимаем base_dt за ноль, все смещения в тесте пишем относительно него. """
            return base_dt + timedelta(days=d)

        request = self.request_factory.get('/something')
        request.META['HTTP_IF_MODIFIED_SINCE'] = format_http_datetime(MSK_TZ.localize(days(0)))
        wrapper = modified_by_timestamp_cached(self.code)(func)
        with replace_now(days(2)):
            # таймстемпа нет - всегда отдаем новый ответ функции
            func.result = '1'
            self.check_response(wrapper(request), '1', days(2))

        with replace_now(days(3)):
            # проверяем, что при отсутствии таймстемпа ничего не закэшировалось
            func.result = '12'
            self.check_response(wrapper(request), '12', days(3))

        req_old, req_new = self.request_factory.get('/s'), self.request_factory.get('/s')
        req_old.META['HTTP_IF_MODIFIED_SINCE'] = format_http_datetime(MSK_TZ.localize(days(0)))
        req_new.META['HTTP_IF_MODIFIED_SINCE'] = format_http_datetime(MSK_TZ.localize(days(2)))
        wrapper = modified_by_timestamp_cached(self.code)(func)
        with replace_now(days(3)):
            # таймстемп есть, но нет кэша
            db_timestamp = days(1)
            self.set_db_timestamp(db_timestamp)
            func.result = '1'
            self.check_response(wrapper(req_old), '1', db_timestamp)
            assert wrapper(req_new).status_code == 304

            # то же самое с кэшем
            self.check_response(wrapper(req_old), '1', db_timestamp)
            assert wrapper(req_new).status_code == 304

        # изменилось время и таймстемп, все запросы должны получить новый контент
        with replace_now(days(4)):
            db_timestamp = days(4)
            self.set_db_timestamp(db_timestamp)
            func.result = '12'
            self.check_response(wrapper(req_old), '12', db_timestamp)
            self.check_response(wrapper(req_new), '12', db_timestamp)

    def test_params_changed(self):
        def func(request, *args, **kwargs):
            return HttpResponse(func.result)

        wrapper = modified_by_timestamp_cached(self.code)(func)
        now = datetime(2015, 1, 1)
        with replace_django_cache('default'), replace_now(now):
            request = RequestFactory().get('/path?a=42')
            func.result = '111'
            self.check_response(wrapper(request), '111', now)

            func.result = '222'
            self.check_response(wrapper(request), '111', now)

            request = RequestFactory().get('/path?a=43')
            self.check_response(wrapper(request), '222', now)

    def test_passing_hash_func(self):
        def func(*args, **kwargs):
            return HttpResponse(func.result)

        def m_hash_func(m_func, request):
            return request.GET.get('a')

        hash_func = mock.Mock(side_effect=m_hash_func)
        wrapper = modified_by_timestamp_cached(self.code, hash_func=hash_func)(func)
        now = datetime(2015, 1, 1)

        with replace_django_cache('default'), replace_now(now):
            request_1 = RequestFactory().get('/path?a=42')
            func.result = 'result_1'
            result = wrapper(request_1)
            assert hash_func.call_args_list == [
                mock.call(func, request_1),
                mock.call(func, request_1)
            ]
            assert result.content == 'result_1'

            func.result = 'result_2'
            result = wrapper(request_1)
            assert hash_func.call_args_list == [
                mock.call(func, request_1),
                mock.call(func, request_1),
                mock.call(func, request_1)
            ]
            assert result.content == 'result_1'

            request_2 = RequestFactory().get('/path?a=43')
            result = wrapper(request_2)
            assert hash_func.call_args_list == [
                mock.call(func, request_1),
                mock.call(func, request_1),
                mock.call(func, request_1),
                mock.call(func, request_2),
                mock.call(func, request_2)
            ]
            assert result.content == 'result_2'
