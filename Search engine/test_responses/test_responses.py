#!/usr/bin/env python

from __future__ import print_function

import sys
reload(sys)
sys.setdefaultencoding('UTF8')

import contextlib
import functools
import json
import mmap
import multiprocessing
import os
import re
import requests
import shutil
import socket
import subprocess
import tarfile
import time

import yatest.common

from datetime import datetime, timedelta

os.environ["MKL_CBWR"] = "COMPATIBLE"

sys.path.append(yatest.common.source_path('sandbox/projects/common/wizard'))
sys.path.append(yatest.common.source_path('sandbox/projects/websearch/begemot/tasks/BegemotCreateResponsesDiff'))
try:
    import jsondiff
    import printwizard as pw
finally:
    sys.path[-2:] = []

try:
    unicode
except NameError:
    unicode = str


SRC_ROOT = os.environ['YA_SOURCE_ROOT']  # uncontrolled, unsuitable for distbuild
TEST_ROOT = os.path.join(SRC_ROOT, 'search', 'wizard', 'test_responses')
# All the logs and diffs go there:
OUT_DIR = os.path.join(TEST_ROOT, 'out')

INTERNAL_CA = yatest.common.source_path('certs/cacert.pem')
SANDBOX_DEDICATED_HOST = 'sandbox954.search.yandex.net'


def download_sandbox_resource(item, local_path, nattempts=10):
    url = item['http']['proxy']
    rbtorrent = item.get('skynet_id', None)
    filename = item['file_name']

    if rbtorrent is not None:
        local_dir = os.path.dirname(local_path)
        tmp_dir = os.path.join(local_dir, '.skytmp')
        os.mkdir(tmp_dir)
        try:
            yatest.common.execute(['sky', 'get', '-pu', '-d', tmp_dir, rbtorrent])
            shutil.move(os.path.join(tmp_dir, filename), local_path)
            return
        except Exception as e:
            log('`sky get` failed: %s\nFalling back to direct sandbox download (unreliable)' % e)
        finally:
            shutil.rmtree(tmp_dir)

    for attempt in xrange(nattempts):
        response = requests.get(url, stream=True)
        if response.status_code == 200:
            break
        log('Retrying sandbox download, attempt %s out of %s' % (attempt + 1, nattempts))
    else:
        response.raise_for_status()
    with open(local_path, 'wb') as output:
        shutil.copyfileobj(response.raw, output)


def download_runtime(item):
    runtime_file = yatest.common.output_path('wizard.runtime.tar')
    download_sandbox_resource(item, runtime_file)
    runtime_dir = os.path.dirname(runtime_file)
    runtime_tar = tarfile.open(runtime_file, 'r')
    runtime_tar.extractall(runtime_dir)


