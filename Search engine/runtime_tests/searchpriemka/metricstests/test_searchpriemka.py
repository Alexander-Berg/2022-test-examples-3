__author__ = 'aokhotin'

import logging

from runtime.tools.metrics import LaunchManager
from runtime.simcity import METRICS_LAUNCH_MANAGER_URL, METRICS_QE_URL

NOAPACHE_PRIEMKA_TEMPLATE_ID = "aa82863848fe1c9e0148fe26d5840000"

Logger = logging.getLogger("tests.results.acceptance.metrics")


def test_noapache_priemka_with_metrics():
    launcher = LaunchManager(METRICS_LAUNCH_MANAGER_URL, METRICS_QE_URL)
    launch = launcher.launch_template(NOAPACHE_PRIEMKA_TEMPLATE_ID)
    logging.info("launch id: {}".format(launch['id']))
    launch_info = launcher.wait_for_launch_done(launch['id'])

    Logger.info("Metrics Tests Results: '{status}';\nReport: {report}".format(
        status=str(launch_info["status"]),
        report=launcher.get_user_report(launch['id'])
    ))
    assert False, launcher.get_user_report(launch['id'])
