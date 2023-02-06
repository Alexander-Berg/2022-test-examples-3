# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, timedelta, time

import pytz

from common.tester.factories import create_station, create_thread, create_train_schedule_plan
from common.tester.utils.datetime import replace_now

from travel.rasp.api_public.api_public.v3.thread.rasp_db_thread import get_thread
from travel.rasp.api_public.tests.v3 import ApiTestCase
from travel.rasp.api_public.tests.v3.factories import create_request
from travel.rasp.api_public.tests.v3.helpers import check_response_invalid_date
from travel.rasp.api_public.api_public.v3.thread.json_helpers import get_thread_json


class TestThreadInfo(ApiTestCase):
    def test_valid(self):
        create_thread(uid="123")

        query = {"uid": "123"}
        response = self.api_get("thread", query)
        assert response.status_code == 200

    @replace_now("2001-01-01 00:00:00")
    def test_date_range(self):
        thread = create_thread()
        date = "1920-01-01"
        thread_json = self.api_get("thread", {"uid": thread.uid, "date": date})
        check_response_invalid_date(thread_json, date)

        date = "2010-01-01"
        thread_json = self.api_get("thread", {"uid": thread.uid, "date": date})
        check_response_invalid_date(thread_json, date)

    @replace_now("2005-10-5 00:00:00")
    def test_result_timezone(self):
        departure_date = datetime(2005, 10, 6)

        thread = create_thread(
            __={"calculate_noderoute": True},
            uid="12345",
            year_days=[departure_date.date()],
            tz_start_time=time(22),
            schedule_v1=[
                [None, 0, create_station(time_zone="Europe/Moscow")],
                [10, None, create_station()],
            ],
        )

        thread_json = self.api_get_json("thread", {"uid": thread.uid})
        assert thread_json["start_time"] == "22:00"
        assert thread_json["start_date"] == "2005-10-06"
        assert thread_json["days"] == u"только 6 октября"

        thread_json = self.api_get_json("thread", {"uid": thread.uid,
                                                   "result_timezone": "Asia/Krasnoyarsk"})
        assert thread_json["start_time"] == "02:00"
        assert thread_json["start_date"] == "2005-10-07"
        assert thread_json["days"] == u"только 7 октября"

    @replace_now("2016-10-5 00:00:00")
    def test_iso_date(self):
        departure_date = datetime(2005, 10, 6)

        thread = create_thread(
            __={"calculate_noderoute": True},
            uid="12345",
            year_days=[departure_date.date()],
            tz_start_time=time(22),
            schedule_v1=[
                [None, 0, create_station(time_zone="Europe/Moscow")],
                [10, None, create_station()],
            ],
        )

        thread_json = self.api_get_json("thread", {"uid": thread.uid, "date": "2016-10-07T01:50:10+05:00"})
        assert thread_json["start_time"] == "22:00"
        assert thread_json["start_date"] == "2016-10-06"
        assert thread_json["days"] == u"только 6 октября"

        thread_json = self.api_get_json("thread", {"uid": thread.uid, "date": "2016-10-07T02:50:10+05:00"},
                                        resp_status_code=404)
        assert thread_json["error"]["text"] == u"Рейс 12345 не ходит 2016-10-07"


class TestGetThreadJson(ApiTestCase):
    def test_valid(self):
        departure_date = (datetime.now() + timedelta(days=2))

        station_from = create_station()
        station_to = create_station()
        thread = create_thread(
            year_days=[departure_date.date()],
            tz_start_time=time(departure_date.hour, departure_date.minute),
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        )

        request = create_request()
        thread, path, naive_start_dt = get_thread(thread.uid, departure_date)
        result = get_thread_json(thread, path, naive_start_dt, station_from, station_to,
                                 national_version=request.national_version,
                                 tld=request.tld)
        assert len(result["stops"]) == 2
        assert result["start_date"] == departure_date.strftime("%Y-%m-%d")
        assert result["gone"] is False

    def test_trains_schedule_plan(self):
        departure_date = datetime.now()
        plan = create_train_schedule_plan(start_date=departure_date + timedelta(days=2))
        current_plan, next_plan = plan.get_current_and_next(datetime.now().date())

        station_from, station_to = create_station(), create_station()
        thread = create_thread(
            schedule_plan=plan,
            translated_days_texts='[{}, {"ru": "ежедневно"}]',
            __={"calculate_noderoute": True},
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        )
        request = create_request()

        thread, path, naive_start_dt = get_thread(thread.uid, departure_date)
        thread_json = get_thread_json(thread, path, naive_start_dt, station_from, station_to,
                                      national_version=request.national_version,
                                      tld=request.tld)
        assert thread_json.get("days") == u"ежедневно " + next_plan.L_appendix()

    def test_no_trains_schedule_plan(self):
        departure_date = datetime.now()

        station_from, station_to = create_station(), create_station()
        thread = create_thread(
            translated_days_texts=u'[{}, {"ru": "ежедневно"}]',
            __={"calculate_noderoute": True},
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        )
        request = create_request()

        thread, path, naive_start_dt = get_thread(thread.uid, departure_date)
        thread_json = get_thread_json(thread, path, naive_start_dt, station_from, station_to,
                                      national_version=request.national_version,
                                      tld=request.tld)
        assert thread_json.get("days") == u"ежедневно"

    @replace_now("2016-01-10 00:00:00")
    def test_start_date(self):
        departure_date = datetime(2016, 1, 10)
        bryansk_time_zone = "Asia/Barnaul"
        bryansk_tz = pytz.timezone(bryansk_time_zone)

        station_from = create_station(time_zone=bryansk_time_zone)
        station_to = create_station()
        thread = create_thread(
            year_days=[departure_date.date()],
            tz_start_time=time(22),
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        )
        local_departure_date = thread.pytz.localize(
            datetime.combine(departure_date, thread.tz_start_time)
        ).astimezone(bryansk_tz)
        assert local_departure_date.date() == (departure_date + timedelta(1)).date()

        thread, path, naive_start_dt = get_thread(thread.uid, local_departure_date)
        result = get_thread_json(thread, path, naive_start_dt, station_from, station_to,
                                 tld="ru", national_version="ru")

        assert result["start_date"] == local_departure_date.strftime("%Y-%m-%d")

    def test_no_stops(self):
        departure_date = (datetime.now() + timedelta(days=2))
        station_from, station_no_stop, station_to = create_station(), create_station, create_station()
        thread = create_thread(
            translated_days_texts=u'[{}, {"ru": "ежедневно"}]',
            __={"calculate_noderoute": True},
            schedule_v1=[
                [None, 0, station_from],
                [5, 5, station_no_stop],
                [10, None, station_to],
            ],
        )

        request = create_request()
        thread, path, naive_start_dt = get_thread(thread.uid, departure_date)
        result = get_thread_json(thread, path, naive_start_dt, station_from, station_to,
                                 national_version=request.national_version,
                                 tld=request.tld)
        assert len(result["stops"]) == 2  # show stations with stops only
