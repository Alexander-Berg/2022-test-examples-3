import datetime
import json
import os
import time
import calendar
from typing import List

import requests
from retrying import retry

from set_secret import set_secret


@retry(stop_max_attempt_number=3, wait_fixed=10000)
def send_data_to_stat(report, scale, data):
    headers = {'Authorization': 'OAuth %s' % os.environ["STAT_TOKEN"]}
    r = requests.post(
        'https://upload.stat.yandex-team.ru/_api/report/data',
        headers=headers,
        data={
            'name': report,
            'scale': scale,
            'data': json.dumps({'values': data}),
        },
        verify=False,
    )


def get_data_from_stat_for_month():
    month_yesterday = (datetime.date.today() - datetime.timedelta(days=1)).strftime("%Y-%m")
    month_three_weeks_ago = (datetime.date.today() - datetime.timedelta(days=21)).strftime("%Y-%m")
    print(month_yesterday)
    print(month_three_weeks_ago)
    headers = {'Authorization': 'OAuth %s' % os.environ["STAT_TOKEN"]}
    r = requests.get(
        'https://upload.stat.yandex-team.ru/_api/statreport/json/Mail/Others/Autotests/AutotestsRegular',
        headers=headers,
        data={
            'scale': 'm',
            'date_min': month_three_weeks_ago,
            'date_max': month_yesterday,
        },
        verify=False,
    )
    r_testopithecus = requests.get(
        'https://upload.stat.yandex-team.ru/_api/statreport/json/Mail/Others/Autotests/AutotestsTestopithecus',
        headers=headers,
        data={
            'scale': 'm',
            'date_min': month_three_weeks_ago,
            'date_max': month_yesterday,
        },
        verify=False,
    )
    data = r.json()['values'] + r_testopithecus.json()['values']
    return data


def delete_data_from_stat(filter):
    headers = {'Authorization': 'OAuth %s' % os.environ["STAT_TOKEN"]}
    r = requests.post(
        f'https://upload.stat.yandex-team.ru/_api/report/delete_data/Mail/Others/AutotestsStat?{filter}',
        headers=headers,
        verify=False,
    )
    print(r.json())


def fill_day_scale(service, day_range=1):
    if service == 'regular':
        report = '/Mail/Others/AutotestsStat'
        output_report = '/Mail/Others/Autotests/AutotestsRegular'
    else:
        report = '/Mail/Others/MobAutoTestsStat'
        output_report = 'Mail/Others/Autotests/AutotestsTestopithecus'
    for day in range(1, day_range + 1):
        previous_day = datetime.date.today() - datetime.timedelta(days=day)
        previous_previous_day = datetime.date.today() - datetime.timedelta(days=day+1)
        day_previous = previous_day.strftime("%Y-%m-%d")
        day_previous_previous = previous_previous_day.strftime("%Y-%m-%d")
        headers = {'Authorization': 'OAuth %s' % os.environ["STAT_TOKEN"]}
        r = requests.get(
            f'https://upload.stat.yandex-team.ru/_api/statreport/json/{report}',
            headers=headers,
            data={
                'scale': 's',
                'date_min': f'{day_previous_previous} 21:00:00',
                'date_max': f'{day_previous} 06:00:00',
            },
            verify=False,
        )
        # print(r.json())
        # print(r.json()['values'][0])
        print(len(r.json()['values']))
        results = sum_data(r.json()['values'])
        for result in results:
            if service != 'regular':
                result['is_prod'] = 1
            if not result['is_prod']:
                result['is_prod'] = 0
        results_morning = results
        r = requests.get(
            f'https://upload.stat.yandex-team.ru/_api/statreport/json/{report}',
            headers=headers,
            data={
                'scale': 's',
                'date_min': f'{day_previous} 06:00:00',
                'date_max': f'{day_previous} 21:00:00',
            },
            verify=False,
        )
        # print(r.json())
        # print(r.json()['values'][0])
        print(len(r.json()['values']))
        results = sum_data(r.json()['values'])
        for result in results:
            if service != 'regular':
                result['is_prod'] = 0
            if not result['is_prod']:
                result['is_prod'] = 0
        results_day = results
        final_results = sum_data(results_day + results_morning)
        for result in final_results:
            result['fielddate'] = day_previous
        with open(f'day-check.json', 'w', encoding='utf-8') as f:
            json.dump(final_results, f, ensure_ascii=False, indent=4)
        send_data_to_stat(output_report, 'd', final_results)


