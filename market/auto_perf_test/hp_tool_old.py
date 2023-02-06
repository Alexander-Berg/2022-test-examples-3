#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import functools
import threading

from auto_perf_test_old import *


HOST_ORDINARY = 1
HOST_SNIPPET = 2


def create_ssh_command(host_name, command, immediate_output):
    ssh_command = ['ssh']
    if immediate_output:
        ssh_command += ['-tt', '-q']
    ssh_command += [
        '-o', 'StrictHostKeyChecking no',
        '-o', 'CheckHostIP no',
        '-o', 'UserKnownHostsFile /dev/null',
        '-o', 'LogLevel ERROR',
        '{}.market.yandex.net'.format(host_name)
    ]
    ssh_command += command
    return ssh_command


class CommandExecutor(object):
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

    class ImmediateOutputThread(threading.Thread):

        def __init__(self, pipe, prefix):
            self.pipe = pipe
            self.prefix = prefix
            threading.Thread.__init__(self)

        def run(self):
            while True:
                line = self.pipe.readline()
                if not line:
                    break
                line = line.rstrip()
                if line:
                    print self.prefix, line

    def __init__(self, host_name, command, command_queue, immediate_output):
        self.host_name = host_name
        self.command = command
        self.ssh_command = create_ssh_command(host_name, command, immediate_output)
        self.command_queue = command_queue
        self.immediate_output = immediate_output
        self.buffered_stderr = list()
        self.buffered_stdout = list()

    def output_handler(self, buffered_output, line):
        buffered_output.append(line)

    def __call__(self):
        try:
            process = subprocess.Popen(self.ssh_command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            if self.immediate_output:
                stderr_reader_thread = self.ImmediateOutputThread(process.stderr, self.host_name + ' ERR')
                stdout_reader_thread = self.ImmediateOutputThread(process.stdout, self.host_name + ' OUT')
            else:
                stderr_reader_thread = self.PipeReaderThread(process.stderr, functools.partial(self.output_handler, self.buffered_stderr))
                stdout_reader_thread = self.PipeReaderThread(process.stdout, functools.partial(self.output_handler, self.buffered_stdout))
            stderr_reader_thread.start()
            stdout_reader_thread.start()
            ret_code = process.wait()
            stderr_reader_thread.join()
            stdout_reader_thread.join()
            self.command_queue.put(ret_code)
            self.command_queue.put(self.buffered_stderr)
            self.command_queue.put(self.buffered_stdout)
        except Exception:
            traceback.print_exc()


def execute_command_in_parallel(host_command_list, immediate_output):
    result = list()
    workers = list()
    command_queues = list()
    for cluster_index, host_index, host_name, command in host_command_list:
        command_queue = multiprocessing.Queue()
        processor = CommandExecutor(host_name, command, command_queue, immediate_output)
        process_name = '{host_name}'.format(host_name=host_name.split('.')[0])
        workers.append(multiprocessing.Process(target=processor, name=process_name))
        command_queues.append((cluster_index, host_index, host_name, command_queue))
    try:
        for worker in workers:
            worker.start()
        for cluster_index, host_index, host_name, command_queue in command_queues:
            ret_code = command_queue.get()
            err = command_queue.get()
            out = command_queue.get()
            result.append((cluster_index, host_index, host_name, ret_code, err, out))
    except Exception:
        for worker in workers:
            worker.terminate()
        raise
    for worker in workers:
        worker.join()
    return result


def execute_command_on_hp_cluster(cluster_index, host_types, command, snippet_command=None, immediate_output=False):
    host_command_list = list()
    if host_types & HOST_ORDINARY:
        host_command_list.extend([(
            cluster_index,
            host_index,
            get_hp_host_name(cluster_index, host_index),
            command
        ) for host_index in range(0, CLUSTER_SIZE)])
    if host_types & HOST_SNIPPET:
        host_command_list.append((
            cluster_index,
            SNIPPET_HOST_INDEX,
            get_hp_snippet_host_name(cluster_index),
            snippet_command if snippet_command is not None else command
        ))
    for cluster_index, host_index, host_name, ret_code, err, out in execute_command_in_parallel(host_command_list, immediate_output):
        print '{cluster_index:02} {host_index} {host_name}: {result}'.format(
            cluster_index=cluster_index,
            host_index=host_index,
            host_name=host_name,
            result='OK' if ret_code == 0 else 'FAILED with code {}'.format(ret_code)
        )
        for line in err:
            print 'ERR: {}'.format(line)
        for line in out:
            print 'OUT: {}'.format(line)
        if err or out:
            print


def _main():
    arg_parser = argparse.ArgumentParser(description='Manage custom Report on HP cluster.')
    arg_parser.add_argument('--cluster', type=int, choices=ALLOWED_HP_CLUSTERS, default=ALLOWED_HP_CLUSTERS[0], help='Zero-based index of HP cluster')
    subparsers = arg_parser.add_subparsers(dest='command', help='Action to perform')

    parser_install = subparsers.add_parser('install', formatter_class=argparse.RawTextHelpFormatter, help='Install custom Report')
    parser_install.add_argument('--svn-dir', required=True, help='Path to SVN working copy. It will be used to checkout source code and build Report binary.')
    parser_install.add_argument('--git-dir', help='Path to GIT repository, required to apply GIT branches')
    parser_install.add_argument('--config-type', choices=REPORT_NAMES, required=True)
    parser_install.add_argument('--build-type', choices=['release', 'debug', 'profile'], default='release')
    parser_install.add_argument(
        '--no-dist-build', action='store_false', dest='dist_build',
        help='Distributed build is generally faster. You may want to disable it if you have everything already cached.')
    parser_install.add_argument('--extra-build-options', help='Additional options for ya make')
    parser_install.add_argument('--enable-docfetcher', action='store_true', help='Enable population of RTY index')
    source_rev_group = parser_install.add_mutually_exclusive_group(required=True)
    source_rev_group.add_argument('--patch', help='This will apply patch created by "svn diff"')
    source_rev_group.add_argument(
        'revision',
        nargs='?',
        help='Source code revision. Can be:\n'
             'SVN revision: 2793588\n'
             'SVN branch: ^/branches/market/report-2017.1.72/arcadia\n'
             'SVN branch and revision: ^/branches/market/report-2017.1.72/arcadia,2793588\n'
             'GIT branch: my_feature_branch\n')

    subparsers.add_parser('uninstall', help='Uninstall custom Report')

    parser_start = subparsers.add_parser('start', help='Start Report')
    subparsers.add_parser('stop', help='Stop Report')
    parser_restart = subparsers.add_parser('restart', help='Restart Report')

    parser_unpack_index = subparsers.add_parser('unpack_index', help='Unpack index generation that is already available on HP')
    parser_unpack_index.add_argument('--blue', action='store_true', help='Unpack blue index')
    parser_unpack_index.add_argument('index_gen')

    current_index = subparsers.add_parser('current_index', help='Display current index generation installed on HP')
    current_index.add_argument('--blue', action='store_true', help='Get current blue index')

    subparsers.add_parser('lock', help='Lock HP cluster')
    subparsers.add_parser('unlock', help='Unlock HP cluster')

    parser_exec = subparsers.add_parser('exec', help='Execute arbitrary command')
    host_type_group = parser_exec.add_mutually_exclusive_group()
    host_type_group.add_argument('--all-hosts', action='store_true', help='Include snippet host')
    host_type_group.add_argument('--snippet-only', action='store_true', help='Execute on snippet host only')
    host_type_group.add_argument('--imm', action='store_true', help='Immediate output mode')

    subparsers.add_parser('bc', help='Execute backctld command e.g. "reload 20180306_1031 100"')

    parser_url_query = subparsers.add_parser('url', help='Query report via http e.g. "admin_action=versions"')
    parser_url_query.add_argument('query')

    def add_port_arg(subparser):
        subparser.add_argument('--report-port', type=int, default=CUSTOM_PORT, help='TCP port number for Report')
    add_port_arg(parser_install)
    add_port_arg(parser_start)
    add_port_arg(parser_restart)
    add_port_arg(parser_url_query)

    args, extra_args = arg_parser.parse_known_args()
    if args.command not in ('exec', 'bc'):
        args = arg_parser.parse_args()

    if args.command == 'lock':
        print '>>> Acquiring report lock for HP'
        create_report_lock(args.cluster).acquire()
    elif args.command == 'unlock':
        print '>>> Releasing report lock for HP'
        create_report_lock(args.cluster).release()
    else:
        print '>>> Making sure report lock for HP is acquired'
        create_report_lock(args.cluster).ensure_acquired()
        if args.command == 'install':
            svn_dir = expand_path(args.svn_dir)
            check_i_am_not_run_from(svn_dir)
            git_dir = expand_path(args.git_dir)
            if args.revision is not None:
                use_git, git_branch, svn_revision, svn_branch = parse_revision(args.revision, git_dir)
                svn_patch = None
            else:
                use_git, git_branch, svn_branch = False, None, None
                svn_patch = expand_path(args.patch)
                svn_revision = extract_original_revision_from_patch(svn_patch)
            revision_descr = get_revision_descr2(git_branch, svn_branch, svn_revision, svn_patch)

            stop_services_on_hp(args.cluster)

            if args.build_type == 'release':
                build_type = BUILD_RELEASE
            elif args.build_type == 'debug':
                build_type = BUILD_DEBUG
            elif args.build_type == 'profile':
                build_type = BUILD_PROFILE
            if use_git:
                build_report_from_git_branch(git_dir, svn_dir, git_branch, build_type, args.dist_build, args.extra_build_options, revision_descr)
            else:
                build_custom_report(svn_dir, svn_revision, svn_branch, svn_patch, build_type, args.dist_build, args.extra_build_options, revision_descr)

            report_type = REPORT_NAMES.index(args.config_type)
            generate_config(args.cluster, svn_dir, report_type, args.enable_docfetcher, args.report_port)
            install_custom_report_on_hp(args.cluster, svn_dir, build_type, report_type, args.report_port)
        elif args.command == 'uninstall':
            uninstall_custom_report_on_hp(args.cluster)
            start_services_on_hp(args.cluster)
        elif args.command == 'start':
            start_report_on_hp(args.cluster)
            wait_for_report_to_start_on_hp(args.cluster, BUILD_RELEASE, args.report_port)
        elif args.command == 'stop':
            stop_report_on_hp(args.cluster)
        elif args.command == 'restart':
            restart_report_on_hp(args.cluster)
            wait_for_report_to_start_on_hp(args.cluster, BUILD_RELEASE, args.report_port)
        elif args.command == 'unpack_index':
            unpack_index(args.cluster, args.index_gen, args.blue)
        elif args.command == 'current_index':
            print get_current_index_gen(args.cluster, args.blue)
        elif args.command == 'exec':
            if args.all_hosts:
                host_types = HOST_ORDINARY | HOST_SNIPPET
            elif args.snippet_only:
                host_types = HOST_SNIPPET
            else:
                host_types = HOST_ORDINARY
            execute_command_on_hp_cluster(args.cluster, host_types, extra_args, immediate_output=args.imm)
        elif args.command == 'bc':
            def get_backctld_command(name):
                return ['echo "{} {}" | nc localhost 9002'.format(name, ' '.join(extra_args))]
            execute_command_on_hp_cluster(
                args.cluster,
                HOST_ORDINARY | HOST_SNIPPET,
                get_backctld_command('marketsearch3'),
                get_backctld_command('marketsearchsnippet')
            )
        elif args.command == 'url':
            execute_command_on_hp_cluster(
                args.cluster,
                HOST_ORDINARY,
                ['curl -sS \'http://localhost:{port}/yandsearch?{query}\''.format(port=args.report_port, query=args.query)]
            )


if __name__ == "__main__":
    _main()
