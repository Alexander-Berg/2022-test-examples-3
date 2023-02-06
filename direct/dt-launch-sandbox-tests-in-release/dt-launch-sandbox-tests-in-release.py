import argparse
import json
import logging
import os
import re
import requests
import shlex
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import yaml
import random

from kazoo.client import KazooClient
from sandbox.common import rest
from startrek_client import Startrek

ROLE_CODE = 'direct-np-duty'
ABC_URL = 'https://abc-back.yandex-team.ru/api/v4/services/members/'
SERVICE = '5649'

JAVA_UNIT_TESTS_COMMON_DIR_WO_APP_DIR = ['direct/common', 'direct/core', 'direct/libs', 'direct/libs-internal']
STATE_WAITING_FOR_NEW_RELEASE = 'waiting_for_new_release'
STATE_NEED_TESTS_TICKET = 'need_tests_ticket'
STATE_NEED_LAUNCH_TASK = 'need_launch_task'
STATE_POLL_SANDBOX_FOR_TASKS_LAUNCHED_BY_CI = 'poll_sandbox_for_tasks_launched_by_ci'
STATE_NEED_COMMENT_ON_LAUNCH = 'need_comment_on_launch'
STATE_WAITING_FOR_TASK = 'waiting_for_task'
STATE_NEED_COMMENT_ON_FINISH = 'need_comment_on_finish'
STATE_NEED_CLEAR_DONT_REMIND_TAG = 'need_clear_dont_remind_tag'
STATE_NEED_PARAMETERS_FOR_RESTART = 'need_parameters_for_restart'
STATE_FINISHED_FOR_THIS_RELEASE = 'finished_for_this_release'
STATE_WAITING_FOR_NEWCI_RESTART = 'waiting_for_restart_newci'

TYPE_JAVA_UNIT_TESTS = 'java-unit-tests'
TYPE_DNA_MOLLY = 'dna-molly'
TYPE_UAC_TESTS = 'uac-tests'

TASKLET_RUN_COMMAND = 'TASKLET_RUN_COMMAND'
TASKLET_YA_MAKE_2 = 'YA_MAKE_2'
UAC_SANDBOX_TAG_PREFIX_MAP = {
    'HERMIONE_E2E_DESKTOP': 'UAC-RELEASE-HERMIONE-E2E-DESKTOP',
    'HERMIONE_E2E_MOBILE': 'UAC-RELEASE-HERMIONE-E2E-MOBILE',
    'HERMIONE_E2E_EXTERNAL_DESKTOP': 'UAC-RELEASE-HERMIONE-E2E-EXTERNAL-DESKTOP',
    'HERMIONE_E2E_EXTERNAL_MOBILE': 'UAC-RELEASE-HERMIONE-E2E-EXTERNAL-MOBILE',
    'UNIT_MOBILE': 'UAC-RELEASE-UNIT-MOBILE',
    'UNIT_DESKTOP': 'UAC-RELEASE-UNIT-DESKTOP',
    'HERMIONE_STORYBOOK_MOBILE': 'UAC-RELEASE-STORYBOOK-MOBILE',
    'HERMIONE_STORYBOOK_DESKTOP': 'UAC-RELEASE-STORYBOOK-DESKTOP',
}
UAC_SANDBOX_REPORT_PATH_BY_TYPE = {
    'hermione-report': '/report-hermione:ci/index.html',
    'jest-report': '/report-unit/index.html',
}

RECOVERABLE_STATES = set(['FAILURE', 'TIMEOUT'])
UNRECOVERABLE_STATES = set(['SUCCESS', 'EXCEPTION', 'STOPPED'])
FINISH_STATES = RECOVERABLE_STATES | UNRECOVERABLE_STATES

DNA_MOLLY_INSTRUCTIONS_COMMENT = '''
 <{**Инструкция по разбору запусков**
 1. Если все запуски завершились со статусом !!(green)**уязвимостей не обнаружено**!!, то можно смело закрывать тикет
 2. Если у запуска статус !!(red)**обнаружены уязвимости**!!:
 - открыть отчет по ссылке %%отчёт в Molly%%
 <{пример
 https://jing.yandex-team.ru/files/trapitsyn/browser_pUeQk6ZfUT.png}>
 - найти в отчете ошибку с %%Критичность: medium%%
 - кнопкой "Создать тикет в ST" завести тикет и передать дежурному (как и баги на ТС с неизвестным происхождением)
 - после этого тикет с запуском Молли можно закрыть
---------------------------------------------
**Если задачи в санбоксе вместо !!(green)Success!! имеют статусы !!(red)Fail/Timeout!!, то на любом ппцдеве можно выполнить команду:**

Перезапустить упавшие:
%%dt-launch-sandbox-tests-in-release --state-zk-node /direct/release-dna-molly --max-launches 3 --type dna-molly --app dna --restart-failed-tests%%

Перезапустить все:
%%dt-launch-sandbox-tests-in-release --state-zk-node /direct/release-dna-molly --max-launches 3 --type dna-molly --app dna --restart-all-tests%%
}>
'''

UNIT_TEST_DOC = '((https://docs.yandex-team.ru/direct-dev/guide/troubleshooting/unittests-in-release "Как читать отчет по юнит-тестам")) \n'

