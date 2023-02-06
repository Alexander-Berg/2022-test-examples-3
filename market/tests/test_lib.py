import pytest
import dataclasses
import typing as tp
from market.assortment.ecom_log.lib.export.internal_export import set_traffic_source


@dataclasses.dataclass
class TraficSourceCase:
    engine_ids: tp.Dict[bytes, tp.Optional[int]]
    result_trafic_source: str


TRAFIC_SOURCE_CASES = [
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': None,
            b'AdvEngineID': None,
            b'TraficSourceID': None
        },
        result_trafic_source=u'Другое'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': None,
            b'AdvEngineID': 12,
            b'TraficSourceID': None
        },
        result_trafic_source=u'Яндекс.Маркет'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 14,
            b'AdvEngineID': None,
            b'TraficSourceID': None
        },
        result_trafic_source=u'Яндекс.Маркет'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 2,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Яндекс.Органика'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 13,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Яндекс.Органика'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 181,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Яндекс.Органика'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 2,
            b'AdvEngineID': 1,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'Поиск.Яндекс.Директ'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 2,
            b'AdvEngineID': 71,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'Поиск.Яндекс.Директ'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 2,
            b'AdvEngineID': 73,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'Поиск.Яндекс.Директ'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 13,
            b'AdvEngineID': 71,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'Поиск.Яндекс.Директ'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 181,
            b'AdvEngineID': 73,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'Поиск.Яндекс.Директ'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': None,
            b'AdvEngineID': 1,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'ВнеПоиска.Яндекс.Директ'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': None,
            b'AdvEngineID': 71,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'ВнеПоиска.Яндекс.Директ'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': None,
            b'AdvEngineID': 73,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'ВнеПоиска.Яндекс.Директ'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': -1,
            b'AdvEngineID': 73,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'ВнеПоиска.Яндекс.Директ'
    ),

    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 3,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Google.Органика'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 67,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Google.Органика'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 68,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Google.Органика'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 224,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Google.Органика'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 3,
            b'AdvEngineID': 2,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'Поиск.Google.Ads'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 67,
            b'AdvEngineID': 67,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'Поиск.Google.Ads'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': None,
            b'AdvEngineID': 2,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'ВнеПоиска.Google.Ads'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': None,
            b'AdvEngineID': 67,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'ВнеПоиска.Google.Ads'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': -1,
            b'AdvEngineID': 2,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'ВнеПоиска.Google.Ads'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 239,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Яндекс.Вертикаль.Товары'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 239,
            b'AdvEngineID': 1,
            b'TraficSourceID': 3
        },
        result_trafic_source=u'Поиск.Яндекс.Вертикаль.Товары'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 1,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Яндекс.Вертикаль.Картинки'
    ),
    TraficSourceCase(
        engine_ids={
            b'SearchEngineID': 235,
            b'AdvEngineID': None,
            b'TraficSourceID': 2
        },
        result_trafic_source=u'Поиск.Яндекс.Вертикаль.Картинки'
    )
]


@pytest.mark.parametrize('case', TRAFIC_SOURCE_CASES)
def test_set_traffic_source(case: TraficSourceCase) -> None:
    fields = case.engine_ids
    result_trafic_source = case.result_trafic_source

    assert set_traffic_source(fields) == result_trafic_source
