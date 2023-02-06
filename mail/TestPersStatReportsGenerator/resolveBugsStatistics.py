# -*- coding: utf-8 -*-
import json
import time
import urllib3
import os

import requests
import yaml
from startrek_client import Startrek

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
scriptPath = os.path.dirname(os.path.abspath(__file__))
yaconfig = yaml.load(open(scriptPath + "/config.yaml"))

QUEUES = ['DARIA', 'QUINN', 'MAYA']


def get_resolve_bugs(queue):
    print(queue)
    issuesKeys = []
    client = Startrek(useragent="curl/7.53.1", token=yaconfig["AUTH_ST"])

    issues = client.issues.find(
        '((Queue:' + queue + ' AND Type: Bug AND  Stage: Production ) AND ("resolved": "2018-07-04" .. "2018-10-01")) '
                             'AND (Resolution: 1) AND (Priority: normal OR Priority: blocker OR Priority: critical) '
                             'AND ((Components: !"Темы") AND (Components: !"Новые темы"))'
    )

    for issue in issues:
        issuesKeys.append(issue.key)

    print(issuesKeys)
    print(len(issuesKeys))

    return len(issuesKeys)


def send_to_stat(count_per_bug, queue):
    queue = queue.lower()
    data = [
        {
            "fielddate": time.strftime("%Y-%m-%d"), "count_of_bugs": count_per_bug, "queue": queue
        }
    ]
    r = requests.post(
        'https://upload.stat.yandex-team.ru/_api/report/data',
        headers={'Authorization': 'OAuth %s' % yaconfig["AUTH_STAT"]},
        data={
            'name': 'Mail/Others/CountOfResolveBugs',
            'scale': 'd',
            'data': json.dumps({'values': data}),
        },
    )


for queue in QUEUES:
    countOfBugs = get_resolve_bugs(queue)
    send_to_stat(countOfBugs, queue)
