# coding: utf-8

from mock import patch
import pytest
from datetime import timedelta, datetime
from market.idx.admin.mi_agent.lib.checkers import indexing_time_checker
import market.idx.pylibrary.mindexer_core.geninfo.geninfo as geninfo
from collections import namedtuple

Generation = namedtuple('Generation', ['status', 'name', 'end_date'])
Config = namedtuple('Config', ['indexing_active_threshold_minutes', 'indexing_reserve_threshold_minutes', 'indexing_time_check_generation_count'])

DATASOURCES = 'datasources'
MITYPE_GIBSON = 'gibson'
MITYPE_STRAT = 'stratocaster'
THRESHOLD = 200
GEN_COUNT = 3
Zk = namedtuple('Zk', ['get_master_dir'])

CANCELLED_GEN = Generation(geninfo.STATUS_CANCELLED, '', '')
INPROGRESS_GEN = Generation(geninfo.STATUS_INPROGRESS, '', '')
COMPLETED_GEN = Generation(geninfo.STATUS_COMPLETED, '', '')

mi_agent_config = Config(indexing_active_threshold_minutes=THRESHOLD,
                         indexing_reserve_threshold_minutes=THRESHOLD,
                         indexing_time_check_generation_count=GEN_COUNT)


# если из базы не вернулось ни одного поколения - проверка не пройдет ни на активном, ни на резервном
@pytest.mark.parametrize("data", [
    {'active_mitype': MITYPE_GIBSON, 'current_mitype': MITYPE_GIBSON},
    {'active_mitype': MITYPE_GIBSON, 'current_mitype': MITYPE_STRAT},
])
def test_fail_empty(data):
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: []):
        checker = indexing_time_checker.IndexingTimeChecker(mitype=data['current_mitype'],
                                                            active_mitype=data['active_mitype'],
                                                            datasources=DATASOURCES,
                                                            config=mi_agent_config)
        assert not checker.check()


# проверки на активном: смотрим на время сборки inprogress поколения
@pytest.mark.parametrize("data", [
    {
        'generation_name': '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD - 3)),
        'expected': True
    },
    {
        'generation_name': '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD + 3)),
        'expected': False
    },
])
def test_active(data):
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: [
        Generation(
            status=geninfo.STATUS_INPROGRESS,
            name=data['generation_name'],
            end_date=''
        )
    ]):
        checker = indexing_time_checker.IndexingTimeChecker(MITYPE_GIBSON, MITYPE_GIBSON, DATASOURCES, mi_agent_config)
        assert data['expected'] == checker.check()


# проверки на резервном
test_data = [
    # все укладываются в лимит
    {
        'inprogress_generations': [
            Generation(
                geninfo.STATUS_INPROGRESS,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD - 3)),
                ''
            )
        ],
        'completed_generations': [
            Generation(
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            ),
            Generation(
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            ),
            Generation(
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            ),
        ],
        'expected': True,
    },

    # inprogress поколение собирается дольше положенного
    {
        'inprogress_generations': [
            Generation(
                geninfo.STATUS_INPROGRESS,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD + 3)),
                ''
            )
        ],
        'completed_generations': [],
        'expected': False,
    },

    # inprogress ок, одно из completed - не ок
    {
        'inprogress_generations': [
            Generation(
                geninfo.STATUS_INPROGRESS,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD - 3)),
                ''
            )],
        'completed_generations': [
            Generation(
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            ),
            Generation(  # не ок поколение
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD - 10)
            ),
            Generation(
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            ),
        ],
        'expected': False,
    },
    # нет inprogress (он completed)
    {
        'inprogress_generations': [
            Generation(
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            )],
        'completed_generations': [
            Generation(
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            ),
            Generation(  # не ок поколение
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            ),
            Generation(
                geninfo.STATUS_COMPLETED,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD * 2)),
                datetime.now() - timedelta(minutes=THRESHOLD + 10)
            ),
        ],
        'expected': False,
    },
    # нет completed
    {
        'inprogress_generations': [
            Generation(
                geninfo.STATUS_INPROGRESS,
                '{:%Y%m%d_%H%M}'.format(datetime.now() - timedelta(minutes=THRESHOLD - 3)),
                ''
            )],
        'completed_generations': [],
        'expected': False,
    },
]


def mock_due_to_arg(data, *args, **kwargs):
    if kwargs and 'only_successfull' in kwargs and kwargs['only_successfull']:
        return data['completed_generations']
    else:
        return data['inprogress_generations']


@pytest.mark.parametrize("data", test_data)
def test_reserve(data):
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: mock_due_to_arg(data, **kwargs)):
        checker = indexing_time_checker.IndexingTimeChecker(MITYPE_GIBSON, MITYPE_STRAT, DATASOURCES, mi_agent_config)
        assert data['expected'] == checker.check()
