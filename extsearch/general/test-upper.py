#!/usr/bin/env python
#-*- coding: utf-8 -*-

import sys
import time
import random
import os.path
import threading

path = os.path.abspath('extsearch/audio/scripts/quality')
if os.path.isdir(path):
    sys.path.append(path)

import chunked_post

def parse_args():
    from optparse import OptionParser
    parser = OptionParser()
    parser.usage = '%prog '
    parser.set_defaults(host='localhost',
                        port=9044,
                        xml=False,
                        hr=False,
                        rpm=20.0,
                        max_requests = 30, 
                        sample_dir = None,
                        sample_path = None,
                        close_socket = False,
                        mobile = False,
                        output_file=None)
    parser.add_option('--host', dest='host',
                      help='hostname of the server')
    parser.add_option('--port', dest='port', type='int',
                      help='port number of the server')
    parser.add_option('--xml', dest='xml', action='store_true',
                      help='Request xml output')
    parser.add_option('--hr', dest='hr', action='store_true',
                      help='Request human readable protobuf output')
    parser.add_option('--rpm', dest='rpm', type='float',
                      help='Requests per minutes')
    parser.add_option('--max-requests', dest='max_requests', type='int',
                      help='Maximum number of requests to send')
    parser.add_option('--sample-dir', dest='sample_dir',
                      help='Directory with samples to send')
    parser.add_option('--sample-file', dest='sample_path',
                      help='File with sample to send')
    parser.add_option('--close-socket', dest='close_socket', action='store_true',
                      help='Close socket after response from backend')
    parser.add_option('--mobile', dest='mobile', action='store_true',
                      help='Simulate mobile client')
    parser.add_option('-o', '--out', dest='output_file', type='str',
                      help='File to write a response (MAX_REQUESTS must be 1).')
    (options, args) = parser.parse_args()

    if options.mobile:
        if options.host == 'localhost':
            options.host = 'match.music.yandex.net'
        options.baseurl = 'https://%s/match/upload' % (options.host)
        options.mobile_headers = { 'Content-Type': 'audio/pcm-data', 
            'X-Yandex-Music-Device': 'os=Android; os_version=4.3; manufacturer=HTC; model=HTC One; clid=${clid.number}; device_id=28854b6a3e571ef47e82c5142e731795; uuid=e23b2630527f43368f5301e24e97dc9a',
            'X-Yandex-Music-Client': 'YandexMusicAndroid/1.6.0'}
    else:
        options.baseurl = 'http://%s:%d/yandsearch?text=audio&nocache=da' % (options.host, int(options.port))

    options.params = ''

    if options.xml:
        options.params += '&xml=da'
    else:
        options.params += '&ms=proto'

    if options.hr and not options.xml:
        options.params += '&hr=da'

    options.rps = options.rpm / 60.0
    options.avg_delay = 1.0 / options.rps
    options.delay_range = options.avg_delay / 10

    if options.sample_dir is not None:
        options.sample_dir = os.path.abspath(os.path.expanduser(options.sample_dir))
    else:
        if options.sample_path is None:
            options.sample_path = os.path.abspath('extsearch/audio/upper/good.wav')
        else:
            options.sample_path = os.path.abspath(os.path.expanduser(options.sample_path))
        assert os.path.isfile(options.sample_path)

    if options.output_file:
        assert options.max_requests == 1

    return options

def print_options(options):
    print "Requests per minute (rpm): ", options.rpm
    print "Requests per second (rps): ", options.rps
    print "Average delay between requests (seconds): ", options.avg_delay
    print "Range of random addition to average delay between requests (seconds): ", options.delay_range
    if options.sample_dir is not None:
        print "Directory with samples: ", options.sample_dir

class PerformanceTest:
    def __init__(self, options):
        self.options = options
        self.sample_data = None
        self.sample_list = None
        self.sample_index = 0
        if self.options.sample_dir is None:
            with open(self.options.sample_path, 'rb') as sample_file:
                self.sample_data = sample_file.read()
        else:
            self.sample_list = os.listdir(self.options.sample_dir)

        self.lock = threading.Lock()
        self.threads = {}

    def run(self):
        options = self.options
        for i in xrange(options.max_requests):
            delay = random.uniform(options.avg_delay - options.delay_range, options.avg_delay + options.delay_range)
            time.sleep(delay)
            reqid = self._make_reqid()
            speed = self._select_speed()
            sample_filename = self._select_sample()
            print delay, reqid, speed, len(self.threads), sample_filename if sample_filename is not None else ''
            self.start_request(reqid, speed, sample_filename)
        self.wait_requests()

    def request_thread(self, reqid, speed, sample_filename, output_file):
        request = self._prepare_request(reqid, speed, sample_filename)
        response = self._send_request(request)
        if output_file:
            with open(output_file, 'w') as of:
                of.write(response)
        with self.lock:
            del self.threads[threading._get_ident()]

    def start_request(self, reqid, speed, sample_filename = None):
        thread = threading.Thread(target = self.request_thread, args=(reqid, speed, sample_filename, self.options.output_file))
        thread.start()
        with self.lock:
            self.threads[thread.ident] = thread

    def wait_requests(self):
        for thread in self.threads.values():
            thread.join()

    def _prepare_request(self, reqid, speed, sample_filename = None):
        class Request:
            pass
        request = Request()
        options = self.options

        if options.mobile:
            request.url = options.baseurl
            request.headers = options.mobile_headers
        else:
            request.url = options.baseurl + options.params + '&reqid=' + reqid
            request.headers = None

        if sample_filename is not None:
            with open(sample_filename, 'rb') as sample_file:
                request.data = sample_file.read()
        else:
            assert self.sample_data
            request.data = self.sample_data

        request.speed = speed
        request.portion_size = 1024
        if request.speed == float('inf'):
            request.portion_size = sys.maxint
        return request

    def _send_request(self, request):
        # chunked_post.slow_send does not use size_per_chunk parameter
        return chunked_post.slow_send(request.url, 
                                      request.data, 
                                      size_per_chunk=None, 
                                      speed=request.speed, 
                                      portion_size=request.portion_size,
                                      send_zero_chunk=not self.options.close_socket,
                                      headers=request.headers)

    @staticmethod
    def _make_reqid():
        import uuid
        return str(uuid.uuid4())

    @staticmethod
    def _select_speed():
        #return 16000
        return 8000

        KILO = 1024
        MEGA = 1024 * KILO

        # uniform probability distribution is not appropriate here
        #case = random.randint(1, 3)
        #case = random.randint(2, 3)
        #case = 1
        if case == 1:
            return float('inf') # no delays
        if case == 2:
            # Wi-Fi
            wifi_speeds = [ 1 * MEGA, 2 * MEGA, 54 * MEGA, 600 * MEGA ]
            speed = float(wifi_speeds[ random.randint(0, len(wifi_speeds)-1) ]) / 10.0 # convert from bits/second to bytes/second
            return speed
        if case == 3:
            # GPRS and EDGE
            time_slots = random.randint(1, 5)
            slot_speed = random.uniform(9.05, 59.2) * float(KILO) # in bits/second
            return time_slots * slot_speed / 10.0
        assert False

    def _select_sample(self):
        if not isinstance(self.sample_list, list) or len(self.sample_list) == 0:
            return None
        filename = self.sample_list[self.sample_index]
        self.sample_index = (self.sample_index + 1) % len(self.sample_list)
        return os.path.join(self.options.sample_dir, filename)

def main():
    options = parse_args()
    print_options(options)
    PerformanceTest(options).run()
    return 0

if __name__ == "__main__":
    sys.exit(main())