DEFAULT_REVISION_AND_VERSION_REGEX = r'выложить (?P<version>1\.(?P<base_rev>[0-9]+)(\.(?P<head_rev>[0-9]+))?-1)'
NEWCI_REVISION_AND_VERSION_REGEX = (
    r'сборка из NewCI v(?P<version>\d+(?:\.\d+)?)\s-\sвыложить\s1\.(?P<base_rev>[0-9]+)(\.(?P<head_rev>[0-9]+))?-1'
)


def get_dir_dependencies(app_dir, rev):
    tempdir = tempfile.mkdtemp(dir='/tmp/temp-ttl/ttl_1d')
    ya_cache_dir = tempfile.mkdtemp(dir='/tmp/temp-ttl/ttl_1d')
    arcadia_dir = os.path.join(tempdir, 'arcadia')
    store_dir = os.path.join(tempdir, 'store')
    os.makedirs(arcadia_dir)
    os.makedirs(store_dir)

    old_wd = os.getcwd()
    old_ya_cache_dir_value = os.environ.get('YA_CACHE_DIR', None)
    old_arc_token_path_value = os.environ.get('ARC_TOKEN_PATH', None)
    try:
        os.environ['ARC_TOKEN_PATH'] = '/etc/direct-tokens/oauth_arc_robot-direct-arc-p'
        subprocess.check_call(['arc', 'mount', '-m', arcadia_dir, '-S', store_dir])
        os.chdir(arcadia_dir)
        if rev.isdigit():
            subprocess.check_call(['arc', 'checkout', 'r' + str(rev)])
        else:
            subprocess.check_call(['arc', 'checkout', str(rev)])

        ya_path = os.path.join(arcadia_dir, 'ya')
        os.environ['YA_CACHE_DIR'] = ya_cache_dir
        dir_graph_output = subprocess.check_output(
            [ya_path, 'dump', 'dir-graph', '--plain', os.path.join(arcadia_dir, app_dir)]
        )
        return json.loads(dir_graph_output)
    finally:
        os.chdir(old_wd)
        subprocess.call(['arc', 'unmount', arcadia_dir])

        if old_ya_cache_dir_value is not None:
            os.environ['YA_CACHE_DIR'] = old_ya_cache_dir_value
        if old_arc_token_path_value is not None:
            os.environ['ARC_TOKEN_PATH'] = old_arc_token_path_value
        shutil.rmtree(tempdir, ignore_errors=True)
        shutil.rmtree(ya_cache_dir, ignore_errors=True)


def get_revisions_and_version_from_summary(summary, revisions_and_version_regex=DEFAULT_REVISION_AND_VERSION_REGEX):
    m = re.search(revisions_and_version_regex, summary)

    if m:
        base_rev = m.group('base_rev')
        head_rev = m.group('head_rev')
        version = m.group('version')
        return (base_rev, head_rev, version)
    else:
        return (None, None, None)


def get_sandbox_task_for_dir(dir, sandbox, app, version):
    task_id = 0
    dir_tag = dir.upper()
    sandbox_app_tag = 'UNIT-TEST-{0}-{1}'.format(app.upper(), version)
    sandbox_task_search_result = sandbox.task.read(type=TASKLET_YA_MAKE_2, tags=[sandbox_app_tag], limit=15)['items']
    if len(sandbox_task_search_result) > 0 and not sandbox_task_search_result[0] is None:
        for task in sandbox_task_search_result:
            if dir_tag in task['tags']:
                if task_id < task['id']:
                    task_id = task['id']
        return task_id
    return task_id


def get_current_np_duty_shift():
    with open('/etc/direct-tokens/abc_oauth_token_ppc', 'r') as f:
        abc_token = f.read().rstrip()
    response = requests.get(
        ABC_URL, params={'service': SERVICE, 'role__code': ROLE_CODE}, headers={"Authorization": 'OAuth %s' % abc_token}
    )
    duty = []
    for person in response.json()['results']:
        duty.append(person['person']['login'])
    return duty


