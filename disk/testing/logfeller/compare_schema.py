#!/usr/bin/python
# -*- coding: utf-8 -*-
import argparse
import json
import sys
import yaml


def main():
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("-l",
                        "--logfeller-config",
                        default="",
                        help="Logfeller parser config path")
    parser.add_argument("-m",
                        "--mpfs-config",
                        default='../../../apps/common/conf/mpfs/global_settings.yaml',
                        help="MPFS config path")
    parser.add_argument("-t",
                        "--log-type",
                        default="tskv-requests",
                        help="MPFS log type")

    args = parser.parse_args()
    mpfs_logschemas = yaml.load(open(args.mpfs_config), Loader=yaml.SafeLoader)['logger']['dict_config']['formatters']
    logfeller_schema = json.load(open(args.logfeller_config))
    if args.log_type not in mpfs_logschemas:
        sys.exit(2)
    mpfs_log_fields = set(i.split('=')[0] for i in mpfs_logschemas[args.log_type]['format'].split('\t')[1:])
    logfeller_fields = set(i['name'] for i in logfeller_schema['fields'])
    if mpfs_log_fields ^ logfeller_fields:
        print("%s != \n%s" % (sorted(mpfs_log_fields), sorted(logfeller_fields)))
        sys.exit(1)
    else:
        sys.exit(0)


if __name__ == '__main__':
    main()
