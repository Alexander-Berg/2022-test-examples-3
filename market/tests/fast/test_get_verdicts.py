# coding: utf-8

import pytest
import requests
from hamcrest import assert_that, not_, raises, calling

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.errors.Explanation_pb2 as EXP
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import TPicrobotStateBatch
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.api.GetVerdicts_pb2 import (
    GetVerdictsResponse,
    GetVerdictsBatchRequest,
    GetVerdictsBatchResponse
)
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import dict2tskv, create_meta
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.pylibrary.proto_utils import message_from_data

from market.idx.pictures.yatf.resources.api import PicrobotApi

# Сервер партнера возвращает стандартные Http коды ошибок
PICTURE_STD_PARTNER_ERRORS = [500, 404]
# Специфичные коды робота, относящиеся к партнерским ошибкам
PICTURE_SPEC_PARTNER_ERRORS = {
    1001: 'Unable to connect to the server',
    1017: 'Incorrect HTTP message length',
    1008: 'Non-standard HTTP message format',
    1005: 'Bad mime type',
    1014: 'Invalid HTTP message encoding',
    1020: 'HTTP header too large',
    1021: 'HTTP url too large',
    1028: 'Server returned some error',
    1039: 'Timeout while bytes receiving',
    1003: 'Indexing is disallowed by robots.txt or meta-tags',
    2010: 'File is not an image',
    2024: 'Server returned empty response',
    1002: 'Image too large'
}
# Наши внутренние ошибки (партнер не виноват)
PICTURE_INTERNAL_ERRORS = [1030, 1034]
PICTURE_DOWNLOADING_IN_PROGRESS_DESCRIPTION = "Downloading image in progress"


def gen_std_partner_error_pic_url(code):
    return 'https://partner.err/std_code_{}.jpg'.format(code)


def gen_spec_partner_error_pic_url(code):
    return 'https://partner.err/custom_code_{}.jpg'.format(code)


def gen_internal_error_pic_url(code):
    return 'https://internal.err/code_{}.jpg'.format(code)


def gen_verdicts(code, applications, source=None, two_messages=False, different_params=False):
    default_params = [
        DTC.Explanation.Param(
            name='ParamName',
            value='ParamValue'
        )
    ]
    second_params = [
        DTC.Explanation.Param(
            name='ParamName_2',
            value='ParamValue_2'
        )
    ]

    messages = [
        DTC.Explanation(
            code=code,
            params=default_params,
            level=EXP.Explanation.Level.ERROR
        )
    ] if not two_messages else [
        DTC.Explanation(
            code=code,
            params=default_params
        ),
        DTC.Explanation(
            code=code,
            params=default_params if not different_params else second_params
        )
    ]

    return DTC.Verdicts(
        meta=DTC.UpdateMeta(
            source=source
        ),
        verdict=[
            DTC.Verdict(
                results=[
                    DTC.ValidationResult(
                        applications=applications,
                        messages=messages
                    )
                ]
            )
        ]
    )


def gen_model_recommendation_verdict():
    messages = [
        DTC.Explanation(
            code="some_recommendation_warning",
            params=[
                DTC.Explanation.Param(
                    name='SomeRecommendationParam1',
                    value='SomeRecommendationParamValue1'
                )
            ],
            level=EXP.Explanation.Level.WARNING
        ),
        DTC.Explanation(
            code="some_recommendation_error",
            params=[
                DTC.Explanation.Param(
                    name='SomeRecommendationParam2',
                    value='SomeRecommendationParamValue2'
                )
            ],
            level=EXP.Explanation.Level.ERROR
        ),
    ]

    return DTC.ValidationResult(
        recommendation_status=DTC.RESOLVE_BLOCKERS,
        applications=[DTC.DBS, DTC.FBS],
        messages=messages
    )


def gen_verdicts_with_model_recommendation(source, with_ordinary_verdict=False):
    results = [gen_model_recommendation_verdict()]

    if with_ordinary_verdict:
        results.append(
            DTC.ValidationResult(
                messages=[
                    DTC.Explanation(
                        code="not_recommendation_verdict",
                        params=[
                            DTC.Explanation.Param(
                                name='NotRecommendationParamName',
                                value='NotRecommendationParamValue'
                            )
                        ],
                        level=EXP.Explanation.Level.ERROR
                    )
                ]
            )
        )

    return DTC.Verdicts(
        meta=DTC.UpdateMeta(
            source=source
        ),
        verdict=[
            DTC.Verdict(
                results=results
            )
        ]
    )


def gen_actual_pictures():

    def gen_market_picture(failed=False):
        return DTC.MarketPicture(
            status=DTC.MarketPicture.Status.AVAILABLE if not failed else DTC.MarketPicture.Status.FAILED,
            namespace='marketpic'
        )

    pics = {
        # Хорошая картинка, по ней не должны показывать ошибки
        'https://good.pic/pic.jpg': gen_market_picture(),
        # Картинка помечена как FAILED, но она есть в стейте пикробота
        # Скорее всего она починилась, но мы еще не успели получить по ней данные в хранилище
        # Показываем сообщение, что ещё качаем
        'https://failed.but/in_picrobot_state.jpg': gen_market_picture(failed=True),
        # Возникла ошибка аватарницы при копировании из другого неймспейса
        # Должны показать, что это наша внутренняя ошибка
        'https://failed.by/copier.jpg': gen_market_picture(failed=True),
    }
    for code in PICTURE_STD_PARTNER_ERRORS:
        pics[gen_std_partner_error_pic_url(code)] = gen_market_picture(failed=True)
    for code in list(PICTURE_SPEC_PARTNER_ERRORS.keys()):
        pics[gen_spec_partner_error_pic_url(code)] = gen_market_picture(failed=True)
    for code in PICTURE_INTERNAL_ERRORS:
        pics[gen_internal_error_pic_url(code)] = gen_market_picture(failed=True)

    return pics


