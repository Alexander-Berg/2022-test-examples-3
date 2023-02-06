import requests
import retrying


class MdbSaveApi(object):
    def __init__(self, location='http://localhost:8080'):
        self.location = location
        self.service = 'nsls'
        self.session_id = 'ConnectionId-EnvelopeId'

    @retrying.retry(stop_max_delay=15000)
    def save(self, body):
        return requests.post(
            self.location + '/1/save',
            params={'service': self.service, 'session_id': self.session_id},
            json=body,
            timeout=5
        )
