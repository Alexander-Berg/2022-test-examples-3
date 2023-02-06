__author__ = 'aokhotin'
import logging

import runtime.logging


def pytest_addoption(parser):
    parser.addoption("--beta", type="string", help="beta url", default='http://hamster.yandex.ru/yandsearch')
    parser.addoption("--startrek-ticket", type="string", help="log test result to the specified ticket", default=None)
    parser.addoption("--startrek-server", type="string", help="startrek server for logging",
                     default="http://st-api.yandex-team.ru/")


def pytest_configure(config):
    if config.option.startrek_ticket:
        handler = runtime.logging.StartrekHandler(config.option.startrek_server, config.option.startrek_ticket)
        handler.level = logging.INFO
        logger = logging.getLogger("tests.results.acceptance")
        logger.setLevel(logging.INFO)
        logger.addHandler(handler)
