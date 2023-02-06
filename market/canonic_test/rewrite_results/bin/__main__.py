import re
import argparse
import logging
import sys

from market.dynamic_pricing.pricing.common.canonic_test.rewrite_results.rewrite_results import copy_tables, remove_tables


def parse_args():
    parser = argparse.ArgumentParser(description='Prepare data for dynamic pricing pipeline')
    yt_group = parser.add_argument_group('YT')
    yt_group.add_argument('--cluster',
                          default='hahn')
    yt_group.add_argument('--copy-tables',
                          default='//home/market/production/monetize/dynamic_pricing/output/prices/2020-05-21T08:00:00;',
                          help='Tables to copy, separated by ";" or enter')
    yt_group.add_argument('--copy-prefix',
                          default="//home/market/production")
    yt_group.add_argument('--copy-destination-dir',
                          default='//home/market/testing/monetize/dynamic_pricing/canonic_test_2/prod_copy')
    yt_group.add_argument('--remove-tables',
                          default='',
                          help='Tables to remove, separated by ";" or enter')

    return parser.parse_args()


if __name__ == '__main__':
    logging.basicConfig(
        stream=sys.stdout,
        level=logging.INFO,
        format='%(levelname)s %(asctime)s %(message)s')

    args = vars(parse_args())
    tables = re.split(';|\r\n|\n', args.get('copy_tables'))
    copy_tables(tables, args.get('copy_destination_dir'), args.get('cluster'), args.get('copy_prefix'))

    tables = re.split(';|\r\n|\n', args.get('remove_tables'))
    if tables:
        remove_tables(tables, args.get('cluster'))
