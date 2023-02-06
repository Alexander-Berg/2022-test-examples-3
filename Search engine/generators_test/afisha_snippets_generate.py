#  -*- coding: utf-8 -*-
import os
import sys
import json
import argparse
import yt.wrapper as yt
from datetime import datetime, timedelta
from itertools import groupby, chain

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc

from afisha_snippets_helpers import *


def gen_price(prices):
    data = {}
    if len(prices) != 0:
        for price in prices:
            if 'min' not in data or price['value'] < data['min']:
                data.update({'min': price['value']})
            if 'max' not in data or price['value'] > data['max']:
                data.update({'max': price['value']})
            if 'currency' not in data:
                data.update({'currency': price['currency_code']})
        return data


def make_occurences(group):
    format_value = None
    tags = []
    for tag in group[0]['tags']:
        if tag["type"] in AVAILABLE_TAGS:
            if tag['type'] == 'film_format':
                format_value = tag
            else:
                tags.append(tag)
    session = [{'datetime': schedule['datetime'],
                'datetime_utc': schedule['datetime_utc'],
                'social_date': schedule['social_date'],
                'ticket': {'id': schedule['tickets_session_key'],
                           'price': gen_price(schedule.get('prices', []))}
               } for schedule in group]
    return {'format': format_value,
            'tags': tags,
            'sessions': session}


@yt.with_context
def gen_snippets(key, rows, context):
    events = []
    place_id = key['place.id']
    for i, row in enumerate(rows):
        event_data = row['event']
        schedules, groups = get_schedules_and_groups(row)
        if schedules and groups:
            if len(schedules) > 0:
                events.append({
                    'event': {
                        'id': event_data['id'],
                        'age_restricted': event_data['age_restricted'],
                        'title': event_data['title'],
                        'original_title': event_data['original_title'],
                        'url': event_data['url'],
                        'type': {
                            'name': event_data['type']['name'],
                            'type': event_data['type']['type'],
                            'code': event_data['type']['code'],
                        },
                        'poster': event_data['poster'],
                        'image': event_data['image'],
                        'user_rating': event_data['user_rating'],
                        'ratings': event_data['ratings']
                    },
                    'schedule': [make_occurences(group) for group in groups]
                })
    if events:
        yield {'Url': 'afisha~{place_id}'.format(place_id=place_id),
               'afisha_json/1.x': json.dumps(events),
               '@table_index': 0}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates booking snippets')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL') or '',
                                args.cluster)
    yt_client.run_reduce(binary=gen_snippets,
                         source_table=params.get('pre_processing_out'),
                         destination_table=params.get('generating_out') or params.get('processing_out'),
                         format=yt.JsonFormat(control_attributes_mode='row_fields',
                                              attributes={'encode_utf8': False}),
                         reduce_by=['place.id'])
