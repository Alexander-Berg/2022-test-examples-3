# -*- coding: utf-8 -*-

import os
import re
import socket
import time
import json
import getpass
from copy import deepcopy

import pytest
from fabric.api import *
from fabric.contrib.files import append, exists
import fabric.api
import fabric.utils

@pytest.fixture(scope='function')
def enable_kuka(request, report_path, sandbox):
    filename = os.path.join(report_path, 'conf/report/enable_kuka')

    def fin():
        if os.path.exists(filename):
            os.remove(filename)

    request.addfinalizer(fin)

    def set_kuka(query):
        rewrite_file(sandbox, filename, '')
        query.headers.set_custom_headers({'X-Yandex-Enable-Kuka': 'c4456632cca9'})
        query.set_internal()

    return set_kuka

def sudo(*keys, **kwargs):
    # if request.config.getoption('verbose') < 2:
    #     kwargs['quiet'] = True

    kwargs['quiet'] = True
    r = fabric.api.sudo(*keys, **kwargs)
    if r.failed:
        msg = "Error!\n\nRequested: %s\nExecuted: %s\nReturn value: %s\nSTDOUT: %s\nSTDERR: %s" % (
            r.command, r.real_command, r.return_code, r.stdout, r.stderr
        )
        fabric.utils.error(message=msg)
    return r

def run(*keys, **kwargs):
    # if request.config.getoption('verbose') < 2:
    #     kwargs['quiet'] = True

    kwargs['quiet'] = True
    r = fabric.api.run(*keys, **kwargs)
    if r.failed:
        msg = "Error!\n\nRequested: %s\nExecuted: %s\nReturn value: %s\nSTDOUT: %s\nSTDERR: %s" % (
            r.command, r.real_command, r.return_code, r.stdout, r.stderr
        )
        fabric.utils.error(message=msg)
    return r


@pytest.fixture(scope='session')
def report_path(request):
    return request.config.option.reportpath

@pytest.fixture(scope='session')
def apache_path(request):
    return os.path.join(request.config.option.reportpath, 'apache_bundle')

@pytest.fixture(scope='session')
def templates_schema_path(request):
    # TODO make list.txt with schemas for ALL templates projects
    return os.path.join(request.config.option.reportpath, 'report-templates', 'YxWeb', 'web4', 'schema')

@pytest.fixture(scope='session')
def eventlog_reader_path(apache_path):
    return os.path.join(apache_path, 'bin', 'evlogdump')

@pytest.fixture(scope='session')
def report_flags_path(report_path):
    return os.path.join(report_path, 'conf/report/flags.json')

@pytest.fixture(scope='session')
def upperconf_path(report_path):
    return os.path.join(report_path, 'report/scripts/dev/upperconf.pl')

@pytest.fixture(scope='session')
def requestsjson_path(report_path):
    return os.path.join(report_path, 'conf/request.json')

@pytest.fixture(scope='session')
def logs_path(report_path, sandbox):
    if sandbox:
        return os.path.join(report_path, 'logs_report')
    else:
        return os.path.join(report_path, 'logs')

@pytest.fixture(scope='session')
def eventlog_path(logs_path):
    return os.path.join(logs_path, 'eventlog.U')

@pytest.fixture(scope='session')
def errorlog_path(logs_path):
    return os.path.join(logs_path, 'error_log')

@pytest.fixture(scope='session')
def accesslog_path(logs_path):
    return os.path.join(logs_path, 'access_log')

@pytest.fixture(scope='session')
def xml_reqanslog_path(logs_path):
    return os.path.join(logs_path, 'xmlreqans_log')

@pytest.fixture(scope='session')
def reqanslog_path(logs_path):
    return os.path.join(logs_path, 'reqans_log')

@pytest.fixture(scope='session')
def abuselog_path(logs_path):
    return os.path.join(logs_path, 'search_abuse_log')

@pytest.fixture(scope='session')
def profilelog_path(logs_path):
    return os.path.join(logs_path, 'profile_log')

@pytest.fixture(scope='session')
def dataruntime_path(report_path):
    return os.path.join(report_path, 'data.runtime')

@pytest.fixture(scope='session')
def dataruntime_real_path(dataruntime_path, report_path, sandbox):
    if sandbox:
        return dataruntime_path
    else:
        data_dir = sudo("readlink -f %s" % dataruntime_path)
        return os.path.join(report_path, str(data_dir.rstrip()))

@pytest.fixture(scope='session')
def datapermanent_path(report_path):
    return os.path.join(report_path, 'data.permanent')

@pytest.fixture(scope='session')
def dataruntimeold_path(report_path):
    return os.path.join(report_path, 'data.runtime.old')

