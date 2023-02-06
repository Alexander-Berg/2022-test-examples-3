from __future__ import unicode_literals
import logging
import os
import yaml
import yamlordereddictloader
import argparse
from collections import OrderedDict

from tp_api_client import tp_api_client as tp

TESTPALM_TOKEN = os.environ['OAUTH_TOKEN_TESTPALM']
FIELDS = ['id', 'status', 'name', 'description', 'preconditions', 'stepsExpects', 'attributes']
TREE = ['Components', 'Case priority', 'Feature iOS', 'Feature Android', 'Screen']

logger = logging.getLogger('testpalm_logger')
logging.basicConfig(format=u'%(asctime)s [%(levelname)s] %(module)s: %(message)s', level=logging.INFO)


def handle_options():
    parser = argparse.ArgumentParser()
    parser.add_argument("--range", dest="range", action='store_const', const=True)
    parser.add_argument("-prj", "--project", dest="project")
    parser.add_argument("-f", "--from ", dest="from_tc")
    parser.add_argument("-t", "--to", dest="to_tc")
    parser.add_argument("-l", "--list", dest="tc_list")
    parser.add_argument("-flt", "--filter", dest="tc_filter")
    parser.add_argument("-pth", "--path", dest="path", default='./')
    return parser


def change_attribute_name(project, data):
    attributes_id_by_title = client.get_attributes_id_by_title(project=project)
    for key, value in attributes_id_by_title.items():
        if value in data['attributes'].keys():
            data['attributes'][key] = data['attributes'].pop(value)


def order_steps(data, steps_expects):
    step_attr = ['step', 'expect', 'sharedStepId']
    for step in steps_expects:
        ordered_step = OrderedDict()
        for attr in step_attr:
            if attr in step:
                ordered_step.update({attr: step[attr]})
        data['stepsExpects'].append(ordered_step)


def format_tc(tc, project):
    if len(tc) == 0:
        return None
    data = OrderedDict()
    tc = tc[0]
    for key in FIELDS:
        if key in tc:
            data.update({key: tc[key]})

    data.update({'stepsExpects': []})

    change_attribute_name(project, data)
    order_steps(data, tc['stepsExpects'])

    return data


def create_new_folder(data, path, project):
    if project == 'mobilemail':
        attributes = data['attributes']
        for attribute in TREE:
            if attribute in attributes:
                for elem in attributes[attribute]:
                    path += elem.replace('/', '') + '/'
    if not os.path.exists(path):
        os.makedirs(path)
    return path


if __name__ == '__main__':
    args = handle_options().parse_args()
    project = args.project
    init_path = args.path
    client = tp.TestPalmClient(auth=TESTPALM_TOKEN)

    if args.tc_filter:
        cases = [case['id'] for case in client.get_testcases(project=project, expression=args.tc_filter, include='id')]
    elif args.range:
        cases = range(int(args.from_tc), int(args.to_tc) + 1)
    else:
        cases = args.tc_list.replace(' ', '').split(',')

    for case_id in cases:
        tc = client.get_testcase(project=project, case_id=case_id)
        data = format_tc(tc, project)
        if data is not None:
            path = create_new_folder(data, init_path, project) + data['name'].replace('/', ' ') + '.yaml'
            logger.info(path)
            with open(path, 'w', encoding='utf-8') as f:
                yaml.dump(data, f, Dumper=yamlordereddictloader.Dumper, default_flow_style=False, allow_unicode=True)

