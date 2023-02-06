#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import subprocess
import threading

from remote import *

HD_SUFFIX = 'hd'


class ProcessRunner(object):

    class PipeReaderThread(threading.Thread):

        def __init__(self, process_runner, pipe, tag):
            self.process_runner = process_runner
            self.pipe = pipe
            self.tag = tag
            threading.Thread.__init__(self)

        def run(self):
            for line in self.pipe:
                self.process_runner.process_line(line.rstrip('\r\n'), self.tag)

    def __init__(self, command, cwd=None):
        print '>>>{0}'.format(' '.join(command))
        self.process = subprocess.Popen(
            command, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self.stdout_watcher_thread = ProcessRunner.PipeReaderThread(self, self.process.stdout, 'OUT')
        self.stderr_watcher_thread = ProcessRunner.PipeReaderThread(self, self.process.stderr, 'ERR')
        self.stdout_watcher_thread.start()
        self.stderr_watcher_thread.start()

    def wait(self):
        ret_code = self.process.wait()
        self.stdout_watcher_thread.join()
        self.stderr_watcher_thread.join()
        return ret_code

    def process_line(self, line, tag):
        print '{0}: {1}'.format(tag, line)


def _main():
    arg_parser = argparse.ArgumentParser(description='Copy index generation from HD to HP.')
    arg_parser.add_argument('--cluster', '-c', type=int, choices=(0, 1, 2), required=True, help='Target cluster index')
    arg_parser.add_argument('--generation', '-g', required=True, help='Index generation number')
    args = arg_parser.parse_args()

    MAKE_TMP_DIR_COMMAND = 'mkdir -p {0}'.format(get_remote_temp_dir())
    execute_command_on_cluster_in_parallel(
        HD_SUFFIX, 0, MAKE_TMP_DIR_COMMAND, snippet_command=MAKE_TMP_DIR_COMMAND)
    execute_command_on_hp_in_parallel(
        args.cluster, MAKE_TMP_DIR_COMMAND, snippet_command=MAKE_TMP_DIR_COMMAND)

    try:
        execute_command_on_cluster_in_parallel(
            HD_SUFFIX, 0,
            'cp -a /var/lib/search/marketsearch/{0} {1}'.format(args.generation, get_remote_temp_dir()),
            snippet_command='cp -a /var/lib/search/snippet_index/download/{0} {1}'.format(args.generation, get_remote_temp_dir())
        )

        copiers = list()
        for index in range(CLUSTER_SIZE):
            copiers.append(ProcessRunner([
                'ssh', '-A', '-o', 'StrictHostKeyChecking no', '{0}.market.yandex.net'.format(get_hp_host_name(args.cluster, index)),
                'scp -o "StrictHostKeyChecking no" -r {0}.market.yandex.net:{1} {2}'.format(
                    get_cluster_host_name(HD_SUFFIX, 0, index),
                    os.path.join(get_remote_temp_dir(), args.generation), get_remote_temp_dir()
                )
            ]))
        copiers.append(ProcessRunner([
            'ssh', '-A', '-o', 'StrictHostKeyChecking no', '{0}.market.yandex.net'.format(get_hp_snippet_host_name(args.cluster)),
            'scp -o "StrictHostKeyChecking no" -r {0}.market.yandex.net:{1} {2}'.format(
                get_snippet_host_name(HD_SUFFIX, 0),
                os.path.join(get_remote_temp_dir(), args.generation), get_remote_temp_dir()
            )
        ]))
        success = True
        for copier in copiers:
            res = copier.wait()
            if res != 0:
                success = False
        if not success:
            raise Exception('FAIL!')

        execute_command_on_hp_in_parallel(
            args.cluster,
            'sudo cp -a {0} /var/lib/search/marketsearch'.format(os.path.join(get_remote_temp_dir(), args.generation)),
            snippet_command='sudo cp -a {0} /var/lib/search/snippet_index/download'.format(os.path.join(get_remote_temp_dir(), args.generation))
        )

    finally:
        REMOVE_TMP_DIR_COMMAND = 'sudo rm -rf {0}'.format(get_remote_temp_dir())
        execute_command_on_cluster_in_parallel(
            HD_SUFFIX, 0, REMOVE_TMP_DIR_COMMAND, snippet_command=REMOVE_TMP_DIR_COMMAND
        )
        execute_command_on_hp_in_parallel(
            args.cluster, REMOVE_TMP_DIR_COMMAND, snippet_command=REMOVE_TMP_DIR_COMMAND
        )

if __name__ == "__main__":
    _main()
