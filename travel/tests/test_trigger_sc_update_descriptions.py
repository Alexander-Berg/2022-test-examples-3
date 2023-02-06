# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import SCUpdateDescriptionsStageConfig
from travel.hotels.content_manager.data_model.options import (
    NirvanaWorkflowOptions, StageOptions, TriggerOptions, TolokaPoolOptions
)
from travel.hotels.content_manager.data_model.stage import SCUpdateDescriptionsData
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageSCDescription
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict
from travel.hotels.content_manager.lib.storage import SCDescriptionKey


START_TS = int(datetime(2019, 10, 25).timestamp())

STORAGE_DESCRIPTIONS = [
    StorageSCDescription(
        carrier_code='carrier_code_0',
        car_type_code='car_type_code_0',
        sc_code='sc_code_0',
        sc_description='sc_description_0',
        status_sc_update_descriptions=StageStatus.NOTHING_TO_DO,
    ),
    StorageSCDescription(
        carrier_code='carrier_code_1',
        car_type_code='car_type_code_1',
        sc_code='sc_code_1',
        sc_description='sc_description_1',
        status_sc_update_descriptions=StageStatus.TO_BE_PROCESSED,
    ),
    StorageSCDescription(
        carrier_code='carrier_code_2',
        car_type_code='car_type_code_2',
        sc_code='sc_code_2',
        sc_description='',
        status_sc_update_descriptions=StageStatus.TO_BE_PROCESSED,
    ),
]


# noinspection PyUnusedLocal
def get_dict_data(*args):
    return {
        'carrier_code_0': {
            'enabled': True,
            'carrier_name': 'carrier_name_0',
            'country': 'country_0',
            'url': 'url_0',
        },
        'carrier_code_1': {
            'enabled': True,
            'carrier_name': 'carrier_name_1',
            'country': 'country_1',
            'url': 'url_1',
        },
        'carrier_code_2': {
            'enabled': True,
            'carrier_name': 'carrier_name_2',
            'country': 'country_2',
            'url': 'url_2',
        },
        'car_type_code_0': {
            'enabled': True,
            'car_type_name': 'car_type_name_0',
        },
        'car_type_code_1': {
            'enabled': True,
            'car_type_name': 'car_type_name_1',
        },
        'car_type_code_2': {
            'enabled': True,
            'car_type_name': 'car_type_name_2',
        },
    }


def test_run(helper: Helper):
    trigger_options = TriggerOptions(
        workflow_options=NirvanaWorkflowOptions(
            singleProjectId='0',
            singlePoolId='0',
        ),
        pool_options=TolokaPoolOptions(
            reward_per_assignment=0,
        ),
    )
    options = StageOptions(triggers={'default': trigger_options})
    trigger = helper.get_trigger(SCUpdateDescriptionsStageConfig, start_ts=START_TS, options=options)
    trigger.producer_cls.get_dict_data = get_dict_data
    helper.prepare_storage(sc_descriptions=STORAGE_DESCRIPTIONS)

    trigger.process()

    storage = helper.get_storage()

    assert 3 == len(storage.sc_descriptions)

    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_0', 'car_type_code_0', 'sc_code_0')]
    assert StageStatus.NOTHING_TO_DO == description.status_sc_update_descriptions
    assert 0 == description.status_sc_update_descriptions_ts

    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_1', 'car_type_code_1', 'sc_code_1')]
    assert StageStatus.IN_PROCESS == description.status_sc_update_descriptions
    assert START_TS == description.status_sc_update_descriptions_ts

    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_2', 'car_type_code_2', 'sc_code_2')]
    assert StageStatus.IN_PROCESS == description.status_sc_update_descriptions
    assert START_TS == description.status_sc_update_descriptions_ts

    # trigger data
    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '0', 'descriptions')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 2 == len(data)

    # description 1 info
    rec: SCUpdateDescriptionsData = dc_from_dict(SCUpdateDescriptionsData, data[0])

    assert 'carrier_code_1' == rec.input.carrier_code
    assert 'car_type_code_1' == rec.input.car_type_code
    assert 'sc_code_1' == rec.input.sc_code
    assert 'country_1' == rec.input.country
    assert 'url_1' == rec.input.url
    assert 'carrier_name_1' == rec.input.carrier_name
    assert 'car_type_name_1' == rec.input.car_type_name
    assert '' == rec.input.sc_name
    assert 'sc_description_1' == rec.input.sc_description

    # description 2 info
    rec: SCUpdateDescriptionsData = dc_from_dict(SCUpdateDescriptionsData, data[1])

    assert 'carrier_code_2' == rec.input.carrier_code
    assert 'car_type_code_2' == rec.input.car_type_code
    assert 'sc_code_2' == rec.input.sc_code
    assert 'country_2' == rec.input.country
    assert 'url_2' == rec.input.url
    assert 'carrier_name_2' == rec.input.carrier_name
    assert 'car_type_name_2' == rec.input.car_type_name
    assert '' == rec.input.sc_name
    assert '' == rec.input.sc_description

    # workflow options
    options_path = helper.persistence_manager.join(trigger_data_path, '@_workflow_options')
    workflow_options = helper.persistence_manager.get(options_path)

    assert dc_to_dict(options.triggers['default'].workflow_options) == workflow_options

    # pool options
    options_path = helper.persistence_manager.join(trigger_data_path, '@_pool_options')
    pool_options = helper.persistence_manager.get(options_path)

    exp_pool_options = dc_to_dict(options.triggers['default'].pool_options)
    exp_pool_options['private_name'] = '2019-10-25 03:00:00+03:00 [sc_update_descriptions]  0'
    assert exp_pool_options == pool_options
