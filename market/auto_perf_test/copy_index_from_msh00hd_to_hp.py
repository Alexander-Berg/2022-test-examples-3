#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

from lock import *
from remote import *


def main():
    arg_parser = argparse.ArgumentParser(description='Run this on msh00hd to copy production index to HP cluster.')
    arg_parser.add_argument('--cluster', '-c', type=int, choices=ALLOWED_HP_CLUSTERS, default=ALLOWED_HP_CLUSTERS[0], help='Zero-based index of HP cluster')
    arg_parser.add_argument('--generation', '-g', required=True, help='Index generation number')
    args = arg_parser.parse_args()

    print '>>> Acquiring report lock for HP'
    report_lock = create_report_lock(args.cluster)
    report_lock.acquire()
    try:
        execute_command_on_hp_in_parallel(
            args.cluster, 'mkdir -p {0}'.format(os.path.join(get_remote_temp_dir(), args.generation)))
        execute_command_on_host(
            get_hp_snippet_host_name(args.cluster),
            'mkdir -p {0}'.format(get_remote_temp_dir()))

        for index in range(CLUSTER_SIZE):
            source_list = [
                'book-part-{0}'.format(index),
                'model-part-{0}'.format(index),
                'search-cards',
                'search-part-{0}'.format(index),
                'search-part-{0}'.format(index + 8),
                'search-report-data',
                'search-stats',
                'search-wizard'
            ]
            copy_files_to_host(
                get_hp_host_name(args.cluster, index),
                [os.path.join('/var/lib/search/marketsearch', args.generation, src) for src in source_list],
                os.path.join(get_remote_temp_dir(), args.generation))

        copy_files_to_host(
            get_hp_snippet_host_name(args.cluster),
            os.path.join('/var/lib/search/snippet_index/download', args.generation),
            get_remote_temp_dir())

        execute_command_on_hp_in_parallel(
            args.cluster,
            'sudo cp -a {0} /var/lib/search/marketsearch'.format(os.path.join(get_remote_temp_dir(), args.generation)))

        execute_command_on_host(
            get_hp_snippet_host_name(args.cluster),
            'sudo cp -a {0} /var/lib/search/snippet_index/download'.format(os.path.join(get_remote_temp_dir(), args.generation)))

        execute_command_on_hp_in_parallel(
            args.cluster, 'rm -rf {0}'.format(get_remote_temp_dir()))
        execute_command_on_host(
            get_hp_snippet_host_name(args.cluster),
            'rm -rf {0}'.format(get_remote_temp_dir()))
    finally:
        print '>>> Releasing report lock for HP'
        report_lock.release()

if __name__ == "__main__":
    main()
