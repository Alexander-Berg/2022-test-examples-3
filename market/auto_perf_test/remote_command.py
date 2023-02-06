# -*- coding: utf-8 -*-

import base64
import functools
import getpass
import logging
import multiprocessing
import os
import subprocess
import sys
import threading

log = logging.getLogger(__name__)
log.setLevel(logging.INFO)


def _get_short_host_name(host_name):
    return host_name.split('.')[0]


def _get_short_host_name_and_port_string(host_name, _):
    return _get_short_host_name(host_name)


def _create_ssh_command(host_name, instance_port, run_on_hypervisor, command):
    result = [
        'ssh',
        '-o', 'StrictHostKeyChecking no',
        '-o', 'CheckHostIP no',
        '-o', 'UserKnownHostsFile /dev/null',
        '-o', 'LogLevel ERROR',
        '-o', 'ConnectTimeout 5'
    ]
    if not run_on_hypervisor:
        result += [
            '-p', '10046',
            '-l', '//user:{user}//slot:{instance_port}@{host_name}'.format(
                user=getpass.getuser(), instance_port=instance_port, host_name=host_name
            )
        ]
    result.append(host_name)
    result.append(command)
    return result


def _create_remote_command(host_name, instance_port, run_on_hypervisor, run_as_root, command):
    if run_as_root:
        if run_on_hypervisor:
            bash = 'sudo bash'
        else:
            bash = 'bash'
    else:
        if run_on_hypervisor:
            bash = 'bash'
        else:
            bash = 'sudo -u loadbase bash'
    command = 'echo {command} | base64 -d | {bash}'.format(
        command=base64.b64encode(command),
        bash=bash
    )
    return _create_ssh_command(host_name, instance_port, run_on_hypervisor, command)


class PipeReaderThread(threading.Thread):

    def __init__(self, pipe, handler):
        self.pipe = pipe
        self.handler = handler
        threading.Thread.__init__(self)

    def run(self):
        for line in self.pipe:
            line = line.rstrip()
            if line:
                self.handler(line)


