#!/usr/bin/python
# -*- coding: utf-8 -*-

import sys
import codecs

from matcher import RulesParser, Url, TskvParser

# test.py action_test_cases rules/technical_rules.tskv rules/action_rules.tskv
# test.py landing_test_cases rules/technical_rules.tskv rules/landing_rules.tskv

test_filename = sys.argv[1]
rules_filenames = sys.argv[2:]

# кастомные правила исключаются
rules = RulesParser().parse_all(rules_filenames)

with codecs.open(test_filename, 'r', 'utf-8') as test_file:
    parser = TskvParser(['exp', 'url'])
    for line in test_file:
        (exp_rule_name, request) = parser.parse_as_list(line)
        if exp_rule_name is None and request is None:
            continue
        url = Url(request)
        matched_rule = None
        for rule in rules:
            action = rule.apply_to_url(url)
            if action is not None:
                matched_rule = rule
                break

        if matched_rule is None and exp_rule_name != "None":
            print('For url [{0}] no rules matched, but expected rule [{1}]'.format(request, exp_rule_name))
        if matched_rule is not None and exp_rule_name != matched_rule.name:
            print('For url [{0}] matched rule [{1}], but expected rule [{2}]'.format(request, matched_rule.name, exp_rule_name))

