# -*- coding: utf-8 -*-


import pytest

from os.path import dirname, abspath
import sys

sys.path.append(dirname(dirname(abspath(__file__))))
from split_users import TestIdMapper


class Record(object):
    def __init__(self, uuid, device_id):
        self.uuid = uuid
        self.device_id = device_id


class Stream():

    def __init__(self,):
        self.records = []

    def put(self, record):
        self.records.append(record)


def compare_streams(s1, s2):
    if len(s1.records) != len(s2.records):
        return False

    s1_uuids = [r.uuid for r in s1.records]
    s2_uuids = [r.uuid for r in s2.records]

    if len(set(s1_uuids)) != len(s1_uuids) or len(set(s2_uuids)) != len(s2_uuids):
        return False

    for uuid in s1_uuids:
        if uuid not in s2_uuids:
            return False

    return True


def unique_streams(s1, s2):
    s1_uuids = [r.uuid for r in s1.records]
    s2_uuids = [r.uuid for r in s2.records]

    for uuid in s1_uuids:
        if uuid in s2_uuids:
            return False

    return True


def get_streams(numbers_of_groups):
    outputs_streams = [Stream() for i in range(numbers_of_groups)]
    outputs = [i.put for i in outputs_streams]

    return outputs, outputs_streams


def get_records(n):
    return [Record('uuid_%s' % i, 'device_id%s' % i) for i in range(n)]


def get_avg_groups_size(number_of_users, number_of_groups, group_size):
    records = get_records(number_of_users)

    sizes = [[] for x in range(number_of_groups)]

    for i in range(1000):
        outputs, outputs_streams = get_streams(number_of_groups)
        mapper = TestIdMapper(
            exp_salt='test%s' % i,
            total_users=len(records),
            group_size=group_size,
            numbers_of_groups=len(outputs))
        mapper.__call__(records, *outputs)

        for j in range(number_of_groups):
            sizes[j].append(len(outputs_streams[j].records))

    avg_sizes = [0] * number_of_groups

    for i in range(number_of_groups):
        avg_sizes[i] = sum(sizes[i]) / float(len(sizes[i]))

    return avg_sizes

#########################################################################


def test_correct_split():
    """Проверят, что разбиение воспроизводимо, выборки не пересекаются. """

    outputs_1, outputs_streams_1 = get_streams(2)
    outputs_2, outputs_streams_2 = get_streams(2)
    outputs_3, outputs_streams_3 = get_streams(2)
    records = get_records(10000)

    mapper = TestIdMapper(exp_salt='test', total_users=len(records), group_size=500, numbers_of_groups=len(outputs_1))
    mapper.__call__(records, *outputs_1)

    mapper = TestIdMapper(exp_salt='test', total_users=len(records), group_size=500, numbers_of_groups=len(outputs_2))
    mapper.__call__(records, *outputs_2)

    mapper = TestIdMapper(exp_salt='test', total_users=len(records), group_size=500, numbers_of_groups=len(outputs_3))
    mapper.__call__((records), *outputs_3)

    # разбиение одинаковое
    assert compare_streams(outputs_streams_1[0], outputs_streams_2[0])
    assert compare_streams(outputs_streams_1[1], outputs_streams_2[1])
    assert compare_streams(outputs_streams_1[0], outputs_streams_3[0])
    assert compare_streams(outputs_streams_1[1], outputs_streams_3[1])

    # выборки уникальные
    assert unique_streams(outputs_streams_1[0], outputs_streams_1[1])


def test_split_size():
    """ Формирует выборки заказанного размера. """

    group_size = 600
    avg_size = get_avg_groups_size(number_of_users=2000, number_of_groups=3, group_size=group_size)

    for d in avg_size:
        assert abs(group_size - d) / float(group_size) < 0.05


def test_split_size_lack():
    """ Формирует выборки заказанного размера - максимально возможного. """

    group_size = 600
    avg_size = get_avg_groups_size(number_of_users=int(900 * 1.1), number_of_groups=3, group_size=group_size)

    for d in avg_size:
        assert abs(300 - d) / 300. < 0.05


def test_eternal_control():
    """ Проверяем, что исключается «вечный» контроль."""

    eternal_divice_id = [
        "8db279bb1458adf4705a832f7ca745a7",
        "8db27d70a3980be19918c62fb394d472",
        "8db31956f3dbf0dc5bd04521a38b7443",
        "8db3467817a8bae8d0018be4d7d6342a",
        "8db36e26d98d0636aeae0e9ff8c19d4d",
        "8db37cfe3f18994616f1d68aaa93e416",
        "8db392121953d3ccebfb5dfb5c80fdc6",
        "8db3c66e45565ed7abb7c8be6f76d30e",
        "8db3f58d682ea685b6fe00834388a30d",
        "8db439f5ae839157721e35edd559eed5",
        "8db4e491b77a01ebd07c3d00dc6982b3"
    ]

    records = get_records(1000)
    for d in eternal_divice_id:
        records.append(Record(d, d))

    outputs, outputs_streams = get_streams(2)
    group_size = 500
    mapper = TestIdMapper(
        exp_salt='test1',
        total_users=len(records),
        group_size=group_size,
        numbers_of_groups=len(outputs)
    )
    mapper.__call__(records, *outputs)

    s1_ids = [r.device_id for r in outputs_streams[0].records]
    s2_ids = [r.device_id for r in outputs_streams[1].records]

    for d in eternal_divice_id:
        assert not (d in s1_ids or d in s2_ids)
