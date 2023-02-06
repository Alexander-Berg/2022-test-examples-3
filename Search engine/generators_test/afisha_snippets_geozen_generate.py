#  -*- coding: utf-8 -*-
import os
import sys
import json
import argparse
import yt.wrapper as yt

from afisha_snippets_generate import parse_date, in_interval
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc

def clear_row(row):
    remove_from_root = ["event.id", "place", "event.type.code", "place.city.id", "place.id", "rank_features", "schedule_info", "stats", "rank"]
    for key in remove_from_root:
        if key in row:
            row.pop(key)
    remove_from_schedule = ["daytime", "tags", "ticket_session_key", "social_date"]
    if "schedule" in row and len(row["schedule"]) > 0:
        for key in remove_from_schedule:
            if key in row["schedule"][0]:
                row["schedule"][0].pop(key)
    if "image" in row["event"] and row["event"]["image"]:
        if "type" in row["event"]["image"]:
            row["event"]["image"].pop("type")
        if "subtype" in row["event"]["image"]:
            row["event"]["image"].pop("subtype")
    if len(row["schedule"][0]["prices"]) > 0:
        row["schedule"][0]["prices"] = [row["schedule"][0]["prices"][0]]
    return row

@yt.with_context
def gen_snippets(key, rows, context):
    events = []
    place_id = key['place.id']
    for i, row in enumerate(rows):
        schedules = [
            schedule for schedule in row['schedule']
            if schedule.get('social_date')
            and (in_interval(parse_date(schedule.get('social_date')), days_num=180) and row['event']['type']['name'] != 'cinema' or in_interval(parse_date(schedule.get('social_date')), days_num=7))
        ]
        if schedules:
            event_data = row['event']
            row['event'] = {
                'title': event_data['title'],
                'url': event_data['url'],
                'image': event_data['image'],
                'type': event_data['rubric']['code']
            }
            if len(schedules) > 0:
                row['schedule'] = [schedules[0]]
            events.append(clear_row(row))
    if events:
        yield {'Url': 'afisha~{place_id}'.format(place_id=place_id),
               'afisha_json_geozen/1.x': json.dumps(events),
               '@table_index': 0}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates booking snippets')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ['YT_POOL'],
                                args.cluster)
    yt_client.run_reduce(binary=gen_snippets,
                         source_table=params.get('pre_processing_out'),
                         destination_table=params.get('generating_out') or params.get('processing_out'),
                         format=yt.JsonFormat(control_attributes_mode="row_fields",
                                              attributes={"encode_utf8": False}),
                         reduce_by=['place.id'])
