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
projects = yaml.load(open(os.path.dirname(scriptPath) + "/bugsPerLine/projects.yaml"))


def get_bugs_num(queue):
    client = Startrek(useragent="curl/7.53.1", token=yaconfig["AUTH_ST"])
    print queue
    issues = client.issues.find('Queue: %s AND (Type: Bug) AND (Tags:InProd) AND (Priority: !Minor) '
                                'AND (Priority: !Trivial) AND (Status: !Closed) %s'
                                % (queue, projects[queue].get('filter', '')))

    print("Total bugs count is: %s" % len(issues))
    return len(issues)


def get_code_lines_in_component(project_id):
    r = requests.get(
        'https://sonar.qatools.yandex-team.ru/api/measures/component',
        params={
            'metricKeys': 'lines_to_cover,uncovered_lines',
            'componentKey': project_id,
        },
        verify=False,
    )
    lines = 0
    print r.json()
    for measure in r.json()['component']['measures']:
        if measure['metric'] == 'lines_to_cover':
            lines = lines + int(measure['value'])
        if measure['metric'] == 'uncovered_lines':
            lines = lines + int(measure['value'])
    return lines


def get_all_code_lines(project_name):
    components = projects[project_name]['components']
    lines = 0
    for comp in components:
        lines = lines + get_code_lines_in_component(comp)

    return lines


def count_bugs_per_1000_lines(bugs, lines):
    return bugs / (float(lines) / 1000)


def send_to_stat(count_per_bug, queue):
    queue = queue.lower()
    data = [
        {
            "fielddate": time.strftime("%Y-%m-01"), "bugs_per_line": count_per_bug, "queue": queue
        }
    ]
    r = requests.post(
        'https://upload.stat.yandex-team.ru/_api/report/data',
        headers={'Authorization': 'OAuth %s' % yaconfig["AUTH_STAT"]},
        data={
            'name': 'Mail/Others/BugsPer1000Line',
            'scale': 'm',
            'data': json.dumps({'values': data}),
        },
    )
    print (r.text)


for pr in projects:
    bugs_in_queue = get_bugs_num(pr)
    lines_in_repo = get_all_code_lines(pr)
    print(lines_in_repo)
    bugs_per_1000_lines = count_bugs_per_1000_lines(bugs_in_queue, lines_in_repo)
    print(bugs_per_1000_lines)
    send_to_stat(bugs_per_1000_lines, pr)
