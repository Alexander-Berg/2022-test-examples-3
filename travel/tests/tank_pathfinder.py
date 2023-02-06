# -*- coding: utf-8 -*-

import logging

from xml.etree import cElementTree as ET
import urllib2

PATHFINDER_URL = 'http://localhost:8093'
MAX_INT = 10000000

logging.basicConfig(filename='tank_pathfinder.log', level=logging.DEBUG)


def find_routes(url, request):
    url = url + request

    try:
        response = urllib2.urlopen(url, timeout=5).read()
    except Exception:
        response = None
        logging.warning('url: %s' % url)

    return parse_response(response), response


class Routes(object):
    """Выдача маршрутов с пересадками"""

    def __init__(self, element):
        try:
            self.result = element.get('result') or 'NO RESULT'
            self.price = float(element.get('bestPrice') or MAX_INT)
            if self.price <= 0:
                self.price = MAX_INT
            self.time = int(element.get('bestTime') or MAX_INT)
            self.discomfort = int(element.get('bestDiscomfort') or MAX_INT)
            self.changes = int(element.get('bestChanges') or MAX_INT)
            if self.changes <= 0:
                self.changes = MAX_INT
            self.bestChangesNumber = int(element.get('bestChangesNumber') or 0)
            self.routesNumber = int(element.get('routesNumber') or 0)
            self.pricedRoutesNumber = int(element.get('pricedRoutesNumber') or 0)
        except AttributeError:
            self.result = 'NO RESULT'
            self.time = self.price = self.discomfort = self.changes = MAX_INT
            self.bestChangesNumber = self.routesNumber = self.pricedRoutesNumber = 0

    def __repr__(self):
        if self.result:
            return '(result={}, time={}, price={}, discomfort={}, ch={}({}+{}), priced={})'\
                .format(self.result, self.time, self.price, self.discomfort,
                        self.changes, self.bestChangesNumber, self.routesNumber-self.bestChangesNumber,
                        self.pricedRoutesNumber)
        else:
            return None


def parse_response(response):
    try:
        tree = ET.fromstring(response)
    except Exception:
        tree = None
        logging.warning('bad response')

    return Routes(tree)


if __name__ == '__main__':
    with open('targetpathfinder.log.get', 'r') as f:
        for request in f.readlines():
            request = request.rstrip()
            if len(request) <= 0:
                break
            logging.info("Request: %s" % request)
            (routes_new, response_new) = find_routes(PATHFINDER_URL, request)
            logging.info("NEW response: %s", response_new)
            print(request)
            print('NEW:', repr(routes_new))

