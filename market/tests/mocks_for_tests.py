# coding: utf-8

import logging
import logging.handlers
import os

import lib.state

OFFERS_COUNT = 5


def request_get_mock(request, timeout=0):
    assert timeout >= 0
    if request.find('.market.yandex.net/marketindexer/whoismaster.py') == -1:
        assert request.split('?')[0] == 'http://report.tst.vs.market.yandex.net:17051/yandsearch'
        params = request.split('?')[1].split('&')
        assert 'place=stat_numbers' in params and 'pp=18' in params
        args = [param.split('=')[0] for param in params]
        assert ('rids' in args and ('rgb' in args or 'cpa' in args)) or ('promo-type' in args and 'rearr-factors' in args)
    return DummyResponse()


class DummyResponse(object):
    text = 'stratocaster\n'

    def raise_for_status(self):
        pass

    def json(self):
        return {
            'result': {
                'offersCount': OFFERS_COUNT,
                'filters': {
                    'DELIVERY': 1,
                    'ABO_MARKET_SKU': 2,
                    'OFFER_FILTERED_OUT_BY_WAREHOUSE_ID': 10
                }
            }
        }

def send_metric_mock(metric, value):
    pass

def solomon_mock(sensor, value, labels):
    pass

def prepare_test(location, environment):
    logger = logging.getLogger()
    logger.setLevel(logging.ERROR)
    file_handle = logging.handlers.WatchedFileHandler('log')
    formatter = logging.Formatter(
        '%(levelname)-5s | thread %(thread)d | in %(funcName)s line %(lineno)d | %(message)s')
    file_handle.setFormatter(formatter)
    logger.addHandler(file_handle)

    if environment == 'production':
        cfg = """
BlueOffersCountConfig {
    ReportHost: "report.tst.vs.market.yandex.net"
    GraphiteHost: "blue-report"
    SendPeriod: 1
    Dummy: False
    MetricConfig: "prod_cfg"
    ReportPort: 17051
    SolomonCluster: "stable"
}"""
    else:
        cfg = """
BlueOffersCountConfig {
    ReportHost: "report.tst.vs.market.yandex.net"
    GraphiteHost: "blue-report-tst"
    SendPeriod: 1
    Dummy: False
    MetricConfig: "test_cfg"
    ReportPort: 17051
    SolomonCluster: "testing"
}"""
    with open('./cfg.pb.txt', "w") as config:
        config.write(cfg)
    lib.state.sender_threads = []
    lib.state.stop_event = None
    lib.state.configs = []
    os.environ['BSCONFIG_ITAGS'] = 'a_dc_{} a_ctype_{}'.format(location, environment)


def finalize_test():
    with open('log') as log_file:
        log_file_contents = log_file.read()
        error_pos = log_file_contents.find('ERROR')
        exception_message = ''
        while error_pos != -1:
            log_file.seek(error_pos)
            exception_message += log_file.readline()
            error_pos = log_file_contents.find('ERROR', error_pos + 1)
        if len(exception_message) > 0:
            raise Exception(exception_message)
    os.remove('log')
