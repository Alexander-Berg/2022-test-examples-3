# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import YangRoomsStageConfig
from travel.hotels.content_manager.data_model.options import NirvanaWorkflowOptions, StageOptions, TriggerOptions
from travel.hotels.content_manager.data_model.stage import PermalinkOffer, YangRoomsData
from travel.hotels.content_manager.data_model.storage import (
    StageStatus, StorageMapping, StoragePermalink, StoragePermaroom, StorageUrl
)
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict
from travel.hotels.content_manager.lib.storage import UrlKey


START_TS = int(datetime(2019, 10, 25).timestamp())

PERMALINKS = [
    StoragePermalink(
        id=uint(3),
        hotel_url='hotel_url_3',
        status_yang_rooms=StageStatus.TO_BE_PROCESSED,
    ),
    StoragePermalink(
        id=uint(4),
        hotel_url='',
        status_yang_rooms=StageStatus.TO_BE_PROCESSED,
    ),
    # permalink with no offers
    StoragePermalink(
        id=uint(5),
        hotel_url='',
        status_yang_rooms=StageStatus.NOTHING_TO_DO,
    ),
]

PERMAROOMS = [
    StoragePermaroom(
        id=0,
        permalink=uint(4),
        name='permaroom_name_0',
        comment='permaroom_comment_0',
        alternative_names='alternative_names_0',
    ),
    StoragePermaroom(
        id=2,
        permalink=uint(4),
        name='permaroom_name_2',
        comment='permaroom_comment_2',
        alternative_names='alternative_names_2',
    ),
    StoragePermaroom(
        id=3,
        permalink=uint(4),
        name='permaroom_name_3',
        comment='permaroom_comment_3',
        alternative_names='alternative_names_3',
        is_deleted=True,
    ),
]

MAPPINGS = [
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_1',
        orig_hotel_id='1',
        mapping_key='mapping_key_1',
        permaroom_id=None,
        orig_room_name='orig_room_name_1',
        url='offer_url_1',
        comment='offer_comment_1',
        need_new_permaroom=True,
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_2',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=2,
        orig_room_name='orig_room_name_2',
        url='offer_url_2',
        comment='offer_comment_2',
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_3',
        orig_hotel_id='3',
        mapping_key='mapping_key_3',
        permaroom_id=None,
        orig_room_name='orig_room_name_3',
        url='offer_url_3',
        comment='offer_comment_3',
        need_new_permaroom=True,
    ),
]

URLS = [
    StorageUrl(
        permalink=uint(3),
        provider='provider_0',
        url='provider_0_url_0',
    ),
    StorageUrl(
        permalink=uint(3),
        provider='provider_0',
        url='provider_0_url_1',
    ),
    StorageUrl(
        permalink=uint(3),
        provider='provider_1',
        url='provider_1_url_0',
    ),
    StorageUrl(
        permalink=uint(3),
        provider='provider_1',
        url='provider_1_url_1',
    ),
]


