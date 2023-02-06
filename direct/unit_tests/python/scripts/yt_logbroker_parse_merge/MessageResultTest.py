import unittest
from yandex.tap import tap_run
import sys, os
from subprocess import call

call([os.getenv('PYTHONPATH')+"/../protected/maintenance/make_yaml_settings.pl"])
from scripts.yt_logbroker_parse_merge import MessagesResult


def _norm(data):
    ret = list(MessagesResult().normalize({'log_type': 'msg', 'data': data}))
    return ret


class MessageResultTest(unittest.TestCase):
    def test_perl_format(self):
        """ parsing perl messages.log """
        common = {'class_name': None, 'prefix': None, 'log_level': None, 'parent_id': 0, 'host': 'ppcdev4.yandex.ru', 'service': 'direct.script', 'span_id': 1314708194963030138, 'log_time': '2016-07-22 18:42:00', 'log_time_nanos': 0, 'trace_id': 1314708194963030138, 'log_type': 'msg', 'method': 'ppcMonitorYTResourceUsage'};
        self.assertEqual(
            _norm("2016-07-22:18:42:00 ppcdev4.yandex.ru,direct.script/ppcMonitorYTResourceUsage,1314708194963030138:0:1314708194963030138 message"), 
            [dict(common, message='message')]
        )
        self.assertEqual(
            _norm(r"""2016-07-22:18:42:00.012 ppcdev4.yandex.ru,direct.script/ppcMonitorYTResourceUsage,1314708194963030138:0:1314708194963030138#bulk ["message", "message2"]"""),
            [dict(common, message='message', log_time_nanos=12000000), dict(common, message='message2', log_time_nanos=12000000)]
        )
        self.assertEqual(
            _norm(r"""2016-07-22:18:42:00.12345678901 ppcdev4.yandex.ru,direct.script/ppcMonitorYTResourceUsage,1314708194963030138:0:1314708194963030138#bulk ["message", {}, []]"""),
            [dict(common, message='message', log_time_nanos=123456789), dict(common, message='{}', log_time_nanos=123456789), dict(common, message='[]', log_time_nanos=123456789)]
        )

    def test_java_format(self):
        """ parsing java messages.log """
        common = {'parent_id': 0, 'host': 'ppcdev-java-2.haze.yandex.net', 'service': 'direct.jobs', 'span_id': 1314708194963030138,
                  'log_time': '2017-05-12 00:00:00', 'log_time_nanos': 0, 'trace_id': 1314708194963030138, 'log_type': 'msg',
                  'method': 'campaignlastchange.CampAggregatedLastchangeFeeder', 'prefix':'direct-job-pool_Worker-5',
                  'log_level': 'INFO', 'class_name': 'ru.yandex.direct.jobs.interceptors.JobLoggingInterceptor'}

        self.assertEqual(
            _norm(r"""2017-05-12:00:00:00 ppcdev-java-2.haze.yandex.net,direct.jobs/campaignlastchange.CampAggregatedLastchangeFeeder,1314708194963030138:0:1314708194963030138 [direct-job-pool_Worker-5] INFO  ru.yandex.direct.jobs.interceptors.JobLoggingInterceptor - START shard_7"""),
            [dict(common, message='START shard_7')]
        )

if __name__ == '__main__':
    tap_run(tests=2)

