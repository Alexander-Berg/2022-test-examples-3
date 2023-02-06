#!/usr/bin/env python

import json
from os.path import join as pj
from subprocess import check_output

import yt.wrapper as yt
from yt.wrapper.cypress_commands import exists, get_attribute, set_attribute
from yt.wrapper.table_commands import copy_table


def yt_table_checksum(path_to_script, proxy, path_to_table, format='structured', sorted=False):
    cmd = [
        path_to_script,
        '--proxy', proxy,
        '--input-type', format,
        '--table', path_to_table
    ]
    if sorted:
        cmd.append('--sorted')
    return check_output(cmd)


class TCheckerTable(object):
    __ATTR_CHECKSUM = 'checksum'

    def __init__(self, args):
        self.format = args.format
        self.sorted = args.sorted
        self.mutable = args.mutable

        if args.config is not None:
            with open(args.config) as f:
                self.config = json.load(f)
                self.proxy = self.config['MR']['Server']
        elif args.proxy is not None:
            self.proxy = args.proxy

        with open(args.params) as f:
            params = json.load(f)
            self.canon_prefix = params['canon_prefix']
            self.path_checksum_script = params['path_checksum_script']

        self._configure_yt()

    def _configure_yt(self):
        if self.proxy is None:
            raise Exception('Yt proxy url is not defined')
        yt.config['proxy']['url'] = self.proxy

    def _get_path(self, table_name):
        if table_name.startswith('//'):
            return table_name

        prefix = self.config['MR']['Prefix']

        if table_name.startswith('/'):
            return pj(prefix, table_name[1:])

        if table_name not in self.config['MR']['Paths']:
            raise ValueError('Table "{}" is not found in config'.format(table_name))

        local_path = self.config['MR']['Paths'][table_name]
        return pj(prefix, local_path) if not local_path.startswith('//') else local_path

    def _get_canon_path(self, table_name):
        return pj(self.canon_prefix, *table_name.split('/'))

    def _generate_canon_table(self, table_name):
        canon_path = self._get_canon_path(table_name)
        if exists(canon_path):
            return

        raw_path = self._get_path(table_name)
        copy_table(source_table=raw_path, destination_table=canon_path)

        checksum = yt_table_checksum(self.path_checksum_script, self.proxy, canon_path, self.format, self.sorted)
        set_attribute(canon_path, self.__ATTR_CHECKSUM, checksum)

    def _get_canon_checksum(self, table_name):
        self._generate_canon_table(table_name)

        default_val = '<default_val>'
        checksum = get_attribute(self._get_canon_path(table_name), self.__ATTR_CHECKSUM, default_val)
        assert checksum != default_val

        return checksum

    def is_good(self, table_name):
        if self.mutable:
            self._generate_canon_table(table_name)
            return True

        return yt_table_checksum(self.path_checksum_script, self.proxy, self._get_path(table_name), self.format, self.sorted) \
            == self._get_canon_checksum(table_name)


def main():
    from argparse import ArgumentParser

    parser = ArgumentParser(description='Compare table with canonized version')

    parser.add_argument('-t', '--tables', dest='tables', nargs='+',
                        required=True, help='List of table name. Possible ways: '
                                            '1)full path (start with //), '
                                            '2)path without prefix (start with /), '
                                            '3)table alias in config, '
                                            'for points 2) and 3) required config')
    parser.add_argument('-p', '--parameters', dest='params', required=True, help='Info about "path_checksum_script" and "canon_prefix" on yt')
    parser.add_argument('-m', '--mutable', dest='mutable', action='store_true', default=False, help='If tables are mutable')
    parser.add_argument('-c', '--config', dest='config', help='File with full info about tables')
    parser.add_argument('--proxy', dest='proxy', help='yt proxy')
    parser.add_argument('--format', '--input-type', dest='format', choices=['yamr', 'structured', 'md5'],
                        default='structured', help='Possible options: yamr, structured or md5')
    parser.add_argument('--sorted', action='store_true', default=False, help='Consider sorting (applies to all tables in list)')

    args = parser.parse_args()

    checker = TCheckerTable(args)
    crashed_tables = []
    for table in args.tables:
        if not checker.is_good(table):
            crashed_tables.append(table)

    if crashed_tables:
        raise Exception('Crashed tables: ' + str(crashed_tables))


if __name__ == '__main__':
    main()
