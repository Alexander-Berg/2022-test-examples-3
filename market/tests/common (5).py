#!/usr/bin/env python

import subprocess
import re

valid_report_types = set(['market{0}-report'.format(s) for s in ('', '-parallel', '-ppcshop', '-snippet', '-offline', '-mbo-preview')])
param_value_splitter = re.compile('\s*[=:]{0,1}\s*')

def check_output(cmd, **kwargs):
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kwargs)
    stdout, stderr = proc.communicate()
    if proc.returncode:
        raise Exception('Failed to run with code {0}, text: {1}'.format(proc.returncode, stdout + ' ' + stderr))

    return stdout

def generate_config(report_type, template_filename, props_filename, **kwargs):
    if report_type not in valid_report_types:
        raise Exception('Invalid report type: {0}'.format(report_type))

    extra_args = str()
    if kwargs:
        for k, v in kwargs.iteritems():
            extra_args += '--set-prop "{0}={1}" '.format(k, v)

    command = './market_search_cfg_generator.py --log-file /dev/null etc/{template} - --set-prop "REPORT_TYPE={type}" --default-props=tests/data/{props} {extra_args}' \
        .format(template=template_filename, type=report_type, props=props_filename, extra_args=extra_args)
    output = check_output(command, shell=True, stdin=None)
    return output


class ConfigSection(object):
    def __init__(self, name, declaration_line):
        self.name = name
        self.decl = declaration_line
        self.subsections = []
        self.values = {}

    def get_sections(self, name=None):
        return filter(lambda s: name is None or s.name == name, self.subsections)

    def find_section(self, name, condition_functor):
        for s in self.get_sections(name):
            if condition_functor(s):
                return s

    def find_sections(self, name, condition_functor):
        res = []
        for s in self.get_sections(name):
            if condition_functor(s):
                res.append(s)
        return res

    def __getitem__(self, key):
        try:
            return self.values[key]
        except KeyError:
            return ''

    def __setitem__(self, key, value):
        self.values[key] = value

    def __contains__(self, key):
        return key in self.values

    def __str__(self):
        return '{{ConfigSection "{0}": '.format(self.name) + str(self.values) + '\n' + '\n'.join(map(str, self.subsections)) + '}'

    __repr__ = __str__


def parse_config(raw):
    secs_stack = []
    current_section = ConfigSection('', '')
    for line in map(lambda s: s.strip(), raw.split('\n')):
        if line.startswith('</'):
            current_section = secs_stack.pop()
        elif line.startswith('<'):
            sec_name = line[1:line.find(' ')]
            new_sec = ConfigSection(sec_name, line)
            current_section.subsections.append(new_sec)
            secs_stack.append(current_section)
            current_section = new_sec
        elif line:
            pp = param_value_splitter.split(line, 1)
            if len(pp) >= 2:
                current_section[pp[0]] = pp[1]
    return current_section


def _get_collection(conf, collection_name):
    return conf.find_section('Collection', lambda sec: 'id="{0}"'.format(collection_name) in sec.decl)


def get_cardsearch(conf):
    return _get_collection(conf, 'cardsearch')


def get_wizards_base_collection(conf):
    return _get_collection(conf, 'catalogsearch')


def get_basesearch(conf, shard):
    return _get_collection(conf, 'basesearch{0}'.format(shard))


def get_basesearch_diff(conf, shard):
    return _get_collection(conf, 'basesearch-diff{0}'.format(shard))


def get_yandsearch(conf):
    return _get_collection(conf, 'yandsearch')


def get_marketreport(conf):
    return conf.get_sections('MarketReport')[0]

