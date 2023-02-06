import sys
from copy import deepcopy
import numpy as np
import os
from test_utils import *


def append_arcadia_root_dir_to_path(current_dir):
    path_list = current_dir.split("/")
    arcadia_ix = next(ix for ix, v in enumerate(reversed(path_list)) if v == 'arcadia')
    arcadia_root_dir = '/'.join(path_list[:-arcadia_ix])

    if arcadia_root_dir not in sys.path:
        sys.path.append(arcadia_root_dir)


np.seterr(divide='ignore', invalid='ignore')
BOOST_SIMULATOR_DIR = os.path.dirname(os.path.realpath(__file__))
append_arcadia_root_dir_to_path(BOOST_SIMULATOR_DIR)

import market.analytics.market_search.boost_optimizer.offline.boost_simulator.main as bs
from market.analytics.market_search.boost_optimizer.offline.boost_simulator.library.simulator import Serp, Component


def compare_debug_boost(correct, output, msg, unwrap=True):
    if len(correct) != len(output):
        raise ValueError('incorrect output size: expected: {}, got: {}'.format(len(correct), len(output)))

    for ix, (ll, rr) in enumerate(zip(correct, output)):
        rr = rr['json.debugBoost'] if unwrap else rr
        if ll != rr:
            raise ValueError(make_json_diff_msg(ll, rr, msg))


def test_serpSetPreparer(input, correct_output,
                         fill_inbetween_docs_without_meta=True, debug=False, add_report_indices=False):
    prep = bs.SerpSetPreparer(
        fill_inbetween_docs_without_meta=fill_inbetween_docs_without_meta,
        debug=debug,
        add_report_indices=add_report_indices
    )

    serp = {'components': input}
    out = prep._prepare_serp(serp)
    out_components = out['components']
    compare_debug_boost(correct_output, out_components, '')


