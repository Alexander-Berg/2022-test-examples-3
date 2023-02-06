# -*- coding: utf-8 -*-

from collections import OrderedDict
from textwrap import dedent

from travel.hotels.content_manager.lib.persistence_manager import Condition, YtPersistenceManager, check_condition


def test_check_condition():
    c = Condition('f1', '!=', 3) & Condition('f1', '==', 5)
    assert not check_condition({'f1': 3}, c)
    assert not check_condition({'f1': 4}, c)
    assert check_condition({'f1': 5}, c)


def test_select_query():
    schema = OrderedDict([
        ('field_1', 'string'),
        ('field_2', 'int32'),
        ('field_3', 'boolean'),
    ])
    conditions = list()
    conditions.append(Condition('field_1', 'in', ['1', '2', '3']))
    conditions.append(Condition('field_2', '==', 5) & Condition('field_2', '!=', 7))
    conditions.append(Condition('field_3', 'is not'))

    exp = '''
        INSERT INTO `dst_table`
        SELECT
            field_1,
            field_2,
            field_3
        FROM `src_table`
        WHERE
            field_1 IN ("1", "2", "3") OR
            (field_2 == 5 AND
            field_2 != 7) OR
            field_3 IS NOT NULL
    '''

    assert dedent(exp) == YtPersistenceManager.get_select_query('src_table', 'dst_table', schema, conditions)


def test_upsert_query():
    schema = OrderedDict([
        ('field_1', 'string'),
        ('field_2', 'int32'),
        ('field_3', 'boolean'),
    ])

    key_fields = ['field_1', 'field_2']
    fields_to_update = ['field_2', 'field_3']

    exp = '''
        INSERT INTO `dst_table` WITH TRUNCATE
        SELECT
            dst.field_1 ?? src.field_1 AS field_1,
            src.field_2 ?? dst.field_2 AS field_2,
            src.field_3 ?? dst.field_3 AS field_3
        FROM `dst_table` AS dst
        FULL JOIN `src_table` AS src
        USING(field_1, field_2);
    '''
    res = YtPersistenceManager.get_join_query('src_table', 'dst_table', schema, key_fields, fields_to_update, 'FULL')

    assert dedent(exp) == res


def test_remove_query():
    schema = OrderedDict([
        ('field_1', 'string'),
        ('field_2', 'int32'),
        ('field_3', 'boolean'),
    ])

    key_fields = ['field_1', 'field_2']

    exp = '''
        INSERT INTO `dst_table` WITH TRUNCATE
        SELECT
            dst.field_1 ?? src.field_1 AS field_1,
            dst.field_2 ?? src.field_2 AS field_2,
            dst.field_3 ?? src.field_3 AS field_3
        FROM `dst_table` AS dst
        LEFT ONLY JOIN `src_table` AS src
        USING(field_1, field_2);
    '''
    res = YtPersistenceManager.get_join_query('src_table', 'dst_table', schema, key_fields, [], 'LEFT ONLY')

    assert dedent(exp) == res
