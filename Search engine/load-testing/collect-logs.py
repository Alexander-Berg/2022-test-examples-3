#!/usr/bin/env python2.7

import argparse
import getpass
import logging
import os
import shlex
import subprocess
import traceback

import concurrent.futures
import yaml

COLLECT_LOG_DIR = "./logs/"


class GroupConfiguration(object):
    """
    Group configuration class. Contains remote hosts file paths and other usefull data.
    """

    DEFAULT_TMP = './logs/'

    def __init__(self, group, remote_tmp=None, local_tmp=None):
        """
        :param group: Group selector in format <group_name>:<port>. Port is used to generate balancer log file name.
        :param remote_tmp: directory to store collected logs on remote hosts.
        :param local_tmp: directory to store collected logs on local side.
        """

        group_name, remote_log_path = self.parse_group(group)

        self.group_name = group_name
        self.remote_log_path = remote_log_path

        self.__local_tmp = local_tmp or self.DEFAULT_TMP
        self.__remote_tmp = remote_tmp or self.DEFAULT_TMP

    @property
    def remote_log_dir(self):
        return os.path.dirname(self.remote_log_path)

    @property
    def remote_log_file(self):
        return os.path.basename(self.remote_log_path)

    @property
    def local_log_dir(self):
        return self.__local_tmp

    @property
    def local_log_file(self):
        return self.remote_log_file

    @property
    def local_log_path(self):
        return os.path.join(self.local_log_dir, self.local_log_file)

    @property
    def remote_tmp_dir(self):
        return self.__remote_tmp

    @property
    def remote_tmp_file(self):
        return self.remote_log_file

    @property
    def remote_tmp_path(self):
        return os.path.join(self.remote_tmp_dir, self.remote_tmp_file)

    @staticmethod
    def parse_group(group):
        return group.strip().split(':')


class LogsCollector(object):

    def __init__(self, max_workers=3, aliases=None):
        self.executor = concurrent.futures.ThreadPoolExecutor(max_workers=max_workers)
        self.aliases = aliases or {}

    @staticmethod
    def run_command(cmd, comment=''):
        if isinstance(cmd, (str, unicode)):
            cmd = shlex.split(cmd)

        if comment:
            comment = '({0})'.format(comment)

        logging.debug("Running command '{}' {}...".format(cmd, comment))

        process = subprocess.Popen(cmd, shell=False)

        return process.wait()

    def remote_collect(self, group_config, size):
        """
        :param group_config: configuration of group: log paths, tmp file paths, etc.
        :type group_config: GroupConfiguration

        :param size: number of lines to collect from log on each host.
                     Result log size will be <size> * <hosts_number>.
        :type size: int

        :return: command
        :rtype: str
        """

        logging.info('Collecting logs on remote nodes...')

        remote_command = 'mkdir -p {tmp_dir};' \
                         'tail -n 0 -f {log_file} ' \
                         '| head -n {size} > {tmp_file}'.format(tmp_dir=group_config.remote_tmp_dir,
                                                                tmp_file=group_config.remote_tmp_path,
                                                                log_file=group_config.remote_log_path,
                                                                size=size)

        command = ['sky', 'run', '-U', remote_command, group_config.group_name]

        return self.run_command(command, 'remote_collect')

    def local_collect(self, group_config):

        logging.info('Fetching logs to local side...')

        command = ['sky', 'download', '-U', group_config.remote_tmp_path,
                   group_config.local_log_dir,
                   group_config.group_name]

        return self.run_command(command, 'local_collect')

    def remote_cleanup(self, group_config=None):

        logging.info('Cleaning remote caches...')

        remove_log = 'rm -f {}'.format(group_config.remote_tmp_path)
        # remove_dir = 'rmdir {} 2>/dev/null || true'.format(group_config.remote_tmp_dir)

        command = ['sky', 'run', '-U', remove_log, group_config.group_name]

        return self.run_command(command, 'remote_cleanup')

    def collect_group(self, group_config, size):
        self.remote_collect(group_config=group_config, size=size)
        self.local_collect(group_config=group_config)
        self.remote_cleanup(group_config=group_config)

    def collect(self, groups, size, remote_tmp=None, local_tmp=None):
        future_to_group = {}
        for group_spec in groups:
            if group_spec in self.aliases:
                future_to_group.update(
                    {self.executor.submit(self.collect_group,
                                          GroupConfiguration(group, remote_tmp, local_tmp),
                                          size): group for group in self.aliases[group_spec]}
                )
            else:
                future = self.executor.submit(self.collect_group,
                                              GroupConfiguration(group_spec, remote_tmp, local_tmp),
                                              size)
                future_to_group[future] = group_spec

        for future in concurrent.futures.as_completed(future_to_group):
            group_spec = future_to_group[future]
            try:
                # import pdb; pdb.set_trace()
                future.result()
            except Exception as exc:
                logging.error("Group '{0}' treatment error: {1}".format(group_spec, exc))
                logging.error(traceback.format_exc())
            else:
                print("Group '{0}' treatment succeed.".format(group_spec))


def parse_args(args=None):
    parser = argparse.ArgumentParser(prog='collect-logs')

    parser.add_argument("--loglevel", type=str, dest='loglevel', default="INFO",
                        help="Set logging level to LOGLEVEL")
    parser.add_argument("-d", "--debug", action="store_const", dest="loglevel",
                        const="DEBUG", help="Set logging level to DEBUG")
    parser.add_argument("-v", "--verbose", action="store_const", dest="loglevel",
                        const="INFO", help="Set logging level to INFO")
    parser.add_argument("-q", "--quiet", action="store_const", dest="loglevel",
                        const="CRITICAL", help="Set logging level to CRITICAL")

    parser.add_argument('--threads', '-t', type=int, default=3)
    parser.add_argument('--size', '-s', type=int, default=1000)

    remote_tmp_default = "/home/{0}/logs".format(getpass.getuser())
    parser.add_argument('--remote-tmp', type=str, default=remote_tmp_default)
    parser.add_argument('--local-tmp', type=str, default='./logs/')

    parser.add_argument('groups', nargs='+', help='Hosts or groups for collect logs.')

    return parser.parse_args(args)


def convert_options(options):
    options.loglevel = options.loglevel.upper()


def read_config(config=None):
    if config is None:
        config = "./collect-logs.conf"

    if isinstance(config, (str, unicode)) and os.path.exists(config):
        config = file(config, 'r')

    return yaml.load(config)


def main(args=None):
    options = parse_args(args)

    convert_options(options)

    # Change logging level before any other actions.
    logging.root.setLevel(options.loglevel)

    config = read_config()

    collector = LogsCollector(options.threads, config['aliases'])
    collector.collect(options.groups, options.size, options.remote_tmp, options.local_tmp)


if __name__ == '__main__':
    main()
