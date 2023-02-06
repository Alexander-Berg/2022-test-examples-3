from __future__ import unicode_literals
import logging
import os
import yaml
import argparse
from tp_api_client import tp_api_client as tp

logger = logging.getLogger('testpalm_logger')
logging.basicConfig(format=u'%(asctime)s [%(levelname)s] %(module)s: %(message)s', level=logging.INFO)


TESTPALM_TOKEN = os.environ['OAUTH_TOKEN_TESTPALM']
FIELDS = ['name', 'attributes', 'stepsExpects', 'status', 'id', 'preconditions', 'description']
TREE = ['Components', 'Case priority', 'Feature iOS', 'Feature Android', 'Screen']


def handle_options():
    parser = argparse.ArgumentParser()
    parser.add_argument("-r", "--recursive", dest="recursive", action='store_const', const=True)
    parser.add_argument("-prj", "--project", dest="project")
    parser.add_argument("-pth", "--path", dest="path", default='./')
    parser.add_argument("-upd", "--update_tc_if_needed", dest="update_tc_if_needed", action='store_const', const=True)
    return parser


def parse_tc(tc):
    data = {}
    for key in FIELDS:
        if key in tc:
            data.update({key: tc[key]})

    steps = data['stepsExpects']
    formatted_keys = ['stepFormatted', 'expectFormatted']
    for step in steps:
        for key in formatted_keys:
            if key in step:
                step.pop(key)
    return data


def change_attributes(data, attr):
    for key, value in attr.items():
        if key in data['attributes'].keys():
            data['attributes'][value] = data['attributes'].pop(key)
    return data


def find_tc(tc_name):
    with open(tc_name, 'r') as f:
        test_case = yaml.safe_load(f)
    return test_case


def testcase_has_id(test_case: dict) -> bool:
    return 'id' in test_case.keys()


if __name__ == '__main__':
    args = handle_options().parse_args()
    project = args.project
    init_path = args.path
    update_tc_if_needed = args.update_tc_if_needed
    files = []
    client = tp.TestPalmClient(auth=TESTPALM_TOKEN)
    attributes_id_by_title = client.get_attributes_id_by_title(project=project)

    if args.recursive:
        for root, dir, file in os.walk(init_path, topdown=False):
            for name in file:
                files.append(os.path.join(root, name))
            for name in dir:
                files.append(os.path.join(root, name))
    else:
        files = os.listdir(init_path)

    try:
        filtered_files = list(filter(lambda x: x.endswith('.yaml'), files))
    except:
        filtered_files = []

    for file in filtered_files:
        file = file if args.recursive else init_path + file
        raw = find_tc(file)
        test_case = change_attributes(raw, attributes_id_by_title)
        if testcase_has_id(test_case):
            if update_tc_if_needed:
                client.update_testcase(project=project, data=test_case)
                os.remove(file)
            else:
                logger.warning(f"Testcase {test_case['id']} will not be updated because option update_tc_if_needed is disabled")
                continue
        else:
            client.create_testcase(project=project, data=test_case)
            os.remove(file)
