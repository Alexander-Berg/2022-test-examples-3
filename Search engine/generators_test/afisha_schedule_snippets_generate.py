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

SNIPPET_NAME = 'afisha_schedule_json/1.x'


def make_session(schedule):
    session = {
        'datetime_utc': schedule['datetime_utc'],
        'datetime': schedule['datetime'],
        'ticket_id': schedule['tickets_session_key']
    }
    for price in schedule.get('prices', []):
        if 'price' not in session or price['value'] < session['price']:
            session['price'] = price['value']
            session['currency'] = price['currency_code']
    return session


def make_sessions(group):
    return [make_session(schedule) for schedule in group]


@yt.with_context
def gen_snippets(key, rows, context):
    events = []
    place_id = key['place.id']
    for row in rows:
        event_data = row['event']
        schedules, groups = get_schedules_and_groups(row)
        if not groups:
            continue

        sessions = []
        for group in groups:
            sessions += make_sessions(group)
        if len(sessions) > 0:
            events.append({
                'id': event_data['id'],
                'title': event_data['title'],
                'sessions': sessions
            })
    if events:
        yield {'key': 'afisha~{place_id}'.format(place_id=place_id),
               'value': '{snipname}={events}'.format(snipname=SNIPPET_NAME, events=json.dumps(events)),
               '@table_index': 0}
        yield {'Url': 'afisha~{place_id}'.format(place_id=place_id),
               SNIPPET_NAME: json.dumps(events),
               '@table_index': 1}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates afisha schedule snippets')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL') or '',
                                args.cluster)
    yt_client.run_reduce(binary=gen_snippets,
                         source_table=params.get('pre_processing_out'),
                         destination_table=[params.get('generating_out') or params.get('processing_out'),
                                             params.get('ferryman_out')],
                         format=yt.JsonFormat(control_attributes_mode='row_fields',
                                              attributes={'encode_utf8': False}),
                         reduce_by=['place.id'])
