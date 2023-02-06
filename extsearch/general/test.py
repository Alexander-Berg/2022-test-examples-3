import extsearch.audio.yamrec.upper.config as config
from extsearch.audio.yamrec.upper.logger import Logger
import yatest

import httplib
import json
from multiprocessing import Pool
import sys
import tornado.testing
import unittest

# __import__ is called this way because of:
# https://stackoverflow.com/questions/9806963/how-to-use-pythons-import-function-properly-import

# mock heavy modules that require tensorflow
sys.modules['extsearch.audio.yamrec.upper.audioprocessor'] = __import__('extsearch.audio.yamrec.upper.testmocks', globals(), locals(),
            ['parse_wave_header', 'extract_audio', 'FingerprintsCalculator', 'MusicClassifier', 'classifier', 'fingerprints_calculator'], -1)

# mock httpsearchclient to introduce fake middlesearch when needed
sys.modules['httpsearchclient'] = __import__('extsearch.audio.yamrec.upper.testmocks', globals(), locals(), ['HttpSearchClient'], -1)

# mock make_mel from outer directory, as it is not really used in tests
sys.modules['extsearch.audio.yamrec.upper.make_mel'] = __import__('extsearch.audio.yamrec.upper.testmocks', globals(), locals(), ['build_spectrogram'], -1)

from extsearch.audio.yamrec.upper.audiouppersearch import create_application, process_init

config_file = yatest.common.source_path('extsearch/audio/yamrec/upper/tests/test.cfg')


class TestUppersearch(tornado.testing.AsyncHTTPTestCase):
    def get_app(self):
        class TestConfig:
            def __init__(self):
                self.config_file = config_file
                self.port = None
                self.host = None
                self.host6 = None
                self.work_dir = None
                self.model = None
                self.confidence_threshold = None
                self.Server = None
        conf = config.parse_config(TestConfig())
        self.logger = Logger(conf.Server)
        pool = Pool(conf.Server.WorkerProcesses, process_init, [conf])
        with open(yatest.common.source_path('extsearch/audio/yamrec/upper/tests/test.wav')) as input_file:
            self.test_audio = input_file.read()
        return create_application(conf, self.logger, pool)

    def tearDown(self):
        self.logger.close()
        tornado.testing.AsyncHTTPTestCase.tearDown(self)

    def get_new_ioloop(self):
        return tornado.ioloop.IOLoop.instance()

    def test_ping(self):
        response = self.fetch('/yandsearch?info=ping')
        self.assertEqual(response.code, httplib.OK)
        self.assertEqual(response.body, "I'm alive!\n")

    def test_getconfig(self):
        response = self.fetch('/yandsearch?info=getconfig')
        self.assertEqual(response.code, httplib.OK)

    def test_getversion(self):
        response = self.fetch('/yandsearch?info=getversion')
        self.assertEqual(response.code, httplib.OK)

    def test_unknowninfo(self):
        response = self.fetch('/yandsearch?info=unknown')
        self.assertEqual(response.code, httplib.BAD_REQUEST)

    def test_recognize(self):
        response = self.fetch('/', method='POST', body=self.test_audio)
        r = json.loads(response.body)
        self.assertEqual(response.code, httplib.OK)
        self.assertEqual(len(r['result']['tracks']), 2)
        self.assertIn(r['result']['tracks'][0]['trackid'], set(['2381618', '2166552']))

    def test_recognize_middlesearch_error(self):
        response = self.fetch('/', method='POST', body=self.test_audio)
        self.assertEqual(response.code, httplib.OK)
        # second fetch will take 2 attempts to complete due to the mocked httpsearchclient
        response = self.fetch('/', method='POST', body=self.test_audio)
        self.assertEqual(response.code, httplib.OK)

    def test_recognize_g(self):
        response = self.fetch('/recognize?RelevanceThreshold=0.6&g=0..1', method='POST', body=self.test_audio)
        r = json.loads(response.body)
        self.assertEqual(response.code, httplib.OK)
        self.assertEqual(len(r['result']['tracks']), 1)
        self.assertIn(r['result']['tracks'][0]['trackid'], set(['2381618', '2166552']))

    def test_recognize_threshold(self):
        response = self.fetch('/recognize?RelevanceThreshold=0.6', method='POST', body=self.test_audio)
        r = json.loads(response.body)
        self.assertEqual(response.code, httplib.OK)
        self.assertGreater(len(r['result']['tracks']), 1)
        self.assertIn(r['result']['tracks'][0]['trackid'], set(['2381618', '2166552']))

    def test_classify(self):
        response = self.fetch('/classify', method='POST', body=self.test_audio)
        self.assertEqual(response.code, 200)

    def test_client_in_tass(self):
        client = 'unittest'
        response = self.fetch('/recognize?client={}'.format(client), method='POST', body=self.test_audio)
        response = self.fetch('/classify?client={}'.format(client), method='POST', body=self.test_audio)
        response = self.fetch('/tass')
        r = json.loads(response.body)
        found = False
        for metric in r:
            if metric[0].startswith('client'):
                self.assertEqual(metric[0], 'client_{}_dmmm'.format(client))
                self.assertEqual(metric[1], 2)
                found = True
        self.assertTrue(found)


if __name__ == '__main__':
    unittest.main()
