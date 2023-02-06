import mock
import pytest
from collections import namedtuple

from hamcrest import (
    assert_that,
    equal_to,
)

import market.idx.pylibrary.mindexer_core.geninfo.geninfo as geninfo
from market.idx.pylibrary.mindexer_core.mbo_stuff_checks.mbo_stuff_checks import (
    MboStuffStatus,
    check_mbo_stuff_version,
    get_mbo_stuff_status,
    is_mbo_stuff_suspicious,
    use_mbo_stuff_from_last_complete,
)


Generation = namedtuple('Generation', ['name', 'status', 'mbo_stuff'])
MiConfig = namedtuple('Config', ['mitype'])

MITYPE = 'gibson'
MBO_STUFF = '20200601_1000'
MBO_STUFF_OLD = '20200601_0800'
MBO_STUFF_NEW = '20200601_1200'

COMPLETED_OLD_GEN = Generation(name='20200528_0800', status=geninfo.STATUS_COMPLETED, mbo_stuff=MBO_STUFF_OLD)
FAILED_OLD_GEN = Generation(name='20200528_0900', status=geninfo.STATUS_FAILED, mbo_stuff=MBO_STUFF)
COMPLETED_GEN = Generation(name='20200528_1000', status=geninfo.STATUS_COMPLETED, mbo_stuff=MBO_STUFF)
FAILED_GEN = Generation(name='20200528_1100', status=geninfo.STATUS_FAILED, mbo_stuff=MBO_STUFF)
FAILED_GEN_OLD_MBO_STUFF = Generation(name='20200528_1200', status=geninfo.STATUS_FAILED, mbo_stuff=MBO_STUFF_OLD)
FAILED_GEN_WITH_EMPTY_MBO_STUFF = Generation(name='20200528_1300', status=geninfo.STATUS_FAILED, mbo_stuff='')
NO_GENERATION = None


@pytest.fixture
def config():
    return MiConfig(mitype=MITYPE)


@pytest.mark.parametrize("complete, failed, expected", [
    # успешное поколение свежее упавших
    (COMPLETED_GEN, FAILED_OLD_GEN, False),
    # у последнего упавшего поколения и последнего успешного одинаковый mbo_stuff
    (COMPLETED_GEN, FAILED_GEN, False),
    # у последнего упавшего поколения более старый mbo_stuff
    (COMPLETED_GEN, FAILED_GEN_OLD_MBO_STUFF, True),
    # у последнего упавшего поколения более новый mbo_stuff
    (COMPLETED_OLD_GEN, FAILED_GEN, True),
    # у последнего упавшего поколения нет mbo_stuff
    (COMPLETED_GEN, FAILED_GEN_WITH_EMPTY_MBO_STUFF, False),
])
def test_is_mbo_stuff_suspicious(complete, failed, expected):
    assert_that(
        is_mbo_stuff_suspicious(complete, failed),
        equal_to(expected),
    )


@pytest.mark.parametrize("complete, failed, recent_getter, expected", [
    # успешное поколение свежее упавших
    (COMPLETED_GEN, FAILED_OLD_GEN, MBO_STUFF_OLD, MboStuffStatus.STATUS_OK),
    # у последнего упавшего поколения и последнего успешного одинаковый mbo_stuff
    (COMPLETED_GEN, FAILED_GEN, MBO_STUFF_OLD, MboStuffStatus.STATUS_OK),
    # у последнего упавшего поколения более старый mbo_stuff
    (COMPLETED_GEN, FAILED_GEN_OLD_MBO_STUFF, MBO_STUFF_OLD, MboStuffStatus.STATUS_FAILED),
    # у последнего упавшего поколения более новый mbo_stuff
    (COMPLETED_OLD_GEN, FAILED_GEN, MBO_STUFF, MboStuffStatus.STATUS_FAILED),
    # у последнего упавшего поколения более новый mbo_stuff, но свежее поколение в геттере отличается
    (COMPLETED_OLD_GEN, FAILED_GEN, MBO_STUFF_NEW, MboStuffStatus.STATUS_NEW),
    # у последнего упавшего поколения нет mbo_stuff
    (COMPLETED_GEN, FAILED_GEN_WITH_EMPTY_MBO_STUFF, MBO_STUFF, MboStuffStatus.STATUS_OK),
    # одно из поколений не найдено
    (NO_GENERATION, FAILED_GEN, MBO_STUFF, MboStuffStatus.STATUS_UNDEFINED),
    (COMPLETED_GEN, NO_GENERATION, MBO_STUFF, MboStuffStatus.STATUS_UNDEFINED),
])
def test_get_mbo_stuff_status(complete, failed, recent_getter, expected):
    assert_that(
        get_mbo_stuff_status(complete, failed, recent_getter),
        equal_to(expected),
    )


