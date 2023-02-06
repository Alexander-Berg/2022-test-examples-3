# -*- coding: utf-8 -*-
import json
import os
import time
import urllib
import datetime
import random

import requests
import yaml
from retrying import retry

from make_run import make_run

script_path = os.path.dirname(os.path.abspath(__file__) + '/SupportQueueMonitoring')

responsible_list = {"crafty": [1, 6], "a-zoshchuk": [2, 7], "cosmopanda": [3, 8], "marchart": [4, 9],
                   "mariya-murm": [5, 10]}
month = datetime.datetime.today().month


def get_requester():
    for key in responsible_list:
        if month in responsible_list.get(key):
            return key
        else:
            users = responsible_list.keys()
            responsible = users[random.randrange(0, len(users), 1)]
    return responsible


yaconfig = yaml.load(open(os.path.dirname(script_path) + "/config.yaml"))
instruction = yaconfig["instruction"]
special_condition = yaconfig["specialCondition"]
telegram = yaconfig["telegram"]
test_stand = yaconfig["testStend"]
browsers = yaconfig["browsers"]
requester = yaconfig["requester"]  # change to yaconfig["requester"] if not running regular regress


@retry(wait_fixed=5000, stop_max_attempt_number=3)
def write_to_nirvana(data_props):
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'OAuth %s' % yaconfig['AUTH_HITMAN']
    }

    hitman = 'https://hitman.yandex-team.ru/'
    start_proc = 'api/v1/execution/start/'
    service = "testung_mail_autostart"
    start_proc_url = hitman + start_proc + service

    data = {}
    data['requester'] = requester
    data['properties'] = data_props

    resp = requests.post(start_proc_url, verify=False, headers=headers, json=data)
    print(resp.text)


# фильтр для БП
data_filter = {"type": "AND", "left":{"type": "AND", "left":{"type": "AND", "left":{"type": "AND", "left":{"type": "AND", "left":{"type": "EQ", "key": "status", "value": "actual"}, "right":{"type": "EQ", "key": "isAutotest", "value": "false"}}, "right":{"type": "OR", "left":{"type": "OR", "left":{"type": "AND", "left":{"type": "EQ", "key": "attributes.5d5b233ca6a6dd7b6bcb3a46", "value": "High"}, "right":{"type": "EQ", "key": "attributes.5d5b2320f97f2449b408c4bf", "value": "High"}}, "right":{"type": "AND", "left":{"type": "EQ", "key": "attributes.5d5b233ca6a6dd7b6bcb3a46", "value":"Medium"}, "right":{"type":"EQ","key":"attributes.5d5b2320f97f2449b408c4bf","value":"High"}}}, "right":{"type":"AND","left":{"type":"EQ","key":"attributes.5d5b233ca6a6dd7b6bcb3a46","value":"High"},"right":{"type":"EQ","key":"attributes.5d5b2320f97f2449b408c4bf","value":"Medium"}}}}, "right":{"type":"NEQ","key":"attributes.55a7b654e4b0de1599b0c517","value":"композ 2019 (H)"}}, "right":{"type":"NEQ","key":"attributes.55a7b654e4b0de1599b0c517","value":"Новая шапка (H)"}}, "right":{"type":"NEQ","key":"attributes.55a7b654e4b0de1599b0c517","value":"Корп* (ред) (M)"}}

version_name = "20 10 новая BL " + time.strftime('%d.%m.%y %H.%M.%S')


def make_st_version():
    headers = {
        "Authorization": yaconfig["AUTH_ST"],
        "Content-Type": "application/json",
        "User-Agent": "curl/7.53.1",
        "Connection": "close",
        "Accept": "*/*"
    }
    data = {
        "queue": "MAILEXP",
        "name": version_name
    }
    raw_body = json.dumps(data, separators=(",", ": "))

    r = requests.post("https://st-api.yandex-team.ru/v2/versions", data=raw_body, headers=headers)  # заводим версию в ST
    version_id = r.json()["id"]

    data = {
        "queue": "MAILEXP",
        "summary": "Протестировать асессорами релиз " + version_name,
        "type": "task",
        "assignee": requester,
        "fixVersions": [version_id]
    }
    raw_body = json.dumps(data, ensure_ascii=False, separators=(",", ": "))
    # rawBody = rawBody.encode("utf-8")
    r = requests.post("https://st-api.yandex-team.ru/v2/issues", data=raw_body, headers=headers)  # заводим тикет на
    # тестирование асессорами
    task_key = r.json()["key"]
    print(task_key)

    headers = {
        "TestPalm-Api-Token": yaconfig["AUTH_TP"],
        "Content-Type": "application/json"
    }
    data = {
        "trackerVersion": {
            "groupId": "MAILEXP",
            "isClosed": False,
            "title": version_name,
            "trackerId": "Startrek",
            "versionId": str(version_id),
            "url": "https://st.yandex-team.ru/MAILEXP/filter?fixVersions=" + str(version_id)
        },
        "id": version_name + " Асессоры"
    }
    raw_body = json.dumps(data, ensure_ascii=False, separators=(",", ": "))
    # rawBody = rawBody.encode('utf-8')
    r = requests.post("https://testpalm.yandex-team.ru:443/api/version/mail-liza", data=raw_body, headers=headers)
    return task_key