@pytest.fixture(scope='session')
def report_ctx(upperconf_path, report_flags_path, report_path, requestsjson_path, report_ip, report_port, dataruntime_path, dataruntime_real_path, dataruntimeold_path, datapermanent_path,
               logs_path, profilelog_path, reqanslog_path, xml_reqanslog_path, accesslog_path, errorlog_path, abuselog_path, eventlog_path, eventlog_reader_path):
    ctx = dict()
    ctx['upperconf.pl'] = upperconf_path
    ctx['flags.json'] = report_flags_path
    ctx['path'] = report_path
    ctx['ip'] = report_ip
    ctx['port'] = report_port
    ctx['request.json'] = requestsjson_path
    ctx['data.runtime'] = dataruntime_path
    ctx['data.runtime.real'] = dataruntime_real_path
    ctx['data.permanent'] = datapermanent_path
    ctx['data.runtime.old'] = dataruntimeold_path
    ctx['eventlog_reader'] = eventlog_reader_path
    ctx['logs'] = logs_path
    ctx['logs.eventlog'] = eventlog_path
    ctx['logs.profile_log'] = profilelog_path
    ctx['logs.reqans_log'] = reqanslog_path
    ctx['logs.search_abuse_log'] = abuselog_path
    ctx['logs.xml_reqans_log'] = xml_reqanslog_path
    ctx['logs.access_log'] = accesslog_path
    ctx['logs.error_log'] = errorlog_path
    return ctx

@pytest.fixture(scope='session', autouse=True)
def setup_fabric(report_ip):
    TEAMCITY_PASSWORD = 'Cleph8OvOwd'
    if getpass.getuser() == 'teamcity':
        env.password = TEAMCITY_PASSWORD
    env.host_string = report_ip

def rewrite_file(sandbox, path, data):
    if sandbox:
        f = local
    else:
        f = sudo

    # чтобы у файла изменился mtime
    time.sleep(1)

    if 'controls' in path:
        tmp_filename = path + str(time.time())
        f("touch " + tmp_filename)
        f('echo \'' + data + '\' >> ' + tmp_filename)
        f('touch %s' % path)
        f("mv %s %s" % (path, path + str(time.time()-100)))
        f("mv %s %s" % (tmp_filename, path))
    else:
        f("rm -rf %s" % path)
        f('echo \'' + data + '\' >> ' + path)

def create_runtime_test_data(sandbox, report_ctx):
    if sandbox:
        f = local
    else:
        f = sudo
    if report_ctx['data.runtime'] == report_ctx['data.runtime.real']:
        f("cp -r %s %s" % (report_ctx['data.runtime.real'], report_ctx['data.runtime.old']))
    else:
        f("rm -v %s" % report_ctx['data.runtime'])
        f("cp -r %s %s" % (report_ctx['data.runtime.real'], report_ctx['data.runtime']))
    f("chmod -R a+w %s" % report_ctx['data.runtime'])

def remove_runtime_test_data(sandbox, report_ctx, restart=True):
    if sandbox:
        f = local
    else:
        f = sudo
    f("rm -rv %s" % report_ctx['data.runtime'])
    if report_ctx['data.runtime'] == report_ctx['data.runtime.real']:
        f("cp -r %s %s" % (report_ctx['data.runtime.old'], report_ctx['data.runtime.real']))
    else:
        f("ln -s %s %s" % (report_ctx['data.runtime.real'], report_ctx['data.runtime']))

def create_permanent_test_data(sandbox, report_ctx):
    if sandbox:
        f = local
    else:
        f = sudo

    f("cp -r %s %s" % (report_ctx['data.runtime.real'], report_ctx['data.permanent']))
    f("chmod -R a+w %s" % report_ctx['data.permanent'])

def remove_permanent_test_data(sandbox, report_ctx):
    if sandbox:
        f = local
    else:
        f = sudo
    f("rm -rv %s" % report_ctx['data.permanent'])

def set_json_data(sandbox=False, filename=None, data=None):
    new_data = json.dumps(data, indent=4)
    rewrite_file(sandbox, filename, new_data)

@pytest.fixture(scope='session', autouse=True)
def global_sources(report_ctx, sandbox):
    if sandbox:
        output = local("cat " + report_ctx['request.json'], capture=True)
    else:
        output = run("cat " + report_ctx['request.json'])
    return json.loads(output)

def get_log_size(log_path, sandbox):
    size=''
    if sandbox:
        size = local('du -bL %s' % log_path, capture=True)
    else:
        size = run('du -bL %s' % log_path)
    size = int(size.split('\t')[0])

    return size

def read_lines(sandbox, log_path, lines=1, reqid=None):
    if reqid:
        cmd = 'grep "%s" %s' % (reqid, log_path)
    else:
        cmd = 'egrep -v "^(RecodeToUnicode|REQID:)" %s | tail -n %d' % (log_path, lines)

    if sandbox:
        return local(cmd, capture=True)
    else:
        return run(cmd)

