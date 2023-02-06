# -*- coding: utf-8 -*-

import logging

from xml.etree import cElementTree as ET
import urllib2

PATHFINDER_OLD_URL = 'http://localhost:8095'
PATHFINDER_URL = 'http://localhost:8096'
MAX_INT = 10000000

logging.basicConfig(filename='test_pathfinder.log', level=logging.DEBUG)


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

    def better(self, other):
        if self.changes == other.changes and self.time == other.time \
                and self.price == other.price and self.discomfort == other.discomfort:
            return False;
        if self.changes <= other.changes and self.time <= other.time \
                and self.price <= other.price and self.discomfort <= other.discomfort:
            return True;
        return False

    def equal(self, other):
        if self.changes == other.changes and self.time == other.time \
                and self.price == other.price and self.discomfort == other.discomfort:
            return True;
        return False


def parse_response(response):
    try:
        tree = ET.fromstring(response)
    except Exception:
        tree = None
        logging.warning('bad response')

    return Routes(tree)


if __name__ == '__main__':
    res_old_better = res_old_better_max_time = 0
    res_new_better = res_new_better_max_time = 0
    res_not_equal = res_not_equal_max_time = 0

    with open('targetpathfinder.log.get', 'r') as f:
        for request in f.readlines():
            request = request.rstrip()
            if len(request) <= 0:
                break
            logging.info("Request: %s" % request)
            (routes_old, response_old) = find_routes(PATHFINDER_OLD_URL, request)
            (routes_new, response_new) = find_routes(PATHFINDER_URL, request)
            if response_new != response_old:
                logging.info("OLD response: %s", response_old)
                logging.info("NEW response: %s", response_new)
                print(request)
                print('OLD:', repr(routes_old))
                print('NEW:', repr(routes_new))
                if routes_old.better(routes_new):
                    if routes_new.result == 'max-time':
                        print('OLD better (max-time)')
                        res_old_better_max_time += 1
                    else:
                        print('OLD better!')
                        res_old_better += 1
                elif routes_new.better(routes_old):
                    if routes_old.result == 'max-time':
                        print('NEW better (max-time)')
                        res_new_better_max_time += 1
                    else:
                        print('NEW better!')
                        res_new_better += 1
                elif not routes_new.equal(routes_old):
                    if routes_old.result == 'max-time' or routes_new.result == 'max-time':
                        print('NOT EQUAL (max-time)')
                        res_not_equal_max_time += 1
                    else:
                        print('NOT EQUAL')
                        res_not_equal += 1

    print('Statistics:')
    print('OLD better (max-time): {}'.format(res_old_better_max_time))
    print('OLD better: {}'.format(res_old_better))
    print('NEW better (max-time): {}'.format(res_new_better_max_time))
    print('NEW better: {}'.format(res_new_better))
    print('NOT EQUAL (max-time): {}'.format(res_not_equal_max_time))
    print('NOT EQUAL: {}'.format(res_not_equal))