def main():
    zk_node = None
    max_launches = None
    parser = argparse.ArgumentParser()
    parser.add_argument('--state-zk-node', required=True)
    parser.add_argument('--max-launches', type=int, required=False)
    parser.add_argument('--type', required=True, choices=[TYPE_JAVA_UNIT_TESTS, TYPE_DNA_MOLLY, TYPE_UAC_TESTS])
    parser.add_argument('--app', required=True)
    parser.add_argument('--restart-failed-tests', required=False, action='store_true')
    parser.add_argument('--restart-all-tests', required=False, action='store_true')
    args = parser.parse_args()
    if not args.max_launches and not (args.restart_failed_tests or args.restart_all_tests):
        sys.exit('error: --max-launches is required if --restart-failed-tests or --restart-all-tests is not specified')

    zk_node = args.state_zk_node
    max_launches = args.max_launches
    type = args.type
    app = args.app
    force_restart_failed = args.restart_failed_tests
    force_restart_all = args.restart_all_tests

    script_name = os.path.basename(sys.argv[0])
    hostname = subprocess.check_output(['hostname', '--fqdn']).decode('utf-8').rstrip()
    comment_signature = '----\nСкрипт {} с машины {}'.format(script_name, hostname)

    with open('/etc/yandex-direct/direct-apps.conf.yaml', 'r') as f:
        apps_config = yaml.load(f, Loader=yaml.SafeLoader)['apps']
    if app not in apps_config:
        sys.exit("{}: unknown app '{}'".format(script_name, app))
    app_config = apps_config[app]
    affectedApps = app_config['tracker-affected-app']
    logging.basicConfig(stream=sys.stderr, level=logging.INFO, format='[%(asctime)s]\t%(message)s')
    zk_servers = [
        'ppctest-zookeeper01i.sas.yp-c.yandex.net:2181',
        'ppctest-zookeeper01f.myt.yp-c.yandex.net:2181',
        'ppctest-zookeeper01v.vla.yp-c.yandex.net:2181',
    ]
    zk = KazooClient(','.join(zk_servers))
    zk_lock = zk.Lock(zk_node, 'lock')
    zk.start()
    if (force_restart_failed or force_restart_all) and not zk.exists(zk_node):
        sys.exit("error: node '{}' doesn't exist, restarting tests requires existing state node".format(zk_node))
    zk.ensure_path(zk_node)
    if not zk_lock.acquire(blocking=False):
        logging.info("couldn't acquire lock on {}, exiting".format(zk_node))
        sys.exit(0)

    state = {}

    def update_state(new_state, **kwargs):
        logging.info('changed state from {} to {}'.format(state.get('state', None), new_state))
        state['state'] = new_state
        for k, v in kwargs.items():
            state[k] = v
        zk.set(zk_node, json.dumps(state, sort_keys=True, indent=4).encode('utf-8'))

    def reset_state():
        ks = list(state.keys())
        for k in ks:
            del state[k]
        update_state(STATE_WAITING_FOR_NEW_RELEASE, app=app, type=type)

    def check_ticket_freshness(st, release_ticket_key):
        st_query = (
            'Queue: DIRECT Type: Release Components: "%s" (Status: "Ready for test" or Status: "Testing") "Sort by": key desc'
            % app_config['tracker-component']
        )
        testing_release_tickets = list(st.issues.find(st_query))
        if len(testing_release_tickets) > 0:
            last_release_ticket = testing_release_tickets[0]
            if release_ticket_key < last_release_ticket.key:
                logging.info(
                    'release ticket {} is too old, newer ticket {} exists, resetting state and exiting'.format(
                        release_ticket_key, last_release_ticket.key
                    )
                )
                reset_state()
                sys.exit(0)
            return last_release_ticket

    def check_version_freshness(release_ticket_key, version, last_release_ticket):

        revisions_and_version_regex = NEWCI_REVISION_AND_VERSION_REGEX

        (
            last_release_ticket_base_rev,
            last_release_ticket_head_rev,
            last_release_ticket_version,
        ) = get_revisions_and_version_from_summary(last_release_ticket.summary, revisions_and_version_regex)

        if (release_ticket_key == last_release_ticket.key
                and version is not None
                and last_release_ticket_version != version):
            logging.info(
                'version for release {} changed from {} to {}, restarting unit tests'.format(
                    release_ticket_key, version, last_release_ticket_version
                )
            )
            update_state(
                STATE_POLL_SANDBOX_FOR_TASKS_LAUNCHED_BY_CI,
                launches_left=3,
                task_ids={},
                finished_tasks={},
                restart_tests={},
                base_rev=last_release_ticket_base_rev,
                head_rev=last_release_ticket_head_rev,
                version=last_release_ticket_version,
            )
            sys.exit(0)

    state_json_encoded, stat = zk.get(zk_node)
    if not state_json_encoded:
        reset_state()
    else:
        for k, v in json.loads(state_json_encoded.decode('utf-8')).items():
            state[k] = v

    if app != state['app']:
        logging.error(
            'error: app in zk state "{}" and app in arguments "{}" mismatch, exiting'.format(state['app'], app)
        )
        sys.exit(1)
    if type != state['type']:
        logging.error(
            'error: type in zk state "{}" and type in arguments "{}" mismatch, exiting'.format(state['type'], type)
        )
        sys.exit(1)

    if (force_restart_failed or force_restart_all) and state['state'] != STATE_FINISHED_FOR_THIS_RELEASE:
        sys.exit(
            '--restart-failed-tests or --restart-all-tests are allowed only for state "{}", current state is "{}"'.format(
                STATE_FINISHED_FOR_THIS_RELEASE, state['state']
            )
        )

    with open('/etc/direct-tokens/startrek', 'r') as f:
        tracker_token = f.read().rstrip()

    sandbox = rest.Client(auth=open('/etc/direct-tokens/sandbox_oauth_token_ppc', 'r').read().rstrip())
    st = Startrek(token=tracker_token, useragent='Direct/{}'.format(script_name))

    if state['state'] == STATE_WAITING_FOR_NEW_RELEASE:
        st_query = (
            'Queue: DIRECT Type: Release Components: "%s" (Status: "Ready for test" or Status: "Testing") "Sort by": key desc'
            % app_config['tracker-component']
        )
        testing_release_tickets = list(st.issues.find(st_query))
        if len(testing_release_tickets) > 0:
            last_release_ticket = testing_release_tickets[0]
            type = state['type']
            revisions_and_version_regex = DEFAULT_REVISION_AND_VERSION_REGEX
            if type in [TYPE_UAC_TESTS, TYPE_JAVA_UNIT_TESTS]:
                revisions_and_version_regex = NEWCI_REVISION_AND_VERSION_REGEX

            base_rev, head_rev, version = get_revisions_and_version_from_summary(
                last_release_ticket.summary, revisions_and_version_regex
            )
            update_state(
                STATE_NEED_TESTS_TICKET,
                release_ticket=last_release_ticket.key,
                base_rev=base_rev,
                head_rev=head_rev,
                version=version,
            )
        else:
            logging.info('no testing release tickets found, exiting')
            sys.exit(0)

    if state['state'] == STATE_NEED_TESTS_TICKET:
        release_ticket = st.issues[state['release_ticket']]

        tests_ticket = None
        next_state = STATE_NEED_LAUNCH_TASK

        if type == TYPE_JAVA_UNIT_TESTS:
            on_duty = get_current_np_duty_shift()
            duty = random.choice(on_duty) if len(on_duty) > 0 and app == 'java-web' else release_ticket.assignee.login
            additional_ticket_params = {}
            if affectedApps:
                additional_ticket_params['affectedApps'] = affectedApps
            tests_ticket = st.issues.create(
                queue='DIRECT',
                summary='Запуск юнит-тестов в релизе {} {}'.format(app, release_ticket.key),
                type={'name': 'Task'},
                assignee=duty,
                tags=['dont_remind', 'direct_release_tests'],
                **additional_ticket_params
            )
            release_ticket.comments.create(text='Создан тикет на сбор запусков юнит-тестов: {}'.format(tests_ticket.key))
            next_state = STATE_POLL_SANDBOX_FOR_TASKS_LAUNCHED_BY_CI
            logging.info('created tests ticket: {}'.format(tests_ticket.key))
        elif type == TYPE_DNA_MOLLY:
            # TODO ходить в API ABC напрямую и с TVM-тикетом
            get_duty_cmd = [
                '/usr/bin/perl',
                '-MYandex::ABC',
                '-e',
                'print Yandex::ABC::get_data_from_abc("duty/on_duty", service__slug=>"direct", schedule__slug=>"release-qa-duty", with_watcher=>1)->[0]->{person}->{login}',
            ]
            assignee = None
            try:
                assignee = subprocess.check_output(get_duty_cmd).decode('utf-8').rstrip()
                logging.info("assigning tests ticket to direct_web_dna_release_duty from ABC: {}".format(assignee))
            except subprocess.CalledProcessError:
                logging.warn(
                    "couldn't get direct_web_dna_release_duty from ABC, command failed: {}".format(
                        shlex.join(get_duty_cmd)
                    )
                )
                assignee = release_ticket.createdBy.login
                logging.info("assigning tests ticket to release author: {}".format(assignee))

            tests_ticket = st.issues.create(
                queue='DIRECT',
                summary='Запуск Molly в релизе DNA {}'.format(release_ticket.key),
                type={'name': 'Task'},
                assignee=assignee,
                tags=['dont_remind', 'direct_release_tests'],
            )
            release_ticket.comments.create(text='Создан тикет на запуск Molly: {}'.format(tests_ticket.key))
            logging.info('created tests ticket: {}'.format(tests_ticket.key))
        elif type == TYPE_UAC_TESTS:
            additional_ticket_params = {}

            if affectedApps:
                additional_ticket_params['affectedApps'] = affectedApps

            tests_ticket = st.issues.create(
                queue='DIRECT',
                summary='Запуск тестов в релизе {} {}'.format(app, release_ticket.key),
                type={'name': 'Task'},
                assignee=release_ticket.assignee.login,
                tags=['dont_remind', 'direct_release_tests'],
                **additional_ticket_params
            )
            release_ticket.comments.create(text='Создан тикет на запуск тестов: {}'.format(tests_ticket.key))

            next_state = STATE_POLL_SANDBOX_FOR_TASKS_LAUNCHED_BY_CI

            logging.info('created tests ticket: {}'.format(tests_ticket.key))

        if tests_ticket.status.key == 'closed':
            logging.info('ticket {} is already closed, resetting state and exiting'.format(tests_ticket.key))
            reset_state()
            sys.exit(0)
        update_state(next_state, tests_ticket=tests_ticket.key)

    if state['state'] == STATE_NEED_LAUNCH_TASK:
        release_ticket = st.issues[state['release_ticket']]
        launches_left = state.get('launches_left', max_launches)
        finished_tasks = state.get('finished_tasks', {})
        restart_tests = state.get('restart_tests', None)

        task_ids = {}
        if type == TYPE_JAVA_UNIT_TESTS:
            if 'base_rev' in state and 'head_rev' in state and 'version' in state:
                base_rev = state['base_rev']
                head_rev = state['head_rev']
                version = state['version']
            else:
                # для совместимости с состояниями до появления проставления из STATE_WAITING_FOR_NEW_RELEASE
                base_rev, head_rev, version = get_revisions_and_version_from_summary(release_ticket.summary)
            arcadia_url = None
            if head_rev:
                arcadia_url = 'arcadia:/arc/branches/direct/release/{}/{}/arcadia@{}'.format(app, base_rev, head_rev)
            else:
                arcadia_url = 'arcadia:/arc/trunk/arcadia@{}'.format(base_rev)
            dep_dirs = get_dir_dependencies(app_config['primary-svn-dir'], base_rev)
            # в зависимости почему-то попадает целый direct/libs, пока просто убираем: https://st.yandex-team.ru/DIRECT-126151#5f3e2d33980f6c5d74a19092
            dep_dirs = [d for d in dep_dirs if d != 'direct/libs']
            for dir in [
                app_config['primary-svn-dir'],
                'direct/common',
                'direct/core',
                'direct/libs',
                'direct/libs-internal',
            ]:
                if dir in finished_tasks:
                    continue
                task_id = sandbox.task({'type': 'YA_MAKE_2'})['id']
                if restart_tests and dir in restart_tests:
                    targets = [d for d in dep_dirs if (d == dir or d.startswith(dir + '/')) and d in restart_tests[dir]]
                else:
                    targets = [d for d in dep_dirs if d == dir or d.startswith(dir + '/')]
                if len(targets) == 0:
                    logging.info('target list for {} is empty, skipping'.format(dir))
                    continue
                sandbox.task[task_id] = {
                    'owner': 'DIRECT',
                    'priority': {'class': 'SERVICE', 'subclass': 'NORMAL'},
                    'description': 'Запуск юнит-тестов для приложения {}, версия {}, директория {}'.format(
                        app, version, dir
                    ),
                    'notifications': [],
                    'requirements': {
                        'platform': 'linux_ubuntu_14.04_trusty',
                        'disk_space': 30 * 1024 * 1024 * 1024,
                    },  # аналогично sandbox-ya-package
                    'kill_timeout': 21600,  # по договорённости с trapitsyn@, потому что на момент внедрения задачи могли не укладываться в 3 часа
                    'custom_fields': [
                        {'name': 'checkout_arcadia_from_url', 'value': arcadia_url},
                        {'name': 'use_aapi_fuse', 'value': True},
                        {'name': 'use_arc_instead_of_aapi', 'value': True},
                        {'name': 'keep_on', 'value': True},
                        {'name': 'test', 'value': True},
                        {'name': 'targets', 'value': ';'.join(targets)},
                        {'name': 'checkout', 'value': False},
                        {
                            'name': 'ya_yt_token_yav_secret',
                            'value': 'sec-01crzhdg2vbrx7exarvapq1nat#yt_robot-direct-yt-ro',
                        },
                    ],
                }
                task_ids[dir] = task_id
        if type == TYPE_DNA_MOLLY:
            urls = [
                'https://test-direct.yandex.ru/dna/grid',
                'https://test-direct.yandex.ru/dna/campaigns-edit?campaigns-ids=217318593&ulogin=yndx-dna-molly-test',
                'https://test-direct.yandex.ru/dna/groups-edit?campaigns-ids=217318593&groups-ids=4640726158&ulogin=yndx-dna-molly-test',
                'https://test-direct.yandex.ru/dna/banners-edit?banners-ids=9801692773&campaigns-ids=217318593&ulogin=yndx-dna-molly-test',
                'https://test-direct.yandex.ru/registered/main.pl?ulogin=yndx-dna-molly-test&ClientID=1338745722&cmd=showTurboLandings',
                'https://test-direct.yandex.ru/dna/vc/list?ulogin=yndx-dna-molly-test',
                'https://test-direct.yandex.ru/dna/mobile-apps/list?ulogin=yndx-dna-molly-test',
                'https://test-direct.yandex.ru/dna/tools?ulogin=yndx-dna-molly-test',
            ]
            for url in urls:
                url_path = urllib.parse.urlparse(url).path
                if url_path in finished_tasks:
                    continue
                task_id = sandbox.task({'type': 'MOLLY_RUN'})['id']
                sandbox.task[task_id] = {
                    'owner': 'DIRECT',
                    'priority': {'class': 'SERVICE', 'subclass': 'NORMAL'},
                    'description': 'Запуск Molly в релизе dna {} на URL {}'.format(release_ticket.key, url),
                    'custom_fields': [
                        {'name': 'profile', 'value': 'Yandex'},
                        {
                            'name': 'auth_profile',
                            'value': 'b76b80e8-2e14-40c3-a713-59d036ac6d24',
                        },  # https://st.yandex-team.ru/DIRECT-124845#5f3cf66edc748406769ab806
                        {'name': 'target_uri', 'value': url},
                        {'name': 'severity', 'value': 30},  # DIRECT-129163
                    ],
                }
                task_ids[url_path] = task_id

        sandbox.batch.tasks.start.update(list(task_ids.values()))
        launches_left -= 1
        update_state(STATE_NEED_COMMENT_ON_LAUNCH, task_ids=task_ids, launches_left=launches_left)

    if state['state'] == STATE_POLL_SANDBOX_FOR_TASKS_LAUNCHED_BY_CI:
        task_ids = state.get('task_ids', {})
        release_ticket_key = state['release_ticket']

        version = state.get('version', None)
        if version is None:
            logging.error('No version specified')
            sys.exit(1)

        has_unfound_check_type = False
        last_release_ticket = check_ticket_freshness(st, release_ticket_key)
        check_version_freshness(release_ticket_key, version, last_release_ticket)
        if type == TYPE_UAC_TESTS:

            for uac_check_type in UAC_SANDBOX_TAG_PREFIX_MAP:
                sandbox_tag = UAC_SANDBOX_TAG_PREFIX_MAP.get(uac_check_type) + '-' + version
                sandbox_task_search_result = sandbox.task.read(type=TASKLET_RUN_COMMAND, tags=[sandbox_tag], limit=1)[
                    'items'
                ]

                if len(sandbox_task_search_result) > 0 and not sandbox_task_search_result[0] is None:
                    if uac_check_type not in task_ids:
                        task_ids[uac_check_type] = sandbox_task_search_result[0]["id"]
                else:
                    has_unfound_check_type = True

        if type == TYPE_JAVA_UNIT_TESTS:
            for dir in JAVA_UNIT_TESTS_COMMON_DIR_WO_APP_DIR + [app_config['primary-svn-dir']]:
                task_id = get_sandbox_task_for_dir(dir, sandbox, app, version)
                if task_id != 0:
                    task_ids[dir] = task_id
                else:
                    has_unfound_check_type = True

        if has_unfound_check_type:
            if len(task_ids) > 0:
                tsks = list(task_ids.values())
                logging.info(tsks)
                task_data = sandbox.task[tsks[0]].read()
                check_rev = task_data['input_parameters']['checkout_arcadia_from_url']
                rv = check_rev.split('#')[1]
                dir_dep = get_dir_dependencies(app_config['primary-svn-dir'], rv)
                logging.info(dir_dep)
                dep_dirs = [d for d in dir_dep if d != 'direct/libs']
                for dir in JAVA_UNIT_TESTS_COMMON_DIR_WO_APP_DIR + [app_config['primary-svn-dir']]:
                    targets = [d for d in dep_dirs if d == dir or d.startswith(dir + '/')]
                    if (len(targets) == 0) and (dir not in list(task_ids.keys())):
                        update_state(STATE_NEED_COMMENT_ON_LAUNCH, task_ids=task_ids, launches_left=0)
                        sys.exit(0)

            logging.info('tasks are not launched by new ci, exiting')
            update_state(STATE_POLL_SANDBOX_FOR_TASKS_LAUNCHED_BY_CI, task_ids=task_ids)
            sys.exit(0)

        update_state(STATE_NEED_COMMENT_ON_LAUNCH, task_ids=task_ids, launches_left=0)

    if state['state'] == STATE_NEED_COMMENT_ON_LAUNCH:
        tests_ticket = st.issues[state['tests_ticket']]
        task_ids = state['task_ids']
        version = state.get('version', None)
        if type == TYPE_JAVA_UNIT_TESTS and not version:
            version = get_revisions_and_version_from_summary(last_release_ticket.summary)[2]

        if len(task_ids.keys()) == 0:
            logging.info('no tasks launched, no need to comment')
        else:
            comment = None
            if type == TYPE_JAVA_UNIT_TESTS:
                comment = 'Найдены запущенные юнит-тесты Java в Sandbox для версии {}:\n'.format(version)
                for dir in sorted(task_ids.keys()):
                    task_id = task_ids[dir]
                    comment += '{}: https://sandbox.yandex-team.ru/task/{}\n'.format(dir, task_id)
            elif type == TYPE_DNA_MOLLY:
                comment = 'Запущено сканирование Molly в Sandbox:\n'
                for url in sorted(task_ids.keys()):
                    task_id = task_ids[url]
                    comment += '{}: https://sandbox.yandex-team.ru/task/{}\n'.format(url, task_id)
            elif type == TYPE_UAC_TESTS:
                comment = 'Найдены запущенные проверки в new CI:\n'
                for uac_check_type, task_id in sorted(task_ids.items()):
                    comment += '{}: https://sandbox.yandex-team.ru/task/{}\n'.format(uac_check_type, task_id)

            full_comment = comment + comment_signature
            if tests_ticket.status.key == 'closed':
                tests_ticket.transitions['reopen'].execute(comment=full_comment)
            else:
                tests_ticket.comments.create(text=full_comment)
        update_state(STATE_WAITING_FOR_TASK)

    if state['state'] == STATE_WAITING_FOR_TASK:
        task_ids = state['task_ids']
        finished_task_statuses = state.get('finished_task_statuses', {})
        finished_tasks = state.get('finished_tasks', {})
        task_outputs = state.get('task_outputs', {})

        all_finished = True
        for dir, task_id in task_ids.items():
            if task_id in finished_task_statuses:
                if dir in finished_tasks.keys():
                    continue
                else:
                    all_finished = False
                    task_id = get_sandbox_task_for_dir(dir, sandbox, app, version)

            task_data = sandbox.task[task_id].read()
            task_status = task_data['status']
            if task_status in FINISH_STATES:
                task_output = {}
                if type == TYPE_DNA_MOLLY:
                    task_output['molly_report'] = task_data.get('output_parameters', {}).get('report', None)
                    task_output['status'] = task_data.get('output_parameters', {}).get('status', None)
                elif type == TYPE_JAVA_UNIT_TESTS:
                    failed_tests = None
                    test_results = None
                    if task_status in ['FAILURE', 'SUCCESS']:
                        build_output_resource = [
                            item
                            for item in sandbox.task[task_id].resources.read()['items']
                            if item['type'] == 'BUILD_OUTPUT'
                        ][0]
                        try:
                            results_json_url = build_output_resource['http']['proxy'] + '/results.json'
                            results = requests.get(results_json_url).json()
                            test_results = [
                                item
                                for item in results.get('results', [])
                                if item['type'] in ('test', 'style') and not item.get('suite', False)
                            ]
                        except ValueError:
                            logging.warn(
                                'could not get test results for task {}, failed to parsed JSON from link: {}'.format(
                                    task_id, results_json_url
                                )
                            )
                    if task_status == 'FAILURE' and test_results:
                        failed_tests = [
                            {
                                'name': item['name'],
                                'error_type': item['error_type'],
                                'path': item['path'],
                                'log': item.get('links', {}).get('log', None),
                            }
                            for item in test_results
                            if item['status'] == 'FAILED'
                        ]
                        task_output['failed_tests_count'] = len(failed_tests)
                    task_output['failed_tests'] = failed_tests
                    if test_results:
                        task_output['total_tests_count'] = len(test_results)
                        task_output['passed_tests_count'] = len(
                            [item for item in test_results if item['status'] == 'OK']
                        )
                        task_output['skipped_tests_count'] = len(
                            [item for item in test_results if item['status'] == 'SKIPPED']
                        )
                elif type == TYPE_UAC_TESTS:
                    report_resource_result = [
                        item
                        for item in sandbox.task[task_id].resources.read()['items']
                        if item['type'] == 'OTHER_RESOURCE'
                        and (
                            item['attributes']['type'] == 'hermione-report'
                            or item['attributes']['type'] == 'jest-report'
                        )
                    ]

                    if len(report_resource_result) > 0:
                        report_resource = report_resource_result[0]
                        report_type = report_resource['attributes']['type']

                        task_output['report'] = (
                            'https://proxy.sandbox.yandex-team.ru/'
                            + str(report_resource['id'])
                            + UAC_SANDBOX_REPORT_PATH_BY_TYPE.get(report_type, '')
                        )
                        task_output['status'] = (
                            task_data.get('output_parameters', {}).get('state', {}).get('message', None)
                        )

                # явно преобразуем ключи к строкам, потому что если какие-то ключи будут числами, не сможем сериализовать в json с sort_keys=True
                task_outputs[str(task_id)] = task_output
                finished_task_statuses[str(task_id)] = task_status

            else:
                all_finished = False

        if all_finished:
            update_state(
                STATE_NEED_COMMENT_ON_FINISH, finished_task_statuses=finished_task_statuses, task_outputs=task_outputs
            )
        else:
            logging.info('some tasks are still going, exiting')
            # остаться в том же состоянии, но обновить finished_task_statuses
            update_state(
                STATE_WAITING_FOR_TASK, finished_task_statuses=finished_task_statuses, task_outputs=task_outputs
            )
            sys.exit(0)

    if state['state'] == STATE_NEED_COMMENT_ON_FINISH:
        tests_ticket = st.issues[state['tests_ticket']]
        task_ids = state['task_ids']
        finished_tasks = state.get('finished_tasks', {})
        finished_task_statuses = state['finished_task_statuses']
        task_outputs = state['task_outputs']
        launches_left = state.get('launches_left', 0)

        need_restart = False
        all_tasks_succeeded = True
        if len(task_ids.keys()) == 0:
            logging.info('no tasks launched, no need to comment')
        else:
            comment = ''
            for key in sorted(task_ids.keys()):
                task_id = task_ids[key]

                task_status = finished_task_statuses[str(task_id)]
                task_output = task_outputs[str(task_id)]

                need_restart_this_task = task_status in RECOVERABLE_STATES and launches_left > 0
                success = False
                if type == TYPE_DNA_MOLLY and task_status == 'SUCCESS':
                    comment += '{}: ((https://sandbox.yandex-team.ru/task/{} запуск в Sandbox)) завершился'.format(
                        key, task_id
                    )
                    report = task_output.get('molly_report', None)
                    molly_status = task_output.get('status', None)
                    if not isinstance(molly_status, bool):
                        comment += ', не удалось определить результат'
                    elif molly_status:
                        comment += ', !!(red)**обнаружены уязвимости**!!'
                    elif not molly_status:
                        comment += ', !!(green)**уязвимостей не обнаружено**!!'
                        success = True

                    if report:
                        comment += ', (({} отчёт в Molly))'.format(report)
                elif type == TYPE_UAC_TESTS:
                    formatted_task_key = '**{}**'.format(key)
                    formatted_status = task_status

                    if task_status == 'SUCCESS':
                        formatted_status = '!!(green)**{}**!!'.format(task_status)
                    elif task_status == 'FAILURE':
                        formatted_status = '!!(red)**{}**!!'.format(task_status)

                    comment += '{}: ((https://sandbox.yandex-team.ru/task/{} запуск в Sandbox)) завершился со статусом {}.'.format(
                        formatted_task_key, task_id, formatted_status
                    )

                    report = task_output.get('report', None)
                    if report:
                        comment += ' (({} Ссылка на отчёт))'.format(report)
                else:
                    comment += '{}: ((https://sandbox.yandex-team.ru/task/{} запуск в Sandbox)) завершился со статусом {}'.format(
                        key, task_id, task_status
                    )

                if type == TYPE_JAVA_UNIT_TESTS:
                    if 'total_tests_count' in task_output:
                        comment += ' (всего тестов: {}, пропущено: {}, прошло: {}, упало: {})'.format(
                            task_output['total_tests_count'],
                            task_output.get('skipped_tests_count', 0),
                            task_output.get('passed_tests_count', 0),
                            task_output.get('failed_tests_count', 0),
                        )
                    failed_tests = task_output.get('failed_tests', [])
                    if task_status == 'FAILURE' and failed_tests:
                        comment += (
                            '\n<{упавшие тесты (формат предварительный, может меняться)\n%%\n'
                            + json.dumps(failed_tests, indent=4, sort_keys=True)
                            + '\n%%\n}>'
                        )
                    if (
                        task_status == 'FAILURE' and isinstance(failed_tests, list) and len(failed_tests) == 0
                    ):  # тесты прошли, задача в FAILURE по какой-то другой причине
                        comment += ', упавших тестов нет'
                        need_restart_this_task = False
                    if task_status == 'SUCCESS':
                        success = True
                comment += '\n\n'

                if task_status in UNRECOVERABLE_STATES:
                    finished_tasks[key] = task_id
                need_restart = need_restart or need_restart_this_task
                all_tasks_succeeded = all_tasks_succeeded and success
            if need_restart:
                comment += '**Тесты будут перезапущены (осталось попыток: {})**\n'.format(launches_left)
            else:
                comment += '**Тесты больше перезапускаться не будут.**\n'
                new_ci_flow_url = None
                if type == TYPE_DNA_MOLLY:
                    comment += DNA_MOLLY_INSTRUCTIONS_COMMENT
                elif type == TYPE_JAVA_UNIT_TESTS:
                    version = state.get('version', None)
                    app = state.get('app', None)
                    new_ci_flow_url = 'https://a.yandex-team.ru/projects/direct/ci/releases/flow?dir=direct%2F{0}&id=deploy-release&version={1}'.format(
                        app.replace('java-', ''),
                        str(version)
                    )
                elif type == TYPE_UAC_TESTS:
                    version = state.get('version', None)
                    new_ci_flow_url = 'https://a.yandex-team.ru/projects/direct/ci/releases/flow?dir=adv%2Ffrontend&id=uac-release&version={}'.format(
                        str(version)
                    )
                if new_ci_flow_url:
                    comment += 'Для перезапуска проверок воспользуйтесь интерфейсом (({} new CI))\n'.format(
                        new_ci_flow_url
                    )
                comment += UNIT_TEST_DOC

            result_comment = comment + comment_signature
            task_can_be_closed = 'close' in map(lambda x: x.id, tests_ticket.transitions.get_all())
            if all_tasks_succeeded and task_can_be_closed:
                try:
                    additional_ticket_params = {}
                    if affectedApps:
                        additional_ticket_params['testedApps'] = affectedApps
                    tests_ticket.transitions['close'].execute(
                        comment=result_comment, resolution='fixed', **additional_ticket_params
                    )
                except Exception as e:
                    logging.error(
                        '{exception}: can\'t close ticket {ticket_id} with status {status}'.format(
                            exception=e, ticket_id=tests_ticket.key, status=tests_ticket.status
                        )
                    )
                    tests_ticket.comments.create(text=result_comment)
            else:
                tests_ticket.comments.create(text=result_comment)

        if need_restart:
            if type == TYPE_JAVA_UNIT_TESTS:
                update_state(STATE_WAITING_FOR_NEWCI_RESTART)
            else:
                update_state(STATE_NEED_PARAMETERS_FOR_RESTART, finished_tasks=finished_tasks)
        else:
            update_state(STATE_NEED_CLEAR_DONT_REMIND_TAG, finished_tasks=finished_tasks)

    if state['state'] == STATE_NEED_CLEAR_DONT_REMIND_TAG:
        tests_ticket = st.issues[state['tests_ticket']]
        if 'dont_remind' in tests_ticket.tags:
            tests_ticket.update(tags=[t for t in tests_ticket.tags if t != 'dont_remind'])
        update_state(STATE_FINISHED_FOR_THIS_RELEASE)

    if state['state'] == STATE_FINISHED_FOR_THIS_RELEASE:
        release_ticket_key = state['release_ticket']
        tests_ticket_key = state['tests_ticket']
        version = state.get('version', None)
        type = state['type']

        if force_restart_failed:
            update_state(STATE_NEED_PARAMETERS_FOR_RESTART, launches_left=1)
        elif force_restart_all:
            update_state(
                STATE_NEED_PARAMETERS_FOR_RESTART, launches_left=1, task_ids={}, finished_tasks={}, restart_tests={}
            )
        else:
            last_release_ticket = check_ticket_freshness(st, release_ticket_key)
            if type == TYPE_JAVA_UNIT_TESTS:

                check_version_freshness(release_ticket_key, version, last_release_ticket)
                # отсюда и до обработки STATE_NEED_PARAMETERS_FOR_RESTART не должно быть выполняющегося кода
            else:
                logging.info('launches already finished for release {}, exiting'.format(release_ticket_key))
                sys.exit(0)
        if force_restart_failed or force_restart_all:
            sys.stderr.write('restarter state updated, wait for comment in ticket {}\n'.format(tests_ticket_key))
            sys.exit(0)

    if state['state'] == STATE_NEED_PARAMETERS_FOR_RESTART:
        task_ids = state['task_ids']
        finished_tasks = state['finished_tasks']
        task_outputs = state['task_outputs']

        to_restart = {}
        if type == TYPE_JAVA_UNIT_TESTS:
            for key in task_ids:
                task_id = task_ids[key]
                if key in finished_tasks:
                    continue

                failed_tests = task_outputs.get(task_id, {}).get('failed_tests', None)
                if failed_tests is not None:
                    to_restart[key] = []
                    for item in failed_tests:
                        to_restart[key].append(item['path'])
        if to_restart.keys():
            update_state(STATE_NEED_LAUNCH_TASK, restart_tests=to_restart)
        else:
            update_state(STATE_NEED_LAUNCH_TASK)

    if state['state'] == STATE_WAITING_FOR_NEWCI_RESTART:
        for dir in JAVA_UNIT_TESTS_COMMON_DIR_WO_APP_DIR + [app_config['primary-svn-dir']]:
            if dir not in state['finished_tasks'].keys():
                task_id = get_sandbox_task_for_dir(app, sandbox, app, version)
                if (task_id != 0) and (task_id not in state.get("finished_task_statuses")):
                    task_ids[dir] = task_id
                else:
                    logging.info('no new tasks found')
                    sys.exit(0)
        update_state(STATE_POLL_SANDBOX_FOR_TASKS_LAUNCHED_BY_CI)


if __name__ == '__main__':
    main()
