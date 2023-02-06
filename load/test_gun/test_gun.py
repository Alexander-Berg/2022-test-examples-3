import logging
import time, os, requests
import json

logger = logging.getLogger("classic_ultimat_test")

class LoadTest(object):
    def __init__(self, gun):
        self.gun = gun
        self.target = self.gun.init_param["target"]

    def test1(self, missile):
        try:
            with self.gun.measure("test1") as sample:
                sample["net_code"], sample["proto_code"], results, exc = self.simple_request(missile, "test1")
                if sample["proto_code"] != 200 and sample["net_code"] !=1:
                    logger.error('Some http error, answer is: %s', results.text)
                    pass
                else:
                    logger.info('done 200')
                if sample["net_code"] == 1:
                    logger.error('Exceotion: %s, request was: %s', exc, results)
                    pass
        except RuntimeError as e:
            logger.error('Scenario failed with %s', e)

    def simple_request (self, sleep, handler):
        req = None
        if isinstance(sleep, bytes):
            sleep = sleep.decode('utf-8')
        try:
            req = "http://{}/{}?sleep={}".format(self.target, handler, sleep)
            headers = {
                'Host': 'test.yandex-team.ru',
                'Connection': 'close',
            }
            answ = requests.get(req, verify=False, headers=headers, timeout=1000)
            return 0, answ.status_code, answ, None
        except Exception as exc:
            return 1, 0, req, exc

    def default(self, missile):
        try:
            with self.gun.measure("default") as sample:
                sample["net_code"], sample["proto_code"], results, exc = self.simple_request(missile, "test2")
                if sample["proto_code"] != 200 and sample["net_code"] !=1:
                    logger.error('Some http error, answer is: %s', results.text)
                    pass
                else:
                    logger.info('done 200')
                if sample["net_code"] == 1:
                    logger.error('Exceotion: %s, request was: %s', exc, results)
                    pass
        except RuntimeError as e:
            logger.error('Scenario failed with %s', e)


    def setup(self, param):
        ''' this will be executed in each worker before the test starts '''
        logger.info("Setting up LoadTest: %s", param)

    def teardown(self):
        ''' this will be executed in each worker after the end of the test '''
        logger.info("All done, tearing down")
        os._exit(0)