def verdict_response(business_id, shop_sku, shop_id, warehouse_id, is_relevant):
    return {
        'messages': [{
            "explanation": {
                "code": "non_relevant_miner_verdict",
                "namespace": "shared.indexer.error.codes",
            },
            "identifiers": [
                {
                    "business_id": business_id,
                    "is_relevant": is_relevant,
                    "offer_id": shop_sku,
                    "shop_id": shop_id,
                    "warehouse_id": warehouse_id
                }
            ],
            "is_relevant": is_relevant
        }, ]
    }


@pytest.fixture(scope='module')
def basic_offers():
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1'
            ),
            content=DTC.OfferContent(
                status=DTC.ContentStatus(
                    content_system_status=DTC.ContentSystemStatus(
                        status_content_version=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    actual_content_version=DTC.VersionCounter(
                        counter=20
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('basic_cpc', [DTC.CPC]),
                    gen_verdicts('duplicatedParams', [], two_messages=True),
                    gen_verdicts('differentParams', [], two_messages=True, different_params=True),
                ]
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o2'
            ),
            content=DTC.OfferContent(
                status=DTC.ContentStatus(
                    content_system_status=DTC.ContentSystemStatus(
                        status_content_version=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    actual_content_version=DTC.VersionCounter(
                        counter=21
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('basic_cpc', [DTC.CPC]),
                ],
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o3'
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('basic_cpc', [DTC.CPC]),
                ],
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.no.mapping'
            ),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.with.mapping'
            ),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    approved=DTC.Mapping(
                        market_sku_id=100
                    )
                )
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='45l.no.mapping'
            ),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.with.mapping.and.empty.shopsdat'
            ),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    approved=DTC.Mapping(
                        market_sku_id=100
                    )
                )
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_49i'
            ),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='zero.warehouse'
            ),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='picture.errors'
            ),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    actual=gen_actual_pictures()
                )
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='another.picture.errors'
            ),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    actual={
                        gen_std_partner_error_pic_url(PICTURE_STD_PARTNER_ERRORS[0]): DTC.MarketPicture(
                            status=DTC.MarketPicture.Status.FAILED,
                            namespace='marketpic'
                        ),
                        # Неканоничный урл, с ним тоже должно все работать
                        "failed.by/copier.jpg": DTC.MarketPicture(
                            status=DTC.MarketPicture.Status.FAILED,
                            namespace='marketpic'
                        )
                    }
                )
            )
        )),
    ] + [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=mdm_version
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=source_version
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    DTC.Verdicts(
                        meta=DTC.UpdateMeta(
                            source=DTC.MARKET_MDM,
                        ),
                        verdict=[
                            DTC.Verdict(
                                results=[
                                    DTC.ValidationResult(
                                        applications=[DTC.DBS],
                                        messages=[
                                            DTC.Explanation(
                                                code="basic_mdm_verdict",
                                                level=EXP.Explanation.Level.WARNING
                                            )
                                        ]
                                    ),
                                    DTC.ValidationResult(
                                        applications=[DTC.CPA, DTC.FULFILLMENT],
                                        messages=[
                                            DTC.Explanation(
                                                code="basic_mdm_verdict",
                                                level=EXP.Explanation.Level.ERROR
                                            )
                                        ]
                                    ),
                                ],
                            )
                        ]
                    )
                ]
            )
        )) for offer_id, source_version, mdm_version in [
            ('relevant.mdm_verdict', 20, 20),
            ('not.relevant.by.basic.mdm_verdict', 21, 20),
            ('not.relevant.by.service.mdm_verdict', 20, 20),
        ]
    ] + [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=2,
                offer_id='o2'
            ),
        ))
    ] + [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=4,
                offer_id='o4'
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    actual_content_version=DTC.VersionCounter(
                        counter=1754160626541791200
                    ),
                    master_data_version=DTC.VersionCounter(
                        counter=1754160626541791200
                    ),
                    offer_version=DTC.VersionCounter(
                        counter=1755097611238378500
                    ),
                    original_partner_data_version=DTC.VersionCounter(
                        counter=1751575375130921200
                    ),
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('basic_resolution_4', []),
                ],
            )
        ))
    ] + [
        # Офферы для проверки фильтрации рекомендаций по размещению среди вердиктов:
        # Оффер с вердиктом от одного источника и с рекомендациями по размещению от другого
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_with_recommendation1'
            ),
            content=DTC.OfferContent(
                status=DTC.ContentStatus(
                    content_system_status=DTC.ContentSystemStatus(
                        status_content_version=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    actual_content_version=DTC.VersionCounter(
                        counter=20
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    # обычный вердикт
                    gen_verdicts(
                        code='some_verdict1',
                        applications=[],
                        source=DTC.MARKET_MBO
                    ),
                    # чисто рекомендации по размещению (без вердиктов от того же источника)
                    gen_verdicts_with_model_recommendation(
                        source=DTC.MARKET_MBO
                    )
                ]
            )
        )),
        # Оффер с вердиктом от одного источника и с рекомендациями по размещению и вердиктом от другого
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_with_recommendation2'
            ),
            content=DTC.OfferContent(
                status=DTC.ContentStatus(
                    content_system_status=DTC.ContentSystemStatus(
                        status_content_version=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    actual_content_version=DTC.VersionCounter(
                        counter=20
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    # обычный вердикт
                    gen_verdicts(
                        code='some_verdict2',
                        applications=[]
                    ),
                    # рекомендация по размещению с вердиктом от того же источника
                    gen_verdicts_with_model_recommendation(
                        source=DTC.MARKET_ABO,
                        with_ordinary_verdict=True
                    )
                ]
            )
        ))
    ] + [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='dbs_for_mdm'
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=20
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('basic_mdm_verdict', [DTC.DBS], source=DTC.MARKET_MDM),
                ]
            ),
            meta=create_meta(10, scope=DTC.BASIC)
        ))
    ] + [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='mdm_not_relevant'
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=30
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('mdm_not_relevant_verdict', [DTC.FULFILLMENT], source=DTC.MARKET_MDM),
                ]
            ),
            meta=create_meta(10, scope=DTC.BASIC)
        ))
    ] + [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=20
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('basic_gutgin_verdict', [], source=DTC.MARKET_GUTGIN),
                ]
            ),
            meta=create_meta(10, scope=DTC.BASIC)
        )) for offer_id in [
            'relevant.gutgin',
            'not.relevant.gutgin',
        ]
    ] + [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=20
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('basic_mbo_verdict', [], source=DTC.MARKET_MBO),
                ]
            ),
            meta=create_meta(10, scope=DTC.BASIC)
        )) for offer_id in [
            'relevant.mbo',
            'not.relevant.mbo.1',
            'not.relevant.mbo.2',
            'not.relevant.mbo.3',
        ]
    ]


