#!/usr/bin/env python
# -*- coding: utf-8 -*-

import six
import yatest.common

from crypta.profile.utils.segment_utils.yql_word_filter import YqlWordFilter


def canonize_query(query_text, word_conditions, prefix):
    query_path = yatest.common.work_path('{}_query.yql'.format(prefix))
    word_conditions_path = yatest.common.work_path('{}_word_conditions.txt'.format(prefix))

    with open(query_path, 'w') as query_file:
        query_file.write(six.ensure_str(query_text))

    with open(word_conditions_path, 'w') as word_conditions_file:
        word_conditions_file.write(six.ensure_str(word_conditions))

    return [
        yatest.common.canonical_file(query_path, local=True),
        yatest.common.canonical_file(word_conditions_path, local=True),
    ]


def test_yql_word_filter():
    word_filter = YqlWordFilter()
    word_filter.add_condition_string(
        rule_revision_id=0,
        condition_string=u'(mazda OR мазда) AND (6 OR cx5) AND NOT ахура'
    )
    word_filter.add_condition_string(
        rule_revision_id=1,
        condition_string=u'(форум OR forum OR портал) AND (военный OR исторический) AND NOT китай'
    )
    word_filter.add_condition_string(
        rule_revision_id=2,
        condition_string=u'((дримкас AND ф) OR ((модуль AND касса) OR модулькасса))'
    )
    word_filter.add_condition_string(
        rule_revision_id=3,
        condition_string=u'телевизор'
    )
    word_filter.add_condition_string(
        rule_revision_id=4,
        condition_string=u'(mazda OR'
    )

    query = word_filter.get_yql_query(
        input_table='//input/table',
        output_table='//output/table',
        word_rules_file='tmp/word_rules',
        rule_revision_ids=(1, 2, 3, 4, 5),
        data_size_per_job="100M",
    )

    return canonize_query(query.query_text, query.word_conditions, prefix='short')


def test_long_word_filter():
    count = 3000
    expr = u'(foo OR bar OR baz)'
    rule = u' AND '.join([expr] * count)

    word_filter = YqlWordFilter()
    word_filter.add_condition_string(
        rule_revision_id=0,
        condition_string=rule,
    )

    query = word_filter.get_yql_query(
        input_table='//input/table',
        output_table='//output/table',
        word_rules_file='tmp/long_world_rules',
        rule_revision_ids=None,
    )

    return canonize_query(query.query_text, query.word_conditions, prefix='long')
