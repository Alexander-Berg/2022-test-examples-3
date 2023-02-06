import requests


class ClientApiWrapper(object):
    def __init__(self, host='http://localhost', port=None):
        self.Host = '{}:{}'.format(host, port) if port else host

    def get(self, url, **kwargs):
        return requests.get(self.Host + url, **kwargs)

    def post(self, url, **kwargs):
        return requests.post(self.Host + url, **kwargs)
