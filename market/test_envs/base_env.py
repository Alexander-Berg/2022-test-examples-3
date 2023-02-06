# coding: utf-8

import copy
import datetime
import logging
import os
import six

from hamcrest import assert_that, all_of
from uuid import uuid4
from sys import (
    stdin as istream,
    stderr as ostream,
)

from market.pylibrary.thread_pool.bounded_thread_pool import BoundedThreadPoolExecutor
from market.idx.yatf.common import ignore_errors, get_test_output_path, is_running_from_test, get_source_path
from market.idx.yatf.resources.yt_stuff_archive import YATF_FLUSH_YT_ARCHIVE
from market.idx.yatf.test_envs.local_shell import LocalShell

from yt.wrapper.job_tool import (
    prepare_job_environment,
    FULL_INPUT_MODE,
    INPUT_CONTEXT_MODE,
)
import yatest.common


NOW_STR = datetime.datetime.now().strftime("%H_%M_%S")

_THREAD_POOL = None


def COMMON_STUBS_DIR():
    return os.path.join(
        get_source_path(),
        'market', 'idx', 'yatf', 'resources', 'stubs',
    )


def get_thread_pool():
    global _THREAD_POOL
    if not _THREAD_POOL:
        _THREAD_POOL = BoundedThreadPoolExecutor(10, 10)
    return _THREAD_POOL


def make_list(value):
    if isinstance(value, (list, tuple)):
        return value
    return [value]


def _topsort(resource_dependencies, resources):
    visited = set()
    init_order = []

    def dfs(resource_name):
        if resource_name in visited:
            return
        visited.add(resource_name)

        to = resource_dependencies.get(resource_name, [])
        for el in make_list(to):
            dfs(el)

        init_order.append(resource_name)

    for resource_name in resources:
        if resource_name not in visited:
            dfs(resource_name)

    return init_order


