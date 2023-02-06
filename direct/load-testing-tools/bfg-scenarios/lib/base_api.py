# coding=utf-8
import json
import logging
import ssl
from urllib2 import Request, urlopen, HTTPError

logging.basicConfig()
logger = logging.getLogger(__name__)
logger.setLevel('DEBUG')


class BaseApi(object):
    def __init__(self, measurer, java_api_url, old_api_url, client_login, token, use_always_old_api=False):
        super(BaseApi, self).__init__()
        self.measurer = measurer
        self.java_api_url = java_api_url
        self.old_api_url = old_api_url
        self.client_login = client_login
        self.token = token
        self.use_always_old_api = use_always_old_api

    def get_java_api_url(self):
        return self.java_api_url if not self.use_always_old_api else self.old_api_url

    def get_old_api_url(self):
        return self.old_api_url

    def send_request(self, url, data):
        context = ssl._create_unverified_context()  # FIXME: починить TLS
        request = Request(url, data=json.dumps(data), headers={
            'Authorization': 'Bearer ' + self.token,
            'Fake-Login': self.client_login,
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'Accept-Language': 'ru'
        })
        try:
            response = urlopen(request, context=context)
            contents = response.read()
        except HTTPError, error:
            contents = error.read()
            logger.error("Request to %s returned with error. Content: %s", url, contents)
        return json.loads(contents)


def measured(func):
    """
    Декоратор, позволяет удобно помечать функции, время выполнения которых должно измеряться
    В качестве тега, отправляемого в лунапарк, будет использовано имя функции
    """

    def func_wrapper(self, *args, **kwargs):
        if self.measurer:
            with self.measurer(func.__name__):
                return func(self, *args, **kwargs)
        else:
            return func(self, *args, **kwargs)

    return func_wrapper


def measured_with(tag):
    """
    Декоратор, позволяет удобно помечать тегами функции, время выполнения которых должно измеряться
    :param tag: тег, отправляемый в лунапарк
    """

    def wrapper(func):
        def func_wrapper(self, *args, **kwargs):
            if self.measurer:
                with self.measurer(tag):
                    return func(self, *args, **kwargs)
            else:
                return func(self, *args, **kwargs)

        return func_wrapper

    return wrapper