def test_run(helper: Helper):
    trigger_options = {
        'create_structure': TriggerOptions(
            workflow_options=NirvanaWorkflowOptions(
                singleProjectId='5',
                singlePoolId='1',
            ),
        ),
        'edit_structure': TriggerOptions(
            workflow_options=NirvanaWorkflowOptions(
                singleProjectId='5',
                singlePoolId='2',
            ),
        ),
    }
    options = StageOptions(triggers=trigger_options)

    trigger = helper.get_trigger(YangRoomsStageConfig, start_ts=START_TS, options=options)
    helper.prepare_storage(permalinks=PERMALINKS, permarooms=PERMAROOMS, mappings=MAPPINGS, urls=URLS)

    trigger.process()

    storage = helper.get_storage()

    assert 3 == len(storage.permalinks)

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    assert StageStatus.IN_PROCESS == permalink.status_yang_rooms
    assert START_TS == permalink.status_yang_rooms_ts

    # permalink 3 mappings
    assert 0 == len(storage.get_permalink_permarooms(permalink))
    assert 1 == len(storage.get_permalink_other_mappings(permalink))

    # permalink 3 urls
    assert 'hotel_url_3' == permalink.hotel_url
    assert StageStatus.IN_PROCESS == permalink.status_yang_rooms

    permalink_urls = storage.get_permalink_urls(permalink)
    assert 2 == len(permalink_urls)

    url = permalink_urls[UrlKey(uint(3), 'provider_0')]
    assert 'provider_0_url_0' == url[0].url
    assert 'provider_0_url_1' == url[1].url

    url = permalink_urls[UrlKey(uint(3), 'provider_1')]
    assert 'provider_1_url_0' == url[0].url
    assert 'provider_1_url_1' == url[1].url

    # permalink 4
    permalink = storage.permalinks[uint(4)]
    assert StageStatus.IN_PROCESS == permalink.status_yang_rooms
    assert START_TS == permalink.status_yang_rooms_ts

    # permalink 4 mappings
    assert 3 == len(storage.get_permalink_permarooms(permalink))
    assert 1 == len(storage.get_permalink_other_mappings(permalink))

    # permalink 4 urls
    permalink_urls = storage.get_permalink_urls(permalink)
    assert 0 == len(permalink_urls)

    # permalink 5
    permalink = storage.permalinks[uint(5)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_yang_rooms
    assert 0 == permalink.status_yang_rooms_ts

    # permalink 5 mappings
    assert 0 == len(storage.get_permalink_permarooms(permalink))
    assert 0 == len(storage.get_permalink_other_mappings(permalink))

    # permalink 5 urls
    permalink_urls = storage.get_permalink_urls(permalink)
    assert 0 == len(permalink_urls)

    # trigger data - create_structure
    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '0', 'yang_rooms')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 1 == len(data)

    # permalink 3 info
    rec: YangRoomsData = dc_from_dict(YangRoomsData, data[0])

    exp_offers = [
        PermalinkOffer(
            operator_id='OI_1',
            orig_hotel_id='1',
            mapping_key='mapping_key_1',
            orig_room_name='orig_room_name_1',
            _price='0',
            _prices_per_night='',
            _count='0',
            _params='',
            _partner_url='offer_url_1',
            _requests_range='',
        ),
    ]

    assert '3' == rec.input.permalink
    assert 'https://altay.yandex-team.ru/cards/perm/3' == rec.input.altay_url
    assert 'hotel_url_3' == rec.input.hotel_url
    exp_urls = [
        {'provider_0': 'provider_0_url_0'},
        {'provider_0': 'provider_0_url_1'},
        {'provider_1': 'provider_1_url_0'},
        {'provider_1': 'provider_1_url_1'},
    ]
    assert exp_urls == rec.input.partner_urls

    assert 1 == len(rec.input.permalink_offers)
    assert exp_offers[0] == rec.input.permalink_offers[0]

    assert 1 == len(rec.input.offer_info)
    offer_info = rec.input.offer_info[0]
    assert 'orig_room_name_1' == offer_info.offer_room_name
    assert 'offer_comment_1' == offer_info.offer_comment
    assert 'offer_url_1' == offer_info.partner_offer_url

    assert 0 == len(rec.input.available_permarooms)

    options_path = helper.persistence_manager.join(trigger_data_path, '@_workflow_options')
    workflow_options = helper.persistence_manager.get(options_path)

    assert dc_to_dict(options.triggers['create_structure'].workflow_options) == workflow_options

    # trigger data - edit_structure
    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '1', 'yang_rooms')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 1 == len(data)

    # permalink 4 info
    rec: YangRoomsData = dc_from_dict(YangRoomsData, data[0])

    exp_offers = [
        PermalinkOffer(
            operator_id='OI_2',
            orig_hotel_id='2',
            mapping_key='mapping_key_2',
            orig_room_name='orig_room_name_2',
            _price='0',
            _prices_per_night='',
            _count='0',
            _params='',
            _partner_url='offer_url_2',
            _requests_range='',
        ),
        PermalinkOffer(
            operator_id='OI_3',
            orig_hotel_id='3',
            mapping_key='mapping_key_3',
            orig_room_name='orig_room_name_3',
            _price='0',
            _prices_per_night='',
            _count='0',
            _params='',
            _partner_url='offer_url_3',
            _requests_range='',
        ),
    ]

    assert '4' == rec.input.permalink
    assert 'https://altay.yandex-team.ru/cards/perm/4' == rec.input.altay_url
    assert 'https://yandex.ru/search/?text=4' == rec.input.hotel_url
    assert [] == rec.input.partner_urls

    assert 2 == len(rec.input.permalink_offers)
    assert exp_offers[0] == rec.input.permalink_offers[0]
    assert exp_offers[1] == rec.input.permalink_offers[1]

    assert 1 == len(rec.input.offer_info)
    offer_info = rec.input.offer_info[0]
    assert 'orig_room_name_3' == offer_info.offer_room_name
    assert 'offer_comment_3' == offer_info.offer_comment
    assert 'offer_url_3' == offer_info.partner_offer_url

    assert 2 == len(rec.input.available_permarooms)
    permaroom = rec.input.available_permarooms[0]
    assert '0' == permaroom.permaroom_id
    assert 'permaroom_name_0' == permaroom.permaroom_name
    assert 'alternative_names_0' == permaroom.alternative_names
    assert 'permaroom_comment_0' == permaroom.permaroom_comment

    permaroom = rec.input.available_permarooms[1]
    assert '2' == permaroom.permaroom_id
    assert 'permaroom_name_2' == permaroom.permaroom_name
    assert 'alternative_names_2' == permaroom.alternative_names
    assert 'permaroom_comment_2' == permaroom.permaroom_comment

    options_path = helper.persistence_manager.join(trigger_data_path, '@_workflow_options')
    workflow_options = helper.persistence_manager.get(options_path)

    assert dc_to_dict(options.triggers['edit_structure'].workflow_options) == workflow_options
