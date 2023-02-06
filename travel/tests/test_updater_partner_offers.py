# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import PartnerOffersStageConfig
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageMapping, StoragePermalink, StorageUrl
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_to_dict, get_dc_yt_schema
from travel.hotels.content_manager.lib.storage import UrlKey


START_TS = int(datetime(2019, 10, 25).timestamp())

STORAGE_PERMALINKS = [
    StoragePermalink(
        id=uint(1),
    ),
    StoragePermalink(
        id=uint(2),
    ),
    StoragePermalink(
        id=uint(3),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(4),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
    # permalink with no offers
    StoragePermalink(
        id=uint(5),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
]

STAGE_PERMALINKS = [
    StoragePermalink(
        id=uint(3),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(4),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(5),
        status_partner_offers=StageStatus.IN_PROCESS,
    ),
]

MAPPINGS = [
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_0',
        orig_hotel_id='0',
        mapping_key='mapping_key_0',
        permaroom_id=None,
        orig_room_name='orig_room_name_0',
    ),
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_1',
        orig_hotel_id='1',
        mapping_key='mapping_key_1',
        permaroom_id=None,
        orig_room_name='orig_room_name_1',
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_2',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=None,
        orig_room_name='orig_room_name_2',
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_3',
        orig_hotel_id='3',
        mapping_key='mapping_key_3',
        permaroom_id=None,
        orig_room_name='orig_room_name_3',
    ),
]

URLS = [
    StorageUrl(
        permalink=uint(3),
        provider='main',
        url='hotel_url_3',
    ),
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
]


def test_run(helper: Helper):
    updater = helper.get_updater(PartnerOffersStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks=STAGE_PERMALINKS)
    updater_args = helper.get_updater_args(PartnerOffersStageConfig.name)
    output_path, _ = updater_args
    permalinks_table = helper.persistence_manager.join(output_path, 'permalinks')
    mappings_table = helper.persistence_manager.join(output_path, 'mappings')
    urls_table = helper.persistence_manager.join(output_path, 'urls')

    permalinks = (dc_to_dict(p) for p in STAGE_PERMALINKS)
    mappings = (dc_to_dict(m) for m in MAPPINGS)
    urls = (dc_to_dict(u) for u in URLS)
    helper.persistence_manager.write(permalinks_table, permalinks, get_dc_yt_schema(StoragePermalink))
    helper.persistence_manager.write(mappings_table, mappings, get_dc_yt_schema(StorageMapping))
    helper.persistence_manager.write(urls_table, urls, get_dc_yt_schema(StorageUrl))

    updater.run(*updater_args)

    storage = helper.get_storage()

    assert 3 == len(storage.permalinks)

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts
    assert StageStatus.TO_BE_PROCESSED == permalink.status_yang_rooms
    assert START_TS == permalink.status_yang_rooms_ts

    # permalink 3 mappings
    assert 2 == len(storage.get_permalink_other_mappings(permalink))

    # permalink 3 urls
    assert 'hotel_url_3' == permalink.hotel_url

    permalink_urls = storage.get_permalink_urls(permalink)
    assert 1 == len(permalink_urls)

    url = permalink_urls[UrlKey(uint(3), 'provider_0')]
    assert 'provider_0_url_0' == url[0].url
    assert 'provider_0_url_1' == url[1].url

    # permalink 4
    permalink = storage.permalinks[uint(4)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts
    assert StageStatus.TO_BE_PROCESSED == permalink.status_yang_rooms
    assert START_TS == permalink.status_yang_rooms_ts

    # permalink 4 mappings
    assert 2 == len(storage.get_permalink_other_mappings(permalink))

    # permalink 4 urls
    assert '' == permalink.hotel_url

    permalink_urls = storage.get_permalink_urls(permalink)
    assert 0 == len(permalink_urls)

    # permalink 5
    permalink = storage.permalinks[uint(5)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_partner_offers
    assert START_TS == permalink.status_partner_offers_ts
    assert StageStatus.NOTHING_TO_DO == permalink.status_yang_rooms
    assert 0 == permalink.status_yang_rooms_ts

    # permalink 5 mappings
    assert 0 == len(storage.get_permalink_other_mappings(permalink))

    # permalink 5 urls
    assert '' == permalink.hotel_url

    permalink_urls = storage.get_permalink_urls(permalink)
    assert 0 == len(permalink_urls)