def get_log_lines(log_path, sandbox, prev_size, reqid=None, lines=1, wait=150):
    cnt = wait
    while True:
        cnt -= 1
        cur_size = get_log_size(log_path, sandbox)

        if cur_size > prev_size:
            break

        if cnt == 0:
            if sandbox:
                assert local('ls -la %s' % log_path, capture=True)
            else:
                assert run('ls -la %s' % log_path)

            raise Exception("Can't wait log to be flushed. Was apache request here really???")

        print '... waiting apache flushing logs'
        time.sleep(1)

    return read_lines(sandbox, log_path, lines, reqid=reqid)

def summary_json(request, data_type, report_ctx, sandbox):
    if data_type == 'runtime':
        summary_path = os.path.join(report_ctx['data.runtime'], '.metainfo', 'summary.json')
    else:
        summary_path = os.path.join(report_ctx['data.permanent'], '.metainfo', 'summary.json')

    if sandbox:
        output = local("cat " + summary_path, capture=True)
    else:
        output = run("cat " + summary_path)

    summary_json = json.loads(output)
    return summary_json

@pytest.fixture(scope='function')
def runtime_summary_json(request, report_ctx, sandbox):
    return summary_json(request, 'runtime', report_ctx, sandbox)

@pytest.fixture(scope='function')
def permanent_summary_json(request, report_ctx, sandbox):
    return summary_json(request, 'permanent', report_ctx, sandbox)

@pytest.fixture(scope='function')
def write_data(sandbox, report_ctx):
    def write_data(data_type, path, data):
        rewrite_file(sandbox, os.path.join(report_ctx[data_type], path), data)

    return write_data

@pytest.fixture(scope='function', autouse = True)
def get_all_logs(request, sandbox, report_ctx, fs_manager):
    def fin():
        if not request.node.report.passed:
            logs = [report_ctx['logs.access_log'], report_ctx['logs.error_log']]
            for log in logs:
                lines = read_lines(sandbox, log, 5)
                log_file = fs_manager.create_file(log.split('/')[-1])
                with open(log_file, 'wb') as f:
                    f.write(str(lines))
    request.addfinalizer(fin)

import report.util
@pytest.fixture(scope='session', autouse=True)
def get_logs(setup_fabric, report_ctx, sandbox, request):
    verbose = request.config.getoption('verbose')

    report.util.EventLog(report_ctx, sandbox, verbose)
    report.util.ProfileLog(report_ctx['logs.profile_log'], sandbox, verbose)
    report.util.AccessLog(report_ctx['logs.access_log'], sandbox, verbose)
    report.util.ReqansLog(report_ctx['logs.reqans_log'], sandbox, verbose)
    report.util.SearchAbuseLog(report_ctx['logs.search_abuse_log'], sandbox, verbose)

@pytest.fixture(scope='session')
def itsflags_path(request, report_path, sandbox):
    if sandbox:
        local("mkdir -p %s" % os.path.join(report_path, 'controls'))
    else:
        sudo("mkdir -p %s" % os.path.join(report_path, 'controls'))

    def fin():
        if sandbox:
            local("rm -rf %s" % os.path.join(report_path, 'controls', 'flags.json1*'))
        else:
            sudo("rm -rf %s" % os.path.join(report_path, 'controls', 'flags.json1*'))
    request.addfinalizer(fin)
    return os.path.join(report_path, 'controls/flags.json')

def get_simple_flags(sandbox, filename):
    flags = {}
    if sandbox:
        if os.path.exists(filename):
            output = local("cat " + filename, capture=True)
            flags = json.loads(output)[0]
        else:
            local("touch " + filename)
            local("echo \'[{}]\' >> " + filename)
    else:
        if exists(filename):
            output = run("cat " + filename)
            if output:
                flags = json.loads(output)[0]
        else:
            sudo("touch " + filename)
            append(filename, '[{}]', use_sudo=(sandbox == False))
    return flags

def get_its_flags(sandbox, itsflags_path):
    return get_simple_flags(sandbox, itsflags_path)

def set_its_flags(sandbox, itsflags_path, flags):
    if sandbox:
        local("rm -rf " + itsflags_path)
    else:
        sudo("rm -rf " + itsflags_path)
    return add_its_flags(sandbox, itsflags_path, flags)

def add_its_flags(sandbox, itsflags_path, flags):
    if not flags: return
    new_flags = get_its_flags(sandbox, itsflags_path)
    new_flags.update(flags)
    set_json_data(sandbox, itsflags_path, new_flags)

@pytest.fixture(scope='function')
def write_its_flags(request, sandbox, itsflags_path):
    old_flags = get_its_flags(sandbox, itsflags_path)

    def fin():
        set_its_flags(sandbox, itsflags_path, old_flags)
    request.addfinalizer(fin)

    def write_flags(flags):
        add_its_flags(sandbox, itsflags_path, flags)

    return write_flags