@pytest.fixture(scope='module')
def service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=1
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    original_partner_data_version=DTC.VersionCounter(
                        counter=10
                    ),
                    # vesion in service part is different from actual_content_version in basic
                    # versions by content are checked on basic parrt
                    actual_content_version=DTC.VersionCounter(
                        counter=10
                    )
                ),
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('service_1', []),
                    gen_verdicts('relevant_mbo_verdict', [], DTC.MARKET_MBO)
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=2
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    original_partner_data_version=DTC.VersionCounter(
                        counter=11
                    )
                ),
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('service_2', [])
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o2',
                shop_id=1
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('service_2', []),
                    gen_verdicts('not_relevant_mbo_verdict', [], DTC.MARKET_MBO)
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.no.mapping',
                shop_id=1
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.with.mapping',
                shop_id=1
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='45l.no.mapping',
                shop_id=1
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.with.mapping.and.empty.shopsdat',
                shop_id=12345
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_49i',
                shop_id=12345
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='zero.warehouse',
                shop_id=3
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('service_3', [])
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='picture.errors',
                shop_id=1
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='picture.errors',
                shop_id=2
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='another.picture.errors',
                shop_id=2
            )
        )),
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id,
                shop_id=3
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=mdm_version
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=source_version
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('service_mdm_verdict', [DTC.CPA, DTC.FULFILLMENT], source=DTC.MARKET_MDM),
                ]
            )
        )) for offer_id, source_version, mdm_version in [
            ('relevant.mdm_verdict', 20, 20),
            ('not.relevant.by.basic.mdm_verdict', 20, 20),
            ('not.relevant.by.service.mdm_verdict', 21, 20),
        ]
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=2,
                offer_id='o2',
                shop_id=2
            ),
        ))
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='dbs_for_mdm',
                shop_id=3
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=20
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=20
                    )
                ),
                united_catalog=DTC.Flag(
                    flag=True
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('service_mdm_verdict', [DTC.DBS], source=DTC.MARKET_MDM),
                    gen_verdicts('49r', [], source=DTC.MARKET_IDX),
                ]
            ),
            partner_info=DTC.PartnerInfo(
                is_dsbs=True
            ))
        )
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='mdm_not_relevant',
                shop_id=3
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE
            ),
            status=DTC.OfferStatus(
                united_catalog=DTC.Flag(
                    flag=True
                )
            )
        ))
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id,
                shop_id=3
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=20
                        )
                    )
                ),
                status=DTC.ContentStatus(
                    content_system_status=DTC.ContentSystemStatus(
                        cpc_state=cpc_state
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=20
                    )
                ),
                united_catalog=DTC.Flag(
                    flag=True
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('service_gutgin_verdict', [], source=DTC.MARKET_GUTGIN),
                ]
            ),
            partner_info=DTC.PartnerInfo(
                is_dsbs=True
            ))
        ) for offer_id, cpc_state in [
            ('relevant.gutgin', DTC.CPC_CONTENT_READY),
            ('not.relevant.gutgin', DTC.CPC_CONTENT_PROCESSING)
        ]
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id,
                shop_id=3
            ),
            content=DTC.OfferContent(
                master_data=DTC.MarketMasterData(
                    version=DTC.VersionCounterValue(
                        value=DTC.VersionCounter(
                            counter=20
                        )
                    )
                ),
                status=DTC.ContentStatus(
                    content_system_status=DTC.ContentSystemStatus(
                        service_offer_state=service_offer_state
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=20
                    )
                ),
                united_catalog=DTC.Flag(
                    flag=True
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('service_mbo_verdict', [], source=DTC.MARKET_MBO),
                ]
            ),
            partner_info=DTC.PartnerInfo(
                is_dsbs=True
            ))
        ) for offer_id, service_offer_state in [
            ('relevant.mbo', DTC.CONTENT_STATE_READY),
            ('not.relevant.mbo.1', DTC.CONTENT_STATE_IN_WORK),
            ('not.relevant.mbo.2', DTC.CONTENT_STATE_REVIEW),
            ('not.relevant.mbo.3', DTC.CONTENT_STATE_CONTENT_PROCESSING)
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=1,
                warehouse_id=1
            ),
            status=DTC.OfferStatus(
                publication=DTC.PublicationStatus(
                    original_partner_data_version=DTC.VersionCounter(
                        counter=10
                    )
                )
            ),
            tech_info=DTC.OfferTechInfo(
                last_mining=DTC.MiningTrace(
                    original_partner_data_version=DTC.VersionCounter(
                        counter=10
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('aso', []),
                    gen_verdicts('relevant_miner_verdict', [], DTC.MARKET_IDX),
                    gen_verdicts('relevant_generation_verdict', [], DTC.MARKET_IDX_GENERATION)
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=2,
                warehouse_id=1
            ),
            status=DTC.OfferStatus(
                publication=DTC.PublicationStatus(
                    original_partner_data_version=DTC.VersionCounter(
                        counter=10
                    )
                )
            ),
            tech_info=DTC.OfferTechInfo(
                last_mining=DTC.MiningTrace(
                    original_partner_data_version=DTC.VersionCounter(
                        counter=10
                    )
                )
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('aso', []),
                    gen_verdicts('not_relevant_miner_verdict', [], DTC.MARKET_IDX),
                    gen_verdicts('not_relevant_generation_verdict', [], DTC.MARKET_IDX_GENERATION)
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.no.mapping',
                shop_id=1,
                warehouse_id=1
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('49S', [], DTC.MARKET_IDX),
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.with.mapping',
                shop_id=1,
                warehouse_id=1
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('49S', [], DTC.MARKET_IDX),
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='45l.no.mapping',
                shop_id=1,
                warehouse_id=1
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('45l', [], DTC.MARKET_IDX),
                ]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='49S.with.mapping.and.empty.shopsdat',
                shop_id=12345,
                warehouse_id=1
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('49S', [], DTC.MARKET_IDX),
                    gen_verdicts('45a', [], DTC.MARKET_IDX),
                ]
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_49i',
                shop_id=12345,
                warehouse_id=1
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('49i', [], DTC.MARKET_IDX),
                    gen_verdicts('45z', [], DTC.MARKET_IDX),
                ]
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='zero.warehouse',
                shop_id=3,
                warehouse_id=0
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('12f', [], DTC.MARKET_IDX),
                ]
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='picture.errors',
                shop_id=1,
                warehouse_id=2
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='picture.errors',
                shop_id=2,
                warehouse_id=1
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='another.picture.errors',
                shop_id=2,
                warehouse_id=1
            ),
        )),
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=2,
                offer_id='o2',
                shop_id=2,
                warehouse_id=2
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdicts('non_relevant_miner_verdict', [], DTC.MARKET_IDX),
                ]
            ),
            status=DTC.OfferStatus(
                publication=DTC.PublicationStatus(
                    original_partner_data_version=DTC.VersionCounter(
                        counter=20
                    )
                )
            ),
            tech_info=DTC.OfferTechInfo(
                last_mining=DTC.MiningTrace(
                    original_partner_data_version=DTC.VersionCounter(
                        counter=10
                    )
                )
            ),
        )),
    ]