class BaseEnv(object):

    _STUBS = {}

    _MATCHERS = []

    def __init__(self, parallel_env_initializing=True, **resources):
        super(BaseEnv, self).__init__()
        self.parallel_env_initializing = parallel_env_initializing
        self.resources = copy.deepcopy(resources)
        self.resources.update({
            name: copy.deepcopy(self._STUBS[name])
            for name in set(self._STUBS.keys()) - set(resources.keys())
        })
        self.outputs = {}
        self.exec_result = None
        self._id = str(uuid4()) if is_running_from_test() else NOW_STR

    @staticmethod
    def merge_stubs(*dict_args):
        result = {}
        for dictionary in dict_args:
            result.update(dictionary)
        return result

    @property
    def description(self):
        return 'base_env'

    @property
    def input_dir(self):
        dir_name = 'input-{}-{}'.format(
            self.description,
            self._id
        )
        return ignore_errors(get_test_output_path, OSError)(dir_name)

    @property
    def output_dir(self):
        dir_name = 'output-{}-{}'.format(
            self.description,
            self._id
        )
        return ignore_errors(get_test_output_path, OSError)(dir_name)

    def verify(self, matchers=None):
        msg = '{} env is valid'.format(self.description)
        if matchers is not None:
            assert_that(self, all_of(*matchers), msg)
        else:
            assert_that(self, all_of(*self._MATCHERS), msg)

    @property
    def resource_dependencies(self):
        '''
        Словарь зависимостей ресурсов для их инициализации.
        {
            'first': 'second',
            'second': ['multiple', 'dependencies']
        }
        '''
        return dict()

    def __enter__(self):
        logging.info('init environment...')

        ignore_errors(os.makedirs, OSError, with_logging=False)(self.input_dir)
        ignore_errors(os.makedirs, OSError, with_logging=False)(self.output_dir)

        pool = get_thread_pool()
        tasks = dict()

        def init_resource(res, res_name):
            if self.parallel_env_initializing:
                # wait all depends
                if res_name in self.resource_dependencies:
                    for depend in make_list(self.resource_dependencies[res_name]):
                        task = tasks.get(depend)
                        if task:
                            task.result()

                tasks[res_name] = pool.submit(res.init, self)
            else:
                res.init(self)

        for resource_name in _topsort(self.resource_dependencies, list(self.resources.keys())):
            if resource_name not in self.resources:
                continue

            logging.info('init resource {}'.format(resource_name))
            resource = self.resources[resource_name]
            for resource_item in make_list(resource):
                if not isinstance(resource_item, bool):
                    init_resource(resource_item, resource_name)
            logging.info('init resource {} complited'.format(resource_name))

        if self.parallel_env_initializing:
            for task in list(tasks.values()):
                task.result()

        return self

    def __exit__(self, *args):
        logging.info('clean environment...')

    def _get_user_gdb_desision(self, yatf_gdb_param):
        if yatf_gdb_param == 'true':
            ostream.write('Run "{}" under gdb? [y/n]\n'.format(self.description))
            answer = six.moves.input().lower()
            return answer == 'y'
        return False

    def _get_gdb(self):
        return (
            yatest.common.get_param('gdb')
            or os.environ.get('YATF_GDB', '').lower()
        )

    def select_operation(self):
        ops = [
            {
                'id': op['id'],
                'type': op['type'],
                'str': '{}\t{}\t{}\t{}\t{}'.format(
                    op['id'],
                    op['type'],
                    op['state'],
                    op.get('brief_spec', {}).get('title', None),
                    op.get('brief_progress', {}).get('jobs', None)
                ),
            }
            for op
            in self.yt_client.list_operations()['operations']
        ]
        if len(ops) == 0:
            return None
        result = '\n'.join(
            ['0. Cancel'] +
            ['{}. {}'.format(i + 1, op['str']) for i, op in enumerate(ops)]
        )
        ostream.write('Run operation under gdb?\n{}'.format(
            result
        ))
        answer = int(six.moves.input())
        if not answer:
            return None
        answer -= 1
        return ops[answer]

    def select_job(self, operation_id):
        jobs = [
            {
                'id': job['id'],
                'state': job['state'],
                'str': '{}\t{}\t{}'.format(
                    job['id'],
                    job['type'],
                    job['state']
                ),
                'type': job['type'],
            }
            for job
            in self.yt_client.list_jobs(operation_id)['jobs']
        ]
        result = '\n'.join(
            ['0. Cancel'] +
            ['{}. {}'.format(i + 1, job['str']) for i, job in enumerate(jobs)]
        )
        ostream.write('Run job for operation {} under gdb?\n{}'.format(
            operation_id,
            result
        ))
        answer = int(six.moves.input())
        if not answer:
            return None
        answer -= 1
        return jobs[answer]

    def prepare_yt_gdb_environment(self, cwd, operation_id, job_id, job_state):
        job_path = os.path.join(cwd, 'job_{}'.format(job_id))
        ostream.write('preparing job environment: dir {}'.format(job_path))

        mode = INPUT_CONTEXT_MODE if job_state == 'failed' else FULL_INPUT_MODE
        prepare_job_environment(
            operation_id,
            job_id,
            job_path,
            run=False,
            get_context_mode=mode
        )
        ostream.write('job environment prepared: {}\n'.format(job_path))

        lines = []
        standart_gdb_sh = os.path.join(job_path, 'run_gdb.sh')
        with open(standart_gdb_sh, 'r') as f:
            lines = f.readlines()
            standart_gdb_command = lines[-1]
            lines[-1] = ' '.join(
                [yatest.common.gdb_path()] +
                standart_gdb_command.split(' ')[1:]
            )
        # ostream.writelines(lines)

        patched_gdb_sh = os.path.join(job_path, 'run_gdb_patched.sh')
        with open(patched_gdb_sh, 'w') as f:
            f.writelines(lines)
        os.chmod(patched_gdb_sh, 0o744)

        return patched_gdb_sh

    def try_debug_yt(self, yatf_gdb, cwd=None, env=None):
        if (
            yatf_gdb != 'yt'
            or not hasattr(self, 'yt_client')
            or not self.yt_client
        ):
            return

        YATF_FLUSH_YT_ARCHIVE(self.yt_client)

        operation = self.select_operation()
        if not operation:
            return
        job = self.select_job(operation['id'])
        if not job:
            return

        ostream.write('local YT {}\n'.format(
            self.yt_client.config['proxy']['url']
        ))

        cwd = cwd or self.output_dir
        env = env or os.environ.copy()

        patched_gdb_sh = self.prepare_yt_gdb_environment(cwd, operation['id'], job['id'], job['state'])
        ls = LocalShell(istream, ostream, os.path.dirname(patched_gdb_sh), env)
        cmd = './{}'.format(os.path.basename(patched_gdb_sh))
        ls.run(cmd)

        ostream.write('all done with local YT {}\n'.format(
            self.yt_client.config['proxy']['url']
        ))

    def _local_execute(self,
                       cmd,
                       env=None,
                       cwd=None,
                       wait=True,
                       stdout=None,
                       stderr=None,
                       check_exit_code=None,  # noqa
                       check_sanitizer=None,  # noqa
                       **kwargs):
        return yatest.common.execute(
            cmd,
            env=env,
            cwd=cwd,
            wait=wait,
            stdout=stdout or open(self.description + '.stdout', 'wb+'),
            stderr=stderr or open(self.description + '.stderr', 'wb+'),
            check_exit_code=False,  # not supported without ya make tests
            check_sanitizer=False,  # not supported without ya make tests
            **kwargs
        )

    def try_execute_under_gdb(
            self,
            cmd,
            env=None,
            cwd=None,
            check_exit_code=False,
            wait=True,
            **kwargs
    ):
        if not is_running_from_test():
            # TODO(ymoskalenk0): add support local running under gdb
            return self._local_execute(
                cmd,
                env=env,
                cwd=None,
                wait=wait,
                check_exit_code=check_exit_code,
                **kwargs
            )

        cwd = cwd or self.output_dir
        env = env or os.environ.copy()
        yatf_gdb = self._get_gdb()
        try:
            # collectin test params and run under gdb
            if yatf_gdb == self.description or self._get_user_gdb_desision(yatf_gdb):
                ls = LocalShell(istream, ostream, cwd, env=env, wait=wait)
                cmd = [yatest.common.gdb_path(), '--args'] + cmd
                return ls.run(cmd)
            # simple run
            return yatest.common.execute(
                cmd,
                env=env,
                cwd=cwd,
                check_exit_code=check_exit_code,
                wait=wait,
                **kwargs
            )
        finally:
            self.try_debug_yt(yatf_gdb, cwd, env)
