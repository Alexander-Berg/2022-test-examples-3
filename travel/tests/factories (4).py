# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import six
from hamcrest import assert_that, contains_inanyorder, equal_to

from travel.rasp.library.python.db.cluster import DbInstance


def assert_db_instances_equal(inst1, inst2):
    """Делаем ручную проверку, т.к. __eq__ и __hash__ у DbInstance перегружены"""
    assert_that(vars(inst1), equal_to(vars(inst2)))


def assert_instances_lists_equal(inst_list1, inst_list2,):
    """Делаем ручную проверку, т.к. __eq__ и __hash__ у DbInstance перегружены"""
    raw_list1 = [vars(inst) for inst in inst_list1]
    raw_list2 = [vars(inst) for inst in inst_list2]
    assert_that(raw_list1, contains_inanyorder(*raw_list2))


def get_db_instances(*configs):
    instances = []
    for inst_conf in configs:
        if isinstance(inst_conf, six.string_types):
            inst = DbInstance(host=inst_conf, is_master=False)
        elif isinstance(inst_conf, dict):
            inst = DbInstance(**inst_conf)
        else:
            host, is_master, dc, priority = 'host', False, None, 0
            if len(inst_conf) == 1:
                host = inst_conf[0]
            elif len(inst_conf) == 2:
                host, is_master = inst_conf
            elif len(inst_conf) == 3:
                host, is_master, dc = inst_conf
            elif len(inst_conf) == 4:
                host, is_master, dc, priority = inst_conf
            else:
                raise Exception('bad inst conf', inst_conf)
            inst = DbInstance(host=host, is_master=is_master, dc=dc, priority=priority)
        instances.append(inst)

    return instances
