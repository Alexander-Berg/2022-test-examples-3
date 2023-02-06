# -*- coding: utf-8 -*-

import getpass
import multiprocessing
import os
import re
import shlex
import subprocess
import sys

from constants import *
from utils import *


def create_remote_command(host_name, command, terminal=False):
    ssh_command = ['ssh', '-A', '-o', 'StrictHostKeyChecking no']
    if terminal:
        ssh_command.append('-t')
    ssh_command.append('{0}.market.yandex.net'.format(host_name))
    ssh_command.append(command)
    print '>>>[{0}] {1}'.format(host_name, command)
    return ssh_command


def turn_command_into_string(command):
    return ' '.join(['\'' + cmd + '\'' if ' ' in cmd else cmd for cmd in command])


def execute_command_on_host(host_name, command, terminal=False, get_output=False):
    ssh_command = create_remote_command(host_name, command, terminal)
    if get_output:
        return subprocess.check_output(ssh_command)
    else:
        subprocess.check_call(ssh_command)


def start_command_on_host(host_name, command, terminal=False):
    ssh_command = create_remote_command(host_name, command, terminal)
    return subprocess.Popen(ssh_command, stdout=subprocess.PIPE)


def get_cluster_host_name(cluster_suffix, cluster_index, host_index):
    if host_index == SNIPPET_HOST_INDEX:
        return get_snippet_host_name(cluster_suffix, cluster_index)
    else:
        return 'msh{:02}{}'.format(cluster_index * 8 + host_index + 1, cluster_suffix)


def get_snippet_host_name(cluster_suffix, cluster_index):
    return 'msh-off{:02}{}'.format(cluster_index + 1, cluster_suffix)


def get_host_range():
    return xrange(0, CLUSTER_SIZE)


def execute_command_on_cluster(cluster_suffix, cluster_index, command, terminal=False, get_output=False):
    for host_index in get_host_range():
        host_name = get_cluster_host_name(cluster_suffix, cluster_index, host_index)
        return execute_command_on_host(host_name, command, terminal, get_output)


class CommandExecutionObject(object):

    def __init__(self, cluster_suffix, cluster_index, command, snippet_command, get_output):
        self.cluster_suffix = cluster_suffix
        self.cluster_index = cluster_index
        self.command = command
        self.snippet_command = snippet_command
        self.get_output = get_output

    def __call__(self, host_index):
        if host_index == SNIPPET_HOST_INDEX and self.snippet_command is not None:
            command = self.snippet_command
            host_name = get_snippet_host_name(self.cluster_suffix, self.cluster_index)
        else:
            command = self.command[host_index] if isinstance(self.command, dict) else self.command
            host_name = get_cluster_host_name(self.cluster_suffix, self.cluster_index, host_index)
        try:
            return host_index, execute_command_on_host(host_name, command, get_output=self.get_output)
        except subprocess.CalledProcessError as e:
            raise Exception('{0} failed to execute "{1}" with code {2}'.format(
                host_name, command, e.returncode))


def execute_command_on_cluster_in_parallel(cluster_suffix, cluster_index, command, snippet_command=None, get_output=False, timeout=None):
    pool = multiprocessing.Pool(CLUSTER_SIZE + 1)
    result = dict()
    try:
        if isinstance(command, dict):
            host_range = command.keys()
        elif snippet_command is None:
            host_range = get_host_range()
        else:
            host_range = xrange(0, CLUSTER_SIZE + 1)
        for host_index, output in pool.map_async(
            CommandExecutionObject(cluster_suffix, cluster_index, command, snippet_command, get_output), host_range).get(
                timeout if timeout is not None else sys.maxint
        ):
            result[host_index] = output
        return result
    except:
        pool.terminate()
        raise
    finally:
        pool.close()
        pool.join()


def copy_files_to_host(host_name, src, dst):
    scp_command = ['scp', '-o', 'StrictHostKeyChecking no', '-r', '-q']
    if isinstance(src, str):
        scp_command.append(src)
    else:
        scp_command.extend(src)
    scp_command.append('{}.market.yandex.net:{}'.format(host_name, dst))
    print '>>>[{0}] "{1}" -> "{2}"'.format(host_name, src, dst)
    subprocess.check_call(scp_command)


def copy_files_to_cluster(cluster_suffix, cluster_index, src, dst):
    for host_index in get_host_range():
        copy_files_to_host(get_cluster_host_name(cluster_suffix, cluster_index, host_index), src, dst)


