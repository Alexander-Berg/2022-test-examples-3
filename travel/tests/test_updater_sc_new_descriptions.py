# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import SCNewDescriptionsStageConfig
from travel.hotels.content_manager.data_model.options import StageOptions
from travel.hotels.content_manager.data_model.stage import SCNewDescriptionData
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageSCDescription
from travel.hotels.content_manager.data_model.types import SCDescriptionResult
from travel.hotels.content_manager.lib.common import dc_to_dict, get_dc_yt_schema
from travel.hotels.content_manager.lib.storage import SCDescriptionKey


START_TS = int(datetime(2019, 10, 25).timestamp())

STORAGE_DESCRIPTIONS = [
    StorageSCDescription(
        carrier_code='carrier_code_0',
        car_type_code='car_type_code_0',
        sc_code='sc_code_0',
        sc_description='sc_description_0',
        sc_description_result=SCDescriptionResult.UPDATED
    ),
]

STAGE_PERMALINKS = [
    SCNewDescriptionData(
        carrier_code='carrier_code_0',
        car_type_code='car_type_code_0',
        sc_code='sc_code_0',
    ),
    SCNewDescriptionData(
        carrier_code='carrier_code_1',
        car_type_code='car_type_code_1',
        sc_code='sc_code_1',
    ),
]


def test_run(helper: Helper):
    options = {
        SCNewDescriptionsStageConfig.name: StageOptions(
            triggers=dict(),
            delay=5,
        )
    }

    updater = helper.get_updater(SCNewDescriptionsStageConfig, start_ts=START_TS, options=options)
    updater_args = helper.get_updater_args(SCNewDescriptionsStageConfig.name)
    output_path, _ = updater_args
    descriptions_table = helper.persistence_manager.join(output_path, 'descriptions')

    descriptions = (dc_to_dict(p) for p in STAGE_PERMALINKS)
    helper.persistence_manager.write(descriptions_table, descriptions, get_dc_yt_schema(SCNewDescriptionData))

    helper.prepare_storage(sc_descriptions=STORAGE_DESCRIPTIONS)

    updater.run(*updater_args)

    storage = helper.get_storage()

    assert 2 == len(storage.sc_descriptions)

    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_0', 'car_type_code_0', 'sc_code_0')]
    assert SCDescriptionResult.UPDATED == description.sc_description_result
    assert 'carrier_code_0' == description.carrier_code
    assert 'car_type_code_0' == description.car_type_code
    assert 'sc_code_0' == description.sc_code
    assert 'sc_description_0' == description.sc_description
    assert StageStatus.TO_BE_PROCESSED == description.status_sc_update_descriptions
    assert START_TS == description.status_sc_update_descriptions_ts

    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_1', 'car_type_code_1', 'sc_code_1')]
    assert SCDescriptionResult.UNKNOWN == description.sc_description_result
    assert 'carrier_code_1' == description.carrier_code
    assert 'car_type_code_1' == description.car_type_code
    assert 'sc_code_1' == description.sc_code
    assert '' == description.sc_description
    assert StageStatus.TO_BE_PROCESSED == description.status_sc_update_descriptions
    assert START_TS == description.status_sc_update_descriptions_ts
