import json
import os
from datetime import datetime

import requests
import yaml
from retrying import retry

scriptPath = os.path.dirname(os.path.abspath(__file__))
yaconfig = yaml.load(open("%s/config.yaml" % scriptPath))

packs = {
    'cal': {
        'host': 'https://calendar.yandex.ru',
        'pack': '589b40f8e4b04506a0adcf41'
    },
    'cal-touch': {
        'host': 'https://calendar.yandex.ru',
        'pack': '5cd1a1a38a903c180520155a'
    },
    'homer': {
        'host': 'https://mail.yandex.ru',
        'pack': '51c2dafa84ae15dc701c4197'
    },
    'homer-screen': {
        'host': 'https://mail.yandex.ru',
        'pack': '5857d92fe4b04506a0ac3127'
    }
}

sum_rule = {
    'cal': ['cal', 'cal-touch'],
    'homer': ['homer', 'homer-screen']
}


def get_classes_num(pack_id):
    r = requests.get("http://aqua.yandex-team.ru/aqua-api/services/pack/%s" % pack_id).json()
    return sum([len(project["suites"]) for project in r["projects"]])


def is_launched_in_prod_today(launch, host):
    try:
        launch_host = next(item for item in launch['pack']['properties'] if item.get('key', '') == 'webdriver.base.url')
        start_time = datetime.utcfromtimestamp(launch['startTime'] / 1000)
        now_date = datetime.now()
    except StopIteration:
        return False

    return (launch_host['value'] == host) & (start_time.strftime('%Y-%m-%d') == now_date.strftime('%Y-%m-%d'))


def get_launches_history(pack_id, host):
    classes_in_pack = get_classes_num(pack_id)
    query_params = {
        'skip': '0',
        'limit': '20',
        'packId': pack_id,
        'failedOnly': 'undefined',
        'user': ''
    }
    r = requests.get('http://aqua.yandex-team.ru/aqua-api/services/launch/page/simple', params=query_params).json()
    r = filter(
        lambda launch: is_launched_in_prod_today(launch, host) & (launch['totalSuites'] == classes_in_pack),
        r['launches']
    )
    r = map(
        lambda launch: {
            'failedSuites': launch['failedSuites'],
            'totalSuites': launch['totalSuites'],
            'day': datetime.utcfromtimestamp(launch['startTime'] / 1000).strftime('%Y-%m-%d'),
            'duration': (launch['stopTime'] - launch['startTime']) / 1000 / 60
        },
        r
    )
    if r:
        return [r[0]]
    else:
        return []


@retry(stop_max_attempt_number=3, wait_fixed=3000)
def send_to_stat(launches):

    data = [
        {
            "fielddate": launch['fielddate'],
            "failed_num": launch['failed_num'],
            "service": launch['service'],
            'run_time': launch['run_time']
        }
        for launch in launches
    ]
    r = requests.post(
        'https://upload.stat.yandex-team.ru/_api/report/data',
        headers={
            'Authorization': 'OAuth %s' % yaconfig["AUTH_STAT"]
        },
        data={
            'name': 'Mail/Others/Fail_Test_Classes',
            'scale': 'd',
            'json_data': json.dumps({
                'values': data
            }),
        },
    )
    print(r.request.body)
    print(r.text)


if __name__ == '__main__':
    total_result = {}
    total_launches = []
    sum_full_launches = []
    for project in packs:
        sum_lunches = []
        total_failed = 0
        print('')
        print(packs[project]['pack'])
        full_launches = get_launches_history(packs[project]['pack'], packs[project]['host'])
        for full_launch in full_launches:
            full_launch['project'] = project
        for launch in full_launches:
            total_launches.append(launch)
    print(total_launches)
    for rule in sum_rule:
        total_sum_failed = 0
        for project in sum_rule[rule]:
            failed_in_project = next(item['failedSuites'] for item in total_launches if item.get('project', '') == project)
            total_sum_failed += failed_in_project
        max_run_time = sum([item['duration'] for item in total_launches if item.get('project', '') in sum_rule[rule]])
        sum_full_launches.append({
            "fielddate": next(item['day'] for item in total_launches if item.get('project', '') == project),
            "failed_num": total_sum_failed,
            "service": rule,
            'run_time': max_run_time
        })

    send_to_stat(sum_full_launches)
