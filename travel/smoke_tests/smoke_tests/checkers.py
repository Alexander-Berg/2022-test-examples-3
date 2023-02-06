import logging
from time import sleep

import requests
from requests.adapters import HTTPAdapter

from travel.rasp.smoke_tests.smoke_tests.requests2curl import CurlHook, Request2Curl


def init_log():
    logger = logging.getLogger(__name__)
    logger.level = logging.INFO
    handler = logging.StreamHandler()
    handler.formatter = logging.Formatter('%(levelname)s %(asctime)s %(message)s')
    logger.addHandler(handler)
    logger.propagate = True
    return logger


log = init_log()


class Check(object):
    def __init__(self, module_path):
        self.module_path = module_path


class LogCurlHook(CurlHook):
    def process_curl(self, curl_str, response, **request_kwargs):
        log.info(curl_str)


class AdapterCatchRequests(HTTPAdapter):
    """
    Пытаемся собрать реальные запросы, которые отправляет либа requests.
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.__prepared_requests = []

    def add_headers(self, request, **kwargs):
        """
        Последнее место, где изменяется request перед отправкой в urllib3, поэтому ловим здесь.
        Костыль.
        """
        result = super().add_headers(request, **kwargs)
        self.__prepared_requests.append(request)

        return result

    def get_curls(self):
        return [Request2Curl(r).get_curl() for r in self.__prepared_requests]


class UrlCheck(Check):
    """
    Проверка одного урла, возможно с вложенными проверками
    """
    def __init__(self, module_path, config):
        super(UrlCheck, self).__init__(module_path)

        self.config = config
        self.params = config.params
        self.extra_url_checks = [
            UrlCheck(module_path, extra_config)
            for extra_config in config.extra_url_configs
        ]

    def __str__(self):
        return '{}: {}'.format(self.module_path, self.config.get_description())

    def make_request(self, url):
        """
        Выполнение запроса
        """
        for try_number in range(1, self.params.retries + 1):
            try:
                session = requests.Session()
                adapter = AdapterCatchRequests()
                session.mount('https://', adapter=adapter)
                session.mount('http://', adapter=adapter)

                try:
                    resp = session.request(
                        self.params.method, url,
                        timeout=self.params.timeout,
                        data=self.params.data,
                        headers=self.params.headers,
                        cookies=self.params.cookies,
                        allow_redirects=self.params.allow_redirects,
                        hooks={'response': LogCurlHook()}
                    )

                except requests.ReadTimeout:
                    # для таймаутов нужно собирать curl отдельно, т.к. до хука LogCurlHook не доходит
                    log.info('%s', '\n'.join(adapter.get_curls()))
                    raise

                if resp.status_code != self.params.expected_code:
                    raise Exception(f'{url}\nExpected code {self.params.expected_code} != {resp.status_code}\n')

                # Запуск обработок результата вызова
                for process in self.params.processes:
                    is_allowed = getattr(process, 'is_allowed', lambda *args: True)
                    if is_allowed(self, resp):
                        process(self, resp)
                    else:
                        log.info('>>>> process skipped <<<< %s', process)
                return resp

            except Exception as ex:
                log.error('%s %s', repr(ex), self)
                if try_number == self.params.retries:
                    raise
                sleep(self.params.retries_delay)

    def __call__(self, *args, **kwargs):
        self.config.make()

        if self.params.name:
            log.info('========================================================================')
            log.info(f'====== Run test {self.params.name} ======')
            log.info('========================================================================')

        url = self.config.full_url

        try:
            log.info(f'Check for {url}')
            self.make_request(url)

            # Запуск дополнительных проверок урлов
            for extra_url_check in self.extra_url_checks:
                extra_url_check()

        except Exception:
            log.exception(f'Exception in url {url}')
            raise


class RedirectCheck(object):
    def __init__(self, location):
        self.location = location

    def __call__(self, checker, response):
        response_location = response.headers.get('location')
        if response_location != self.location:
            raise Exception(f'RedirectCheck: response location "{response_location}" != "{self.location}"')
