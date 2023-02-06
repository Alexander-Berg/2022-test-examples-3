# coding: utf-8

import pytest
from hamcrest import assert_that
import tempfile

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.geo_base_files import ContriesFile
from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.pylibrary.proto_utils import message_from_data


GEO_BASE_DIR = tempfile.mkdtemp()


@pytest.fixture(scope='module')
def geo_country_file():
    countries = {
        'Россия': 124,
        'Италия': 125,
    }
    return ContriesFile(countries=countries, preset_file_path=GEO_BASE_DIR)


@pytest.fixture(scope='module')
def additional_geo_country_file():
    countries = {
        'Китай': 126,
    }
    return ContriesFile(countries=countries, filename='additional_countries_utf8.c2n', preset_file_path=GEO_BASE_DIR)


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic, geo_country_file, additional_geo_country_file):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    geo_enricher = cfg.create_geo_enricher(
        geo_base_dir=GEO_BASE_DIR,
        countries_file=geo_country_file.filename,
        additional_countries_file=additional_geo_country_file.filename
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, geo_enricher)
    cfg.create_link(geo_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, geo_country_file, additional_geo_country_file):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'countries_utf8.c2n': geo_country_file,
        'additional_countries_utf8.c2n': additional_geo_country_file,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def write_read_offer_lbk(offer_data, input_topic, output_topic, offer_color=DTC.WHITE):
    message = DatacampMessage(united_offers=[UnitedOffersBatch(
        offer=[UnitedOffer(
            basic=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=offer_data['identifiers']['business_id'],
                    offer_id=offer_data['identifiers']['offer_id'],
                ),
                content=message_from_data(offer_data['content'], DTC.OfferContent())
            ),
            service={offer_data['identifiers']['shop_id']: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=offer_data['identifiers']['business_id'],
                    shop_id=offer_data['identifiers']['shop_id'],
                    offer_id=offer_data['identifiers']['offer_id'],
                ),
                meta=DTC.OfferMeta(rgb=offer_color),
                status=DTC.OfferStatus(
                    united_catalog=DTC.Flag(
                        flag=True
                    )
                )
            )}
        )]
    )])
    input_topic.write(message.SerializeToString())
    return output_topic.read(count=1)


def test_geo_data_enricher_base(miner, input_topic, output_topic):
    """Проверяем, что работает базовый пайплайн заполнения индентификаторов country_of_origin
       из партнерского текстового описания должны получить список идентификаторов"""
    actual_offer = {
        'identifiers': {
            'shop_id': 10296180,
            'business_id': 10296180,
            'offer_id': 'offerWithCorrectCountryOfOrigin',
        },
        'content': {
            'partner': {
                'original': {
                    'country_of_origin': {
                        'value': [
                            'Россия',
                            'китай'
                        ]
                    }
                }
            }
        }
    }

    data = write_read_offer_lbk(actual_offer, input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 10296180,
                        'offer_id': 'offerWithCorrectCountryOfOrigin',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'country_of_origin_id': {
                                    'value': [
                                        124,
                                        126
                                    ]
                                }
                            }
                        }
                    },
                },
                'service': IsProtobufMap({
                    10296180: {
                        'identifiers': {
                            'shop_id': 10296180,
                            'business_id': 10296180,
                            'offer_id': 'offerWithCorrectCountryOfOrigin',
                        },
                    },
                })
            }]
        }]}))


def test_geo_data_enricher_wrong_countries(miner, input_topic, output_topic):
    """Проверяем, что работает базовый пайплайн заполнения индентификаторов country_of_origin:
       если из партнерского текстового описания стран не получилось получить идентификаторы,
       то на выходе в поле получится пустой список"""
    actual_offer = {
        'identifiers': {
            'shop_id': 10296180,
            'business_id': 10296180,
            'offer_id': 'offerWithAllWrongCountryOfOrigin',
        },
        'content': {
            'partner': {
                'original': {
                    'country_of_origin': {
                        'value': [
                            'Nowhere',
                            'HappyEverAfter'
                        ]
                    }
                },
                'actual': {
                    'country_of_origin_id': {
                        'value': [123]
                    }
                }
            }
        }
    }

    data = write_read_offer_lbk(actual_offer, input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 10296180,
                        'offer_id': 'offerWithAllWrongCountryOfOrigin',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'country_of_origin_id': {
                                    'value': []
                                }
                            }
                        }
                    }
                },
                'service': IsProtobufMap({
                    10296180: {
                        'identifiers': {
                            'shop_id': 10296180,
                            'business_id': 10296180,
                            'offer_id': 'offerWithAllWrongCountryOfOrigin',
                        },
                    },
                })
            }]
        }]}))


