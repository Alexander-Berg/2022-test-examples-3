import base64
import pytest
import inject

from collections import defaultdict
from dataclasses import dataclass

from typing import Any, Optional
from market.mars.yql.proto import yt_content_pb2
from market.mars.yql.profit_index_daily.fill_profit_index_table.main import (
    TReadState,
    TAggregationState,
    TProfitIndexEngine,
    EFmcgSegments,
    TPictureData,
    CONTENT_KEY,
    PROFITS_PRODUCTS_KEY,
    FMCG_PROFITS_PRODUCTS_KEY,
    INDEX_VALUE_KEY,
    CATEGORY_INDEX_VALUE_KEY,
    HARDCODED_PROFIT_INDEX,
    TODAY_STR,
    VERSION,
    USE_PROFIT_INDEX_VALUE_ROUNDING,
    ROUND_DIGITS,
)

from google.protobuf.message import Message
from google.protobuf.json_format import MessageToDict

TEST_VERSION = "test_version"


def read_category(category: yt_content_pb2.TCategory) -> dict[str, Any]:
    return {
        category.experiment: {
            category.hid: {
                'category_hid_level_3': category.hid,
                'category_name_level_3': category.name,
                'cashback_impact': category.index_structure.cashback_impact,
                'discount_impact': category.index_structure.discount_impact,
                'price_impact': category.index_structure.price_impact,
                'promo_impact': category.index_structure.promo_impact,
                'index': category.category_index,
                'models': [
                    {
                        'base_price': proto_model.base_price,
                        'model_id': proto_model.model_id,
                        'price_benefit_miltiplier': proto_model.price_benefit_miltiplier,
                    }
                    for proto_model in category.models
                ],
            }
        }
    }


def read_models_block(models_block: yt_content_pb2.TModelsBlock) -> dict[str, Any]:
    return {
        models_block.experiment: [
            {'base_price': model.base_price, 'model_id': model.model_id, 'multiplier': model.price_benefit_multiplier}
            for model in models_block.models
        ],
    }


def read_fmcg_models_block(fmcg_models_block: yt_content_pb2.TSegmentModelsBlock) -> dict[str, Any]:
    return {
        fmcg_models_block.experiment: {
            fmcg_models_block.name: [
                {
                    'base_price': model.base_price,
                    'model_id': model.model_id,
                    'multiplier': model.price_benefit_multiplier,
                }
                for model in fmcg_models_block.models
            ]
        }
    }


def read_index_value(index_value: yt_content_pb2.TIndexValue) -> dict[str, Any]:
    return {
        index_value.experiment: (
            defaultdict(int)
            | {
                "cashback_impact": index_value.index_structure.cashback_impact,
                "discount_impact": index_value.index_structure.discount_impact,
                "price_impact": index_value.index_structure.price_impact,
                "promo_impact": index_value.index_structure.promo_impact,
                "index": index_value.index,
            }
        )
    }


def read_category_index_value(category_index_value: yt_content_pb2.TCategoryIndexValue) -> dict[str, Any]:
    return {
        category_index_value.experiment: {
            category_index_value.hid: defaultdict(int)
            | {
                "category_hid_level_3": category_index_value.hid,
                "category_name_level_3": category_index_value.name,
                "index": category_index_value.index,
                "cashback_impact": category_index_value.index_structure.cashback_impact,
                "discount_impact": category_index_value.index_structure.discount_impact,
                "price_impact": category_index_value.index_structure.price_impact,
                "promo_impact": category_index_value.index_structure.promo_impact,
                "models": [],
            },
        },
    }


@dataclass
class TAggregationCase:
    read_state: TReadState
    expected_aggregation_state: TAggregationState
    name: str
    today_str: Optional[str] = None


def assert_aggregation_states_equal(actual: TAggregationState, expected: TAggregationState) -> None:
    """
    Проверяем, что expected вложимо в actual. Проверяем именно вложимость, потому что
    одна прочитанная таблица может породить несоклько строчек на выходе.
    """
    actual_rows = {row['key']: row for row in actual.rows}
    for expected_row in expected.rows:
        actual_row = actual_rows[expected_row['key']]
        for expected_key in expected_row:
            assert expected_key in actual_row
            if expected_key == CONTENT_KEY:
                message: Message = type(expected_row[expected_key])()
                message.ParseFromString(base64.b64decode(actual_row[expected_key]))
                actual_value = MessageToDict(message)
                expected_value = MessageToDict(expected_row[expected_key])
                assert expected_value.items() <= actual_value.items()
            else:
                assert expected_row[expected_key] == actual_row[expected_key]