@pytest.mark.parametrize("complete, failed, recent_getter, expected", [
    # успешное поколение свежее упавших
    (COMPLETED_GEN, FAILED_OLD_GEN, MBO_STUFF_OLD, False),
    # у последнего упавшего поколения и последнего успешного одинаковый mbo_stuff
    (COMPLETED_GEN, FAILED_GEN, MBO_STUFF_OLD, False),
    # у последнего упавшего поколения более старый mbo_stuff
    (COMPLETED_GEN, FAILED_GEN_OLD_MBO_STUFF, MBO_STUFF_OLD, True),
    # у последнего упавшего поколения более новый mbo_stuff
    (COMPLETED_OLD_GEN, FAILED_GEN, MBO_STUFF, True),
    # у последнего упавшего поколения более новый mbo_stuff, но свежее поколение в геттере отличается
    (COMPLETED_OLD_GEN, FAILED_GEN, MBO_STUFF_NEW, False),
    # у последнего упавшего поколения нет mbo_stuff
    (COMPLETED_GEN, FAILED_GEN_WITH_EMPTY_MBO_STUFF, MBO_STUFF, False),
    # одно из поколений не найдено
    (NO_GENERATION, FAILED_GEN, MBO_STUFF, False),
    (COMPLETED_GEN, NO_GENERATION, MBO_STUFF, False),
])
def test_use_mbo_stuff_from_last_complete(config, complete, failed, recent_getter, expected):
    with mock.patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_last_complete_generation', return_value=complete), \
            mock.patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_last_failed_generation', return_value=failed), \
            mock.patch('market.idx.pylibrary.mindexer_core.mbo_stuff_checks.mbo_stuff_checks.get_recent_getter_mbo_stuff', return_value=recent_getter):
        actual = use_mbo_stuff_from_last_complete(config)
        assert_that(
            actual,
            equal_to(expected)
        )


@pytest.mark.parametrize("complete, failed, recent_getter, expected", [
    # успешное поколение свежее упавших
    (COMPLETED_GEN, FAILED_OLD_GEN, MBO_STUFF_OLD, '0;OK'),
    # у последнего упавшего поколения и последнего успешного одинаковый mbo_stuff
    (COMPLETED_GEN, FAILED_GEN, MBO_STUFF_OLD, '0;OK'),
    # у последнего упавшего поколения более старый mbo_stuff
    (COMPLETED_GEN, FAILED_GEN_OLD_MBO_STUFF, MBO_STUFF_OLD, '2;Failed generation 20200528_1200 with suspicious mbo_stuff: 20200601_0800'),
    # у последнего упавшего поколения более новый mbo_stuff
    (COMPLETED_OLD_GEN, FAILED_GEN, MBO_STUFF, '2;Failed generation 20200528_1100 with suspicious mbo_stuff: 20200601_1000'),
    # у последнего упавшего поколения более новый mbo_stuff, но свежее поколение в геттере отличается
    (COMPLETED_OLD_GEN, FAILED_GEN, MBO_STUFF_NEW, '1;New mbo_stuff downloaded: 20200601_1200'),
    # у последнего упавшего поколения нет mbo_stuff
    (COMPLETED_GEN, FAILED_GEN_WITH_EMPTY_MBO_STUFF, MBO_STUFF, '0;OK'),
    # одно из поколений не найдено
    (NO_GENERATION, FAILED_GEN, MBO_STUFF, '2;Generation stats not found'),
    (COMPLETED_GEN, NO_GENERATION, MBO_STUFF, '2;Generation stats not found'),
])
def test_check_mbo_stuff_version(config, complete, failed, recent_getter, expected):
    with mock.patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_last_complete_generation', return_value=complete), \
            mock.patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_last_failed_generation', return_value=failed), \
            mock.patch('market.idx.pylibrary.mindexer_core.mbo_stuff_checks.mbo_stuff_checks.get_recent_getter_mbo_stuff', return_value=recent_getter):
        actual = check_mbo_stuff_version(config)
        assert_that(
            actual,
            equal_to(expected)
        )
