import uuid

from job_page.artifacts.tank import TankAPICommunicator
from unittest.mock import NonCallableMock


class BaseTestingTankApi(NonCallableMock, TankAPICommunicator):
    @property
    def host(self):
        return 'http://lee.tanks.yandex.net'

    @property
    def port(self):
        return 8080

    @property
    def local_id(self):
        return uuid.UUID('1186322f-4d31-4541-a045-7cd67f822a9e')


class SimpleTankApi(BaseTestingTankApi):
    @property
    def artifacts_list(self):
        return [
            {
                'url': '',
                'filename': 'file.txt',
                'label': 'textfile'
            },
            {
                'url': '',
                'filename': 'statham.json'
            }
        ]

    def get_artifact(self, filename):
        if filename == 'file.txt':
            return b'Some text here'
        if filename == 'statham.json':
            return b'{"a": "b", "c": "d"}'
        raise FileNotFoundError(filename)


class UnavailableTankApi(BaseTestingTankApi):
    @property
    def artifacts_list(self):
        raise ConnectionError('Error from tests. Artifacts config unavailable')

    def get_artifact(self, filename):
        raise ConnectionError('Error from tests. Artifact {} unavailable'.format(filename))


class DisconnectedTankApi(BaseTestingTankApi):
    @property
    def artifacts_list(self):
        return [
            {
                'url': '',
                'filename': 'file.txt',
                'label': 'textfile'
            },
            {
                'url': '',
                'filename': 'statham.json'
            }
        ]

    def get_artifact(self, filename):
        raise ConnectionError('Error from tests. TankAPI disconnected')