class RemoteCommandExecuter(object):

    def __init__(self, host_info, command):
        self.host_info = host_info
        self.command = command
        self.buffered_stderr = list()
        self.buffered_stdout = list()

    def output_handler(self, buffered_output, line):
        buffered_output.append(line)

    def __call__(self):
        try:
            process = subprocess.Popen(self.command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            process.stdin.close()
            stderr_reader_thread = PipeReaderThread(process.stderr, functools.partial(self.output_handler, self.buffered_stderr))
            stdout_reader_thread = PipeReaderThread(process.stdout, functools.partial(self.output_handler, self.buffered_stdout))
            stderr_reader_thread.start()
            stdout_reader_thread.start()
            ret_code = process.wait()
            stderr_reader_thread.join()
            stdout_reader_thread.join()
            exc_info = None
        except:
            ret_code = None
            exc_info = sys.exc_info()[1]
        return (
            self.host_info,
            ret_code,
            self.buffered_stderr,
            self.buffered_stdout,
            exc_info
        )


def _create_remote_extraction_command(host_name, instance_port, run_on_hypervisor, run_as_root, dst_dir):
    if run_as_root:
        if run_on_hypervisor:
            command = 'sudo tar'
        else:
            command = 'tar'
    else:
        if run_on_hypervisor:
            command = 'tar'
        else:
            command = 'sudo -u loadbase tar'
    command += ' xf - --no-same-owner -C {}'.format(dst_dir)
    return _create_ssh_command(host_name, instance_port, run_on_hypervisor, command)


class RemoteFileCopierSsh(object):
    def __init__(self, host_info, run_on_hypervisor, run_as_root, src_path, dst_dir):
        self.host_info = host_info
        self.run_on_hypervisor = run_on_hypervisor
        self.run_as_root = run_as_root
        self.src_path = os.path.abspath(src_path)
        self.dst_dir = dst_dir

    def output_handler(self, buffered_output, line):
        buffered_output.append(line)

    def __call__(self):
        tar_buffered_stderr = list()
        ssh_buffered_stderr = list()
        ssh_buffered_stdout = list()
        tar_ret_code = None
        ssh_ret_code = None
        try:
            src_is_single_file = os.path.isfile(self.src_path)
            if src_is_single_file:
                ssh_command = _create_ssh_command(
                    host_name=self.host_info.host_name,
                    instance_port=self.host_info.instance_port,
                    run_on_hypervisor=self.run_on_hypervisor,
                    command='cat > {}'.format(os.path.join(self.dst_dir, os.path.basename(self.src_path)))
                )
            else:
                ssh_command = _create_remote_extraction_command(
                    host_name=self.host_info.host_name,
                    instance_port=self.host_info.instance_port,
                    run_on_hypervisor=self.run_on_hypervisor,
                    run_as_root=self.run_as_root,
                    dst_dir=self.dst_dir
                )
            log.debug('%s', ' '.join(ssh_command))
            if src_is_single_file:
                ssh_input = open(self.src_path, 'r')
            else:
                tar_command = ['tar', 'cf', '-', '-C', os.path.dirname(self.src_path), os.path.basename(self.src_path)]
                tar_process = subprocess.Popen(tar_command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                tar_process.stdin.close()
                tar_stderr_reader_thread = PipeReaderThread(tar_process.stderr, functools.partial(self.output_handler, tar_buffered_stderr))
                tar_stderr_reader_thread.start()
                ssh_input = tar_process.stdout
            ssh_process = subprocess.Popen(ssh_command, stdin=ssh_input, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            ssh_stderr_reader_thread = PipeReaderThread(ssh_process.stderr, functools.partial(self.output_handler, ssh_buffered_stderr))
            ssh_stdout_reader_thread = PipeReaderThread(ssh_process.stdout, functools.partial(self.output_handler, ssh_buffered_stdout))
            ssh_stderr_reader_thread.start()
            ssh_stdout_reader_thread.start()
            if src_is_single_file:
                ssh_input.close()
            else:
                tar_ret_code = tar_process.wait()
                tar_stderr_reader_thread.join()
            ssh_ret_code = ssh_process.wait()
            ssh_stderr_reader_thread.join()
            ssh_stdout_reader_thread.join()
            exc_info = None
        except:
            tar_ret_code = None
            ssh_ret_code = None
            exc_info = sys.exc_info()[1]
        return (
            self.host_info,
            tar_ret_code or ssh_ret_code,
            tar_buffered_stderr + ssh_buffered_stderr,
            ssh_buffered_stdout,
            exc_info
        )


def _call(x):
    return x()


def _run_command_executers(command_executers, process_limit=None, get_results=False):
    process_pool_size = len(command_executers)
    if process_limit is not None and process_pool_size > process_limit:
        process_pool_size = process_limit

    def initializer():
        pass
        # signal.signal(signal.SIGINT, signal.SIG_IGN)
    pool = multiprocessing.Pool(processes=process_pool_size, initializer=initializer)

    failed = False
    process_results = list()
    try:
        for host_info, ret_code, err, out, exc_info in pool.imap(_call, command_executers):
            if exc_info is not None:
                log_message = 'FAILED with exception \'{}\''.format(exc_info)
                failed = True
            elif ret_code:
                if not get_results:
                    log_message = 'FAILED with code {}'.format(ret_code)
                    failed = True
            else:
                log_message = 'OK'
            if get_results and not failed:
                process_results.append((ret_code, out))
            else:
                log.info('%s: %s', _get_short_host_name_and_port_string(host_info.host_name, host_info.instance_port), log_message)
                for line in err:
                    log.info('ERR: %s', line)
                for line in out:
                    log.info('OUT: %s', line)
    except:
        pool.terminate()
        raise
    else:
        pool.close()
    finally:
        pool.join()
    if failed:
        raise Exception('Remote command execution failed')
    return process_results


class HostInfo(object):
    def __init__(self, host_name, instance_port, is_snippet):
        self.host_name = host_name
        self.instance_port = instance_port
        self.is_snippet = is_snippet


def get_hp_host_list(cluster_index):
    return [
        HostInfo(
            host_name='msh{:02}hp.market.yandex.net'.format(host_index),
            instance_port=17050,
            is_snippet=False
        )
        for host_index in range(cluster_index * 8 + 1, (cluster_index + 1) * 8 + 1)
    ]


def get_hp_snippet_host_list(cluster_index):
    return [
        HostInfo(
            host_name='msh-off{:02}hp.market.yandex.net'.format(cluster_index + 1),
            instance_port=17050,
            is_snippet=False
        )
    ]


def execute_command_on_hosts(host_list, command, snippet_command=None, run_on_hypervisor=True, run_as_root=False, process_limit=None, get_results=False):
    command_executers = list()
    for host_info in host_list:
        host_command = snippet_command if snippet_command and host_info.is_snippet else command
        if not get_results:
            log.info('Remote command: %s: %s', _get_short_host_name_and_port_string(host_info.host_name, host_info.instance_port), host_command)
        remote_command = _create_remote_command(
            host_info.host_name,
            host_info.instance_port,
            run_on_hypervisor=run_on_hypervisor,
            run_as_root=run_as_root,
            command=host_command
        )
        command_executers.append(RemoteCommandExecuter(host_info, remote_command))
    return _run_command_executers(command_executers, process_limit=process_limit, get_results=get_results)


def upload_files_to_hosts(host_list, src_path, dst_dir, run_on_hypervisor=True, run_as_root=False, process_limit=None):
    command_executers = list()
    for host_info in host_list:
        log.info('Remote copy: %s -> %s:%s', src_path, _get_short_host_name_and_port_string(host_info.host_name, host_info.instance_port), dst_dir)
        command_executers.append(RemoteFileCopierSsh(host_info, run_on_hypervisor, run_as_root, src_path, dst_dir))
    return _run_command_executers(command_executers, process_limit=process_limit)