def fill_month_scale(service):
    if service == 'regular':
        report = '/Mail/Others/Autotests/AutotestsRegular'
        output_report = '/Mail/Others/Autotests/AutotestsRegular'
    else:
        report = 'Mail/Others/Autotests/AutotestsTestopithecus'
        output_report = 'Mail/Others/Autotests/AutotestsTestopithecus'
    yesterday = datetime.date.today() - datetime.timedelta(days=1)
    month_yesterday = yesterday.strftime("%Y-%m")
    headers = {'Authorization': 'OAuth %s' % os.environ["STAT_TOKEN"]}
    r = requests.get(
        f'https://upload.stat.yandex-team.ru/_api/statreport/json/{report}',
        headers=headers,
        data={
            'scale': 'd',
            'date_min': f'{month_yesterday}-01',
            'date_max': f'{month_yesterday}-{calendar.monthrange(yesterday.year, yesterday.month)[1]}',
        },
        verify=False,
    )
    # print(r.json())
    print(len(r.json()['values']))
    results = sum_data(r.json()['values'])
    for result in results:
        result['fielddate'] = month_yesterday
    with open(f'month-check.json', 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=4)
    send_data_to_stat(output_report, 'm', results)


def sum_data(data: List[dict]):
    case_info = []
    case_info_with_exec_time = []
    for result in data:
        if result['execution_time']:
            execution_time = result['execution_time']
            num_of_runned = 1
        else:
            execution_time = 0
            num_of_runned = 0
        record = id_and_name_in_list(result, case_info)
        if len(record) == 0:
            record_to_add = {
                'id': result['id'],
                'test_name': result['test_name'],
                'is_passed': result['is_passed'],
                'is_failed': result['is_failed'],
                'is_skipped': result['is_skipped'],
                'is_finally_failed': result['is_finally_failed'],
                'is_intermediate': result['is_intermediate'],
                'execution_time_sum': execution_time,
                'num_of_days': num_of_runned,
                'project': result['project'],
                'is_prod': result.get('is_prod', 0)
            }
            if 'is_started' in result:
                record_to_add['is_started'] = result['is_started']
            case_info.append(record_to_add)
        else:
            record[0]['is_passed'] += result['is_passed']
            record[0]['is_failed'] += result['is_failed']
            record[0]['is_skipped'] += result['is_skipped']
            record[0]['is_finally_failed'] += result['is_finally_failed']
            record[0]['is_intermediate'] += result['is_intermediate']
            record[0]['execution_time_sum'] += execution_time
            record[0]['num_of_days'] += num_of_runned
            record[0]['project'] = result['project']
            record[0]['is_prod'] = result.get('is_prod', 0)
            if 'is_started' in result:
                record[0]['is_started'] += result['is_started']
    for case in case_info:
        record_with_exec_time = {
            'id': case['id'],
            'test_name': case['test_name'],
            'is_passed': case['is_passed'],
            'is_failed': case['is_failed'],
            'is_skipped': case['is_skipped'],
            'is_finally_failed': case['is_finally_failed'],
            'is_intermediate': case['is_intermediate'],
            'execution_time': case['execution_time_sum'] / case['num_of_days'] if case['num_of_days'] else 0,
            'project': case['project'],
            'is_prod': case['is_prod'],
        }
        if 'is_started' in case:
            record_with_exec_time['is_started'] = case['is_started']
        case_info_with_exec_time.append(record_with_exec_time)
    for id in case_info_with_exec_time:
        if id['is_passed'] + id['is_finally_failed'] + id.get('is_started', 0) > 0:
            if ('is_started' in id) & (id.get('is_started', 0) > 0):
                id['total_stability'] = id['is_passed'] / (id['is_passed'] + max(id['is_started'] - id['is_passed'], id['is_finally_failed']))
            else:
                id['total_stability'] = id['is_passed'] / (id['is_passed'] + id['is_finally_failed'])
        else:
            id['total_stability'] = 0
    print(len(case_info_with_exec_time))
    return case_info_with_exec_time


def id_and_name_in_list(record, results):
    return list([result for result in results if (record['id'] == result['id'])
                 & (record['test_name'] == result['test_name']) & (record['is_prod'] == result.get('is_prod', 0))])


if __name__ == '__main__':
    set_secret.set_secrets()
    start = time.time()
    fill_day_scale('regular')
    fill_month_scale('regular')
    fill_day_scale('testopithecus')
    fill_month_scale('testopithecus')
    print(time.time() - start)
    # delete_data_from_stat("scale=s&test_name=_in_table_&_incl_fields=execution_time&_incl_fields=is_failed&_incl_fields=is_finally_failed&_incl_fields=is_intermediate&_incl_fields=is_passed&_incl_fields=is_skipped&date_min=2020-07-02+00%3A00%3A00&date_max=2020-07-13+23%3A59%3A59&execute=1")
    # data = get_data_from_stat_for_month()
    # print(len(data))
    # print(data[0])