def run_SerpSetPreparer():
    # Проверяем Мету
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 10, 'cpmBase': 6}, },
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 9, 'cpmBase': 7}, },
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 8, 'cpmBase': 8}, },
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 10, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': True, 'filledCpmMeta': 9, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 1},
            {'isMeta': True, 'filledCpmMeta': 8, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 2}
        ]
    )

    # Проверяем, что последний не на Мете
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 10, 'cpmBase': 6}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 10}, },
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 10, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 10, 'shouldIgnore': False, 'rank': 1}
        ]
    )

    # Проверяем, что первый документ без Меты заполняется
    # при наличии fill_inbetween_docs_without_meta=True
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 10}, },
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 7, 'cpmBase': 9}, },
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 7, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': True, 'filledCpmMeta': 7, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 1}
        ]
    )

    # Проверяем, что в документах без Меты, которые находятся между документов с Метой,
    # значение Меты заполняется
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 8, 'cpmBase': 8}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 10}, },
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 7, 'cpmBase': 9}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 9}, },
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 6, 'cpmBase': 9}, },
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 8, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': True, 'filledCpmMeta': 7.5, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 1},
            {'isMeta': True, 'filledCpmMeta': 7, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 2},
            {'isMeta': True, 'filledCpmMeta': 6.5, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 3},
            {'isMeta': True, 'filledCpmMeta': 6, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 4}
        ]
    )

    # Проверяем, что если идет два документа без Меты подряд,
    # то эти и последующие документы относятся к Базовому поиску, а не к Мете:
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 8, 'cpmBase': 8}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 10}, },  # <--
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 9}, },  # <--
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 6, 'cpmBase': 9}, },
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 8, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 10, 'shouldIgnore': False, 'rank': 1},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 9, 'shouldIgnore': False, 'rank': 2},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 9, 'shouldIgnore': False, 'rank': 3}
        ]
    )

    # v2: Проверяем, что если идет два документа без Меты подряд,
    # то эти и последующие документы относятся к Базовому поиску, а не к Мете:
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 10}, },  # <--
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 9}, },  # <--
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 6, 'cpmBase': 9}, },
        ],
        [
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 10, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 9, 'shouldIgnore': False, 'rank': 1},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 9, 'shouldIgnore': False, 'rank': 2}
        ]
    )

    # Проверяем, что документы. которые попадают на базовый,
    # но не имеют проставленного значения cpmBase, игнорируются,
    # т.е. мы проставляем им shouldIgnore=True и не проставляем позицию
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 10, 'cpmBase': 6}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': None}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 10}, },
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 7, 'cpmBase': None}, },  # <--
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 7, 'cpmBase': 7}, },
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 10, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            # Игнорируем ,т.к. нет значения базовой.
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'rank': None},
            # Позиция не была инкрементирована на предыдущем этапе
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 10, 'shouldIgnore': False, 'rank': 1},
            # Игнорируем ,т.к. нет значения базовой.
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'rank': None},
            # Позиция не была инкрементирована на предыдущем этапе
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 7, 'shouldIgnore': False, 'rank': 2}
        ]
    )

    # v2 Проверяем, что документы. которые попадают на базовый,
    # но не имеют проставленного значения cpmBase, игнорируются,
    # т.е. мы проставляем им shouldIgnore=True и не проставляем позицию
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 10, 'cpmBase': 6}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': None}, },
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 8, 'cpmBase': 8}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': None}, },  # <--
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 7}, },
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': None}, },  # <--
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 6}, },
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 10, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': True, 'filledCpmMeta': 9.0, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 1},
            {'isMeta': True, 'filledCpmMeta': 8, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 2},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'rank': None},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 7, 'shouldIgnore': False, 'rank': 3},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'rank': None},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 6, 'shouldIgnore': False, 'rank': 4}
        ]
    )

    # Проверяем, что спонсорские сниппеты правильно не влияют на простановку позиции
    # и заполнение меты
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 10, 'cpmBase': 6, 'sponsored': False}},
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': None, 'sponsored': False}},
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': None, 'sponsored': True}},  # <--
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 8, 'cpmBase': 8, 'sponsored': False}},
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 10, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': True, 'filledCpmMeta': 9, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 1},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'rank': 2},
            {'isMeta': True, 'filledCpmMeta': 8, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 3},
        ],
        debug=False
    )

    # v2 Проверяем, что спонсорские сниппеты правильно не влияют на простановку позиции
    # и заполнение меты
    test_serpSetPreparer(
        [
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 10, 'cpmBase': 6, 'sponsored': False}},
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': None, 'sponsored': False}},
            {'json.boostData': {'hasMeta': False, 'cpmMeta': 10., 'cpmBase': None, 'sponsored': True}},  # <--
            {'json.boostData': {'hasMeta': False, 'cpmMeta': 111, 'cpmBase': 19, 'sponsored': True}},  # <--
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 8, 'cpmBase': 8, 'sponsored': False}},
            {'json.boostData': {'hasMeta': False, 'cpmMeta': 111, 'cpmBase': 19, 'sponsored': True}},  # <--
            {'json.boostData': {'hasMeta': False, 'cpmMeta': None, 'cpmBase': 8, 'sponsored': False}},
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 7, 'cpmBase': 8, 'sponsored': False}},
        ],
        [
            {'isMeta': True, 'filledCpmMeta': 10, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 0},
            {'isMeta': True, 'filledCpmMeta': 9, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 1},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'rank': 2},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'rank': 3},
            {'isMeta': True, 'filledCpmMeta': 8, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 4},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'rank': 5},
            {'isMeta': True, 'filledCpmMeta': 7.5, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 6},
            {'isMeta': True, 'filledCpmMeta': 7, 'filledCpmBase': None, 'shouldIgnore': False, 'rank': 7},
        ],
        debug=False
    )

    print('SerpSetPreparer OK!', file=sys.stderr)


