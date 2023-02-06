#!/usr/bin/env python
# -*- encoding: utf8 -*-


import sys
import argparse
import json
import requests
import time
import logging
import os
import tvmauth
from extsearch.video.functionality.upf.common.common_functions import read_yt_table, write_yt_table


class Tests:

    LOG_FORMAT = '%(asctime)s - [%(levelname)s] - %(message)s'
    logging.basicConfig(level=logging.INFO, format=LOG_FORMAT, stream=sys.stdout)

    def init_arguments(self):
        parser = argparse.ArgumentParser(description="Configures")
        parser.add_argument('--tests_table', type=str, help='json with tests_table')
        parser.add_argument('--debug', default=0, type=int, help='1 for debug')
        parser.add_argument('--use_proxy', default=0, type=int)
        parser.add_argument('--verify', default=0, type=int)
        parser.add_argument('--sleep', default=4, type=int, help='time.sleep between requests')
        parser.add_argument('--tests', default=[0, '', ''], nargs='*',
                            help='1 for appending additional test case 2 for creating new test cases (rewrite)')
        return parser.parse_args()

    def __init__(self):
        self.args = self.init_arguments()
        self.tests = dict()
        self.production = dict()
        self.difference = dict()
        self.tests_table = json.load(open(self.args.tests_table))
        self.path = self.args.tests[2]

    def get_response(self):
        logging.info("read %s", self.url)
        retries = 3
        status_code = None

        while retries > 0:
            session = requests.Session()
            try:
                result = session.get(self.url, proxies=self.proxies, verify=self.verify)
                status_code = result.status_code
                if status_code == 200:
                    break
            except:
                logging.exception(Exception)
            retries -= 1
            time.sleep(self.args.sleep)
            logging.info("retry url %s because of status code", self.url)
        try:
            data = result.json()
        except:
            logging.exception(Exception)
            logging.error("data = %s", str(data))

        try:
            cinemas = data["searchdata.docs_right.[0].snippets.full.data.rich_info.cinema_data"][0]["cinemas"]
        except:
            cinemas = []
        if not isinstance(cinemas, list):
            cinemas = []
        return cinemas

    def get_production_cinemas(self):
        production_cinemas = self.get_response()
        retries = 5
        while not production_cinemas and retries > 0:
            production_cinemas = self.get_response()
            retries -= 1
            logging.error("retry because of empty list")
        return production_cinemas

    def buid_dict_from_list(self, cinemas, text):
        result = dict()
        result[text] = dict()
        for cinema in cinemas:
            models = []
            cinema_code = cinema.get("code")
            for variant in cinema.get("variants", []):
                model = variant.get("type")
                if model not in models:
                    models.append(model)
            result[text][cinema_code] = models
        return result

    def get_new_tests(self, requests, additional_requests):
        new_tests = []
        for text in requests:
            self.url = self.build_url(text)
            logging.info("proceessing %s", text)
            production_cinemas = self.get_production_cinemas()
            if production_cinemas:
                new_tests.append(
                    {
                        "request text": text,
                        "cinemas": production_cinemas
                    }
                )
                logging.info("added %s to tests\n", text)
                additional_requests.append(text)
            else:
                logging.error("request %s was not added to tests", text)
        return new_tests

    def rewrite_tests(self):
        new_requests = json.load(open(self.path))
        new_requests = list(set(new_requests))
        new_tests = self.get_new_tests(new_requests, [])
        write_yt_table(cluster=self.tests_table["cluster"], path_to_table=self.tests_table["table"], data=new_tests, append=False)

    def append_tests(self):
        new_requests = json.load(open(self.args.tests[1]))
        prev_requests = json.load(open(self.path))
        additional_requests = []
        new_tests = self.get_new_tests(new_requests, additional_requests)
        prev_requests += additional_requests
        json.dump(prev_requests, open(self.path, 'w'), ensure_ascii=False, indent=4)
        write_yt_table(cluster=self.tests_table["cluster"], path_to_table=self.tests_table["table"], data=new_tests, append=True)

    def build_url(self, text):
        return "https://hamster.yandex.ru/search/?text={}&nocache=1&timeout=999999&waitall=1&no-tests=1&json_dump=searchdata.docs_right.[0].snippets.full.data.rich_info.cinema_data".format(text)

    def build_info_dicts(self):
        logging.info("read_feeds started")
        client = tvmauth.TvmClient(
            tvmauth.TvmApiClientSettings(
                self_tvm_id=int(2026036),
                enable_service_ticket_checking=False,
                enable_user_ticket_checking=tvmauth.BlackboxEnv.Test,
                self_secret=os.environ['TVM_TOKEN'],
                dsts={"gozora": 2023123},
            )
        )

        gozora_user_id = "videoquality_offers"
        tvm = client.get_service_ticket_for('gozora')

        self.proxies = {
            'http': f'http://{gozora_user_id}:{tvm}@go.zora.yandex.net:1080/',
            'https': f'http://{gozora_user_id}:{tvm}@go.zora.yandex.net:1080/',
        }
        if not int(self.args.use_proxy):
            self.proxies = None
        self.verify = False
        if int(self.args.verify):
            self.verify = True

        if int(self.args.tests[0]) == 1:
            self.append_tests()
        elif int(self.args.tests[0]) == 2:
            self.rewrite_tests()

        tests_data = read_yt_table(cluster=self.tests_table["cluster"], path_to_table=self.tests_table["table"])
        for test in tests_data:
            text = test["request text"]
            tests_cinemas = test["cinemas"]
            self.url = self.build_url(text)
            logging.info("proceessing %s", text)
            production_cinemas = self.get_production_cinemas()
            self.tests.update(self.buid_dict_from_list(tests_cinemas, text))
            self.production.update(self.buid_dict_from_list(production_cinemas, text))
            time.sleep(self.args.sleep)

    def compare_dicts(self):
        for request in self.tests:
            lost_cinemas = []
            lost_models = dict()
            test_cinemas = self.tests[request]
            losses = dict()
            production_cinemas = self.production.get(request, [])
            for cinema in test_cinemas:
                if cinema not in production_cinemas:
                    lost_cinemas.append(
                        {cinema: test_cinemas[cinema]}
                    )
                else:
                    for model in test_cinemas[cinema]:
                        if model not in production_cinemas[cinema]:
                            if cinema not in lost_models:
                                lost_models[cinema] = []
                            if model not in lost_models[cinema]:
                                lost_models[cinema].append(model)
            if lost_cinemas:
                losses["lost cinemas"] = lost_cinemas
            if lost_models:
                losses["lost models"] = lost_models
            if losses:
                self.difference[request] = losses
        if self.difference:
            print(json.dumps(self.difference, ensure_ascii=False, indent=4))
            raise "Tests have more cinemas than Production"

    def main(self):
        self.build_info_dicts()
        self.compare_dicts()


if __name__ == "__main__":
    tests = Tests()
    tests.main()
    logging.info("program completed")
