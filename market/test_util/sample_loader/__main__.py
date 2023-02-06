# coding=utf-8

import logging

from yt.wrapper.client import YtClient

import market.replenishment.algorithms.lib23.chyt as chyt
from market.replenishment.algorithms.lib23.yt_config import YTConfig

from market.replenishment.algorithms.legacy.library.data_set import ChDataSetBuilder, DataSetBuilderConfig
from market.replenishment.algorithms.legacy.library.table_def import ChTableDef


def main():
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(name)-12s %(levelname)-8s %(message)s')

    yt_config = YTConfig()
    chyt_config = chyt.ChytConfig(yt_config, "*ch_market_replenishment")
    yt_client = YtClient(yt_config.proxy, token=yt_config.token)

    data_set_builder_config = DataSetBuilderConfig(yt_config, chyt_config, '//home/market/development/replenishment/tmp', [], yt_client)
    builder = ChDataSetBuilder(data_set_builder_config)

    warehouses = builder.create_data_set(ChTableDef(
        'warehouses',
        'SELECT * FROM "//home/market/production/replenishment/order_planning/{today}/inputs/warehouses" WHERE id in (145, 147)'))
    warehouses.to_csv('warehouses.csv', index=False, sep='\t', encoding = 'utf-8')

    suppliers = builder.create_data_set(ChTableDef(
        'suppliers',
        'SELECT * FROM "//home/market/production/replenishment/order_planning/{today}/inputs/suppliers" WHERE id in (145, 147)'))
    suppliers.to_csv('market/replenishment/algorithms/vicugna/ut/test-data/suppliers.csv', index=False, sep='\t', encoding = 'utf-8')


if __name__ == '__main__':
    main()
