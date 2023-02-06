import requests
from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import TJobResult, THttpFeatures


class LogAggregatorClient(object):
    def __init__(self, config, proxy_port):
        self.api_url = 'http://localhost:{}'.format(config.server_port)
        self.proxy_url = 'http://localhost:{}'.format(proxy_port)

    def ping(self):
        try:
            requests.get('{}/ping'.format(self.api_url)).raise_for_status()
        except:
            return False
        return True

    def url_start(self, item):
        requests.post('{}/job/start'.format(self.api_url), data=item.SerializeToString()).raise_for_status()

    def url_update(self, item):
        requests.post('{}/job/update'.format(self.api_url), data=item.SerializeToString()).raise_for_status()

    def url_finish(self):
        resp = requests.get('{}/job/finish'.format(self.api_url))
        resp.raise_for_status()
        job = TJobResult()
        job.ParseFromString(resp.content)
        del job.Profile[:]
        del job.Artifacts[:]
        return job

    def url_get(self, job_id):
        resp = requests.get('{}/job/get?job_id={}'.format(self.api_url, job_id))
        resp.raise_for_status()
        job = TJobResult()
        job.ParseFromString(resp.content)
        return job

    def has_pretty_video(self):
        resp = requests.get('{}/http/pretty_video'.format(self.api_url))
        resp.raise_for_status()
        return resp.json()['has_pretty_video']

    def has_known_player(self):
        resp = requests.get('{}/http/known_player'.format(self.api_url))
        resp.raise_for_status()
        return resp.json()['has_known_player']

    def proxy_stat(self):
        resp = requests.get('{}/stat/reset'.format(self.proxy_url))
        resp.raise_for_status()
        http = THttpFeatures()
        http.ParseFromString(resp.content)
        return http