def run_test_prob_matrix():
    booster = bs.SerpBooster()

    boost_prob = 0.4
    booster.reset_params(
        boost_prob=0.4,
        boost_coeff=1,
        reuse_random_state=False,
        save_random_state=True,
        max_num_docs=3
    )

    weights_expected = 1. - np.array([1., (1 - boost_prob), (1 - boost_prob) ** 2, (1 - boost_prob) ** 3])

    def do():
        print('expected: ', weights_expected)
        print('     got: ', booster.weights)

    assert np.allclose(booster.weights, weights_expected), do()

    # Test probability matrix
    import operator as op
    from functools import reduce
    def ncr(n, r):
        r = min(r, n - r)
        numer = reduce(op.mul, range(n, n - r, -1), 1)
        denom = reduce(op.mul, range(1, r + 1), 1)
        return numer // denom  # or / in Python 2

    def prb(n, k):
        if k > n:
            return 0
        return boost_prob ** k * (1 - boost_prob) ** (n - k)

    probs = np.array([
        [ncr(0, 0) * prb(0, 0), ncr(0, 1) * prb(0, 1), ncr(0, 2) * prb(0, 2), ncr(0, 3) * prb(0, 3)],
        [ncr(1, 0) * prb(1, 0), ncr(1, 1) * prb(1, 1), ncr(1, 2) * prb(1, 2), ncr(1, 3) * prb(1, 3)],
        [ncr(2, 0) * prb(2, 0), ncr(2, 1) * prb(2, 1), ncr(2, 2) * prb(2, 2), ncr(2, 3) * prb(2, 3)],
        [ncr(3, 0) * prb(3, 0), ncr(3, 1) * prb(3, 1), ncr(3, 2) * prb(3, 2), ncr(3, 3) * prb(3, 3)],
    ])

    probs /= (1 - np.array([[1.], [(1 - boost_prob)], [(1 - boost_prob) ** 2], [(1 - boost_prob) ** 3]]))
    probs = probs[:, 1:]

    def do():
        print(probs, file=sys.stderr)
        print(booster.probs, file=sys.stderr)
        return 'booster probs matrix failed'

    assert np.allclose(probs, booster.probs, equal_nan=True), do()
    print('SerpBooster.probs is OK!', file=sys.stderr)


def test_SerpBooster(booster, input_serp, expected, msg=''):
    copy_serp = deepcopy(input_serp)
    booster.run_boost(Serp(copy_serp))
    assert copy_serp == expected, make_json_diff_msg(expected, copy_serp, msg)


def wrap(components):
    return [{'json.debugBoost': c} for c in components]


def test_SerpBooster_unwrapped(booster, input_components, expected_components, expected_serp, msg=''):
    copy_serp = {'components': deepcopy(wrap(input_components))}
    booster.run_boost(Serp(copy_serp))
    assert expected_serp == copy_serp['json.debugBoost'], (expected_serp, copy_serp['json.debugBoost'])
    compare_debug_boost(expected_components, copy_serp['components'], msg=msg, unwrap=True)


def test_SerpBooster_split_serp(booster, components, expected, msg=''):
    copy_components = deepcopy(components)
    copy_components = [Component(c) for c in copy_components]
    meta, base, ignored = booster._split_serp(copy_components)
    meta = [c.data() for c in meta]
    base = [c.data() for c in base]
    assert expected == [meta, base], msg


def test_SerpBooster_boost_block(booster, components, expected, ranks_to_boost, msg=''):
    copy_components = deepcopy(components)
    booster._boost_block([Component(c) for c in copy_components], ranks_to_boost, how='filledCpmMeta')
    assert copy_components == expected, make_json_diff_msg(expected, copy_components, msg)


def test_SerpBooster_generate_indices_to_boost(booster, serp, expected, msg=''):
    booster.reset_params(
        boost_prob=0.1,
        boost_coeff=2.,
        max_num_docs=1,
        reuse_random_state=True,
        save_random_state=False,
        random_seed=None
    )
    serp_copy = deepcopy(serp)
    doc_ixes_to_boost = booster._generate_indices_to_boost(Serp(serp))
    assert serp_copy == serp, (
        msg + '\n' + 'Unexpected modification of serp:\n' +
        make_json_diff_msg(list(expected), list(doc_ixes_to_boost), msg)
    )
    assert doc_ixes_to_boost == expected, make_json_diff_msg(list(expected), list(doc_ixes_to_boost), msg)