def setup_module():
    task = get_latest_sandbox_task()
    rev = get_latest_sandbox_task.revision
    response = requests.get(
        'https://sandbox.yandex-team.ru:443/api/v1.0/task/%s/resources' % task,
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
    response = json.loads(response.text)
    setup_module.resources = response['items']

    response = requests.get(
        'https://sandbox.yandex-team.ru:443/api/v1.0/task/%s/requirements' % task,
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
    response = json.loads(response.text)
    runtime = None

    for item in response['items']:
        if item['type'] == 'WIZARD_RUNTIME_PACKAGE':
            log('runtime data', item['http']['proxy'], 'from task', '%s,' % task, 'r%s' % rev)
            runtime = item
    if runtime:
        download_runtime(runtime)
    else:
        log('runtime data is not used')
    if not os.path.exists(OUT_DIR):
        os.makedirs(OUT_DIR)


class dict2obj:
    def __init__(self, data):
        self.__dict__ = data


def wizard_data_path(sub=None):
    if sub:
        return yatest.common.build_path("search/wizard/data/" + sub)
    else:
        return yatest.common.build_path("search/wizard/data/")


SERVICES = {
    'geo':       ('geo-printwizard', 'test_geo/geo.txt'),
    'geosearch': ('geo',             'test_geosearch/geosearch.txt'),
}

RULES_SHOWN = {
    'geo': {'GeoAddr', 'GeoAddrRoute', 'GeoAddrUntranslit', 'GeoRelev', 'RelevLocale', 'Date', 'Transit', 'Qtree'},
    'geosearch': {'Coord', 'GeoAddr', 'GeoAddrUntranslit', 'GeoAddrRoute', 'OrgNav', 'BusinessNav', 'CustomThesaurus/Geo',
                  'TelOrg', 'GeoRelev', 'CommercialMx', 'GeosearchStopwords', 'PPO', 'RelevLocale', 'Transport', 'Rubrics'},
}

_LOG_FORMATS = {
    'head': ('== \033[1m{message}\033[0m ==', '== {message} =='),
    'fatal': ('\033[31;1mERR\033[0m {message}', '-- FATAL: {message}'),
    'debug': ('\033[36;1mDBG\033[0m {message}', '-- DEBUG: {message}'),
    'stage': ('\033[32;1mFIN\033[0m [\033[1m{duration}\033[0m] {message}', '-- STAGE: {duration} / {message}'),
    'procrun': ('\033[33;1mRUN\033[0m {message}', '-- RUN: {message}'),
    'procout': ('\033[30;1m  + {message}\033[0m', '-- OUT: {message}'),
    None: ('\033[1m{kind}\033[0m {message}', '-- {kind}: {message}'),
}


def service_config(service, root=yatest.common.output_path('configs')):
    return os.path.join(root, SERVICES[service][0]) + '-yaml.cfg'


def requests_file(service, root=yatest.common.source_path('tools/printwzrd/tests')):
    if os.path.exists(SERVICES[service][1]):  # sandbox resource in DATA
        return SERVICES[service][1]
    return os.path.join(root, SERVICES[service][1])


def log(*args, **kwargs):
    kind = kwargs.pop('kind', 'debug')
    with_color = kwargs.pop('with_color', os.environ.get("TERM", "").startswith("xterm") or os.environ.get("TERM", "") == 'screen')
    kwargs['message'] = kwargs.pop('sep', ' ').join(map(str, args))
    kwargs['kind'] = kind.upper()
    if kind == 'head':
        kwargs['message'] = kwargs['message'].upper()
    elif kind == 'procrun':
        dirs = [
            (yatest.common.output_path(''), 'O'),
            (yatest.common.build_path(''), 'B'),
            (yatest.common.binary_path(''), 'B'),
            (yatest.common.source_path(''), 'S'),
        ]
        for dir, code in dirs:
            kwargs['message'] = kwargs['message'].replace(dir, ('\033[30;1m${}\033[0m' if with_color else '${}').format(code))
    print(_LOG_FORMATS.get(kind, _LOG_FORMATS[None])[not with_color].format(**kwargs), file=sys.stderr)

log('output path $(O) =', yatest.common.output_path(''))


def measure(stage_name):
    def decorator(f):
        @functools.wraps(f)
        def inner(*args, **kwargs):
            start = datetime.now()
            try:
                ret = f(*args, **kwargs)
            finally:
                log(stage_name, kind='stage', duration=datetime.now() - start)
            return ret
        return inner
    return decorator


def generate_config(service):
    generator = yatest.common.binary_path("search/wizard/data/wizard/conf/executable/executable")
    yatest.common.execute([
        generator,
        "--shard-prefix", 'wizard',
        '--printwizard', '--quiet',
        '--basedir', yatest.common.source_path("search/wizard/data/wizard/conf"),
        yatest.common.output_path('configs')
    ])

    if service == 'geosearch':
        geosearch_config_path = yatest.common.output_path(os.path.join('configs', service_config(service)))
        with open(geosearch_config_path, 'r') as fd:
            geo_config = fd.read()
            geo_config = re.sub(r'RequestPopularityTrieFile .*', lambda x: x.group(0) + "\nUseTestData yes", geo_config)
        with open(geosearch_config_path, 'w') as fd:
            fd.write(geo_config)


def is_ready(port):
    try:
        sock = socket.create_connection(('127.0.0.1', port))
        sock.close()
    except socket.timeout:
        return False
    except socket.error:
        return False
    return True


def tail(filename, n):
    """
    Returns last n lines from the filename. No exception handling
    https://stackoverflow.com/a/6813975/1266605
    """
    size = os.path.getsize(filename)
    with open(filename, "rb") as f:
        # for Windows the mmap parameters are different
        fm = mmap.mmap(f.fileno(), 0, mmap.MAP_SHARED, mmap.PROT_READ)
        try:
            for i in xrange(size - 1, -1, -1):
                if fm[i] == '\n':
                    n -= 1
                    if n == -1:
                        break
            return fm[i + 1 if i else 0:].splitlines()
        finally:
            fm.close()


def run_and_wait(argv, port, logfile=None, **kwargs):
    log(*argv, kind='procrun')
    if logfile:
        kwargs['stderr'] = open(logfile, 'w')
    proc = subprocess.Popen(argv, bufsize=1, **kwargs)
    while not is_ready(port):
        if proc.poll():
            if logfile:
                log('\n'.join(tail(logfile, 10)), kind='debug')
                log_note = "\nSee the log: %s" % logfile
            raise Exception("wizard {} exited with code {}{}".format(str(argv), proc.returncode, log_note))
        time.sleep(1)
    return proc


def terminate(proc):
    try:
        proc.terminate()
    except Exception:
        pass
    proc.wait()
    if proc.returncode and proc.returncode != -15:
        raise Exception('wizard has died with code {}'.format(proc.returncode))


@contextlib.contextmanager
def pool():
    pool = multiprocessing.Pool(processes=8)
    try:
        yield pool
    finally:
        pool.close()
        pool.join()


def reserve_port():
    '''
    Find and reserve a free port
    '''
    sock = socket.socket(socket.AF_INET6)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('', 0))
    port = sock.getsockname()[1]
    return sock, port


@measure('local response collection')
def run_wizard(service, runtime_data):
    err = os.path.join(OUT_DIR, '%s.err' % service)
    binary = yatest.common.binary_path('web/daemons/wizard/wizard')
    evlog = os.path.join(OUT_DIR, '%s.evlog' % service)
    unpackrichtree = yatest.common.binary_path('tools/unpackrichtree/unpackrichtree')
    unpackrichtree = [[unpackrichtree, '-urrl'], [unpackrichtree, '-us']]
    unpackreqbundle = [yatest.common.binary_path('quality/relev_tools/lboost_ops/unpackreqbundle/unpackreqbundle'), '-dj']
    args = [
        binary,
        '--data', wizard_data_path(),
        '--config', service_config(service),
        '--evlog', evlog,
        '--jobs', str(multiprocessing.cpu_count() / 2),
        '--nocache',
        '--mmap',
        '--runtime-data', runtime_data,
    ]
    reserve_port_sock, port = reserve_port()
    args += ['--port', str(port)]
    wizard = run_and_wait(args, port, logfile=err)
    reserve_port_sock.close()
    host = 'localhost:{}'.format(port)
    try:
        with pool() as p, open(requests_file(service)) as reqs:
            ex_params = pw.extra_cgi_parameters(service)
            responses = dict(pw.printwzrd(host, reqs, ex_params, RULES_SHOWN.get(service, []), p, unpackrichtree, unpackreqbundle))
    finally:
        try:
            requests.get('http://{}/admin?action=shutdown'.format(host))
        except Exception:
            log('could not call action=shutdown')
        terminate(wizard)
    return responses


def get_src_revision():
    try:
        return get_src_revision.result
    except Exception:
        pass

    ya = yatest.common.source_path('ya')

    for svn in [ya, 'tool', 'svn'], ['svn']:
        try:
            svn_ans = subprocess.check_output(svn + ['info', "%s/search" % SRC_ROOT], stderr=subprocess.STDOUT).split('\n')

            for line in svn_ans:
                if not line.startswith('Revision: '):
                    continue
                get_src_revision.result = int(line.split('Revision: ')[1]), 'svn'
                return get_src_revision.result
        except subprocess.CalledProcessError:
            pass

    hg = subprocess.Popen([ya, 'tool', 'hg', 'log', '-T', "{join(extras,'\\n')}", '-r', 'max(ancestors(.) and branch(default))', SRC_ROOT], stdout=subprocess.PIPE)
    for line in hg.stdout:
        if 'arcadia@' not in line:
            continue
        rev = int(line.split('arcadia@')[1].split('branch')[0])
        hg.kill()
        get_src_revision.result = rev, 'hg'
        return get_src_revision.result


def sandbox_task_states(task_id):
    details = requests.get(
        'https://sandbox.yandex-team.ru:443/api/v1.0/task/%s/audit' % task_id,
        headers={'Content-Type': 'application/json'}
    )
    details.raise_for_status()
    details = details.text
    ret = []
    for state in json.loads(details):
        if 'status' in state:
            ret.append(state['status'])
    return ret


def print_tasks_list(lst, annotation):
    return '\n\n{ann}:\n{lst}'.format(ann=annotation, lst='\n'.join(lst)) if lst else ''


def check_dedicated_host_presence():
    '''
    Printwizard has a host to run on to shrink tests queue.
    Maybe after another reconfiguration this host is gone?
    We need to warn the user if it is so.
    Yes, it has already happened once.
    '''
    try:
        s = socket.create_connection((SANDBOX_DEDICATED_HOST, 22), timeout=1)
        s.close()
        return ''
    except Exception as x:
        return '\n'.join([
            '', '',
            'Sandbox host %s dedicated for printwizard is not accessible.' % SANDBOX_DEDICATED_HOST,
            'Please, notify wizard owners and write to st/REQWIZARD-1162 that the host is gone!',
            'If the host is really down, the test may be outdated for many hours (24 maybe).',
            'Python exception: %s' % x,
        ])


def get_latest_sandbox_task():
    try:
        return get_latest_sandbox_task.task
    except Exception:
        pass
    TE_DATABASE = 'ws-begemot-trunk'
    TE_TASKS = ['PRINTWZRD', 'BUILD_PRINTWIZARD_CONFIG', 'BUILD_WIZARD_DATA', 'BUILD_WIZARD_EXECUTABLE']
    ST = 'http://st.yandex-team.ru/TESTENV-1659'
    work_copy_svn_revision, vcs = get_src_revision()
    if not work_copy_svn_revision:
        raise RuntimeError("Failed to detect base SVN revision to compare against\nMake sure you run this test from Arcadia SVN or Hg working copy")
    log('revision', work_copy_svn_revision)

    te_response = requests.get(
        'https://testenv.yandex-team.ru/handlers/get_last_sandbox_task_ids?database={db}&job_names={jobs}&revision={revision}'.format(
            db=TE_DATABASE, jobs=','.join(TE_TASKS), revision=work_copy_svn_revision
        ),
        verify=INTERNAL_CA
    ).text
    te_json = json.loads(te_response)['items']

    if len(te_json) != len(TE_TASKS):
        raise ValueError('Invalid response from TE. Try rerunning the test, more info: %s\n\nGot %d items instead of %d:\n' % (ST, len(te_json), len(TE_TASKS)) + te_response)

    jobs = {}
    for job in te_json:
        jobs[job['job_name']] = dict2obj(job)

    printwizard, cfg, binary, data = jobs['PRINTWZRD'], jobs['BUILD_PRINTWIZARD_CONFIG'], jobs['BUILD_WIZARD_EXECUTABLE'], jobs['BUILD_WIZARD_DATA']

    if cfg.revision < printwizard.revision:
        raise ValueError('Got insane results from TE: printwzrd revision is greater than printwzrd config revision.\n\nTry rerunning the test.\nResponse: %s\nMore info: %s' %
                            (te_response, ST))

    if cfg.revision == printwizard.revision:
        printwizard.states = sandbox_task_states(printwizard.task_id)
    else:
        printwizard.states = []

    # Here we assume that the cfg task is almost never outdated
    if cfg.revision == printwizard.revision and 'SUCCESS' in printwizard.states:
        get_latest_sandbox_task.task = printwizard.task_id
        get_latest_sandbox_task.revision = printwizard.revision
        return printwizard.task_id

    # If we're here, then most likely there are no canonical results yet
    last_ok = dict2obj(requests.get(
        'https://testenv.yandex-team.ru/handlers/get_last_sandbox_task_ids?database=ws-begemot-trunk&job_names=PRINTWZRD&success=1',
        verify=INTERNAL_CA
    ).json()['items'][0])

    # Maybe config was built for another test, but we already have all needed PRINTWZRD results?
    if last_ok.revision >= work_copy_svn_revision:
        get_latest_sandbox_task.task = last_ok.task_id
        get_latest_sandbox_task.revision = last_ok.revision
        return last_ok.task_id

    if os.environ.get("USE_OUTDATED_CANONDATA", 'no') == 'yes':
        log("USE_OUTDATED_CANONDATA == yes, so using outdated canonical data. The diff may be inaccurate.")
        get_latest_sandbox_task.task = last_ok.task_id
        get_latest_sandbox_task.revision = last_ok.revision
        return last_ok.task_id

    waiting_for = []
    enqueued = []
    timedout = []
    failed = []
    for j in (data, binary, printwizard):
        if j.revision != cfg.revision:
            continue
        states = sandbox_task_states(j.task_id)
        url = 'https://sandbox.yandex-team.ru/task/{task}/view  (for rev {rev}, {name}, {status})'.format(task=j.task_id, name=j.job_name, rev=j.revision, status=states[-1])
        if states[-1] == 'FAILURE':
            failed.append(url)
            continue
        if 'FINISHING' not in states:
            if 'PREPARING' not in states:
                enqueued.append(url)
                continue
            if states[-1] == 'TIMEOUT':
                timedout.append(url)
                continue

            waiting_for.append(url)
    raise_errors(waiting_for, enqueued, timedout, failed, last_ok.revision, work_copy_svn_revision, vcs)


def raise_errors(waiting_for, enqueued, timedout, failed, last_ok_rev, work_copy_svn_revision, vcs):
    if failed:
        failed = print_tasks_list(failed, 'The following tasks failed. Try another base revision')
        enqueued = ''
        waiting_for = ''
        timedout = ''
    else:
        enqueued = print_tasks_list(enqueued, 'The following tasks are not ready yet, you can try increasing their priority')
        waiting_for = print_tasks_list(waiting_for, 'The following tasks are running, and you just have to wait for them')
        timedout = print_tasks_list(timedout, 'The following tasks timed out. You need to restart them')
        failed = ''

        if not enqueued and not waiting_for and not timedout:
            waiting_for = '\n'.join([
                '', '',
                'The task has not been created by TestEnv yet, see the timeline:',
                'https://testenv.yandex-team.ru/?screen=timeline&database=ws-begemot-trunk.',
                'You have to wait several minutes (or downgrade your working copy).',
            ])

    dedicated_host_warning = check_dedicated_host_presence()
    workcopy_up_cmd = 'svn up -r %s' % last_ok_rev
    svn_up = "\n\nYou may run `%s` to downgrade to the latest revision with canondata ready." % workcopy_up_cmd

    if vcs == 'hg':
        ya = yatest.common.source_path('ya')
        arc_url = 'svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia/'
        svn_propget = ['svn', 'propget', '--revprop', '-r', str(last_ok_rev)]
        commit_note = subprocess.check_output(svn_propget + ['svn:log', arc_url]).strip()
        commiter = subprocess.check_output(svn_propget + ['svn:author', arc_url]).strip()
        svn_up = '\n\nYou may also downgrade to the revision with ready canondata.\nTo get the revision, run:\n\n' + \
                "stdbuf -o0 ya tool hg log --template \"hg up {node|short}: {join(extras,', ')} {desc}\\n\" --no-merges -k $'%s' -u %s | stdbuf -o0 grep %s | cut -d: -f1\n\n" % (
                    commit_note.replace('\n', '\\n'), commiter, last_ok_rev) + \
                'This command is way too slow to run it automatically from this script.\n'

        # TODO: make this autodetect not so slow (now it takes >1 minute) and run automatically
        def slow_autodetect():
            hg = subprocess.Popen(
                ['stdbuf', '-o0', ya, 'tool', 'hg', 'log', '--template', "{node|short}: {join(extras,', ')} {desc}\n", '-k', commit_note, '-u', commiter, '--no-merges', SRC_ROOT],
                stdout=subprocess.PIPE
            )
            for hg_rev in hg.communicate()[0]:
                if 'arcadia@%s' % last_ok_rev in hg_rev:
                    workcopy_up_cmd = 'hg up %s' % hg_rev.split(':')[0]
                    break
            else:
                workcopy_up_cmd = None
            try:
                hg.kill()
                print(str(workcopy_up_cmd))  # feed pep8
            except Exception:
                pass

    raise RuntimeError('\nThere are no canonical data yet for the revision {src_rev}.{enqueued}{waiting_for}{timedout}{failed}{host_warning}{svn_up}{outdated}\n---'.
                        format(src_rev=work_copy_svn_revision, enqueued=enqueued, waiting_for=waiting_for, timedout=timedout, failed=failed, host_warning=dedicated_host_warning,
                                svn_up=svn_up, outdated="\nAlso, you can `export USE_OUTDATED_CANONDATA=yes` in advance, see Readme.md for details."))


@measure('canonical data fetch')
def download_data(service):
    for item in setup_module.resources:
        if item.get('file_name', None) == '%s.json' % service:
            canonfile = yatest.common.output_path('canon-%s.json' % service)
            log('canonical data', item['http']['proxy'], 'from task', get_latest_sandbox_task.task)
            download_sandbox_resource(item, canonfile)
            break
    else:
        raise Exception('test {} has no canonical data'.format(service))

    runtimefile = yatest.common.output_path('wizard.runtime')

    return canonfile, runtimefile


def filter_responses_for_diff(j):
    for response in j.itervalues():
        try:
            response.pop('eventlog', None)
        except TypeError:
            return


@measure('json diff')
def generate_diff(service, canonical, canonjson, testing, testjson):
    out_diff = os.path.join(OUT_DIR, '%s.diff' % service)
    out_shdf = os.path.join(OUT_DIR, '%s.short.diff' % service)

    filter_responses_for_diff(canonjson)
    filter_responses_for_diff(testjson)
    for req in canonjson:
        if req not in testjson:
            canonjson[req] = "Request removed from test."
    timeout = datetime.now() + timedelta(minutes=10)
    objdiff = jsondiff.diff(canonjson, testjson, timeout)

    has_diff = False
    if objdiff is not None:
        for req, resp in objdiff.iteritems():
            if isinstance(resp, dict) and {k for k, v in resp.iteritems() if v is not None} == {u"Python exception"}:
                objdiff[req] = None
            elif resp is not None:
                has_diff = True
    if not has_diff:
        if os.path.exists(out_diff):
            os.unlink(out_diff)
        if os.path.exists(out_shdf):
            os.unlink(out_shdf)
        return None, None

    with open(out_diff, 'w') as out:
        for chunk in jsondiff.render_text([(canonical, testing, objdiff)]):
            out.write(chunk)
    grouped = jsondiff.group((u'[{}]'.format(k), v) for k, v in objdiff.iteritems() if v is not None)
    with open(out_shdf, 'w') as out:
        for chunk in jsondiff.render_text((vs[0][0] + u' x{}'.format(len(vs)), vs[0][1]) for vs in grouped):
            out.write(chunk)
    return out_diff, out_shdf


class NonEmptyDiffException(Exception):
    pass


def run_test(test):
    log(test, kind='head')
    generate_config(test)
    canon, runtime_data = download_data(test)
    local = yatest.common.output_path('%s.out' % test)
    responses = run_wizard(test, runtime_data)
    with open(canon) as canondata:
        canonjson = json.load(canondata)
    full, short = generate_diff(test, canon, canonjson, local, responses)
    if full:
        with open(local, 'w') as fd:
            json.dump(responses, fd)
        raise NonEmptyDiffException(
            'The diff is not empty.\n'
            '  Canonical: {}\n'
            '  Local run: {}\n'
            '  Full diff: {}\n'
            '  Collapsed: {}\n'.format(canon, local, full, short)
        )


def test_geosearch():
    run_test('geosearch')

def test_geo():
    run_test('geo')
