#!/usr/bin/env python
# encoding: utf8

import json
import pytest
import sys
import os

import tasks_gen


# def test_general():
#     with open('test/tests_config.json') as tests_config_f:
#         test_args_list = json.load(tests_config_f)
#     errors = []
#     for test_args_num, test_args in enumerate(test_args_list):
#         print "Test args item #{}...".format(test_args_num)
#         params_to_check = [param for param in test_args if param.endswith('_')]
#         params_to_check_ans = [param.rstrip('_') for param in params_to_check]
#         tasks_gen.main([''] + test_args)
#         for param, param_ans in zip(params_to_check, params_to_check_ans):
#             print "Checking param {} against judge {}...".format(param, param_ans)
#             with open(param) as param_f, open(param_ans) as param_ans_f:
#                 param_c = param_f.read()
#                 param_ans_c = param_ans_f.read()
#                 if param_c != param_ans_c:
#                     errors.append({'id': test_args_num, 'param': param})
#                     print "Error!"


def test_run():
    with open('test/tests_config.json') as tests_config_f:
        test_args_list = json.load(tests_config_f)
    for test_args_num, test_args in enumerate(test_args_list):
        print "Test args item #{}...".format(test_args_num)
        tasks_gen.main([''] + test_args)