@pytest.fixture(scope='module')
def partners():
    return [
        {
            'shop_id': 1,
            'mbi': '\n\n'.join([
                dict2tskv({'shop_id': 1, 'business_id': 1}),
            ])
        },
        {
            'shop_id': 2,
            'mbi': '\n\n'.join([
                dict2tskv({'shop_id': 2, 'business_id': 1}),
            ])
        },
        {
            'shop_id': 3,
            'mbi': '\n\n'.join([
                dict2tskv({'shop_id': 3, 'business_id': 1, 'warehouse_id': 145}),
            ])
        },
        {
            'shop_id': 2,
            'mbi': '\n\n'.join([
                dict2tskv({'shop_id': 2, 'business_id': 2, 'warehouse_id': 2}),
            ])
        },
        {
            'shop_id': 3,
            'mbi': '\n\n'.join([
                dict2tskv({'shop_id': 3, 'business_id': 3, 'warehouse_id': 3}),
            ])
        },
    ]


@pytest.fixture(scope='module')
def picrobot_responses():

    def gen_failed_picrobot_state(http_code):
        return {
            'CopierMeta': [],
            'DownloadMeta': {
                'HttpCode': http_code,
                'Namespace': 'marketpic'
            },
            'MdsInfo': [],
        }

    pictures_data = {
        # Картинка, которая чудесным образом починилась, и лежит в стейте
        'https://failed.but/in_picrobot_state.jpg': {
            'CopierMeta': [],
            'DownloadMeta': {
                'HttpCode': 200,
                'Namespace': 'marketpic'
            },
            'MdsInfo': [
                {
                    'Height': 600,
                    'Md5': 'aaaaaaaaaaaaaaaaa123',
                    'MdsId': {
                        'GroupId': 100500,
                        'ImageName': 'XXXXX',
                        'Namespace': 'marketpic'
                    },
                    'Width': 600
                }
            ],
        },
        # Картинка зафейлившая при копировании из другого неймспейса
        'https://failed.by/copier.jpg': {
            'CopierMeta': [
                {
                    'MdsHttpCode': 400,
                    'Namespace': 'marketpic'
                }
            ],
            'DownloadMeta': {
                'HttpCode': 200,
                'Namespace': 'direct'
            },
            'MdsInfo': [
                {
                    'Height': 600,
                    'Md5': 'bbbbbbbbbb123',
                    'MdsId': {
                        'GroupId': 12345,
                        'ImageName': 'YYYYYY',
                        'Namespace': 'direct'
                    },
                    'Width': 600
                }
            ],
        },
    }

    for code in PICTURE_STD_PARTNER_ERRORS:
        pictures_data[gen_std_partner_error_pic_url(code)] = gen_failed_picrobot_state(code)
    for code in list(PICTURE_SPEC_PARTNER_ERRORS.keys()):
        pictures_data[gen_spec_partner_error_pic_url(code)] = gen_failed_picrobot_state(code)
    for code in PICTURE_INTERNAL_ERRORS:
        pictures_data[gen_internal_error_pic_url(code)] = gen_failed_picrobot_state(code)

    picrobot_protobuf_response = TPicrobotStateBatch()
    message_from_data({'States': pictures_data}, picrobot_protobuf_response)
    return [
        {'code': requests.codes.ok, 'data': picrobot_protobuf_response.SerializeToString()} for _ in range(2)
    ]


