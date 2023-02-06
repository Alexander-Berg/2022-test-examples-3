# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import TopPermalinksStageConfig
from travel.hotels.content_manager.data_model.stage import PopularHotelsData
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalink
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_to_dict, get_dc_yt_schema


START_TS = int(datetime(2019, 10, 25).timestamp())

PERMALINKS = [
    StoragePermalink(id=uint(1)),
    StoragePermalink(id=uint(2)),
]

PROCESSOR_RESULT = [
    PopularHotelsData(permalink=uint(3)),
    PopularHotelsData(permalink=uint(4)),
]


def test_run(helper: Helper):
    updater = helper.get_updater(TopPermalinksStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks=PERMALINKS)
    updater_args = helper.get_updater_args(TopPermalinksStageConfig.name)
    output_path, _ = updater_args
    permalinks_table = helper.persistence_manager.join(output_path, 'permalinks')

    data = [dc_to_dict(p) for p in PROCESSOR_RESULT]
    helper.persistence_manager.write(permalinks_table, data, get_dc_yt_schema(PopularHotelsData))

    updater.run(*updater_args)

    storage = helper.get_storage()

    permalink = storage.permalinks[uint(3)]
    assert StageStatus.TO_BE_PROCESSED == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts

    permalink = storage.permalinks[uint(4)]
    assert StageStatus.TO_BE_PROCESSED == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts
