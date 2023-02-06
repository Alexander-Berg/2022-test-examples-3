#!/usr/bin/python
# -*- coding: utf-8 -*-

import codecs
import json


def load_config(skip_file_full_path):
    with codecs.open(skip_file_full_path, 'r', encoding='utf8') as skip_file:
        config_obj = json.load(skip_file)
    return config_obj


def save_config(skip_file_full_path, config_obj):
    with codecs.open(skip_file_full_path, 'w', encoding='utf8') as skip_file:
        json.dump(config_obj, skip_file, ensure_ascii=False, indent=4, sort_keys=True, separators=(',', ': '))

