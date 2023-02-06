# coding=utf-8

import os
import pytest

from lib.notificator_common import read_config, read_configs

import yatest.common

from jsonschema import ValidationError

PROJECT_PATH = 'market/mstat/cubes-web'
CONFIG_DIR = 'tests/resources/notification_configs'


@pytest.mark.parametrize('config_file,expected', [
    (os.path.join(yatest.common.source_path(PROJECT_PATH),
                  CONFIG_DIR, 'success', 'test_config_success_1.json'),
     [{'notification_time': [10, 14, 20],
       'sla_time': u'9:00',
       'chat_id': u'-1001405033801',
       'name': u'Заказы',
       "param_group": "test param group1",
       "notify_type": "primary",
       'action_id': u'olap2-etl:cubes/cube_order_dict'},
      {'notification_time': [10, 14, 20],
       'sla_time': None,
       'chat_id': u'-1001405033801',
       'name': u'Позиции заказов',
       "param_group": u"test param group2",
       "notify_type": u"secondary",
       'action_id': u'olap2-etl:cubes/cube_order_item_dict'}
      ]),
    (os.path.join(yatest.common.source_path(PROJECT_PATH),
                  CONFIG_DIR, 'success', 'test_config_success_2.json'),
     [{'notification_time': [10, 14, 20],
       'sla_time': u'9:00',
       'chat_id': u'-1001405033802',
       'name': u'Заказы',
       "param_group": u"test param group1",
       "notify_type": u"secondary",
       'action_id': u'olap2-etl:cubes/cube_order_dict'},
      {'notification_time': [14],
       'sla_time': u'9:00',
       'chat_id': u'-1001405033802',
       'name': u'Юнит экономика (партиционированная)',
       "param_group": u"test param group2",
       "notify_type": u"secondary",
       'action_id': u'olap2-etl:cubes/fact_ue_partitioned'}]),
    (os.path.join(yatest.common.source_path(PROJECT_PATH),
                  CONFIG_DIR, 'success', 'test_config_success_3.json'),
     [{'notification_time': [14],
       'sla_time': u'9:00',
       'chat_id': u'-1001405033803',
       'name': u'Юнит экономика (партиционированная)',
       "param_group": u"test param group",
       "notify_type": u"secondary",
       'action_id': u'olap2-etl:cubes/fact_ue_partitioned'}]),
    (os.path.join(yatest.common.source_path(PROJECT_PATH),
                  CONFIG_DIR, 'success', 'test_config_success_4.json'),
     [{'notification_time': [10, 14, 20],
       'sla_time': u'9:00',
       'chat_id': u'-1001405033804',
       'name': u'Заказы',
       "param_group": u"test param group",
       "notify_type": u"secondary",
       'action_id': u'olap2-etl:cubes/cube_order_dict'}]),
    (os.path.join(yatest.common.source_path(PROJECT_PATH),
                  CONFIG_DIR, 'success', 'test_config_success_5.json'),
     [{'notification_time': [14],
       'sla_time': None,
       'chat_id': u'-1001405033805',
       'name': u'Позиции заказов',
       "param_group": u"cubes-etl/cube_order_item_dict",
       "notify_type": u"secondary",
       'action_id': u'olap2-etl:cubes/cube_order_item_dict'}])
])
def test_read_configs_success(config_file, expected):
    with open(config_file) as f:
        assert read_config(f.read()) == expected


def test_read_all_configs_success():
    read_configs(os.path.join(CONFIG_DIR, 'success'))


@pytest.mark.parametrize('config_file', [os.path.join(yatest.common.source_path(PROJECT_PATH),
                                                      CONFIG_DIR, 'test_config_fail_chat_id.json')])
def test_read_configs_no_chat_id(config_file):
    with pytest.raises(ValidationError, match=r".*u'chat_id' is a required property.*"):
        with open(config_file) as f:
            read_config(f.read())


@pytest.mark.parametrize('config_file', [os.path.join(yatest.common.source_path(PROJECT_PATH),
                                                      CONFIG_DIR, 'test_config_fail_notification_time.json')])
def test_read_configs_no_notification_time(config_file):
    with pytest.raises(ValidationError, match=r".*u'notification_time' is a required property.*"):
        with open(config_file) as f:
            read_config(f.read())


@pytest.mark.parametrize('config_file', [os.path.join(yatest.common.source_path(PROJECT_PATH),
                                                      CONFIG_DIR, 'test_config_fail_action_id.json')])
def test_read_configs_no_action_id(config_file):
    with pytest.raises(ValidationError, match=r".*u'action_id' is a required property.*"):
        with open(config_file) as f:
            read_config(f.read())


@pytest.mark.parametrize('config_file', [os.path.join(yatest.common.source_path(PROJECT_PATH),
                                                      CONFIG_DIR, 'test_config_fail_param_group.json')])
def test_read_configs_no_param_group(config_file):
    with pytest.raises(ValidationError, match=r".*u'param_group' is a required property.*"):
        with open(config_file) as f:
            read_config(f.read())
