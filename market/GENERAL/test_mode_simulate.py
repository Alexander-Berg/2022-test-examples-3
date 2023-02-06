import sys
import numpy as np
import os

np.seterr(divide='ignore', invalid='ignore')
BOOST_SIMULATOR_DIR = os.path.dirname(os.path.realpath(__file__))

try:
    from quality.mstand_metrics.market_offline import LimusV2
    import market.analytics.market_search.boost_optimizer.offline.boost_simulator.main as bs
except:
    BOOST_SIMULATOR_DIR = os.path.dirname(os.path.realpath(__file__))
    arcadia_root_dir = '/'.join(BOOST_SIMULATOR_DIR.split("/")[:-6])
    sys.path.append(arcadia_root_dir)

    from quality.mstand_metrics.market_offline import LimusV2
    import market.analytics.market_search.boost_optimizer.offline.boost_simulator.main as bs
    from offline.boost_simulator.library import mock_metrics_api as metrics_api


def run_tests():
    # TODO: add some tests
    pass


if __name__ == '__main__':
    run_tests()
