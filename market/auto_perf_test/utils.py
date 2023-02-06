# -*- coding: utf-8 -*-

import os
import shutil
import tempfile
import urllib2
import xml.etree.ElementTree as ET

from constants import *


def merge_dicts(a, b, func):
    res = a.copy()
    for k, v in b.iteritems():
        if k in res:
            res[k] = func(res[k], v)
        else:
            res[k] = v
    return res


def format_float(number):
    if number < 10:
        precision = 3
    elif number < 100:
        precision = 2
    elif number < 1000:
        precision = 1
    else:
        precision = 0
    number = round(number, precision)
    return '{0:f}'.format(number).rstrip('0').rstrip('.')


def is_int(arg):
    try:
        int(arg)
    except ValueError:
        return False
    return True


def create_warning(message, prefix='*** Warning: ', newline=True):
    return "{color}{prefix}{message}{reset}{newline}".format(
        color=RED, prefix=prefix, message=message, reset=RESET, newline='\n' if newline else '')


def get_human_readable_size_str(size):
    suffixes = ['B', 'KB', 'MB', 'GB', 'TB']
    suffix_index = 0
    while size >= 1000 and suffix_index < 4:
        suffix_index += 1
        size = size / 1024.0
    return '{0}{1}'.format(format_float(size), suffixes[suffix_index])


def expand_path(path):
    if path is None:
        return None
    else:
        return os.path.expandvars(os.path.expanduser(path))


def strip_esc_codes(text):
    for esc_code in ESC_CODES:
        text = text.replace(esc_code, '')
    return text


def request_xml(url):
    response = urllib2.urlopen(url)
    xml_str = response.read()
    xml_root = ET.fromstring(xml_str)
    return xml_root


def create_directory(path):
    if not os.path.exists(path):
        os.makedirs(path)


def sum_args(*args):
    return sum(args)


class TempDir(object):

    def __init__(self):
        self.temp_dir = None

    def __enter__(self):
        self.temp_dir = tempfile.mkdtemp()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        shutil.rmtree(self.temp_dir)

    @property
    def name(self):
        return self.temp_dir