TEST_CASES_PROFIT_INDEX_AGGREGATION = []

BASELINE_MODELS_BLOCK = yt_content_pb2.TModelsBlock(
    experiment="exp",
    models=[
        yt_content_pb2.TModel(base_price=10, model_id=0, price_benefit_multiplier=0.4),
        yt_content_pb2.TModel(base_price=20, model_id=1, price_benefit_multiplier=2.3),
    ],
    today_str="2021-11-11",
)

TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(experiment_profit_products=read_models_block(models_block)),
        TAggregationState(
            rows=[
                {
                    "key": f"{PROFITS_PRODUCTS_KEY}_{models_block.experiment}",
                    "content": models_block,
                }
            ]
        ),
        "проверяем формат продуктового блока",
        today_str=models_block.today_str,
    )
    for models_block in [BASELINE_MODELS_BLOCK]
]

BASELINE_FMCG_MODELS_BLOCKS = [
    yt_content_pb2.TSegmentModelsBlock(
        experiment="exp",
        id=segment.value.id,
        name=segment.value.translation,
        models=[
            yt_content_pb2.TModel(base_price=10 + segment.value.id, model_id=0, price_benefit_multiplier=0.4),
            yt_content_pb2.TModel(base_price=20 + segment.value.id, model_id=1, price_benefit_multiplier=2.3),
        ],
        today_str="2021-11-11",
    )
    for segment in EFmcgSegments
]

TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(experiment_fmcg_profit_products=read_fmcg_models_block(fmcg_models_block)),
        TAggregationState(
            rows=[
                {
                    "key": f"{FMCG_PROFITS_PRODUCTS_KEY}_{fmcg_models_block.id}_{fmcg_models_block.experiment}",
                    "content": fmcg_models_block,
                }
            ]
        ),
        "проверяем формат блока fmcg продуктов",
        today_str=fmcg_models_block.today_str,
    )
    for fmcg_models_block in BASELINE_FMCG_MODELS_BLOCKS
]

BASELINE_CATEGORY = yt_content_pb2.TCategory(hid=1, name="first categroy", experiment="exp", today_str="2021-11-11")

TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(experiment_top_categories=read_category(category)),
        TAggregationState(
            rows=[
                {
                    "key": f"category_{category.hid}_{category.experiment}",
                    "content": category,
                }
            ]
        ),
        "проверяем формат категории",
        today_str=category.today_str,
    )
    for category in [BASELINE_CATEGORY]
]

BASELINE_INDEX_VALUE = yt_content_pb2.TIndexValue(
    index=10,
    index_structure=yt_content_pb2.TIndexStructure(
        cashback_impact=4.0,
        discount_impact=3.1,
        price_impact=2.0,
        promo_impact=0.9,
    ),
    experiment="exp",
)


TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(experiment_header=read_index_value(index_value)),
        TAggregationState(
            rows=[
                {
                    "key": f"{INDEX_VALUE_KEY}_{index_value.experiment}",
                    "content": index_value,
                }
            ]
        ),
        "проверяем формат значение индекса выгодности",
    )
    for index_value in [BASELINE_INDEX_VALUE]
]

ROUNDING_INDEX_VALUES = [
    yt_content_pb2.TIndexValue(
        index=9.41,
        index_structure=yt_content_pb2.TIndexStructure(
            cashback_impact=4.19,
            discount_impact=3.16,
            price_impact=1.74,
            promo_impact=0.32,
        ),
        experiment="exp_rounding",
    ),
    yt_content_pb2.TIndexValue(
        index=9.1081,
        index_structure=yt_content_pb2.TIndexStructure(
            cashback_impact=3.999,
            discount_impact=2.4545,
            price_impact=2.3334,
            promo_impact=0.3212,
        ),
        experiment="exp_rounding",
    ),
]

TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(experiment_header=read_index_value(index_value)),
        TAggregationState(
            rows=[
                {
                    "key": f"{INDEX_VALUE_KEY}_{index_value.experiment}",
                    "content": yt_content_pb2.TIndexValue(
                        index=round(index_value.index, 1),
                        index_structure=yt_content_pb2.TIndexStructure(
                            cashback_impact=round(index_value.index_structure.cashback_impact, 1),
                            discount_impact=round(index_value.index_structure.discount_impact, 1),
                            price_impact=round(index_value.index_structure.price_impact, 1),
                            promo_impact=round(index_value.index_structure.promo_impact, 1),
                        ),
                        experiment=index_value.experiment,
                    ),
                }
            ]
        ),
        "проверяем округление индекса выгодности",
    )
    for index_value in ROUNDING_INDEX_VALUES
]

TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(
            experiment_header=read_index_value(
                yt_content_pb2.TIndexValue(
                    index=6.196,
                    index_structure=yt_content_pb2.TIndexStructure(
                        cashback_impact=1.549,
                        discount_impact=1.549,
                        price_impact=1.549,
                        promo_impact=1.549,
                    ),
                    experiment="exp_rounding_corner",
                )
            )
        ),
        TAggregationState(
            rows=[
                {
                    "key": f"{INDEX_VALUE_KEY}_exp_rounding_corner",
                    "content": yt_content_pb2.TIndexValue(
                        index=6.2,
                        index_structure=yt_content_pb2.TIndexStructure(
                            cashback_impact=1.6,
                            discount_impact=1.6,
                            price_impact=1.5,
                            promo_impact=1.5,
                        ),
                        experiment="exp_rounding_corner",
                    ),
                }
            ]
        ),
        "проверяем округление (спец кейс с недобором)",
    ),
    TAggregationCase(
        TReadState(
            experiment_header=read_index_value(
                yt_content_pb2.TIndexValue(
                    index=6.24,
                    index_structure=yt_content_pb2.TIndexStructure(
                        cashback_impact=1.56,
                        discount_impact=1.56,
                        price_impact=1.56,
                        promo_impact=1.56,
                    ),
                    experiment="exp_rounding_corner_2",
                )
            )
        ),
        TAggregationState(
            rows=[
                {
                    "key": f"{INDEX_VALUE_KEY}_exp_rounding_corner_2",
                    "content": yt_content_pb2.TIndexValue(
                        index=6.2,
                        index_structure=yt_content_pb2.TIndexStructure(
                            cashback_impact=1.5,
                            discount_impact=1.5,
                            price_impact=1.6,
                            promo_impact=1.6,
                        ),
                        experiment="exp_rounding_corner_2",
                    ),
                }
            ]
        ),
        "проверяем округление (спец кейс с перебором)",
    ),
    TAggregationCase(
        TReadState(
            experiment_header=read_index_value(
                yt_content_pb2.TIndexValue(
                    index=10,
                    index_structure=yt_content_pb2.TIndexStructure(
                        cashback_impact=1.06,
                        discount_impact=1.06,
                        price_impact=1.06,
                        promo_impact=6.82,
                    ),
                    experiment="exp_rounding_corner_3",
                )
            )
        ),
        TAggregationState(
            rows=[
                {
                    "key": f"{INDEX_VALUE_KEY}_exp_rounding_corner_3",
                    "content": yt_content_pb2.TIndexValue(
                        index=10,
                        index_structure=yt_content_pb2.TIndexStructure(
                            cashback_impact=1.0,
                            discount_impact=1.1,
                            price_impact=1.1,
                            promo_impact=6.8,
                        ),
                        experiment="exp_rounding_corner_3",
                    ),
                }
            ]
        ),
        "проверяем округление (спец кейс с превышением значения 10)",
    ),
]

BASELINE_CATEGORY_INDEX_VALUE = yt_content_pb2.TCategoryIndexValue(
    hid=1,
    name="nike",
    index=9.9,
    index_structure=yt_content_pb2.TIndexStructure(
        cashback_impact=4.2,
        discount_impact=3.5,
        price_impact=1.3,
        promo_impact=0.9,
    ),
    experiment="exp",
)


TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(experiment_top_categories=read_category_index_value(category_index_value)),
        TAggregationState(
            rows=[
                {
                    "key": f"{CATEGORY_INDEX_VALUE_KEY}_{category_index_value.hid}_{category_index_value.experiment}",
                    "content": category_index_value,
                }
            ]
        ),
        "проверяем формат значения индекса выгодности для категории",
    )
    for category_index_value in [BASELINE_CATEGORY_INDEX_VALUE]
]


TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(
            experiment_header={"": defaultdict(int) | {"index": 10}},
            category_pictures={1: TPictureData("url.com", 11)},
            experiment_top_categories={
                "": {
                    1: {
                        "combined_rank": 10,
                        "index": 10,
                        "category_hid_level_3": 1,
                        "category_name_level_3": "first",
                        "models": [],
                    }
                    | defaultdict(int)
                }
            },
        ),
        TAggregationState(
            rows=[
                {
                    "key": "head",
                    "content": yt_content_pb2.THead(
                        index=10,
                        top_categories=[
                            yt_content_pb2.TCategoryMeta(
                                hid=1,
                                name="first",
                                image="url.com",
                                imageHd="url.com",
                                image_model_id=11,
                                index=10,
                            )
                        ],
                    ),
                }
            ]
        ),
        "категории с картинками",
    ),
    TAggregationCase(
        TReadState(
            experiment_header={"": defaultdict(int) | {'index': 10}},
            experiment_top_categories={
                "": {
                    1: {
                        "combined_rank": 10,
                        "index": 10,
                        "category_hid_level_3": 1,
                        "category_name_level_3": "first",
                        "models": [],
                    }
                    | defaultdict(int)
                }
            },
        ),
        TAggregationState(
            rows=[
                {
                    "key": "head",
                    "content": yt_content_pb2.THead(
                        index=10,
                        top_categories=[
                            yt_content_pb2.TCategoryMeta(
                                hid=1,
                                name="first",
                                index=10,
                            )
                        ],
                    ),
                }
            ]
        ),
        "категории без картинок",
    ),
    TAggregationCase(
        TReadState(
            experiment_header={
                "exp": defaultdict(int)
                | {"index": 10, "cashback_impact": 3, "discount_impact": 1, "price_impact": 2, "promo_impact": 4}
            }
        ),
        TAggregationState(
            rows=[
                {
                    "key": "head_exp",
                    "content": yt_content_pb2.THead(
                        experiment='exp',
                        index=5,
                        index_structure=yt_content_pb2.TIndexStructure(
                            cashback_impact=1.5, discount_impact=0.5, price_impact=1, promo_impact=2
                        ),
                    ),
                }
            ]
        ),
        "хардкод значения индекса выгодности",
        "2011-11-11",
    ),
    TAggregationCase(
        TReadState(experiment_header={'exp': defaultdict(int) | {'index': 10}}),
        TAggregationState(rows=[{'key': 'head_exp', 'content': yt_content_pb2.THead(top_segments=[0, 2, 1, 3, 4, 5])}]),
        "сегменты в хедере",
    ),
    TAggregationCase(
        TReadState(previous_index_value={'exp': 7}, experiment_header={'exp': defaultdict(int) | {'index': 10}}),
        TAggregationState(rows=[{'key': 'head_exp', 'content': yt_content_pb2.THead(prev_index=7)}]),
        "предыдущее значение индекса выгодности",
    ),
]

TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(experiment_profit_products=read_models_block(models_block)),
        TAggregationState(
            rows=[
                {
                    "key": f"{PROFITS_PRODUCTS_KEY}_{models_block.experiment}",
                    "content": models_block,
                }
            ]
        ),
        "проверяем наличие today_str в модельном блоке",
        "2020-11-11",
    )
    for models_block in [
        yt_content_pb2.TModelsBlock(
            experiment="exp",
            today_str="2020-11-11",
            models=[
                yt_content_pb2.TModel(base_price=10, model_id=0, price_benefit_multiplier=0.4),
                yt_content_pb2.TModel(base_price=20, model_id=1, price_benefit_multiplier=2.3),
            ],
        ),
    ]
]

TEST_CASES_PROFIT_INDEX_AGGREGATION += [
    TAggregationCase(
        TReadState(experiment_profit_products=read_models_block(models_block)),
        TAggregationState(
            rows=[
                {
                    "key": f"{PROFITS_PRODUCTS_KEY}_{models_block.experiment}",
                    "content": models_block,
                }
            ]
        ),
        "проверяем наличие today_str в блоке сегментов",
        models_block.today_str,
    )
    for models_block in [
        yt_content_pb2.TModelsBlock(
            experiment="exp",
            today_str="2020-11-11",
            models=[
                yt_content_pb2.TModel(base_price=10, model_id=0, price_benefit_multiplier=0.4),
                yt_content_pb2.TModel(base_price=20, model_id=1, price_benefit_multiplier=2.3),
            ],
        ),
    ]
]


def init_di(today_str: str) -> None:
    inject.clear_and_configure(
        lambda binder: binder.bind(VERSION, TEST_VERSION)
        .bind(HARDCODED_PROFIT_INDEX, {'2011-11-11': 5})
        .bind(TODAY_STR, today_str)
        .bind(USE_PROFIT_INDEX_VALUE_ROUNDING, True)
        .bind(ROUND_DIGITS, 1)
    )


@pytest.mark.parametrize("t", TEST_CASES_PROFIT_INDEX_AGGREGATION)
def test_aggregaion(t: TAggregationCase) -> None:
    init_di(t.today_str or '2021-01-01')
    assert_aggregation_states_equal(TProfitIndexEngine().Aggregate(t.read_state), t.expected_aggregation_state)
