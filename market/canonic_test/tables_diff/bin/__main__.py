import argparse
import logging
import sys
from market.dynamic_pricing.pricing.common.canonic_test.tables_diff.tables_diff import TablesDiff
from market.dynamic_pricing.pricing.library.utils import str2bool


def parse_args():
    parser = argparse.ArgumentParser(description='Prepare data for dynamic pricing pipeline')
    yt_group = parser.add_argument_group('YT')
    yt_group.add_argument('--cluster',
                          default='hahn')
    yt_group.add_argument('--pool', default=None, help='YT pool')
    yt_group.add_argument('--expected-table',
                          default='//home/market/testing/monetize/dynamic_pricing/expiring_goods/expiring_assortment/expected/2021-03-31T17:37:48')
    yt_group.add_argument('--actual-table',
                          default='//home/market/testing/monetize/dynamic_pricing/expiring_goods/expiring_assortment/actual/2021-03-31T17:37:48')
    yt_group.add_argument('--production-table',
                          default='')
    yt_group.add_argument('--key-columns',
                          default='market_sku;shop_sku;warehouse_id')
    yt_group.add_argument('--skip-columns',
                          default='lots_info')
    yt_group.add_argument('--diff-path',
                          default='//home/market/testing/monetize/dynamic_pricing/canonic_test_2/diff')
    yt_group.add_argument('--diff-eps',
                          default="0.01")
    yt_group.add_argument('--exception-on-diff',
                          type=str2bool,
                          default="true")

    return parser.parse_args()


if __name__ == '__main__':
    logging.basicConfig(
        stream=sys.stdout,
        level=logging.INFO,
        format='%(levelname)s %(asctime)s %(message)s')
    args = vars(parse_args())
    tableDiff = TablesDiff(args.get('cluster'), args.get('pool'))

    production_table = args.get('production_table', '')
    expected_table = args.get('expected_table')
    if production_table:
        tableDiff.copy_from_prod(production_table, expected_table)

    key_columns = frozenset(args.get('key_columns').split(';'))
    skip_columns = frozenset(args.get('skip_columns').split(';'))
    tableDiff.compare(
        expected_table,
        args.get('actual_table'),
        args.get('diff_path'),
        key_columns,
        skip_columns,
        args.get('diff_eps'),
        args.get('exception_on_diff')
    )
