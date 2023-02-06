import argparse
import json
import os
import time
import datetime
from itertools import chain
import yt.wrapper as yt
from testpalm_client import TestPalmApi, Auth
import urllib3

DATE_FORMAT = "%Y-%m-%d"

backoff_config_dict = {
    "mobmail_android": {
            "name": "Android Mail",
            "platform": "Android",
            "api_keys": ["14836", "1009746", "14833"],
            "startrek_queue": "MOBILEMAIL",
            "affected_devices_threshold_per_day": 500,
            "total_crashes_per_day": 1000,
            "affected_devices_percent_per_day": 0.05
    }
}

def module_filter(module):
    module_name = getattr(module, '__name__', '')
    if 'numpy' in module_name:
        return False
    if 'yt_yson_bindings' in module_name:
        return False
    if 'hashlib' in module_name:
        return False
    if 'hmac' in module_name:
        return False

    module_file = getattr(module, '__file__', '')
    if not module_file:
        return False
    if module_file.endswith('.so'):
        return False

    return True

def prepare_cli_options():
    parser = argparse.ArgumentParser(description='Get completed testruns for project and specified date and find events from appmetrika for testruns with uuid')
    parser.add_argument('--username', required=True, help='Username for which OAuth token was received')
    parser.add_argument('--palm_token', required=True, help='TestPalm OAuth Token')
    parser.add_argument('--yt_token', required=True, help='Yt Token')
    parser.add_argument('--cluster', required=False, default='hahn', help='Yt Cluster to save results in')
    parser.add_argument('--project_id', required=True, help='Testpalm project id(like mobmail_android)')
    parser.add_argument('--config_file', required=False, help='Json file with config')
    parser.add_argument('--date', required=False, help='Date in format YYYY-MM-DD, default yesterday')
    parser.add_argument('--base_path', required=True, help='Base path in YT')
    return parser.parse_args()


class FilterEvents(object):
    def __init__(self, api_keys, uuid, start_time_str, end_time_str, execution_time, participants, testrun_id, testpalm_project_id, test_cases ):
        self.api_keys = api_keys
        self.uuid = uuid
        self.start_time_str = start_time_str
        self.end_time_str = end_time_str
        self.execution_time = execution_time
        self.participants = participants
        self.testrun_id = testrun_id
        self.testpalm_project_id = testpalm_project_id
        self.test_cases = test_cases


    def __call__(self, row):
        if row.get("APIKey") in self.api_keys and row.get("UUID") == self.uuid:
            new_row = {
                "APIKey": row.get("APIKey"),
                "UUID": row.get("UUID"),
                "EventTimestamp": row.get("EventTimestamp"),
                "EventName": row.get("EventName"),
                "EventType": row.get("EventType"),
                "EventValue": row.get("EventValue"),
                "AppBuildNumber": row.get("AppBuildNumber"),
                "StartTime": self.start_time_str,
                "EndTime": self.end_time_str,
                "ExecutionTime": self.execution_time,
                "Participants": self.participants,
                "TestRunId": self.testrun_id,
                "ProjectId": self.testpalm_project_id,
                "TestCases": self.test_cases
            }
            yield new_row


def filter_event_by_uuid_and_api_key(row):
    if row.get("type") == "job_started":
        yield row

def get_testpalm_filter_expression(start_timestamp, end_timestamp):
    return {
        "type": "AND",
        "left": {
            "type": "GT",
            "key": "finishedTime",
            "value": "{}".format(start_timestamp)
        },
        "right": {
            "type": "LT",
            "key": "finishedTime",
            "value": "{}".format(end_timestamp)
        }
    }

