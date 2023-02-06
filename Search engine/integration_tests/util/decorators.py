# -*- encoding: utf8 -*-

import yt.wrapper as yt

import logging
import re
import time

__all__ = ["get_global_yt_proxy", "retry_method_on_exceptions"]


def get_global_yt_proxy():
    url = yt.config["proxy"]["url"]
    return re.findall(r'([^\d\.]+)', url)[0]


def retry_method_on_exceptions(func):
    """Decorator for SerpsDownloader methods
    uses SerpsDownloader.network_try_count and SerpsDownloader.network_retry_timeout
    """
    def wrapper(*args, **kwargs):
        self = args[0]
        tries_left = self.network_try_count
        timeout = self.network_retry_timeout
        while tries_left > 0:
            try:
                return func(*args, **kwargs)
            except Exception:
                tries_left -= 1
                if tries_left > 0:
                    logging.exception('Caught the exception in method "{0}". Retrying. Tries left: {1}'.format(func.__name__, tries_left))
                    time.sleep(timeout)
                else:
                    raise
    return wrapper


class IterToStream(object):
    def __init__(self, iterable):
        self.buffered = ""
        self.iter = iter(iterable)

    def read(self, size):
        result = ""
        while size > 0:
            data = self.buffered or next(self.iter, None)
            self.buffered = ""
            if data is None:
                break
            size -= len(data)
            if size < 0:
                data, self.buffered = data[:size], data[size:]
            result += data
        return result