def get_test_cases_by_filter(run_filter):
    run_case_i_ds = []
    req_headers = {
        "TestPalm-Api-Token": yaconfig["AUTH_TP"],
        "Content-Type": "application/json"
    }

    run_filter = json.dumps(run_filter, ensure_ascii=False, separators=(",", ": "))
    print(run_filter)
    url_body = urllib.quote(run_filter)

    url = "https://testpalm.yandex-team.ru:443/api/testcases/mail-liza/preview?includeFields=estimatedTime&expression=" \
          + url_body

    res = requests.get(url, headers=req_headers)

    cases = json.loads(res.text)

    for i in range(len(cases)):
        if cases[i].get('estimatedTime', 0) == 0:
            case_time = 120
            print("not timed!")
        else:
            case_time = cases[i]["estimatedTime"] / 1000

        run_case_i_ds.append([cases[i]["id"], case_time])

    return run_case_i_ds


def get_cases_to_exclude():
    cases_to_exclude = []

    f = open(os.path.dirname(script_path) + "/exclude.txt", "r")
    for line in f:
        cases_to_exclude.append(int(line[:-1]))

    print(cases_to_exclude)
    print(len(cases_to_exclude))
    print("")
    return cases_to_exclude


def run_all_cases_in_one_run(case_ids):
    cases_to_run = []
    run_num = 0
    left_cases_timing = 0
    for caseId in case_ids:
        left_cases_timing += int(caseId[1])
    print("кейсов в сумме на " + str(left_cases_timing / 60) + " минут")
    for caseId in case_ids:
        cases_to_run.append(int(caseId[0]))
    run_num += 1
    run_with_all_browsers_per_run(cases_to_run, run_num, version_name, task_key, browsers)


def run_with_time_limit(case_ids):
    cases_to_run = []
    suite_time = 0
    run_num = 0
    left_cases_timing = 0
    correction = 0
    for caseId in case_ids:
        left_cases_timing += int(caseId[1])
    print("кейсов в сумме на " + str(left_cases_timing / 60) + " минут")
    for caseId in case_ids:
        cases_to_run.append(int(caseId[0]))
        suite_time += int(caseId[1])
        left_cases_timing -= int(caseId[1])
        if suite_time >= 1800 + correction:
            run_num += 1
            run_with_one_browser_per_run(cases_to_run, run_num, version_name, task_key, browsers)
            print(run_num)
            print(cases_to_run)
            print(len(cases_to_run))
            print(suite_time)
            cases_to_run = []
            suite_time = 0
            print("")
            if left_cases_timing < 2400:  # подхачиваем распределение для последних двух ранов, чтобы они вышли чуть
                # больше/меньше но потом не вылез какой-то совсем крохотный ран.
                if left_cases_timing < 1200:
                    correction = left_cases_timing - 800
                else:
                    correction = left_cases_timing / 2 - 800
    if len(cases_to_run) > 1:
        run_num += 1
        run_with_one_browser_per_run(cases_to_run, run_num, version_name, task_key, browsers)
        print(run_num)
        print(cases_to_run)
        print(len(cases_to_run))
        print(suite_time)


def run_with_one_browser_per_run(cases_to_run, run_num, version_name, task_key, browsers):
    browser = browsers[run_num % len(browsers)]
    make_run(cases_to_run, run_num, version_name, task_key, browser)
    print("run is full, make run! " + browser)


def run_with_all_browsers_per_run(cases_to_run, run_num, version_name, task_key, browsers):
    for browser in browsers:
        make_run(cases_to_run, run_num, version_name, task_key, browser)
        print("run is full, make run! " + browser)


task_key = make_st_version()
case_ids = get_test_cases_by_filter(data_filter)
print(case_ids)
print(len(case_ids))
run_with_time_limit(case_ids)

print("Раны созданы! Отдаю задание в хитман...")

data_nirvana = {
    "version": version_name + " Асессоры",
    "instruction": instruction,
    "special_condition": special_condition,
    "test_stend": test_stand,
}

write_to_nirvana(data_nirvana)

print("Задание отдано!")
