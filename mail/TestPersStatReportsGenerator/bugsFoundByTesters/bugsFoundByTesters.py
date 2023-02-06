# -*- coding: utf-8 -*-

import requests
import urllib3
import yaml
import time
import json
import os
from startrek_client import Startrek

scriptPath = os.path.dirname(os.path.abspath(__file__))
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
yaconfig = yaml.load(open(os.path.dirname(scriptPath) + "/config.yaml"))
projects = yaml.load(open(os.path.dirname(scriptPath) + "/bugsFoundByTesters/projects.yaml"))


def get_testers_bugs_num(queue):
    client = Startrek(useragent="curl/7.53.1", token=yaconfig["AUTH_ST"])
    issues = client.issues.find('Queue: %s AND (Type: Bug) AND (Tags:InProd) AND Created: month()'
                                ' (Resolution: ! "Can\'t reproduce" AND Resolution: ! Duplicate AND '
                                'Resolution: ! "Won\'t fix" AND Resolution: ! "Will Not Fix" AND '
                                'Resolution: ! Incomplete AND  Resolution: ! Invalid'
                                'AND Resolution: !"Until next complaint" AND Resolution: !"Later")'
                                'AND ("Bug Detection Method":Manually OR "Bug Detection Method":Asessors OR '
                                '"Bug Detection Method":Autotests %s)'
                                % (queue, projects[queue]))

    print("Testers bugs count is: %s" % len(issues))
    return len(issues)


def get_all_bugs_num(queue):
    client = Startrek(useragent="curl/7.53.1", token=yaconfig["AUTH_ST"])
    issues = client.issues.find('Queue: %s AND (Type: Bug) AND (Tags:InProd) AND Created: month()'
                                ' (Resolution: ! "Can\'t reproduce" AND Resolution: ! Duplicate AND '
                                'Resolution: ! "Won\'t fix" AND Resolution: ! "Will Not Fix" AND '
                                'Resolution: ! Incomplete AND  Resolution: ! Invalid AND Resolution: !"Later"'
                                'AND Resolution: !"Until next complaint" )'
                                ' AND ("Bug Detection Method":notEmpty())' % queue)

    print("Total bugs count is: %s" % len(issues))
    return len(issues)


def get_testers_bugs_prc(queue):
    print queue
    all_bugs = get_all_bugs_num(queue)
    if all_bugs:
        prc = get_testers_bugs_num(queue) * 100 / all_bugs
    else:
        prc = 101
    print prc
    return prc


def send_to_stat(count_per_bug, queue):
    queue = queue.lower()
    data = [
        {
            "fielddate": time.strftime("%Y-%m-01"), "prc": count_per_bug, "queue": queue
        }
    ]
    r = requests.post(
        'https://upload.stat.yandex-team.ru/_api/report/data',
        headers={'Authorization': 'OAuth %s' % yaconfig["AUTH_STAT"]},
        data={
            'name': 'Mail/Others/prcTestersBugs',
            'scale': 'm',
            'data': json.dumps({'values': data}),
        },
    )


for pr in projects:
    prc = get_testers_bugs_prc(pr)
    send_to_stat(prc, pr)

