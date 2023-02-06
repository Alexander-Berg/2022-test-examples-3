import argparse
import datetime
from market.dynamic_pricing.deprecated.tests.lib.pipeline_test import YtClient

def parse_args():
    parser = argparse.ArgumentParser(description='Script to verify autostrategy pipeline')
    default_base_yt_dir = '//tmp/postoev/testing/monetize/dynamic_pricing'

    yt_group = parser.add_argument_group('YT')
    yt_group.add_argument('-c', '--cluster',
                          default='hahn',
                          help='YT cluster with data')
    yt_group.add_argument('--stats-path',
                          default=default_base_yt_dir + '/autostrategy_stats_raw',
                          help='YT table autostrategy raw stats per sku')
    yt_group.add_argument('--bounds-path',
                          default=default_base_yt_dir + '/autostrategy_stats_per_sku',
                          help='YT table autostrategy bounds per sku')
    yt_group.add_argument('--sku-price-path',
                          default=default_base_yt_dir + '/autostrategy_current_prices/sku_price',
                          help='YT table sku price path')
    yt_group.add_argument('--prices-path',
                          default=default_base_yt_dir + '/output/prices',
                          help='YT table with new prices')
    yt_group.add_argument('--margins-path',
                          default=default_base_yt_dir + '/output/margins',
                          help='YT table with margins')
    yt_group.add_argument('--check-path',
                          default=default_base_yt_dir + '/output/check',
                          help='YT table with price check results')
    yt_group.add_argument('--result-path',
                          default=default_base_yt_dir + '/filter_data_for_axapta',
                          help='YT table with prices for AXAPTA')
    yt_group.add_argument('--config-path',
                          default=default_base_yt_dir + '/config',
                          help='YT table with autostrategy config')
    yt_group.add_argument('--groups-path',
                          default=default_base_yt_dir + '/groups',
                          help='YT table with autostrategy groups')
    yt_group.add_argument('--exp-plan-path',
                          default=default_base_yt_dir + '/experiment_plan',
                          help='YT table with experiment plan')
    yt_group.add_argument('--diff-path',
                          default=default_base_yt_dir + '/diff',
                          help='YT table with test diff')

    runtime_group = parser.add_argument_group('Runtime')
    runtime_group.add_argument('--date',
                              default=datetime.datetime.today().strftime('%Y-%m-%d'),
                              help='Date')
    runtime_group.add_argument('--interval',
                              type=int, default=1,
                              help='Check tables interval (seconds)')
    runtime_group.add_argument('--timeout',
                              type=int, default=20,
                              help='Maximum wait time (seconds)')
    return parser.parse_args()


if __name__ == '__main__':
    args = parse_args()

    checked_paths = [
        getattr(args, 'stats_path'),
        getattr(args, 'bounds_path'),
        getattr(args, 'sku_price_path'),
        getattr(args, 'prices_path'),
        getattr(args, 'margins_path'),
        getattr(args, 'check_path'),
        getattr(args, 'result_path'),
    ]
    # verify this date
    date = getattr(args, 'date')

    yt_client = YtClient(
        cluster=getattr(args, 'cluster'),
        date=date
    )
    # remove previous results
    yt_client.cleanup(checked_paths)

    # copy yesterday production tables
    prod_yt_dir = '//home/market/production/monetize/dynamic_pricing'
    yt_client.copy(prod_yt_dir + '/config', getattr(args, 'config_path'))
    yt_client.copy(prod_yt_dir + '/groups', getattr(args, 'groups_path'))
    yt_client.copy(prod_yt_dir + '/experiment_plan', getattr(args, 'exp_plan_path'))
    yt_client.copy(prod_yt_dir + '/autostrategy_current_prices/sku_price', getattr(args, 'sku_price_path'))

    # wait for new ones
    yt_client.check_tables_exist(
        checked_paths,
        getattr(args, 'interval'),
        getattr(args, 'timeout')
    )
    print ('Tables created')
    # compare test and etalon
    diff_table_path = getattr(args, 'diff_path')
    result = yt_client.compare(
        expected_path=prod_yt_dir + '/filter_data_for_axapta',
        result_path=getattr(args, 'result_path'),
        diff_path=diff_table_path
    )
    print ('Test successfully finished')