@pytest.fixture(scope='module')
def picrobot_api(picrobot_responses):
    yield PicrobotApi(picrobot_responses)


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        partners_table,
        picrobot_api,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            partners_table=partners_table,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table,
            picrobot=picrobot_api
    ) as stroller_env:
        yield stroller_env


def test_get_verdicts(stroller):
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=o1&show_all=false')

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1}
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'basic_cpc',
                    'level': EXP.Explanation.Level.ERROR
                }
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1},
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'service_1'
                }
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1},
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'duplicatedParams',
                    'params': [{'name': 'ParamName', 'value': 'ParamValue'}]
                }
            },
            # two messages with same code,namespace, but differentParams part1
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1},
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'differentParams',
                    'params': [
                        {'name': 'ParamName', 'value': 'ParamValue'},
                    ]
                }
            },
            # two messages with same code,namespace, but differentParams part2
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1},
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'differentParams',
                    'params': [
                        {'name': 'ParamName_2', 'value': 'ParamValue_2'}
                    ]
                }
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 2},
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'service_2'
                }
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1, 'warehouse_id': 1},
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'aso'
                }
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1},
                ],
                'explanation': {
                    'code': 'relevant_mbo_verdict'
                }
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1, 'warehouse_id': 1},
                ],
                'explanation': {
                    'code': 'relevant_miner_verdict'
                }
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1, 'warehouse_id': 1},
                ],
                'explanation': {
                    'code': 'relevant_generation_verdict'
                }
            },
        ]
    }))

    not_expected_codes = [
        'not_relevant_mbo_verdict',
        'not_relevant_miner_verdict',
        'not_relevant_generation_verdict'
    ]
    for code in not_expected_codes:
        assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
            'messages': [
                {
                    'explanation': {
                        'code': code
                    }
                },
            ]
        })))

    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
        'message': [
            {
                'identifiers': [
                    {'is_relevant': False}
                ],
                'is_relevant': False
            },
        ]
    })))

    # Не показываем вердикты офферов со складом, которого нет в шопсдате, если у магазина есть другие склады
    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o1', 'shop_id': 2, 'warehouse_id': 1},
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'aso'
                }
            },
        ]
    })))


def test_get_all_verdicts(stroller):
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=o2&show_all=true')

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o2', 'shop_id': 1, 'is_relevant': True}
                ],
                'explanation': {
                    'code': 'basic_cpc'
                },
                'is_relevant': True
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o2', 'shop_id': 1, 'is_relevant': True}
                ],
                'explanation': {
                    'code': 'service_2'
                },
                'is_relevant': True
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'o2', 'shop_id': 1, 'is_relevant': False}
                ],
                'explanation': {
                    'code': 'not_relevant_mbo_verdict'
                },
                'is_relevant': False
            },
        ]
    }))


def test_get_batch_verdicts_by_business(stroller):
    # проверяем, что не возвращается вердикт по офферу, которого не было в запросе
    request = message_from_data({'offer_ids': ['o1', 'o2']}, GetVerdictsBatchRequest())
    response = stroller.post('/v1/partners/1/offers/verdicts', data=request.SerializeToString())

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [
            {
                'verdicts': {
                    'messages': [
                        {
                            # из базовой части
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'o1'}
                            ],
                            'explanation': {
                                'namespace': 'shared.indexer.error.codes',
                                'code': 'basic_cpc',
                                'level': EXP.Explanation.Level.ERROR
                            }
                        },
                        {
                            # из сервисной части
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1}
                            ],
                            'explanation': {
                                'namespace': 'shared.indexer.error.codes',
                                'code': 'service_1',
                                'level': EXP.Explanation.Level.ERROR
                            }
                        },
                        {
                            # из второй сервисной части
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'o1', 'shop_id': 2}
                            ],
                            'explanation': {
                                'namespace': 'shared.indexer.error.codes',
                                'code': 'service_2',
                                'level': EXP.Explanation.Level.ERROR
                            }
                        },
                        {
                            # из складской части
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1, 'warehouse_id': 1}
                            ],
                            'explanation': {
                                'namespace': 'shared.indexer.error.codes',
                                'code': 'aso',
                                'level': EXP.Explanation.Level.ERROR
                            }
                        },
                    ]
                }
            },
            {
                'verdicts': {
                    'messages': [
                        {
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'o2', 'shop_id': 1}
                            ],
                            'explanation': {
                                'namespace': 'shared.indexer.error.codes',
                                'code': 'basic_cpc',
                                'level': EXP.Explanation.Level.ERROR
                            }
                        },
                    ]
                }
            }
        ]
    }))

    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [{
            'verdicts': {
                'messages': [{
                    'identifiers': [{'business_id': 1, 'offer_id': 'o3'}],
                }]
            }
        }
    ]})))


def test_get_batch_verdicts_by_shop(stroller):
    # проверяем, что возвращаются вердикты только для заданного в запросе магазина
    request = message_from_data({'offer_ids': ['o1']}, GetVerdictsBatchRequest())
    response = stroller.post('/v1/partners/1/offers/verdicts?shop_id=1', data=request.SerializeToString())

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [{
            'verdicts': {
                'messages': [{
                    'identifiers': [
                        {'business_id': 1, 'offer_id': 'o1', 'shop_id': 1}
                    ],
                    'explanation': {
                        'namespace': 'shared.indexer.error.codes',
                        'code': 'service_1',
                        'level': EXP.Explanation.Level.ERROR
                    }
                }]
            }
        }]
    }))

    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [{
            'verdicts': {
                'messages': [{
                    'identifiers': [{'business_id': 1, 'offer_id': 'o1', 'shop_id': 2}],
                }]
            }
        }
    ]})))


