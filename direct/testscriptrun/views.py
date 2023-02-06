# -* encoding: utf-8 -*-
from django.conf import settings
from django.contrib.auth.decorators import login_required
from django.http import Http404
from django.shortcuts import render_to_response
from django.template import RequestContext
from releaser.rights.tools import allowed
from releaser.testscriptrun.tools import get_whitelist, parse_stage, run_script
from urllib import urlencode

import urllib2

@login_required
@allowed(['developer', 'test_engineer'])
def index(r):
    if not settings.TESTSCRIPTRUN_ENABLED:
        raise Http404

    stage = r.REQUEST.get('stage')
    script = r.REQUEST.get('script')
    params = r.REQUEST.get('params')
    shard = r.REQUEST.get('shard')
    execute = r.REQUEST.get('do')
    log_tee = bool(r.REQUEST.get('log_tee'))

    restart_dict = {}
    restart_link = None
    if stage:
        restart_dict['stage'] = stage
    if script and script != '--':
        restart_dict['script'] = script
    if params:
        restart_dict['params'] = params
    if shard:
        restart_dict['shard'] = shard
    if restart_dict:
        restart_dict['log_tee'] = log_tee
        restart_link = r.build_absolute_uri('?' + urlencode(restart_dict))

    scripts_list = ['--']
    script_result = dict()
    hint_message = None
    (url, error_message) = parse_stage(stage)

    try:
        if url is not None:
            scripts_list.extend(get_whitelist(url))
            scripts_list.sort()

        if url is not None and execute:
            if script is None or script == '--':
                if error_message is None:
                    error_message = 'Скрипт не выбран'
            else:
                if not params and not shard:
                    execute_params='--help'
                    hint_message="Скрипт был запущен с параметром --help. \
                        Если требуется запустить скрипт без указания параметров - впиши в поле 'с параметрами' хотя бы пробел."
                elif shard:
                    execute_params = '--shard-id %s %s' % (shard, params)
                else:
                    execute_params = params
                script_result.update(run_script(url, r.GET.get('script'), execute_params, log_tee))

    except urllib2.HTTPError as e:
        error_message = 'Ошибка подключения к стенду: %s. Проверьте, что стенд указан правильно и работает.' % e

    return render_to_response('testscriptrun/index.html',
                              {
                                'scripts_list': scripts_list,
                                'script_result': script_result,
                                'stage': stage,
                                'choosed_script': script,
                                'shard': shard,
                                'params': params,
                                'log_tee': log_tee,
                                'restart_link': restart_link,
                                'error_message': error_message,
                                'hint_message': hint_message,
                              },
                              context_instance=RequestContext(r),
                              )
