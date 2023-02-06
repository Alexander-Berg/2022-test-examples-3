import datetime
import json

from retrying import retry
from yql.api.v1.client import YqlClient

from set_secret import set_secret

set_secret.set_secrets()


@retry(stop_max_attempt_number=3, wait_fixed=10000)
def send_data_to_yt(project):
    client = YqlClient(db='hahn')

    names = ['date', 'project', 'id', 'passed', 'broken', 'failed', 'last_run', 'ignored_temporary']
    types = ['Int64', 'String', 'Int64', 'Int64', 'Int64', 'Int64', 'Int64', 'Int64']

    data_to_yt = []
    with open(f'{project}.json', 'r', encoding='utf-8') as f:
        data = json.load(f)

    for id in data:
        data_to_yt.append(
            [
                int(datetime.datetime.now().timestamp()),
                project,
                id,
                data[id]['PASSED'],
                data[id]['BROKEN'],
                data[id]['FAILED'],
                int(data[id]['lastModified'] / 1000),
                data[id]['isIgnoredTemporary']
            ]
        )
    client.write_table('home/mailfront/qa/tests_stability', data_to_yt, names, types, append=True)
