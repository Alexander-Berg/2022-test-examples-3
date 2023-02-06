# coding=utf-8

import os

from lib.notificator_common import read_config

import yatest.common


PROJECT_PATH = 'market/mstat/cubes-web'


def test_all_config_correct():
    chat_ids = []
    for config_file in os.listdir(os.path.join(yatest.common.source_path(PROJECT_PATH),
                                               "lib/resources/notification_configs")):
        if config_file.lower().endswith(".json"):
            with open(os.path.join(yatest.common.source_path(PROJECT_PATH),
                                   "lib/resources/notification_configs",
                                   config_file)) as f:
                conf = read_config(f.read())
                print(conf)
                if conf[0]['chat_id'] not in chat_ids:
                    chat_ids.append(conf[0]['chat_id'])
                else:
                    raise ValueError('Duplicate chat_id in different config files')
