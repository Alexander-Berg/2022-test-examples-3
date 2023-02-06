# coding: utf-8

import pytest
from hamcrest import assert_that

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.contract.Video_pb2 as Video
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.picrobot.proto.event_pb2 import TImageResponse, TVideoResponse, TOffer
from market.idx.datacamp.picrobot.proto.mds_info_pb2 import TMdsInfo, TMdsId, TImageSize, TVideoInfo
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data

BASIC_TABLE_DATA = [
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(0),
                        source=[
                            DTC.SourcePicture(url='https://original.url/'),
                            DTC.SourcePicture(url='next.url/'),
                            DTC.SourcePicture(url='newformat.url/'),
                            DTC.SourcePicture(url='https://error.url/'),
                            DTC.SourcePicture(url='https://existing.url/'),
                        ],
                    ),
                    actual={
                        'https://existing.url/': DTC.MarketPicture(
                            namespace='marketpic'
                        )
                    },
                    multi_actual={
                        'https://existing.url/': DTC.NamespacePictures(
                            by_namespace={
                                'marketpic': DTC.MarketPicture(
                                    namespace='marketpic'
                                )
                            }
                        )
                    }
                ),
                videos=DTC.Videos(
                    source=DTC.SourceVideos(
                        videos=[
                            Video.OriginalVideo(url='https://original.url/'),
                            Video.OriginalVideo(url='next.url/'),
                            Video.OriginalVideo(url='https://error.url/'),
                            Video.OriginalVideo(url='existing.url/'),
                        ]
                    ),
                    actual={
                        'http://existing.url/': DTC.ActualVideos(
                            by_namespace={
                                'direct': DTC.ActualVideo(
                                    video=Video.ProcessedVideo(
                                        id='6789',
                                        status=Video.ProcessedVideo.AVAILABLE,
                                        original_url='http://existing.url/'
                                    )
                                )
                            }
                        )
                    }
                )
            ),
        )
    ),
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        )
    )
    for offer_id in ('T1000',)
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        )
    )
    for offer_id in ('T1000',)
]


def create_image_response(original_url, offer_id, imagename):
    offer_identifiers = DTC.OfferIdentifiers(business_id=1, offer_id=offer_id)
    offer = DTC.Offer(identifiers=offer_identifiers)

    return TImageResponse(
        Url=original_url,
        MdsInfo=TMdsInfo(
            MdsId=TMdsId(
                Namespace='namespace',
                GroupId=2,
                ImageName=imagename,
            ),
            Width=400,
            Height=720,
            OrigAnimated=False,
            OrigFormat='JPEG',
            Sizes=[
            TImageSize(
                Path='//avatars.mds.yandex.net/get-namespace/2/{}/orig'.format(imagename if imagename.startswith('pic') else 'market_' + imagename),
                Width=600,
                Height=600,
                ContainerHeight=600,
                ContainerWidth=600,
            ),
            TImageSize(
                Width=600,
                Alias='orig-optimized',
                Height=600,
                ContainerHeight=600,
                ContainerWidth=600,
            )]
        ),
        Offer=TOffer(OfferId=offer_identifiers.SerializeToString(), Context=offer.SerializeToString()),
    )


def create_image_error_response(original_url, offer_id):
    offer_identifiers = DTC.OfferIdentifiers(business_id=1, offer_id=offer_id)
    offer = DTC.Offer(identifiers=offer_identifiers)

    return TImageResponse(
        Url=original_url,
        MdsInfo=TMdsInfo(
            MdsId=TMdsId(
                Namespace='namespace',
            )
        ),
        Offer=TOffer(OfferId=offer_identifiers.SerializeToString(), Context=offer.SerializeToString()),
    )


def create_video_response(original_url, offer_id, creative_id):
    offer_identifiers = DTC.OfferIdentifiers(business_id=1, offer_id=offer_id)
    offer = DTC.Offer(identifiers=offer_identifiers)

    return TVideoResponse(
        Url=original_url,
        VideoInfo=TVideoInfo(
            Namespace='direct',
            CreativeId=creative_id
        ),
        Offer=TOffer(OfferId=offer_identifiers.SerializeToString(), Context=offer.SerializeToString()),
    )


PICROBOT_IMAGE_RESPONSES = [
    # Проверяем, что картинка успешно вклеивается в оффер
    create_image_response('https://original.url/', 'T1000', 'imagename'),
    create_image_response('http://next.url/', 'T1000', 'imagename'),
    create_image_response('http://newformat.url/', 'T1000', 'picimage'),
    create_image_response('https://existing.url/', 'T1000', 'imagename'),
    # Проверяем, что ошибки скачивания картинок вклеиваются в оффер
    create_image_error_response('https://error.url/', 'T1000')
]


