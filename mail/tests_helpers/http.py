import requests
import retrying

import yatest.common

work_dir = yatest.common.work_path()


@retrying.retry(
    retry_on_exception=lambda e: isinstance(e, requests.RequestException),
    wait_exponential_multiplier=1000, wait_exponential_max=10000,
)
def wait_ping(host):
    r = requests.get(u'{host}/ping'.format(host=host))
    return r.text.strip() == u'pong'
