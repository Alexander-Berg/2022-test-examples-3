# coding: utf-8

from market.idx.datacamp.proto.offer.OfferStatus_pb2 import OfferStatus
from market.idx.datacamp.proto.offer.ContentStatus_pb2 import ResultContentStatus
from market.idx.devtools.dukalis.library.analyzer.common.item_result_state import ItemResultState
from market.idx.devtools.dukalis.library.analyzer.common.analyzer_params import AnalyzerParams

from market.idx.devtools.dukalis.offer_diagnostics.lib.analyzer.items.datacamp_items import (
    INTEGRAL_STATUSES_TABLE,
    CARD_STATUSES_TABLE,
    IntegralStatusAnalizerItem,
    CardStatusAnalizerItem,
)


def test_integral_status_table():
    '''
    Проверяем соответствие статусов в proto и Дукалис. Если этот тест падает, то статус поменялся в протобафе.
    Нужно привести в соответствие таблицу INTEGRAL_STATUSES_TABLE
    '''

    proto_tables = OfferStatus.ResultStatus.keys()
    assert frozenset(proto_tables) == frozenset(INTEGRAL_STATUSES_TABLE), "OfferStatus.proto was changed. Fix  INTEGRAL_STATUSES_TABLE"


def test_card_status_table():
    '''
    Проверяем соответствие статусов в proto и Дукалис. Если этот тест падает, то статус поменялся в протобафе.
    Нужно привести в соответствие таблицу CARD_STATUSES_TABLE
    '''

    proto_tables = ResultContentStatus.CardStatus.keys()
    assert frozenset(proto_tables) == frozenset(CARD_STATUSES_TABLE), "ContentStatus.proto was changed. Fix  CARD_STATUSES_TABLE"


class MockDataCampInfoGetter():
    def __init__(self, integral_status=None, card_status=None):
        self.integral_status = integral_status
        self.card_status = card_status

    def get_integral_status(self):
        return self.integral_status

    def get_card_status(self):
        return self.card_status


def test_IntegralStatusAnalizerItem():
    params = AnalyzerParams()
    item = IntegralStatusAnalizerItem(params)

    datacamp = MockDataCampInfoGetter(integral_status="NOT_PUBLISHED_DISABLED_BY_PARTNER")
    res = item.make_decision(datacamp)
    assert res[0] == "Не готов к продаже"
    assert res[1] == ItemResultState.OK
    assert res[2] == "Скрыт партнером"

    datacamp = MockDataCampInfoGetter(integral_status="SOME_UNKWNOWN_STATUS")
    res = item.make_decision(datacamp)
    assert res[0] == "Статус неизвестен"
    assert res[1] == ItemResultState.Unknown
    assert res[2] == "SOME_UNKWNOWN_STATUS"

    datacamp = MockDataCampInfoGetter(integral_status=None)
    res = item.make_decision(datacamp)
    assert res[0] == "Статус не найден"
    assert res[1] == ItemResultState.Bad
    assert res[2] is None


def test_CardStatusAnalizerItem():
    params = AnalyzerParams()
    item = CardStatusAnalizerItem(params)

    datacamp = MockDataCampInfoGetter(card_status="HAS_CARD_MARKET")
    res = item.make_decision(datacamp)
    assert res[0] == "Есть карточка"
    assert res[1] == ItemResultState.OK
    assert res[2] == "Карточка Маркета"

    datacamp = MockDataCampInfoGetter(card_status="SOME_UNKWNOWN_STATUS")
    res = item.make_decision(datacamp)
    assert res[0] == "Статус неизвестен"
    assert res[1] == ItemResultState.Unknown
    assert res[2] == "SOME_UNKWNOWN_STATUS"

    datacamp = MockDataCampInfoGetter(card_status=None)
    res = item.make_decision(datacamp)
    assert res[0] == "Статус не найден"
    assert res[1] == ItemResultState.Bad
    assert res[2] is None


def check_table(table):
    for k, v in table.items():
        assert k
        assert len(v) == 2
        assert v[0]
        assert v[1]


def test_tables():
    """
    Проверяем правильность заполнения таблиц интегрального статуса и карточки
    """
    check_table(INTEGRAL_STATUSES_TABLE)
    check_table(CARD_STATUSES_TABLE)
