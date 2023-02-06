#!/usr/bin/env python

from __future__ import print_function

import sys
reload(sys)
sys.setdefaultencoding('UTF8')

import json
import os
import requests
import shutil
import subprocess
import yatest.common
import urllib
os.environ["MKL_CBWR"] = "COMPATIBLE"

sys.path.append(yatest.common.source_path('sandbox/projects/websearch/begemot'))
sys.path.append(yatest.common.source_path('search/wizard/test_responses'))
import test_responses as tr
import common
sys.path[-2:] = []

try:
    unicode
except NameError:
    unicode = str

TEST_ROOT = os.path.join(tr.SRC_ROOT, 'search', 'begemot', 'test_responses')
# All the logs and diffs go there:
OUT_DIR = os.path.join(TEST_ROOT, 'out')
BEGEMOT_SHARDS = common.BegemotAllServices().Service.keys()
DATA_PATH = os.path.join(OUT_DIR, 'test_info.json')
try:
    with open(DATA_PATH) as fd:
        DATA_INFO = dict(json.load(fd))
except Exception:
    DATA_INFO = {}


def download_sandbox_resource(field_name, test, item, local_dir, nattempts=10):
    url = item['http']['proxy']
    global DATA_INFO
    if DATA_INFO.get(test).get(field_name) == item["id"]:
        tr.log('Use existing data for {}'.format(field_name))
        return
    rbtorrent = item.get('skynet_id', None)

    if rbtorrent is not None:
        try:
            yatest.common.execute(['sky', 'get', '-pu', '-d', local_dir, rbtorrent])
            fname = DATA_INFO[test][field_name] = os.path.basename(item['file_name'])
            return os.path.join(local_dir, fname)
        except Exception as e:
            tr.log('`sky get` failed: %s\nFalling back to direct sandbox download (unreliable)' % e)

    filename = item['file_name']
    if '/' in filename:
        raise RuntimeError('Failed downloading %s. Sky failed, and sandbox proxy does not allow to download folders' % item['type'])
    for attempt in xrange(nattempts):
        response = requests.get(url, stream=True)
        if response.status_code == 200:
            break
        tr.log('Retrying sandbox download, attempt %s out of %s' % (attempt + 1, nattempts))
    else:
        response.raise_for_status()

    fullname = os.path.join(local_dir, filename)
    with open(fullname, 'wb') as output:
        shutil.copyfileobj(response.raw, output)
    return fullname


def get_sandbox_resource(resource_id):
    if not resource_id:
        return None
    resource = requests.get(
        'https://sandbox.yandex-team.ru:443/api/v1.0/resource/%s' % resource_id,
        headers={'Content-Type': 'application/json'}
    )
    resource.raise_for_status()
    return resource.json()


