# -*- coding: utf-8 -*-
from datetime import datetime, timedelta

from django.core.cache import DEFAULT_CACHE_ALIAS
from django.http import HttpResponse
from django.test import RequestFactory

from travel.avia.library.python.common.models.timestamp import Timestamp
from travel.avia.library.python.common.utils.httpcaching import modified_by_timestamp_cached, format_http_datetime, get_timestamp, call_hash
from travel.avia.library.python.common.utils.date import MSK_TZ
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.utils.datetime import replace_now
from travel.avia.library.python.tester.utils.django_cache import replace_django_cache


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


class TestGetTimestamp(TestCase):
    NOW = datetime(2015, 10, 9, 8, 7, 6)

    @replace_now(NOW)
    def test_valid(self):
        assert get_timestamp('some') == MSK_TZ.localize(TestGetTimestamp.NOW)

        Timestamp.objects.create(code='some', value=None)
        assert get_timestamp('some') == MSK_TZ.localize(TestGetTimestamp.NOW)

        new_dt = TestGetTimestamp.NOW + timedelta(days=1)
        Timestamp.set(code='some', value=new_dt)
        assert get_timestamp('some') == MSK_TZ.localize(new_dt)


class TestModifiedByTimestampCached(TestCase):
    def setUp(self):
        self.code = 'timestamp_code'
        self.request_factory = RequestFactory()

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

    def test_no_if_modified_since(self):
        with replace_django_cache(DEFAULT_CACHE_ALIAS) as cache:
            def func(*args, **kwargs):
                return HttpResponse(func.result)

            request = self.request_factory.get('/something')
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
        with replace_django_cache(DEFAULT_CACHE_ALIAS), replace_now(now):
            request = RequestFactory().get('/path?a=42')
            func.result = '111'
            self.check_response(wrapper(request), '111', now)

            func.result = '222'
            self.check_response(wrapper(request), '111', now)

            request = RequestFactory().get('/path?a=43')
            self.check_response(wrapper(request), '222', now)
