#  -*- coding: utf-8 -*-
import yt.wrapper as yt
from datetime import datetime, timedelta
from itertools import groupby, chain


AVAILABLE_TAGS = ['film_format',
                  'language_type',
                  'none']


def parse_date(str_date):
    return datetime.strptime(str_date, '%Y-%m-%d')


def in_interval(date, days_num=7):
    return datetime.now().date() <= date.date() < datetime.now().date() + timedelta(days=days_num)


def get_schedules_and_groups(row):
    schedules = None
    groups = None

    event_data = row['event']
    if event_data['type']['code'] == 'cinema':
        schedules = list(chain.from_iterable(
            [list(data) for (key, data) in groupby (
                sorted([
                    schedule for schedule in row['schedule']
                    if schedule.get('social_date') is not None and in_interval(parse_date(schedule['social_date']))
                ], key=lambda x: x['social_date']),
                key=lambda x: x['social_date']
            )][:3]
        ))
        for schedule in schedules:
            codes = {'{tag_type}-{tag_code}'.format(tag_type=tag['type'], tag_code=tag['code']) for tag in schedule['tags'] if tag["type"] in AVAILABLE_TAGS}
            schedule['unique_codes'] = ''.join(sorted(codes))
        schedules = sorted(schedules, key=lambda x: x['unique_codes'])
        groups = [list(data) for (key, data) in groupby(schedules, key=lambda x: x ['unique_codes'])]

    return schedules, groups
