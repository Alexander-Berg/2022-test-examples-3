from collections import namedtuple

from market.idx.devtools.common_proxy_monitor.lib.analysis import CloggedQueuesAnalyzer


def test_clogged_queues():
    # arrange
    Parser = namedtuple('Parser', ['processors', 'queue_sizes', 'max_queue_sizes'])
    analyzer = CloggedQueuesAnalyzer()

    # act
    analyzer.measure(Parser(processors=['A', 'B', 'C'], max_queue_sizes={'A': 10, 'B': 5, 'C': 5}, queue_sizes={'A': 10, 'B': 5, 'C': 0}))
    analyzer.measure(Parser(processors=['A', 'B', 'C'], max_queue_sizes={'A': 10, 'B': 5, 'C': 5}, queue_sizes={'A': 10, 'B': 4, 'C': 1}))

    warn, crit = analyzer.report()

    # assert
    assert crit == {'A': 1.0}
    assert warn == {'B': 0.9}


def test_clogged_queues_with_max_measurements():
    # arrange
    Parser = namedtuple('Parser', ['processors', 'queue_sizes', 'max_queue_sizes'])
    analyzer = CloggedQueuesAnalyzer(max_measurements=2)

    # act
    analyzer.measure(Parser(processors=['A'], max_queue_sizes={'A': 10}, queue_sizes={'A': 10}))
    analyzer.measure(Parser(processors=['A'], max_queue_sizes={'A': 10}, queue_sizes={'A': 10}))
    analyzer.measure(Parser(processors=['A'], max_queue_sizes={'A': 10}, queue_sizes={'A': 7}))

    warn, crit = analyzer.report()

    # assert
    assert not crit
    assert not warn
