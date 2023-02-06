from __future__ import unicode_literals
import argparse
import logging
import os
from tp_api_client import tp_api_client as tp

logger = logging.getLogger('testpalm_logger')
logging.basicConfig(format=u'%(asctime)s [%(levelname)s] %(module)s: %(message)s', level=logging.INFO)

TESTPALM_TOKEN = os.environ['OAUTH_TOKEN_TESTPALM']


def handle_options():
    parser = argparse.ArgumentParser()
    parser.add_argument("-p", "--project", dest="project")
    parser.add_argument("-q", "--queues", dest="queues")
    return parser


def get_links(tc, queues):
    bug_ids, bugs = [], tc['bugs']
    for bug in bugs:
        bug_id = bug['id']
        for queue in queues:
            if bug_id.startswith(queue):
                bug_ids.append(bug_id)
    return bug_ids


def main():
    args = handle_options().parse_args()
    testpalm_project = args.project
    queues = args.queues.replace(" ", "").split(',')
    client = tp.TestPalmClient(auth=TESTPALM_TOKEN)
    test_cases = client.get_testcases(project=testpalm_project, include='id,bugs')

    for test_case in test_cases:
        st_issues = get_links(test_case, queues)
        case_id = test_case['id']

        for st_issue in st_issues:
            client.delete_tracker_link(project=testpalm_project, case_id=case_id, st_issue=st_issue)


if __name__ == '__main__':
    main()