def test_get_verdicts_for_mapping_errors(stroller):
    """
    Если подтвержденного маппинга нет, то не показываем ошибки маппинга из майнера, т.к. есть более важные ошибки от КИ.
    """
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=49S.with.mapping')
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': '49S.with.mapping', 'shop_id': 1, 'warehouse_id': 1}
            ],
            'explanation': {
                'code': '49S',
            }
        }, ]
    }))

    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=45l.no.mapping')
    assert_that(response, HasStatus(200))
    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': '45l.no.mapping', 'shop_id': 1, 'warehouse_id': 1}
            ],
        }, ]
    })))

    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=49S.no.mapping')
    assert_that(response, HasStatus(200))
    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': '49S.no.mapping', 'shop_id': 1, 'warehouse_id': 1}
            ],
        }, ]
    })))


def test_skip_49S_because_of_empty_shopdat(stroller):
    """
    Для синих офферов не показываем ошибку 49S, если шопдаты нет, т.к. без нее мы не можем посчитать идентификаторы.
    Партнеру покажется ошибка "Настройте склад".
    """
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=49S.with.mapping.and.empty.shopsdat')
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': '49S.with.mapping.and.empty.shopsdat', 'shop_id': 12345, 'warehouse_id': 1}
            ],
            'explanation': {
                'code': '45a',
            }
        }, ]
    }))
    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': '49S.with.mapping.and.empty.shopsdat', 'shop_id': 12345, 'warehouse_id': 1}
            ],
            'explanation': {
                'code': '49S',
            }
        }, ]
    })))


def test_skip_49i(stroller):
    """
    Не показываем партнеру ошибку 49i.
    """
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=offer_49i')
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': 'offer_49i', 'shop_id': 12345, 'warehouse_id': 1}
            ],
            'explanation': {
                'code': '45z',
            }
        }]
    }))
    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': 'offer_49i', 'shop_id': 12345, 'warehouse_id': 1}
            ],
            'explanation': {
                'code': '49i',
            }
        }]
    })))


def test_skip_because_of_zero_warehouse(stroller):
    """
    Если у магазина по информации из шопдаты д.б. склад, не показываем ошибки с нулевого склада (он удалится)
    """
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=zero.warehouse')
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': 'zero.warehouse', 'shop_id': 3, 'warehouse_id': 0}
            ],
            'explanation': {
                'code': 'service_3',
            }
        }, ]
    }))
    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {'business_id': 1, 'offer_id': 'zero.warehouse', 'shop_id': 3, 'warehouse_id': 0}
            ],
            'explanation': {
                'code': '12f',
            }
        }, ]
    })))


def test_relevant_mdm_verdict(stroller):
    """
    Если версия ответа МДМ в базовой и сервсиной частях не меньше, чем групповая версия оффера, то вердикт релевантный.
    """
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=relevant.mdm_verdict')
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {
                    'business_id': 1,
                    'offer_id': 'relevant.mdm_verdict',
                    'shop_id': 3,
                    'is_relevant': True
                }
            ],
            'explanation': {
                'code': code,
                'level': EXP.Explanation.Level.ERROR,
            },
            'is_relevant': True
        } for code in [
            'basic_mdm_verdict',
            'service_mdm_verdict'
        ]]
    }))


def test_relevant_mdm_verdict_for_dbs(stroller):
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=dbs_for_mdm')
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {
                    'business_id': 1,
                    'offer_id': 'dbs_for_mdm',
                    'shop_id': 3,
                    'is_relevant': True
                }
            ],
            'explanation': {
                'code': code,
                'level': EXP.Explanation.Level.ERROR,
            },
            'is_relevant': True,
            'data_source': DTC.MARKET_MDM,
        } for code in [
            'basic_mdm_verdict',
            'service_mdm_verdict'
        ]]
    }))
    assert_that(
        calling(assert_that).with_args(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
            'messages': [{
                'identifiers': [
                    {
                        'business_id': 1,
                        'offer_id': 'dbs_for_mdm',
                        'shop_id': 3,
                        'is_relevant': True
                    }
                ],
                'explanation': {
                    'code': '49r',
                },
                'is_relevant': True
            }]
        })),
        raises(AssertionError)
    )


def test_gutgin_verdict(stroller):
    def check_relevance_gutgin(offer_id, is_relevant):
        response = stroller.get('/v1/partners/1/offers/verdicts?offer_id='+offer_id)
        assert_that(response, HasStatus(200))
        assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
            'messages': [{
                'identifiers': [
                    {
                        'business_id': 1,
                        'offer_id': offer_id,
                        'shop_id': 3,
                        'is_relevant': is_relevant
                    }
                ],
                'explanation': {
                    'code': code,
                },
                'is_relevant': is_relevant
            } for code in [
                'basic_gutgin_verdict',
                'service_gutgin_verdict'
            ]]
        }))
    check_relevance_gutgin('relevant.gutgin', True)
    check_relevance_gutgin('not.relevant.gutgin', False)


def test_mbo_verdict(stroller):
    def check_relevance_mbo(offer_id, is_relevant):
        response = stroller.get('/v1/partners/1/offers/verdicts?offer_id='+offer_id)
        assert_that(response, HasStatus(200))
        assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
            'messages': [{
                'identifiers': [
                    {
                        'business_id': 1,
                        'offer_id': offer_id,
                        'shop_id': 3,
                        'is_relevant': is_relevant
                    }
                ],
                'explanation': {
                    'code': code,
                },
                'is_relevant': is_relevant
            } for code in [
                'basic_mbo_verdict',
                'service_mbo_verdict'
            ]]
        }))
    check_relevance_mbo('relevant.mbo', True)
    check_relevance_mbo('not.relevant.mbo.1', False)
    check_relevance_mbo('not.relevant.mbo.2', False)
    check_relevance_mbo('not.relevant.mbo.3', False)