class CopyFilesToHostObject(object):

    def __init__(self, cluster_suffix, cluster_index, src, dst):
        self.cluster_suffix = cluster_suffix
        self.cluster_index = cluster_index
        self.src = src
        self.dst = dst

    def __call__(self, host_index):
        host_name = get_cluster_host_name(self.cluster_suffix, self.cluster_index, host_index)
        try:
            copy_files_to_host(host_name, self.src, self.dst)
        except subprocess.CalledProcessError as e:
            raise Exception('{0} failed to copy "{1}" to "{2}" with code {3}'.format(
                host_name, self.src, self.dst, e.returncode))


def copy_files_to_cluster_in_parallel(cluster_suffix, cluster_index, src, dst):
    pool = multiprocessing.Pool(CLUSTER_SIZE)
    pool.map_async(CopyFilesToHostObject(cluster_suffix, cluster_index, src, dst),
                   get_host_range()).get(sys.maxint)
    pool.close()
    pool.join()


class CopyDistinctFilesToHostObject(object):

    def __init__(self, cluster_suffix, cluster_index, src_dst_pairs):
        self.cluster_suffix = cluster_suffix
        self.cluster_index = cluster_index
        self.src_dst_pairs = src_dst_pairs

    def __call__(self, host_index):
        host_name = get_cluster_host_name(self.cluster_suffix, self.cluster_index, host_index)
        src, dst = self.src_dst_pairs[host_index]
        try:
            copy_files_to_host(host_name, src, dst)
        except subprocess.CalledProcessError as e:
            raise Exception('{0} failed to copy "{1}" to "{2}" with code {3}'.format(
                host_name, src, dst, e.returncode))


def copy_distinct_files_to_cluster_in_parallel(cluster_suffix, cluster_index, src_dst_pairs):
    pool = multiprocessing.Pool(CLUSTER_SIZE)
    pool.map_async(CopyDistinctFilesToHostObject(cluster_suffix, cluster_index, src_dst_pairs),
                   get_host_range()).get(sys.maxint)
    pool.close()
    pool.join()


def copy_files_from_host(host_name, src, dst, append_host_to_dst=True):
    scp_command = ['scp', '-o', 'StrictHostKeyChecking no', '-r', '-q']
    file_list = None
    if isinstance(src, str):
        file_list = [src]
    else:
        file_list = src
    for file_path in file_list:
        scp_command.append('{0}.market.yandex.net:{1}'.format(host_name, file_path))
    if append_host_to_dst:
        dst = os.path.join(dst, host_name)
    create_directory(dst)
    scp_command.append(dst)
    print '>>>[{0}] "{1}" -> "{2}"'.format(host_name, src, dst)
    subprocess.check_call(scp_command)


def copy_files_from_cluster(cluster_suffix, cluster_index, src, dst):
    for host_index in get_host_range():
        copy_files_from_host(get_cluster_host_name(cluster_suffix, cluster_index, host_index), src, dst)


class CopyFilesFromHostObject(object):

    def __init__(self, cluster_suffix, cluster_index, target_directory, file_list, snippet_file_list):
        self.cluster_suffix = cluster_suffix
        self.cluster_index = cluster_index
        self.target_directory = target_directory
        self.file_list = file_list
        self.snippet_file_list = snippet_file_list

    def __call__(self, host_index):
        if host_index == SNIPPET_HOST_INDEX and self.snippet_file_list is not None:
            file_list = self.snippet_file_list
            host_name = get_snippet_host_name(self.cluster_suffix, self.cluster_index)
        else:
            file_list = self.file_list[host_index] if isinstance(self.file_list, dict) else self.file_list
            host_name = get_cluster_host_name(self.cluster_suffix, self.cluster_index, host_index)
        try:
            copy_files_from_host(host_name, file_list, self.target_directory)
        except subprocess.CalledProcessError as e:
            raise Exception('{0} failed to copy "{1}" to "{2}" with code {3}'.format(
                host_name, file_list, self.target_directory, e.returncode))


def copy_files_from_cluster_in_parallel(cluster_suffix, cluster_index, target_directory, file_list, snippet_file_list=None):
    pool = multiprocessing.Pool(CLUSTER_SIZE + 1)
    if isinstance(file_list, dict):
        host_range = file_list.keys()
    elif snippet_file_list is None:
        host_range = get_host_range()
    else:
        host_range = xrange(0, CLUSTER_SIZE + 1)
    pool.map_async(CopyFilesFromHostObject(cluster_suffix, cluster_index, target_directory, file_list, snippet_file_list),
                   host_range).get(sys.maxint)
    pool.close()
    pool.join()


def create_temp_dir_on_host(host_name):
    execute_command_on_host(
        host_name,
        'mkdir -p {0}; chmod 777 {0}'.format(get_remote_temp_dir()))


