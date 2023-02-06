from __future__ import unicode_literals
import logging
import argparse
from tp_api_client import tp_api_client as tp

# Example run: python3 update_estimatedTime.py -t AQAD-*** -p mobilemail -s 60

logger = logging.getLogger('testpalm_logger')
logging.basicConfig(format=u'%(asctime)s [%(levelname)s] %(module)s: %(message)s', level=logging.DEBUG)

ARCHIVED = 'ARCHIVED'

def handle_options():
    parser = argparse.ArgumentParser()
    parser.add_argument("-t", "--token", dest="token")
    parser.add_argument("-p", "--project", dest="project")
    parser.add_argument("-s", "--seconds", dest="seconds")
    return parser


if __name__ == '__main__':
    args = handle_options().parse_args()
    project = args.project
    estimatedTime = int(args.seconds) * 1000
    client = tp.TestPalmClient(auth=args.token)

    list_testcase = client.get_testcases(args.project)
    for testcase in list_testcase:
        if testcase['status'] != ARCHIVED:
            if ("estimatedTime" not in testcase) or (testcase['estimatedTime'] == 0):
                client.update_testcase(args.project, {"id": testcase['id'], 'estimatedTime': estimatedTime})
                logger.info(f"Testcase id = {testcase['id']} was updated. Estimated time =  {estimatedTime}")