@pytest.mark.parametrize(
    "offer_id",
    [
        'not.relevant.by.service.mdm_verdict',
        'not.relevant.by.basic.mdm_verdict',
    ]
)
def test_not_relevant_mdm_verdict(stroller, offer_id):
    """
    Если версия ответа МДМ в базовой или сервсиной части отстает от групповой версии оффера, то вердикт нерелевантный.
    Версия считается, как максимум от версий в базовой и сервисной частях.
    """
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id={}'.format(offer_id))
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [{
            'identifiers': [
                {
                    'business_id': 1,
                    'offer_id': offer_id,
                    'shop_id': 3,
                    'is_relevant': False
                }
            ],
            'explanation': {
                'code': code,
            },
            'is_relevant': False
        } for code in [
            'basic_mdm_verdict',
            'service_mdm_verdict'
        ]]
    }))


def test_not_relevant_mdm_verdict_batch(stroller):
    request = message_from_data({'offer_ids': ['mdm_not_relevant']}, GetVerdictsBatchRequest())
    response = stroller.post('/v1/partners/1/offers/verdicts', data=request.SerializeToString())

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [
            {
                'verdicts': {
                    'messages': [{
                        'identifiers': [
                            {
                                'business_id': 1,
                                'offer_id': 'mdm_not_relevant',
                                'shop_id': 3,
                                'is_relevant': False
                            }
                        ],
                        'explanation': {
                            'namespace': 'shared.indexer.error.codes',
                            'code': 'mdm_not_relevant_verdict',
                            'level': EXP.Explanation.Level.ERROR
                        },
                        'is_relevant': False
                    }]
                }
            }
        ]
    }))


# MARKETINDEXER-42940
def test_tech_info_in_actual(stroller):
    business_id = 2
    offer_id = "o2"
    shop_id = 2
    warehouse_id = 2

    response = stroller.get('/v1/partners/{}/offers/verdicts?offer_id={}&shop_id={}'.format(business_id, offer_id, shop_id))
    assert_that(response, HasStatus(200))
    assert_that(
        response.data,
        IsSerializedProtobuf(
            GetVerdictsResponse,
            verdict_response(business_id, offer_id, shop_id, warehouse_id, False)
        )
    )
    assert_that(
        response.data,
        not_(IsSerializedProtobuf(
            GetVerdictsResponse,
            verdict_response(business_id, offer_id, shop_id, warehouse_id, True)
        ))
    )

    request = message_from_data({'offer_ids': [offer_id]}, GetVerdictsBatchRequest())
    response = stroller.post(
        '/v1/partners/{}/offers/verdicts?shop_id={}'.format(business_id, shop_id),
        data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [
            {
                'verdicts': verdict_response(business_id, offer_id, shop_id, warehouse_id, False)
            },
        ]
    }))
    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [
            {
                'verdicts': verdict_response(business_id, offer_id, shop_id, warehouse_id, True)
            },
        ]
    })))


def test_get_common_pictures_errors(stroller):
    """
    Проверяем, дефолтный режим работы ручки. Она должна возвращать общую ошибку о факте наличия проблем с картинками.
    """
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=picture.errors')
    assert_that(response, HasStatus(200))

    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'picture.errors', 'shop_id': 1},
                    {'business_id': 1, 'offer_id': 'picture.errors', 'shop_id': 2}
                ],
                'explanation': {
                    'code': '49d',
                }
            },
        ]
    }))


def test_get_common_pictures_batch_errors(stroller):
    """
    Проверяем, что батчевая ручка тоже возвращает общую ошибку по картинкам
    """

    request = message_from_data({'offer_ids': ['picture.errors', 'another.picture.errors']}, GetVerdictsBatchRequest())
    response = stroller.post('/v1/partners/1/offers/verdicts', data=request.SerializeToString())

    assert_that(response, HasStatus(200))

    assert_that(response.data, IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [
            {
                'verdicts': {
                    'messages': [
                        {
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'picture.errors', 'shop_id': 1},
                                {'business_id': 1, 'offer_id': 'picture.errors', 'shop_id': 2}
                            ],
                            'explanation': {
                                'code': '49d',
                            }
                        },
                    ]
                }
            },
            {
                'verdicts': {
                    'messages': [
                        {
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'another.picture.errors', 'shop_id': 2}
                            ],
                            'explanation': {
                                'code': '49d',
                            }
                        },
                    ]
                }
            }
        ]
    }))