def create_temp_dir_on_hp_cluster(cluster_index):
    execute_command_on_hp_in_parallel(
        cluster_index,
        'mkdir -p {0}; chmod 777 {0}'.format(get_remote_temp_dir()))


def clean_rty_data_on_hp_cluster(cluster_index):
    execute_command_on_hp_in_parallel(
        cluster_index,
        'sudo rm -rf {0}/data {0}/pdata'.format(get_remote_temp_dir()))


EXTRA_MODULES = ['constants.py']


def execute_script_on_host(host_name, command, get_output=False):
    create_temp_dir_on_host(host_name)
    script_name = shlex.split(command)[0]
    module_list = [script_name] + EXTRA_MODULES
    copy_files_to_host(
        host_name,
        [os.path.join(os.path.dirname(__file__), module) for module in module_list],
        get_remote_temp_dir())
    remote_script_path = os.path.join(get_remote_temp_dir(), script_name)
    remote_command = command.replace(script_name, remote_script_path, 1)
    result = execute_command_on_host(host_name, remote_command, get_output=get_output)
    execute_command_on_host(
        host_name,
        'rm -f ' + ' '.join([os.path.join(get_remote_temp_dir(), module) for module in module_list]))
    return result


def execute_script_on_hp_cluster(cluster_index, command, timeout=None):
    create_temp_dir_on_hp_cluster(cluster_index)
    script_name = shlex.split(command)[0]
    module_list = [script_name] + EXTRA_MODULES
    copy_files_to_hp_in_parallel(
        cluster_index,
        [os.path.join(os.path.dirname(__file__), module) for module in module_list],
        get_remote_temp_dir())
    remote_script_path = os.path.join(get_remote_temp_dir(), script_name)
    remote_command = command.replace(script_name, remote_script_path, 1)
    execute_command_on_hp_in_parallel(cluster_index, remote_command, timeout=timeout)
    execute_command_on_hp_in_parallel(
        cluster_index,
        'rm -f ' + ' '.join([os.path.join(get_remote_temp_dir(), module) for module in module_list]))


def execute_command_on_hp_in_parallel(cluster_index, command, snippet_command=None, get_output=False, timeout=None):
    return execute_command_on_cluster_in_parallel(
        'hp', cluster_index, command, snippet_command=snippet_command, get_output=get_output, timeout=timeout
    )


def get_hp_host_name(cluster_index, host_index):
    return get_cluster_host_name('hp', cluster_index, host_index)


def get_hp_snippet_host_name(cluster_index):
    return get_snippet_host_name('hp', cluster_index)


def copy_files_to_hp_in_parallel(cluster_index, src, dst):
    copy_files_to_cluster_in_parallel('hp', cluster_index, src, dst)


def copy_distinct_files_to_hp_in_parallel(cluster_index, src_dst_pairs):
    copy_distinct_files_to_cluster_in_parallel('hp', cluster_index, src_dst_pairs)


def copy_files_from_hp_in_parallel(cluster_index, target_directory, file_list, snippet_file_list=None):
    copy_files_from_cluster_in_parallel('hp', cluster_index, target_directory, file_list, snippet_file_list)


def get_tank_host(cluster_index):
    if cluster_index == 1:
        return 'msh-off02hp'
    return 'tank{:02}ht'.format(1 if cluster_index == 0 else 2)


def get_remote_temp_dir():
    return '/var/tmp/auto_perf_test_{0}'.format(getpass.getuser())


def get_current_index_gen(cluster_index, blue_index):
    current_index_gen = None
    snippet_command = 'cat /var/lib/search/snippet_index/download/current.generation' if not blue_index else None
    current_generations = execute_command_on_hp_in_parallel(
        cluster_index,
        'cat /var/lib/search/marketsearch/current.generation',
        snippet_command=snippet_command,
        get_output=True)
    for index_gen in current_generations.values():
        if current_index_gen is None:
            current_index_gen = index_gen
        elif current_index_gen != index_gen:
            raise Exception('Inconsistent index: {0} {1}'.format(current_index_gen, index_gen))
    INDEX_GEN_RE = r'^\d{8}_\d{4}$'
    if not re.match(INDEX_GEN_RE, current_index_gen):
        raise Exception('Unexpected index generation number {0}'.format(current_index_gen))
    return current_index_gen


def get_recent_ammo(cluster_index):
    return execute_script_on_host(get_tank_host(cluster_index), 'remote_get_recent_ammo.py', get_output=True)


def change_mode(cluster_index, path, mode='777'):
    cmd = 'sudo chmod {0} {1}'.format(mode, path)
    execute_command_on_hp_in_parallel(cluster_index, cmd)