PICROBOT_VIDEO_RESPONSES = [
    create_video_response('https://original.url/', 'T1000', 1234),
    create_video_response('http://next.url/', 'T1000', 4321),
    create_video_response('https://error.url/', 'T1000', 0),
    create_video_response('http://existing.url/', 'T1000', 6789)
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=BASIC_TABLE_DATA)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=SERVICE_TABLE_DATA)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server, config.yt_actual_service_offers_tablepath, data=ACTUAL_SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='module')
def picrobot_response_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def picrobot_video_response_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def united_miner_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, picrobot_response_topic, picrobot_video_response_topic, united_miner_topic):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'picrobot': {
            'response_topic': picrobot_response_topic.topic,
            'video_response_topic': picrobot_video_response_topic.topic,
        },
        'miner': {
            'united_topic': united_miner_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    picrobot_response_topic,
    picrobot_video_response_topic,
    united_miner_topic,
    actual_service_offers_table,
    service_offers_table,
    basic_offers_table,
):
    resources = {
        'config': config,
        'picrobot_response_topic': picrobot_response_topic,
        'picrobot_video_response_topic': picrobot_video_response_topic,
        'united_miner_topic': united_miner_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_read_from_picrobot(piper, picrobot_response_topic, united_miner_topic):
    for response in PICROBOT_IMAGE_RESPONSES:
        picrobot_response_topic.write(response.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(PICROBOT_IMAGE_RESPONSES), timeout=60)

    def expected_actual_pic(imagename):
        return {
            'id': imagename,
            'namespace': 'namespace',
            'mds_host': 'avatars.mds.yandex.net',
            'group_id': '2',
            'thumbnails': [
                {
                    'alias': 'orig',
                    'width': 600,
                    'height': 600,
                    'containerHeight': 600,
                    'containerWidth': 600,
                },
                {
                    'alias': 'orig-optimized',
                    'width': 600,
                    'height': 600,
                    'containerHeight': 600,
                    'containerWidth': 600,
                }
            ],
            'original': {
                'url': '//avatars.mds.yandex.net/get-namespace/2/{}/orig'.format(imagename),
                'alias': 'orig',
                'width': 400,
                'height': 720,
                'containerHeight': 720,
                'containerWidth': 400,
            }
        }

    assert_that(piper.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'T1000',
            },
            'pictures': {
                'partner': {
                    'actual': {
                        'https://original.url/': expected_actual_pic('imagename'),
                        'next.url/': expected_actual_pic('imagename'),
                        'newformat.url/': expected_actual_pic('picimage'),
                        'https://error.url/': {'status': DTC.MarketPicture.Status.FAILED, 'namespace': 'namespace'},
                        'https://existing.url/': {
                            'namespace': 'marketpic'
                        }
                    },
                    'multi_actual': {
                        'https://original.url/': {
                            'by_namespace': {
                                'namespace': expected_actual_pic('imagename')
                            }
                        },
                        'next.url/': {
                            'by_namespace': {
                                'namespace': expected_actual_pic('imagename')
                            }
                        },
                        'newformat.url/': {
                            'by_namespace': {
                                'namespace': expected_actual_pic('picimage')
                            }
                        },
                        'https://error.url/': {
                            'by_namespace': {
                                'namespace': {'status': DTC.MarketPicture.Status.FAILED, 'namespace': 'namespace'}
                            }
                        },
                        'https://existing.url/': {
                            'by_namespace': {
                                'marketpic': {'namespace': 'marketpic'},
                                'namespace': expected_actual_pic('imagename')
                            }
                        }
                    }
                }
            }
        }, DTC.Offer())]))


def test_read_video_from_picrobot(piper, picrobot_video_response_topic, united_miner_topic):
    for response in PICROBOT_VIDEO_RESPONSES:
        picrobot_video_response_topic.write(response.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(PICROBOT_VIDEO_RESPONSES), timeout=60)

    def actual_video(url, id):
        return {
            'by_namespace': {
                'direct': {
                    'meta': {
                        'source': 'MARKET_IDX',
                        'applier': 'NEW_PICROBOT'
                    },
                    'video': {
                        'id': id,
                        'creative_id': int(id),
                        'status': Video.ProcessedVideo.AVAILABLE if id != '0' else None,
                        'original_url': url
                    }
                }
            }
        }

    assert_that(piper.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'T1000',
            },
            'pictures': {
                'videos': {
                    'actual': {
                        'https://error.url/': actual_video('https://error.url/', '0'),
                        'https://original.url/': actual_video('https://original.url/', '1234'),
                        'next.url/': actual_video('http://next.url/', '4321'),
                        'existing.url/': actual_video('http://existing.url/', '6789')
                    }
                }
            }
        }, DTC.Offer())]))