def test_geo_data_enricher_some_wrong_countries(miner, input_topic, output_topic):
    """Проверяем, что работает базовый пайплайн заполнения индентификаторов country_of_origin:
       если из части партнерского текстового описания стран не получилось получить идентификаторы,
       то на выходе в списке будут идентификаторы реальных стран"""
    actual_offer = {
        'identifiers': {
            'shop_id': 10296180,
            'business_id': 10296180,
            'offer_id': 'offerWithSomeWrongCountryOfOrigin',
        },
        'content': {
            'partner': {
                'original': {
                    'country_of_origin': {
                        'value': [
                            'Nowhere',
                            'Италия'
                        ]
                    }
                }
            }
        }
    }

    data = write_read_offer_lbk(actual_offer, input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 10296180,
                        'offer_id': 'offerWithSomeWrongCountryOfOrigin',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'country_of_origin_id': {
                                    'value': [125]
                                }
                            }
                        }
                    }
                },
                'service': IsProtobufMap({
                    10296180: {
                        'identifiers': {
                            'shop_id': 10296180,
                            'business_id': 10296180,
                            'offer_id': 'offerWithSomeWrongCountryOfOrigin',
                        },
                    },
                })
            }]
        }]}))


# для пары тестов ниже создаем отдельный miner со своим конфигом(с включенным флагом ForgiveMistakes) и топиками
# для краткости efm = enabled forgive mistakes
@pytest.fixture(scope='module')
def input_topic_efm(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def output_topic_efm(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def miner_config_efm(log_broker_stuff, input_topic_efm, output_topic_efm, geo_country_file, additional_geo_country_file):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic_efm, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic_efm)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    geo_enricher = cfg.create_geo_enricher(
        geo_base_dir=GEO_BASE_DIR,
        countries_file=geo_country_file.filename,
        additional_countries_file=additional_geo_country_file.filename,
        forgive_mistakes=True,
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, geo_enricher)
    cfg.create_link(geo_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner_efm(miner_config_efm, input_topic_efm, output_topic_efm, geo_country_file, additional_geo_country_file):
    resources = {
        'miner_cfg': miner_config_efm,
        'input_topic': input_topic_efm,
        'output_topic': output_topic_efm,
        'countries_utf8.c2n': geo_country_file,
        'additional_countries_utf8.c2n': additional_geo_country_file,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def test_geo_data_enricher_wrong_countries_but_valid_previous_actual(miner_efm, input_topic_efm, output_topic_efm):
    """Проверяем, что если в original пришли страны, из которых нельзя получить идентификаторы, а в actual уже были
    корректные id реальных стран, мы не скинем хорошие actual"""
    actual_offer = {
        'identifiers': {
            'shop_id': 10296180,
            'business_id': 10296180,
            'offer_id': 'offerWithSomeWrongCountryOfOrigin',
        },
        'content': {
            'partner': {
                'original': {
                    'country_of_origin': {
                        'value': [
                            'Nowhere',
                            'Nowhere-2'
                        ]
                    }
                },
                'actual': {
                    'country_of_origin_id': {
                        'value': [134]
                    }
                }
            }
        }
    }

    data = write_read_offer_lbk(actual_offer, input_topic_efm, output_topic_efm)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 10296180,
                        'offer_id': 'offerWithSomeWrongCountryOfOrigin',
                    },
                    'content': {
                        'partner': {
                            'actual': None,
                            'original': None,
                        }
                    }
                },
                'service': IsProtobufMap({
                    10296180: {
                        'identifiers': {
                            'shop_id': 10296180,
                            'business_id': 10296180,
                            'offer_id': 'offerWithSomeWrongCountryOfOrigin',
                        },
                    },
                })
            }]
        }]}))


def test_geo_data_enricher_empty_countries_but_valid_previous_actual(miner_efm, input_topic_efm, output_topic_efm):
    """Проверяем, что если в original не пришли страны вообще, а в actual уже были корректные id реальных стран,
    мы позволим партнеру избавиться от стран и очистим actual"""
    actual_offer = {
        'identifiers': {
            'shop_id': 10296180,
            'business_id': 10296180,
            'offer_id': 'offerWithSomeWrongCountryOfOrigin',
        },
        'content': {
            'partner': {
                'actual': {
                    'country_of_origin_id': {
                        'value': [134]
                    }
                }
            }
        }
    }

    data = write_read_offer_lbk(actual_offer, input_topic_efm, output_topic_efm)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 10296180,
                        'offer_id': 'offerWithSomeWrongCountryOfOrigin',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'country_of_origin_id': {
                                    'value': []
                                }
                            }
                        }
                    }
                },
                'service': IsProtobufMap({
                    10296180: {
                        'identifiers': {
                            'shop_id': 10296180,
                            'business_id': 10296180,
                            'offer_id': 'offerWithSomeWrongCountryOfOrigin',
                        },
                    },
                })
            }]
        }]}))