def run_SerpBooster():
    booster = bs.SerpBooster()
    booster.reset_params(
        boost_prob=0.1,
        boost_coeff=2.,
        max_num_docs=1,
        reuse_random_state=True,
        save_random_state=False,
        random_seed=None
    )

    # Empty input_serp
    test_SerpBooster(
        booster,
        {'components': [], 'json.debugBoost': {'num_docs_to_boost': 0, 'serpId': 0}},
        {'components': [], 'json.debugBoost': {'num_docs_to_boost': 0, 'serpId': 0}},
        'Empty Serp:'
    )

    serp = {'components': [{'json.debugBoost': {'isMeta': True,
                                                'filledCpmMeta': 10,
                                                'filledCpmBase': None,
                                                'shouldIgnore': False,
                                                'rank': 0,
                                                'priority_for_boost': 0.3163755545817859,
                                                'is_boosted': False}},
                           {'json.debugBoost': {'isMeta': True,
                                                'filledCpmMeta': 9,
                                                'filledCpmBase': None,
                                                'shouldIgnore': False,
                                                'rank': 1,
                                                'priority_for_boost': 0.18391881167709445,
                                                'is_boosted': True}},
                           {'json.debugBoost': {'isMeta': True,
                                                'filledCpmMeta': 8,
                                                'filledCpmBase': None,
                                                'shouldIgnore': False,
                                                'rank': 2,
                                                'priority_for_boost': 0.2045602785530397,
                                                'is_boosted': True}}],
            'json.debugBoost': {'num_docs_to_boost': 2, 'serp_weight': 0.875}}

    test_SerpBooster_generate_indices_to_boost(
        booster,
        deepcopy(serp),
        set([1, 2]),
        msg='Generate Indices'
    )

    test_SerpBooster_split_serp(
        booster,
        [
            {'json.boostData': {'hasMeta': True,
                                'cpmMeta': 10,
                                'cpmBase': 6},
             'json.debugBoost': {'isMeta': True,
                                 'filledCpmMeta': 10,
                                 'filledCpmBase': None,
                                 'shouldIgnore': False,
                                 'rank': 0}},
            {'json.boostData': {'hasMeta': True, 'cpmMeta': 9, 'cpmBase': 7},
             'json.debugBoost': {'isMeta': True,
                                 'filledCpmMeta': 9,
                                 'filledCpmBase': None,
                                 'shouldIgnore': False,
                                 'rank': 1}},
            {'json.boostData': {'hasMeta': False, 'cpmMeta': 8, 'cpmBase': 8},
             'json.debugBoost': {'isMeta': False,
                                 'filledCpmMeta': None,
                                 'filledCpmBase': 8,
                                 'shouldIgnore': False,
                                 'rank': 2}}
        ], [
            [  # meta
                {'json.boostData': {'hasMeta': True,
                                    'cpmMeta': 10,
                                    'cpmBase': 6},
                 'json.debugBoost': {'isMeta': True,
                                     'filledCpmMeta': 10,
                                     'filledCpmBase': None,
                                     'shouldIgnore': False,
                                     'rank': 0}},
                {'json.boostData': {'hasMeta': True, 'cpmMeta': 9, 'cpmBase': 7},
                 'json.debugBoost': {'isMeta': True,
                                     'filledCpmMeta': 9,
                                     'filledCpmBase': None,
                                     'shouldIgnore': False,
                                     'rank': 1}},
            ], [  # base
                {'json.boostData': {'hasMeta': False, 'cpmMeta': 8, 'cpmBase': 8},
                 'json.debugBoost': {'isMeta': False,
                                     'filledCpmMeta': None,
                                     'filledCpmBase': 8,
                                     'shouldIgnore': False,
                                     'rank': 2}}
            ]
        ],
        '2 Meta 1 Base'
    )

    booster.reset_params(
        boost_prob=0.5,
        boost_coeff=1,
        max_num_docs=3,
        reuse_random_state=False,
        save_random_state=True,
        random_seed=12345
    )

    test_SerpBooster_boost_block(
        booster,
        [
            {'json.debugBoost': {'isMeta': True,
                                 'filledCpmMeta': 10,
                                 'filledCpmBase': None,
                                 'shouldIgnore': False,
                                 'rank': 0,
                                 'priority_for_boost': 0.3163755545817859,
                                 'is_boosted': False}},
            {'json.debugBoost': {'isMeta': True,
                                 'filledCpmMeta': 9,
                                 'filledCpmBase': None,
                                 'shouldIgnore': False,
                                 'rank': 1,
                                 'priority_for_boost': 0.18391881167709445,
                                 'is_boosted': True}}
        ], [
            {'json.debugBoost': {'isMeta': True,
                                 'filledCpmMeta': 10 * 1,
                                 'filledCpmBase': None,
                                 'shouldIgnore': False,
                                 'rank': 0,
                                 'priority_for_boost': 0.3163755545817859,
                                 'is_boosted': False}},
            {'json.debugBoost': {'isMeta': True,
                                 'filledCpmMeta': 9,
                                 'filledCpmBase': None,
                                 'shouldIgnore': False,
                                 'rank': 1,
                                 'priority_for_boost': 0.18391881167709445,
                                 'is_boosted': True}}
        ],
        set([1]),
        msg='Boost Meta Block boost=1'
    )

    booster.reset_params(
        boost_prob=0.5,
        boost_coeff=1,
        max_num_docs=3,
        reuse_random_state=False,
        save_random_state=True,
        random_seed=12345
    )

    test_SerpBooster_unwrapped(
        booster,
        [
            {'filledCpmBase': None, 'filledCpmMeta': 10, 'isMeta': True, 'rank': 0, 'shouldIgnore': False},
             {'filledCpmBase': None, 'filledCpmMeta': 9, 'isMeta': True, 'rank': 1, 'shouldIgnore': False},
             {'filledCpmBase': None, 'filledCpmMeta': 8, 'isMeta': True, 'rank': 2, 'shouldIgnore': False}
        ],
        [
            {'filledCpmBase': None, 'filledCpmMeta': 10, 'isMeta': True, 'is_boosted': False, 'priority_for_boost': 0.3163755545817859, 'rank': 0, 'shouldIgnore': False},
             {'filledCpmBase': None, 'filledCpmMeta': 9, 'isMeta': True, 'is_boosted': True, 'priority_for_boost': 0.18391881167709445, 'rank': 1, 'shouldIgnore': False},
             {'filledCpmBase': None, 'filledCpmMeta': 8, 'isMeta': True, 'is_boosted': True, 'priority_for_boost': 0.2045602785530397, 'rank': 2, 'shouldIgnore': False}
        ],
        {'num_docs_to_boost': 2, 'serp_weight': 0.875},
        msg='SerpBooster boost=1'
    )

    boost_coeff = 2
    booster.reset_params(
        boost_prob=0.5,
        boost_coeff=boost_coeff,
        max_num_docs=3,
        reuse_random_state=False,
        save_random_state=True,
        random_seed=12345
    )

    test_SerpBooster_unwrapped(
        booster,
        [
            {'filledCpmBase': None, 'filledCpmMeta': 10, 'isMeta': True, 'is_boosted': False, 'priority_for_boost': 0.3163755545817859, 'rank': 0, 'shouldIgnore': False},
            {'filledCpmBase': None, 'filledCpmMeta': 9, 'isMeta': True, 'is_boosted': True, 'priority_for_boost': 0.18391881167709445, 'rank': 1, 'shouldIgnore': False},
            {'filledCpmBase': 8, 'filledCpmMeta': None, 'isMeta': False, 'is_boosted': True, 'priority_for_boost': 0.2045602785530397, 'rank': 2, 'shouldIgnore': False}
        ],
        [
            {'filledCpmBase': None, 'filledCpmMeta': 10, 'isMeta': True, 'is_boosted': False, 'priority_for_boost': 0.3163755545817859, 'rank': 1, 'shouldIgnore': False},
            {'filledCpmBase': None, 'filledCpmMeta': 18, 'isMeta': True, 'is_boosted': True, 'priority_for_boost': 0.18391881167709445, 'rank': 0, 'shouldIgnore': False},
            {'filledCpmBase': 16, 'filledCpmMeta': None, 'isMeta': False, 'is_boosted': True, 'priority_for_boost': 0.2045602785530397, 'rank': 2, 'shouldIgnore': False}
        ],
        {'num_docs_to_boost': 2, 'serp_weight': 0.875},
        msg='SerpBooster 2 Meta 1 Base boost=2'
    )

    # Проверяем, что документы игнорируются.
    boost_coeff = 2
    booster.reset_params(
        boost_prob=0.5,
        boost_coeff=boost_coeff,
        max_num_docs=3,
        reuse_random_state=False,
        save_random_state=True,
        random_seed=12345
    )

    test_SerpBooster_unwrapped(
        booster,
        [
            {'filledCpmBase': None, 'filledCpmMeta': 10, 'isMeta': True, 'is_boosted': False, 'priority_for_boost': 0.3163755545817859, 'rank': 0, 'shouldIgnore': False},
             {'filledCpmBase': None, 'filledCpmMeta': 9, 'isMeta': True, 'is_boosted': True, 'priority_for_boost': 0.18391881167709445, 'rank': 1, 'shouldIgnore': False},
             {'filledCpmBase': 8, 'filledCpmMeta': None, 'isMeta': False, 'is_boosted': True, 'priority_for_boost': 0.2045602785530397, 'rank': 2, 'shouldIgnore': False}
        ],
        [
            {'filledCpmBase': None, 'filledCpmMeta': 10, 'isMeta': True, 'is_boosted': False, 'priority_for_boost': 0.3163755545817859, 'rank': 1, 'shouldIgnore': False},
            {'filledCpmBase': None, 'filledCpmMeta': 18, 'isMeta': True, 'is_boosted': True, 'priority_for_boost': 0.18391881167709445, 'rank': 0, 'shouldIgnore': False},
            {'filledCpmBase': 16, 'filledCpmMeta': None, 'isMeta': False, 'is_boosted': True, 'priority_for_boost': 0.2045602785530397, 'rank': 2, 'shouldIgnore': False}
        ],
        {'num_docs_to_boost': 2, 'serp_weight': 0.875},
        msg='SerpBooster 2 Meta 1 Base boost=2'
    )

    ### Проверяем, что документы на мете перепрыгивают через спонсорский сниппет
    boost_coeff = 2
    booster.reset_params(
        boost_prob=0.5,
        boost_coeff=boost_coeff,
        max_num_docs=3,
        reuse_random_state=False,
        save_random_state=True,
        random_seed=12345
    )

    test_SerpBooster_unwrapped(
        booster,
        [{'isMeta': True,
          'filledCpmMeta': 10,
          'filledCpmBase': None,
          'shouldIgnore': False,
          'sponsored': False,
          'rank': 0},
         {'isMeta': False,
          'filledCpmMeta': None,
          'filledCpmBase': None,
          'shouldIgnore': True,
          'sponsored': True,
          'rank': 1},
         {'isMeta': True,
          'filledCpmMeta': 6,
          'filledCpmBase': None,
          'shouldIgnore': False,
          'sponsored': False,
          'rank': 2}
         ],
        [{'isMeta': True,
          'filledCpmMeta': 10,
          'filledCpmBase': None,
          'shouldIgnore': False,
          'sponsored': False,
          'rank': 2,
          'priority_for_boost': 0.3163755545817859,
          'is_boosted': False},
         {'isMeta': False,
          'filledCpmMeta': None,
          'filledCpmBase': None,
          'shouldIgnore': True,
          'sponsored': True,
          'rank': 1,
          'priority_for_boost': 0.18391881167709445},
         {'isMeta': True,
          'filledCpmMeta': 6 * boost_coeff,
          'filledCpmBase': None,
          'shouldIgnore': False,
          'sponsored': False,
          'rank': 0,
          'priority_for_boost': 0.2045602785530397,
          'is_boosted': True}
         ],
        {'num_docs_to_boost': 1, 'serp_weight': 0.75},
        msg='SerpBooster meta_sponsored_meta'
    )


    ### Проверяем, что документы на мете перепрыгивают через спонсорский сниппет
    boost_coeff = 1
    booster.reset_params(
        boost_prob=0.5,
        boost_coeff=boost_coeff,
        max_num_docs=6,
        reuse_random_state=False,
        save_random_state=True,
        random_seed=12345
    )

    test_SerpBooster_unwrapped(
        booster,
        [
            {'filledCpmBase': None, 'filledCpmMeta': 10, 'isMeta': True, 'rank': 0, 'shouldIgnore': False, 'sponsored': False},
            {'filledCpmBase': None, 'filledCpmMeta': None, 'isMeta': False, 'rank': 1, 'shouldIgnore': True, 'sponsored': True},
            {'filledCpmBase': None, 'filledCpmMeta': 16, 'isMeta': True, 'rank': 2, 'shouldIgnore': False, 'sponsored': False},
            {'filledCpmBase': 6, 'filledCpmMeta': None, 'isMeta': False, 'rank': 3, 'shouldIgnore': False, 'sponsored': False},
            {'filledCpmBase': None, 'filledCpmMeta': None, 'isMeta': False, 'rank': 4, 'shouldIgnore': True, 'sponsored': True},
            {'filledCpmBase': 7, 'filledCpmMeta': None, 'isMeta': False, 'rank': 5, 'shouldIgnore': False, 'sponsored': False}],
        [
            {'isMeta': True, 'filledCpmMeta': 10, 'filledCpmBase': None, 'shouldIgnore': False, 'sponsored': False, 'rank': 2, 'priority_for_boost': 0.3163755545817859, 'is_boosted': True},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'sponsored': True, 'rank': 1, 'priority_for_boost': 0.18391881167709445},
            {'isMeta': True, 'filledCpmMeta': 16, 'filledCpmBase': None, 'shouldIgnore': False, 'sponsored': False, 'rank': 0, 'priority_for_boost': 0.2045602785530397, 'is_boosted': True},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 6, 'shouldIgnore': False, 'sponsored': False, 'rank': 5, 'priority_for_boost': 0.5677250290816866, 'is_boosted': True},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': None, 'shouldIgnore': True, 'sponsored': True, 'rank': 4, 'priority_for_boost': 0.5955447029792516},
            {'isMeta': False, 'filledCpmMeta': None, 'filledCpmBase': 7, 'shouldIgnore': False, 'sponsored': False, 'rank': 3, 'priority_for_boost': 0.9645145197356216, 'is_boosted': False},
        ],
        {'num_docs_to_boost': 3, 'serp_weight': 0.9375},
        msg='SerpBooster meta_sponsored_meta'
    )

    ### From file v1
    input = json.load(open(BOOST_SIMULATOR_DIR + '/test_data/input1.json'))[0]
    input = bs.SerpSetPreparer(True)._prepare_serp(input)
    booster.reset_params(
        boost_prob=0.05,
        boost_coeff=1.2,
        max_num_docs=len(input['components']),
        reuse_random_state=False,
        save_random_state=True,
        random_seed=12345
    )
    test_SerpBooster(
        booster,
        input,
        json.load(open(BOOST_SIMULATOR_DIR + '/test_data/out1.json'))[0],
        msg='Big serp test 1'
    )

    # From file v2
    # Заполняем пропущенное значение меты и пропускаем документ после позиции 48 (нет базовой)

    input = json.load(open(BOOST_SIMULATOR_DIR + '/test_data/input2.json'))[0]
    input = bs.SerpSetPreparer(True)._prepare_serp(input)
    booster.reset_params(
        boost_prob=0.05,
        boost_coeff=1.2,
        max_num_docs=len(input['components']),
        reuse_random_state=False,
        save_random_state=True,
        random_seed=12345
    )

    test_SerpBooster(
        booster,
        input,
        json.load(open(BOOST_SIMULATOR_DIR + '/test_data/out2.json'))[0],
        msg='Big serp test 2'
    )

    print('SerpBooster is OK!', file=sys.stderr)