def setup_module_begemot(test, task, shard, path):
    response = requests.get(
        'https://sandbox.yandex-team.ru:443/api/v1.0/task/%s/custom/fields' % task,
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
    response = response.json()
    data = {}
    for field in response:
        name = field["name"]
        data[name] = field['value']
        if field['value'] and field['type'] == 'resource' and name not in ['begemot_binary', 'begemot_config']:
            resource = get_sandbox_resource(field['value'])
            resource['file_name'] = shard if field['name'] == 'begemot_shard' else resource['file_name']
            data[name] = download_sandbox_resource(name, test, resource, path)

    response = requests.get(
        'https://sandbox.yandex-team.ru:443/api/v1.0/task/%s/resources' % task,
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
    response = json.loads(response.text)

    for field in response['items']:
        name = {
            'BEGEMOT_RESPONSES_RESULT': 'canon_response',
            'BEGEMOT_CGI_PLAN': 'requests_plan',
        }.get(field['type'])
        if not name:
            continue

        resource = get_sandbox_resource(field['id'])
        data[name] = download_sandbox_resource(name, test, resource, path)
    return data


@tr.measure('local response collection')
def run_begemot(service, output, data, path):
    err = str(os.path.join(path, '%s.err' % service))
    binary = yatest.common.binary_path('web/daemons/begemot/begemot')
    evlog = str(os.path.join(path, '%s.evlog' % service))
    args = [
        binary,
        '--data', data['begemot_shard'],
        '--log', evlog,
        '--test',
    ]
    if data['begemot_fresh']:
        args.extend(['--fresh', data['begemot_fresh']])
    if data['cache_size']:
        args.extend(['--cache_size', str(data['cache_size'])])
    if data['jobs']:
        args.extend(['--jobs', str(data['jobs'])])
    if data['is_cgi']:
        args.append('--test-cgi')

    args.extend(os.environ.get('BG_ARGS').split(' '))
    tr.log('Begemot launch arguments\n{}'.format(' '.join(args)))

    with open(data['requests_plan'], 'r') as input_file, open(output, 'w') as output_file, open(err, 'w') as error_file:
        code = subprocess.call(args, stdin=input_file, stdout=output_file, stderr=error_file)
        if code != 0:
            tr.log("Human, read %s, here's the tail:" % err, kind='fatal')
            subprocess.call(['tail', err], stdout=sys.stderr)
            raise Exception('Begemot exited with code {}'.format(code))


def get_latest_sandbox_task_begemot():
    try:
        return get_latest_sandbox_task_begemot.resourses
    except Exception:
        pass
    TE_DATABASE = 'ws-begemot-trunk'
    TE_TASKS = []
    for shard in BEGEMOT_SHARDS:
        TE_TASKS.extend(['GET_BEGEMOT_RESPONSES_{}'.format(shard), 'GET_BEGEMOT_RESPONSES_APPHOST_{}'.format(shard)])
    ST = 'http://st.yandex-team.ru/TESTENV-1659'
    work_copy_svn_revision, vcs = tr.get_src_revision()
    if not work_copy_svn_revision:
        raise RuntimeError("Failed to detect base SVN revision to compare against\nMake sure you run this test from Arcadia SVN or Hg working copy")
    te_response = requests.get(
        'https://testenv.yandex-team.ru/handlers/get_last_sandbox_task_ids?database={db}&job_names={jobs}&revision={revision}'.format(
            db=TE_DATABASE, jobs=','.join(TE_TASKS), revision=work_copy_svn_revision
        ),
        verify=tr.INTERNAL_CA
    ).text
    te_json = json.loads(te_response)['items']

    if not te_json:
        raise ValueError('Invalid response from TE. Try rerunning the test, more info: %s\n\nGot %d items' % (ST, len(te_json)))

    jobs = {}
    for job in te_json:
        jobs[job['job_name']] = tr.dict2obj(job)
    resourses = {}
    failed_tasks = []
    for test in jobs:
        resourses[test] = [jobs[test].task_id, jobs[test].revision]
        if 'SUCCESS' not in tr.sandbox_task_states(jobs[test].task_id):
            failed_tasks.append(test)

    if not failed_tasks:
        return resourses

    # If we're here, then most likely there are no canonical results yet

    last_ok = tr.dict2obj(requests.get(
        'https://testenv.yandex-team.ru/handlers/get_last_sandbox_task_ids?database=ws-begemot-trunk&job_names={jobs}&success=1'.format(
            jobs=','.join(TE_TASKS)),
        verify=tr.INTERNAL_CA
    ).json()['items'][0])

    last_ok = json.loads(requests.get(
        'https://testenv.yandex-team.ru/handlers/get_last_sandbox_task_ids?database=ws-begemot-trunk&job_names={jobs}&success=1'.format(
            jobs=','.join(TE_TASKS)),
        verify=tr.INTERNAL_CA
    ).text)['items']
    first_revision = min(test['revision'] for test in last_ok)

    if first_revision >= work_copy_svn_revision:
        for test in last_ok:
            resourses[test['job_name']] = [test['task_id'], test['revision']]
        return resourses

    if os.environ.get("USE_OUTDATED_CANONDATA", 'no') == 'yes':
        tr.log("USE_OUTDATED_CANONDATA == yes, so using outdated canonical data. The diff may be inaccurate.")
        for test in last_ok:
            resourses[test['job_name']] = [test['task_id'], test['revision']]
        return resourses

    waiting_for = []
    enqueued = []
    timedout = []
    failed = []
    for test in jobs:
        j = jobs[test]
        states = tr.sandbox_task_states(j.task_id)
        if not states:
            raise RuntimeError('This shard is apparently not per-commit tested. Failed to find any testenv launches. You should use a pre-commit TE job from an arcanum review.')
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
    tr.raise_errors(waiting_for, enqueued, timedout, failed, first_revision, work_copy_svn_revision, vcs)


@tr.measure('canonical data fetch')
def download_data_begemot(shard):
    sb_tasks = get_latest_sandbox_task_begemot()
    path = os.path.join(OUT_DIR, 'tests_data', shard)
    if not os.path.exists(path):
        os.makedirs(path)
    data_cgi = data_apphost = None
    test = 'GET_BEGEMOT_RESPONSES_%s' % shard
    if test in sb_tasks:
        data_cgi = setup_module_begemot(test, sb_tasks[test][0], shard, os.path.join(path, 'cgi'))
    test = 'GET_BEGEMOT_RESPONSES_APPHOST_%s' % shard
    if test in sb_tasks:
        data_apphost = setup_module_begemot(test, sb_tasks[test][0], shard, os.path.join(path, 'apphost'))
    return data_cgi, data_apphost


def create_dicts(requests, canonical, testing, is_cgi):  # TODO: make it more like printwizard
    testjson = {}
    canonjson = {}
    with open(testing) as testdata, open(canonical) as canondata, open(requests) as reqs:
        cnt = 1
        for test, canon, req in zip(testdata, canondata, reqs):
            if not is_cgi:
                request = json.loads(req)
                found = False
                for i in request:
                    if i['name'] == 'INIT':
                        if 'uri' in i['results']:
                            req = i['results'].get('uri')
                            found = True
                            break
                        else:
                            for j in i['results']:
                                if 'uri' in j:
                                    req = j.get('uri', '')
                                    found = True
                                    break
                        if found:
                            break
            req = req.partition('text=')[2]
            req = req.partition('&')[0]
            req = urllib.unquote_plus(req).decode('utf8')
            if req in testjson:
                req += str(cnt)
            testjson[req] = json.loads(test)
            canonjson[req] = json.loads(canon)
            try:
                testjson[req][0]["rules"].pop(".version")
                canonjson[req][0]["rules"].pop(".version")
            except Exception:
                pass
            cnt += 1
    return canonjson, testjson


def run_and_compare(shard, data):
    if not data:
        return
    tr.OUT_DIR = os.path.join(OUT_DIR, '{}_{}'.format(shard, 'cgi' if data['is_cgi'] else 'apphost'))
    path = tr.OUT_DIR
    if not os.path.exists(path):
        os.makedirs(path)
    bg_stdout = os.path.join(path, '%s.out' % shard)
    tr.log('Begemot stdout: %s' % bg_stdout)
    tr.log('Begemot stderr: %s' % bg_stdout.rstrip('out') + 'err')
    tr.log('Requests: %s' % data['requests_plan'])
    run_begemot(shard, bg_stdout, data, path)
    canonjson, testjson = create_dicts(data['requests_plan'], data['canon_response'], bg_stdout, data['is_cgi'])
    full, short = tr.generate_diff(shard, data['canon_response'], canonjson, bg_stdout, testjson)
    if full:
        return data['canon_response'], bg_stdout, full, short


def begemot_testing(shard):
    tr.log(shard, kind='head')
    data_cgi, data_apphost = download_data_begemot(shard)
    return run_and_compare(shard, data_cgi), run_and_compare(shard, data_apphost)


def fill_test_shards():
    global BEGEMOT_SHARDS
    shard_list = []
    for shard in BEGEMOT_SHARDS:
        if os.environ.get(shard, 'no') == 'yes':
            shard_list.append(shard)
    if shard_list:
        BEGEMOT_SHARDS = shard_list
    tr.log('Testing shards: {}'.format(' '.join(BEGEMOT_SHARDS)))
    for key, (task, rev) in get_latest_sandbox_task_begemot().iteritems():
        tr.log("{key}: r{rev}, https://sandbox.yandex-team.ru/task/{task}".format(**locals()))
    for test in get_latest_sandbox_task_begemot():
        if test not in DATA_INFO:
            DATA_INFO[test] = {}
    shard_results = []
    for shard in BEGEMOT_SHARDS:
        shard_results.extend(begemot_testing(shard))
        with open(DATA_PATH, 'w') as fd:
            json.dump(DATA_INFO, fd)
    failed = []
    tr.log(shard_results, kind='failed')
    for result in shard_results:
        if result:
            failed.append(
                '  The diff is not empty.\n'
                '  Canonical: {}\n'
                '  Local run: {}\n'
                '  Full diff: {}\n'
                '  Collapsed: {}\n\n'.format(*result),
            )
    if failed:
        raise tr.NonEmptyDiffException('\n'.join(failed))


def test_begemot_shards():
    fill_test_shards()
