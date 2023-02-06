# -* encoding: utf-8 -*-
from django.conf import settings

import json
import re
import urllib2

stage_pattern = re.compile('^(?:(?P<is_beta>beta)(?P<beta_num>[1-7]):(?P<beta_port>1?[0-9]{4})|(?P<is_test>tc)(?P<tc_num>(?:[1-2]|)))$', re.IGNORECASE)

def jsonrpc_req(host, method, params):
    req = urllib2.Request('https://%s/secret-jsonrpc/TestScriptRun' % host,
                          data=json.dumps({"method": method, "params": params}))
    req.add_header('User-agent', settings.USER_AGENT)
    req.add_header('Content-type', 'application/json; charset=utf8')
    response = urllib2.urlopen(req, timeout=320)
    json_result = response.read()
    return json.loads(json_result, encoding='utf8')

def run_script(host, script, params='--help', log_tee=False):
    assert isinstance(log_tee, bool)
    return jsonrpc_req(host, 'run_script', {'script': script,
                                            'cmdline': params,
                                            'secret': 'b3uPkvVcVQGdGKpu',
                                            'log_tee': log_tee,
                                            },
                       )

def get_whitelist(host):
    return jsonrpc_req(host, 'get_whitelist', {})['result']

def parse_stage(stage):
    (url, error) = (None, None)

    if stage is not None:
        stage_match = re.match(stage_pattern, stage)
        if stage_match is not None:
            parsed_stage = stage_match.groupdict()
            if parsed_stage['is_beta']:
                url = '{beta_port}.beta{beta_num}.direct.yandex.ru'.format(**parsed_stage)
            elif parsed_stage['is_test'] and (parsed_stage['tc_num'] == '' or parsed_stage['tc_num'] == '1'):
                url = 'intapi.test.direct.yandex.ru:9443'
            elif parsed_stage['is_test'] and parsed_stage['tc_num'] == '2':
                url = 'test2-direct.yandex.ru:9443'
            else:
                error = 'Не удалось определить адрес стенда'
        else:
            error = 'Не удалось разобрать обозначение стенда'
    else:
        error = 'Стенд не указан'

    return (url, error)