def main():
    cli_args = prepare_cli_options()
    yt.config["token"] = cli_args.yt_token
    yt.config["proxy"]["url"] = "{}.yt.yandex.net".format(cli_args.cluster)
    yt.config['pickling']['dynamic_libraries']['enable_auto_collection'] = True
    yt.config['pickling']['module_filter'] = module_filter
    project_id = cli_args.project_id
    config_file = cli_args.config_file
    config_dict = get_config_dict(config_file)
    day_end_timestamp, day_start_timestamp, date_str = get_start_end_timestamps(cli_args)
    api = TestPalmApi(Auth(cli_args.username, cli_args.palm_token))
    expression_dict = get_testpalm_filter_expression(day_start_timestamp, day_end_timestamp)
    testpalm_project_config = config_dict[project_id]
    api_keys = testpalm_project_config['api_keys']
    res = api.get_testruns_for_project(project_id, expression=json.dumps(expression_dict))
    for res_item in res:
        started_time = res_item['startedTime']
        finished_time = res_item['finishedTime']
        execution_time = res_item['executionTime']
        status = res_item['status']
        id = res_item['id']
        participants = res_item['participants']
        test_groups = res_item['testGroups']
        test_cases = list(map(lambda x: x['testCases'], test_groups))
        test_cases_flatten = list(chain.from_iterable(test_cases))
        test_cases_uuids = list(map(lambda x: x['uuid'], test_cases_flatten))
        test_cases_comments = list(filter(lambda x: len(x) > 0,list(map(lambda x: api.get_testrun_comments(project_id, id, x), test_cases_uuids))))
        test_cases_comments_flatten = list(chain.from_iterable(test_cases_comments))
        test_cases_comments_with_uuids = list(filter(lambda x: x['text'].startswith('UUID:'), test_cases_comments_flatten))
        base_path = cli_args.base_path
        started_datetime = datetime.datetime.fromtimestamp(started_time / 1000.0)
        finished_datetime = datetime.datetime.fromtimestamp(finished_time / 1000.0)
        if len(test_cases_comments_with_uuids) > 0:
            print(test_cases_comments_with_uuids)
            for comment in test_cases_comments_with_uuids:
                uuid = str(comment[u'text'].replace('UUID:', '')).strip()
                tmp_table = yt.create_temp_table(base_path, 'asessors')
                started_time_formatted = started_datetime.strftime(DATE_FORMAT)
                finished_time_formatted = finished_datetime.strftime(DATE_FORMAT)
                input_tables = prepare_input_tables(started_time, finished_time, api_keys)
                yt.run_map(
                    FilterEvents(
                    api_keys,
                        uuid,
                        started_time_formatted,
                        finished_time_formatted,
                        execution_time,
                        ','.join(participants),
                        id,
                        project_id,
                        ','.join(test_cases_uuids)
                    ), input_tables, tmp_table)
                events_from_asessor = yt.read_table(tmp_table)
                if len(list(events_from_asessor)) == 0:
                    # no events in appmetrica found for specified UUID and dates
                    no_events_path = os.path.join(base_path, 'asessors_monitoring', project_id, 'no_events', date_str)
                    if not yt.exists(no_events_path):
                        yt.create('table', no_events_path, force=True, recursive=True)
                    row = {
                        'UUID': uuid,
                        'started': started_datetime.strftime(DATE_FORMAT),
                        'finished': finished_datetime.strftime(DATE_FORMAT),
                        'execution_time': execution_time,
                        'participants': ','.join(participants),
                        'status': status,
                        'id': id,
                        'testpalm_project': project_id
                    }
                    yt.write_table(yt.TablePath(no_events_path, append=True), [row])
                else:
                    # copy events from tmp table to actual table
                    with_events_path = os.path.join(base_path, 'asessors_monitoring', project_id, 'with_events', date_str, id)
                    yt.copy(tmp_table, with_events_path, recursive=True, force=True)
        else:
            # completed testrun without UUID provided
            no_uuids_path = os.path.join(base_path, 'asessors_monitoring', project_id, 'no_uuids', date_str)
            if not yt.exists(no_uuids_path):
                yt.create('table', no_uuids_path, force=True, recursive=True)
            row = {
                'started': started_datetime.strftime(DATE_FORMAT),
                'finished': finished_datetime.strftime(DATE_FORMAT),
                'execution_time': execution_time,
                'participants': ','.join(participants),
                'status': status,
                'id': id,
                'testpalm_project': project_id
            }
            yt.write_table(yt.TablePath(no_uuids_path, append=True), [row])


def get_config_dict(config_file):
    config_dict = backoff_config_dict
    try:
        with open(config_file, 'r') as config_file_json:
            config_dict = json.loads(config_file_json.read())
    except:
        print("could not parse json with config file!")
        pass
    return config_dict


def get_start_end_timestamps(cli_args):
    date_str = cli_args.date
    if date_str is None:
        yesterday = datetime.datetime.today().date() - datetime.timedelta(days=2)
        date_str = yesterday.strftime(DATE_FORMAT)
    date_obj = datetime.datetime.strptime(date_str, DATE_FORMAT)
    day_start_timestamp = int(time.mktime(date_obj.timetuple()) * 1000.0)
    day_end_timestamp = int(day_start_timestamp + 24 * 60 * 60 * 1000.0)
    return day_end_timestamp, day_start_timestamp, date_str


def daterange(start_date, end_date):
    dates_list = list()
    n = 0
    while n <= int((end_date.date() - start_date.date()).days):
        dates_list.append(start_date + datetime.timedelta(n))
        n += 1
    return dates_list


def prepare_input_tables(started_time, finished_time, api_keys):
    base_metrika_path = '//logs/metrika-mobile-log/1d/{}'
    started_datetime = datetime.datetime.fromtimestamp(started_time / 1000.0)
    finished_datetime = datetime.datetime.fromtimestamp(finished_time / 1000.0)
    dates_list = []
    for single_date in daterange(started_datetime, finished_datetime):
        dates_list.append(list(
            map(lambda x: yt.TablePath(base_metrika_path.format(single_date.strftime(DATE_FORMAT)), exact_key=(x,)),
                api_keys)
        ))
    return dates_list


if __name__ == "__main__":
    main()
