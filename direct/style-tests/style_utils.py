# -*- coding: utf-8 -*-

import os
import os.path
import re


SKIP_PATHS = set([
    'direct/libs/python/tests'
])


def get_java_dirs(root):
    """
    По аркадийной директории выдать вложенные моду типа JAVA_PROGRAM и JAVA_LIBRARY
    """
    for dir, dirnames, files in os.walk(os.path.join(root, "direct")):
        if os.path.relpath(dir, root) in SKIP_PATHS:
            continue
        if 'ya.make' in files:
            ya_make_info = parse_ya_make(os.path.join(dir, 'ya.make'))
            if ya_make_info.module_type.startswith('JAVA'):
                yield dir


def parse_ya_make(file):
    type_rx = re.compile('^(JAVA_PROGRAM|JAVA_LIBRARY)')
    type = 'UNKNOWN'
    for line in open(file):
        line = line.strip()
        type_matcher = type_rx.match(line)
        if type_matcher is not None:
            type = type_matcher.group(1)
    return YaMakeInfo(type)


class YaMakeInfo(object):
    def __init__(self, module_type):
        self.module_type = module_type
