#!/usr/bin/python
# -*- coding: utf-8 -*-

import sys
import optparse


def filter_text(filepath, fields):
    if fields:
        fields = fields.split(',')

    content_lines = open(filepath, 'r').readlines()
    for line in content_lines:
        if fields:
            for field in fields:
                if line.startswith(field):
                    sys.stdout.write(line)
        else:
            sys.stdout.write(line)


def main():
    parser = optparse.OptionParser(usage='usage: %prog pbuf_sn_file')
    parser.add_option('--fields', dest='fields', help='print only those fields',
                      type='string', default=None)

    (options, args) = parser.parse_args()
    if 1 != len(args):
        raise Exception('unsupported')

    return filter_text(filepath=args[0], fields=options.fields)

if '__main__' == __name__:
    sys.exit(main())
