# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import UpdateMappingsStageConfig
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalink
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict


START_TS = int(datetime(2019, 10, 25).timestamp())

PERMALINKS = [
    StoragePermalink(
        id=uint(1),
        status_update_mappings=StageStatus.TO_BE_PROCESSED,
    ),
    StoragePermalink(
        id=uint(2),
        status_update_mappings=StageStatus.TO_BE_PROCESSED,
    ),
    StoragePermalink(
        id=uint(3),
        status_update_mappings=StageStatus.NOTHING_TO_DO,
    ),
]


def test_run(helper: Helper):
    trigger = helper.get_trigger(UpdateMappingsStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks=PERMALINKS)

    permalinks_table = helper.persistence_manager.join(trigger.stage_input_path, '0', 'permalinks')

    trigger.process()

    data = list(helper.persistence_manager.read(permalinks_table))

    # trigger data
    assert 2 == len(data)

    # permalink 1
    permalink = dc_from_dict(StoragePermalink, data[0])
    assert 1 == permalink.id
    assert StageStatus.TO_BE_PROCESSED == permalink.status_update_mappings

    # permalink 2
    permalink = dc_from_dict(StoragePermalink, data[1])
    assert 2 == permalink.id
    assert StageStatus.TO_BE_PROCESSED == permalink.status_update_mappings

    # storage data
    storage = helper.get_storage()

    assert 3 == len(storage.permalinks)

    # permalink 1
    permalink = storage.permalinks[uint(1)]
    assert StageStatus.IN_PROCESS == permalink.status_update_mappings
    assert START_TS == permalink.status_update_mappings_ts

    # permalink 2
    permalink = storage.permalinks[uint(2)]
    assert StageStatus.IN_PROCESS == permalink.status_update_mappings
    assert START_TS == permalink.status_update_mappings_ts

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_update_mappings
    assert 0 == permalink.status_update_mappings_ts