def test_get_detailed_pictures_errors(stroller):
    """
    Проверяем, что ручка вердиктов отдает ошибки по каждой картинке
    """

    def make_picture_verdict(error_code, params):
        return {
            'identifiers': [
                {'business_id': 1, 'offer_id': 'picture.errors', 'shop_id': 1},
                {'business_id': 1, 'offer_id': 'picture.errors', 'shop_id': 2}
            ],
            'explanation': {
                'namespace': 'shared.indexer.error.codes',
                'code': error_code,
                'level': EXP.Explanation.Level.ERROR,
                'params': params
            }
        }

    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=picture.errors&show_pictures=true')
    assert_that(response, HasStatus(200))

    expected_messages = [
        make_picture_verdict(
            error_code='49f',
            params=[
                {'name': 'pictureUrl', 'value': 'https://failed.by/copier.jpg'}
            ]
        ),
        make_picture_verdict(
            error_code='49e',
            params=[
                {'name': 'description', 'value': PICTURE_DOWNLOADING_IN_PROGRESS_DESCRIPTION},
                {'name': 'pictureUrl', 'value': 'https://failed.but/in_picrobot_state.jpg'}
            ]
        )
    ]
    for code in PICTURE_STD_PARTNER_ERRORS:
        expected_messages.append(
            make_picture_verdict(
                error_code='49e',
                params=[
                    {'name': 'code', 'value': str(code)},
                    {'name': 'pictureUrl', 'value': gen_std_partner_error_pic_url(code)}
                ]
            )
        )
    for code, description in list(PICTURE_SPEC_PARTNER_ERRORS.items()):
        expected_messages.append(
            make_picture_verdict(
                error_code='49e',
                params=[
                    {'name': 'description', 'value': description},
                    {'name': 'pictureUrl', 'value': gen_spec_partner_error_pic_url(code)}
                ]
            )
        )
    for code in PICTURE_INTERNAL_ERRORS:
        expected_messages.append(
            make_picture_verdict(
                error_code='49f',
                params=[
                    {'name': 'pictureUrl', 'value': gen_internal_error_pic_url(code)}
                ]
            )
        )

    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': expected_messages
    }))

    # Проверяем, что не показываем ошибки по хорошим картинкам
    assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [
            make_picture_verdict(
                error_code='49f',
                params=[
                    {'name': 'pictureUrl', 'value': 'https://good.pic/pic.jpg'}
                ]
            ),
            make_picture_verdict(
                error_code='49e',
                params=[
                    {'name': 'pictureUrl', 'value': 'https://good.pic/pic.jpg'}
                ]
            ),
        ]
    })))


def test_get_detailed_pictures_batch_errors(stroller):
    """
    Проверяем, что батчевая ручка вердиктов тоже отдает детальные ошибки по картинкам
    """

    request = message_from_data({'offer_ids': ['picture.errors', 'another.picture.errors']}, GetVerdictsBatchRequest())
    response = stroller.post('/v1/partners/1/offers/verdicts?show_pictures=true', data=request.SerializeToString())

    assert_that(response, HasStatus(200))

    assert_that(response.data, IsSerializedProtobuf(GetVerdictsBatchResponse, {
        'entries': [
            {
                # Check some verdicts for first offer
                'verdicts': {
                    'messages': [
                        {
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'picture.errors', 'shop_id': 1},
                                {'business_id': 1, 'offer_id': 'picture.errors', 'shop_id': 2}
                            ],
                            'explanation': {
                                'namespace': 'shared.indexer.error.codes',
                                'code': '49f',
                                'level': EXP.Explanation.Level.ERROR,
                                'params': [
                                    {
                                        'name': 'pictureUrl',
                                        'value': 'https://failed.by/copier.jpg'
                                    },
                                ]
                            }
                        },
                    ]
                }
            },
            {
                # Check some verdicts for second offer
                'verdicts': {
                    'messages': [
                        {
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'another.picture.errors', 'shop_id': 2}
                            ],
                            'explanation': {
                                'namespace': 'shared.indexer.error.codes',
                                'code': '49e',
                                'level': EXP.Explanation.Level.ERROR,
                                'params': [
                                    {
                                        'name': 'pictureUrl',
                                        'value': gen_std_partner_error_pic_url(PICTURE_STD_PARTNER_ERRORS[0])
                                    },
                                    {
                                        'name': 'code',
                                        'value': str(PICTURE_STD_PARTNER_ERRORS[0])
                                    }
                                ]
                            }
                        },
                        {
                            'identifiers': [
                                {'business_id': 1, 'offer_id': 'another.picture.errors', 'shop_id': 2}
                            ],
                            'explanation': {
                                'namespace': 'shared.indexer.error.codes',
                                'code': '49f',
                                'level': EXP.Explanation.Level.ERROR,
                                'params': [
                                    {
                                        'name': 'pictureUrl',
                                        'value': 'https://failed.by/copier.jpg'
                                    },
                                ]
                            }
                        },
                    ]
                }
            }
        ]
    }))


def test_filter_recommendations(stroller):
    not_expected_codes = [
        'some_recommendation_warning',
        'some_recommendation_error'
    ]

    # Один обычный вердикт от одного источника и рекомендации от другого
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=offer_with_recommendation1&show_all=true')
    assert_that(response, HasStatus(200))

    # Не показываем рекомендации по размещению
    for code in not_expected_codes:
        assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
            'messages': [
                {
                    'explanation': {
                        'code': code
                    }
                },
            ]
        })))

    # При этом не потеряли вердикт, который показать должны
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'offer_with_recommendation1'}
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'some_verdict1',
                    'level': EXP.Explanation.Level.ERROR
                },
                'data_source': DTC.MARKET_MBO,
            }
        ]
    }))

    # Вердикт от одного источника, вердикт и рекомендации от другого
    response = stroller.get('/v1/partners/1/offers/verdicts?offer_id=offer_with_recommendation2&show_all=true')
    assert_that(response, HasStatus(200))

    # Не показываем рекомендации по размещению
    for code in not_expected_codes:
        assert_that(response.data, not_(IsSerializedProtobuf(GetVerdictsResponse, {
            'messages': [
                {
                    'explanation': {
                        'code': code
                    }
                },
            ]
        })))

    # Два вердикта должны вернуть
    assert_that(response.data, IsSerializedProtobuf(GetVerdictsResponse, {
        'messages': [
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'offer_with_recommendation2'}
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'some_verdict2',
                    'level': EXP.Explanation.Level.ERROR
                }
            },
            {
                'identifiers': [
                    {'business_id': 1, 'offer_id': 'offer_with_recommendation2'}
                ],
                'explanation': {
                    'namespace': 'shared.indexer.error.codes',
                    'code': 'not_recommendation_verdict',
                    'level': EXP.Explanation.Level.ERROR
                }
            }
        ]
    }))
