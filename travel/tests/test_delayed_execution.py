# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import PartnerOffersStageConfig
from travel.hotels.content_manager.data_model.options import StageOptions
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageMapping, StoragePermalink, StorageUrl
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_to_dict, get_dc_yt_schema


START_TS = int(datetime(2019, 10, 25).timestamp())

TRIGGER_PERMALINKS = [
    StoragePermalink(
        id=uint(1),
        status_partner_offers=StageStatus.TO_BE_PROCESSED,
    ),
    StoragePermalink(
        id=uint(2),
        status_partner_offers=StageStatus.TO_BE_PROCESSED,
        status_partner_offers_ts=uint(START_TS - 5),
    ),
    StoragePermalink(
        id=uint(3),
        status_partner_offers=StageStatus.TO_BE_PROCESSED,
        status_partner_offers_ts=uint(START_TS + 5),
    ),
]

UPDATER_PERMALINKS = [
    StoragePermalink(
        id=uint(0),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(1),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
]

UPDATER_MAPPINGS = [
    StorageMapping(
        permalink=uint(0),
        operator_id='OI_0',
        orig_hotel_id='0',
        mapping_key='mapping_key_0',
        permaroom_id=None,
        orig_room_name='orig_room_name_0',
    ),
    StorageMapping(
        permalink=uint(1),
        operator_id='OI_1',
        orig_hotel_id='1',
        mapping_key='mapping_key_1',
        permaroom_id=None,
        orig_room_name='orig_room_name_1',
    ),
]


def test_trigger(helper: Helper):
    trigger = helper.get_trigger(PartnerOffersStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks=TRIGGER_PERMALINKS)

    trigger.process()

    # storage data
    storage = helper.get_storage()

    assert 3 == len(storage.permalinks)

    # permalink 1
    permalink = storage.permalinks[uint(1)]
    assert StageStatus.IN_PROCESS == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts

    # permalink 2
    permalink = storage.permalinks[uint(2)]
    assert StageStatus.IN_PROCESS == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    assert StageStatus.TO_BE_PROCESSED == permalink.status_partner_offers
    assert START_TS + 5 == permalink.status_partner_offers_ts


def test_updater(helper: Helper):
    options = {
        'yang_rooms': StageOptions(
            triggers=dict(),
            delay=5,
        )
    }
    updater = helper.get_updater(PartnerOffersStageConfig, start_ts=START_TS, options=options)
    helper.prepare_storage(permalinks=UPDATER_PERMALINKS)
    updater_args = helper.get_updater_args(PartnerOffersStageConfig.name)
    output_path, _ = updater_args
    permalinks_table = helper.persistence_manager.join(output_path, 'permalinks')
    mappings_table = helper.persistence_manager.join(output_path, 'mappings')
    urls_table = helper.persistence_manager.join(output_path, 'urls')

    permalinks = (dc_to_dict(p) for p in UPDATER_PERMALINKS)
    mappings = (dc_to_dict(m) for m in UPDATER_MAPPINGS)
    helper.persistence_manager.write(permalinks_table, permalinks, get_dc_yt_schema(StoragePermalink))
    helper.persistence_manager.write(mappings_table, mappings, get_dc_yt_schema(StorageMapping))
    helper.persistence_manager.write(urls_table, list(), get_dc_yt_schema(StorageUrl))

    updater.run(*updater_args)

    storage = helper.get_storage()

    assert 2 == len(storage.permalinks)

    permalink = storage.permalinks[uint(0)]
    assert 0 == permalink.id
    assert StageStatus.NOTHING_TO_DO == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts
    assert StageStatus.TO_BE_PROCESSED == permalink.status_yang_rooms
    assert START_TS + 5 == permalink.status_yang_rooms_ts

    permalink = storage.permalinks[uint(1)]
    assert 1 == permalink.id
    assert StageStatus.NOTHING_TO_DO == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts
    assert StageStatus.TO_BE_PROCESSED == permalink.status_yang_rooms
    assert START_TS + 5 == permalink.status_yang_rooms_ts
