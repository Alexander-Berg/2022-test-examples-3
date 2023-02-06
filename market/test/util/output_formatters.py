# -*- coding: utf-8 -*-


class Date(object):
    def __init__(self, year, month, day):
        self._year = year
        self._month = month
        self._day = day

    def to_json(self):
        return {
            'year': self._year,
            'month': self._month,
            'day': self._day
        }

    def to_yt_json(self):
        return str(self._year) + '-' + str(self._month) + '-' + str(self._day)


class WorkingTime(object):
    def __init__(self, week_day, hour_from, min_from, hour_to, min_to):
        self._week_day = week_day
        self._hour_from = hour_from
        self._min_from = min_from
        self._hour_to = hour_to
        self._min_to = min_to

    def to_json(self):
        return {
            'week_day': self._week_day,
            'from': {
                'hour': self._hour_from,
                'min': self._min_to
            },
            'to': {
                'hour': self._hour_to,
                'min': self._min_to
            }
        }

    def to_yt_json(self):
        return {
            str(self._week_day): {
                'from': str(self._hour_from) + ':' + str(self._min_from) + ':00',
                'to': str(self._hour_to) + ':' + str(self._min_to) + ':00',
            }
        }


class YtOutlet(object):
    def __init__(
        self,
        lms_id,
        is_active,
        updated_at,
        working_time,
        start_date,
        end_date,
        calendar_holidays,
        mbi_id=None
    ):
        self._lms_id = lms_id
        self._mbi_id = mbi_id
        self._is_active = is_active
        self._updated_at = updated_at
        self._working_time = working_time
        self._start_date = start_date
        self._end_date = end_date
        self._calendar_holidays = calendar_holidays

    def to_json(self):
        return {
            'lms_id': self._lms_id,
            'mbi_id': self._mbi_id,
            'is_active': self._is_active,
            'updated_at': self._updated_at,
            'working_time': [i.to_yt_json() for i in self._working_time],
            'calendar_holidays': {
                'dates': [i.to_yt_json() for i in self._calendar_holidays],
                'startDate': self._start_date.to_yt_json(),
                'endDate': self._end_date.to_yt_json()
            }
        }


class FastDataOutlet(object):
    def __init__(
        self,
        id,
        last_update_time,
        is_active,
        start_day=None,
        end_day=None,
        calendar_holidays=None,
        working_time=None,
        mbi_id=None
    ):
        self._id = id
        self._last_update_time = last_update_time
        self._is_active = is_active
        self._start_day = start_day
        self._end_day = end_day
        self._calendar_holidays = calendar_holidays
        self._working_time = working_time
        self._mbi_id = mbi_id

    def to_json(self):
        out = {
            'id': self._id,
            'last_update_time': self._last_update_time,
            'is_active': self._is_active,
        }
        if self._start_day is not None:
            out['start_day'] = self._start_day.to_json()
        if self._end_day is not None:
            out['end_day'] = self._end_day.to_json()
        if self._calendar_holidays is not None:
            out['calendar_holidays'] = [i.to_json() for i in self._calendar_holidays]
        if self._working_time is not None:
            out['working_time'] = [i.to_json() for i in self._working_time]
        if self._mbi_id is not None:
            out['mbi_id'] = self._mbi_id

        return out
