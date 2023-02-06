# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import PartnerOffersStageConfig
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalink
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict


START_TS = int(datetime(2019, 10, 25).timestamp())

PERMALINKS = [
    StoragePermalink(
        id=uint(1),
        status_partner_offers=StageStatus.NOTHING_TO_DO,
    ),
    StoragePermalink(
        id=uint(2),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(3),
        status_partner_offers=StageStatus.TO_BE_PROCESSED,
    ),
]


def test_run(helper: Helper):
    trigger = helper.get_trigger(PartnerOffersStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks=PERMALINKS)

    permalinks_table = helper.persistence_manager.join(trigger.stage_input_path, '0', 'permalinks')

    trigger.process()

    # trigger data
    data = list(helper.persistence_manager.read(permalinks_table))

    assert 1 == len(data)

    permalink = dc_from_dict(StoragePermalink, data[0])
    assert StageStatus.TO_BE_PROCESSED == permalink.status_partner_offers

    # storage data
    storage = helper.get_storage()

    assert 3 == len(storage.permalinks)

    # permalink 1
    permalink = storage.permalinks[uint(1)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_partner_offers
    assert 0 == permalink.status_partner_offers_ts

    # permalink 2
    permalink = storage.permalinks[uint(2)]
    assert StageStatus.IN_PROCESS == permalink.status_partner_offers
    assert 0 == permalink.status_partner_offers_ts

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    assert StageStatus.IN_PROCESS == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts
