#!/usr/bin/python
# coding: utf-8

import os
import sys
import logging


log = logging.getLogger('converters_stub')


def touch(path):
    log.debug('STUB::touch %s', path)
    dirname = os.path.dirname(path)
    if dirname and not os.path.exists(dirname):
        os.makedirs(dirname)
    with open(path, 'w'):
        pass


def run(args):
    map(touch, (a for a in args if not a.startswith('-')))


if __name__ == '__main__':
    run(sys.argv[1:])