def run_test_main_take_docs():
    class Args:
        mode = bs.Mode.TAKE_DOCS_ELIGIBLE_FOR_BOOST
        max_docs_from_report = 50
        enrichment_depth = 12
        min_boost = 0.8
        max_boost = 1.3
        boost_step = 0.1
        boost_probs = [0.05]
        metrics = []
        random_seed = 12345

    args = Args()

    # Убираем одну мету из середины, и один base из середины.
    # Проверяем, что мета заполняется, а base пропускается
    serpset = json.load(open(BOOST_SIMULATOR_DIR + '/test_data/input2.json'))
    serpset_default, filtered_serpset = bs.main_take_docs(args, serpset, save=False, add_report_indices=True)
    expected = json.load(open(BOOST_SIMULATOR_DIR + '/test_data/out2_main_take_docs.json'))
    expected_filtered = json.load(open(BOOST_SIMULATOR_DIR + '/test_data/out2_main_take_docs_filtered.json'))
    assert serpset_default == expected, make_json_diff_msg(serpset_default, expected, msg='serpset_default')
    assert filtered_serpset == expected_filtered, make_json_diff_msg(filtered_serpset, expected_filtered,
                                                                     msg='serpset_default')

    # Добавляем два документа без меты подряд. Проверяем, что все документы после становятся базовыми.
    serpset = json.load(open(BOOST_SIMULATOR_DIR + '/test_data/input3.json'))
    serpset_default, filtered_serpset = bs.main_take_docs(args, serpset, save=False, add_report_indices=True)

    json.dump(filtered_serpset, open('tmp____.json', 'w'), indent=4)
    expected = json.load(open(BOOST_SIMULATOR_DIR + '/test_data/out3_main_take_docs.json'))

    expected_filtered = json.load(open(BOOST_SIMULATOR_DIR + '/test_data/out3_main_take_docs_filtered.json'))
    assert serpset_default == expected, make_json_diff_msg(serpset_default, expected, msg='serpset_default')
    assert filtered_serpset == expected_filtered, make_json_diff_msg(filtered_serpset, expected_filtered,
                                                                     msg='serpset_default')

    print('main_take_docs is OK!', file=sys.stderr)


def run_tests():
    run_SerpSetPreparer()
    run_test_prob_matrix()
    run_SerpBooster()
    run_test_main_take_docs()


if __name__ == '__main__':
    run_tests()
