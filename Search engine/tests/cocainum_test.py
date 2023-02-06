import math
import uuid
import json
from collections import defaultdict
from time import sleep

import multiprocessing

import requests
import yt.wrapper as yt

import core.loop
import processors.abstract_processor
import processors.scraper_processor
import yt_util.yt_util
from ..cocainum import configure_logging


class MockProcessor(processors.abstract_processor.AbstractProcessor):

    def __init__(self, return_status=core.loop.Loop.STATUS_COMPLETE):
        super(MockProcessor, self).__init__()
        self.cancel = []
        self.send = []
        self.status = []
        self.write = []
        self.__next_batch = 0
        self.__return_status = return_status

    def cancel_task(self, task):
        self.cancel.append(task.batch_id())

    def send_request(self, task):
        self.send.append(task.input_table)
        self.__next_batch += 1
        return self.__next_batch

    def request_status(self, task):
        batch_id = task.batch_id()
        self.status.append(batch_id)
        return self.__return_status

    def write_response(self, task):
        self.write.append(str(task))


class YtTestEnv(object):

    def __init__(self, input_api='//tmp/tellurium.api.in.test', output_api='//tmp/tellurium.api.out.test', proxy='freud.yt.yandex.net'):
        self.__yt = yt_util.yt_util.create_yt_client(proxy)
        self.__api_input_root = input_api
        self.__api_output_root = output_api
        self.__yt.create("map_node", self.__api_input_root, ignore_existing=True, recursive=True)
        self.__yt.create("map_node", self.__api_output_root, ignore_existing=True, recursive=True)
        self.__tables = []
        self.__base_names = []

    @property
    def input_root(self):
        return self.__api_input_root

    @property
    def output_root(self):
        return self.__api_output_root

    @property
    def yt_client(self):
        return self.__yt

    @staticmethod
    def fake_records(count):
        return [{u'url': u'https://yandex' + unicode(n) + '.ru', u'tellurium.id': unicode(n)} for n in range(count)]

    @staticmethod
    def fake_tellurium_attributes():
        return {'engine': 'tellurium.chrome.v62',
                'failure': {'client': {'maxrate': 1.0, 'retry': 0L}, 'internal': {'maxrate': 1.0, 'retry': 0L},
                            'external': {'maxrate': 1.0, 'retry': 0L}}, 'window': {'width': 360L, 'height': 640L},
                'region': 'yandex', 'wait': 12L,
                'browser': {'chrome': {'wholepage': True}, 'pixelratio': 3.0, 'language': 'ru', 'mobile': True,
                            'useragent': 'Mozilla/5.0 (Linux; Android 7.0; SAMSUNG SM-A510F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.4 Chrome/51.0.2704.106 Mobile Safari/537.36',
                            'downscale': True}}

    @staticmethod
    def fake_scraper_attributes():
        return {'quota': 'default', 'processor': 'tellurium', 'ttl': 3}

    def create_table_with_fake_data(self, records_count=10, tellurium_control_cancel=False):
        name = str(uuid.uuid4())
        self.create_table(name, self.fake_records(records_count), self.fake_tellurium_attributes(), self.fake_scraper_attributes(),
                          tellurium_control_cancel)
        return name

    def create_table(self, name, records, tellurium_attributes, scraper_attributes, tellurium_control_cancel=False):
        table = yt.ypath_join(self.__api_input_root, name)
        self.__yt.create("table", table, ignore_existing=True)
        self.__yt.write_table(table, (json.dumps(record) for record in records), format=yt.JsonFormat(), raw=True)
        self.__yt.set(yt.ypath_join(table, '@tellurium.parameters'), tellurium_attributes)
        if tellurium_control_cancel:
            self.__yt.set(yt.ypath_join(table, '@tellurium.control'),
                          {yt_util.yt_util.YtAPI.TELLURIUM_CONTROL_ACTION: yt_util.yt_util.YtAPI.TELLURIUM_CONTROL_CANCEL})
        self.__yt.set(yt.ypath_join(table, '@scraper.parameters'), scraper_attributes)
        self.__tables.append(table)
        self.__base_names.append(name)

    def copy_table(self, source, target):
        self.__yt.copy(source, target)
        self.__tables.append(target)

    def remove_created_tables(self):
        for table in self.__tables:
            if self.__yt.exists(table):
                self.__yt.remove(table)
        for name in self.__base_names:
            table = yt.ypath_join(self.__api_output_root, name)
            if self.__yt.exists(table):
                self.__yt.remove(table)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.remove_created_tables()


class BoolCounter(object):

    def __init__(self, count):
        self.__count = count

    def __nonzero__(self):
        if self.__count == 0:
            return False
        self.__count -= 1
        return True


class MockScraperAPI(object):

    def __init__(self):
        self.create = []
        self.statuses = []
        self.results = []

    def create_batch(self, request):
        self.create.append(request)
        return {'ticket': 1}

    def status(self, batch_id):
        self.statuses.append(batch_id)
        return {'status': 'COMPLETE'}

    def result(self, batch_id):
        self.results.append(batch_id)
        return {u'screenshots': [
            {u'status': {u'status': u'SUCCESS', u'details': u'done at 2018-01-19T13:58:11.906+03:00'},
             u'request-id': u'http://yandex1.ru',
             u'screenshot-url': u'http://storage.mds.yandex.net/get-mturk/1138230/5cd04e57-4dab-45f2-a9b2-559a6ac12285.png',
             u'details': {u'status': u"success", u'failure': u''}},
            {u'status': {u'status': u'SUCCESS', u'details': u'done at 2018-01-19T13:58:11.906+03:00'},
             u'request-id': u'http://yandex2.ru',
             u'screenshot-url': u'http://storage.mds.yandex.net/get-mturk/1133998/a5aa0df6-2899-427f-b2cc-a71090114390.png',
             u'details': {u'status': u"success", u'failure': u''}}
        ]}


def test_simple():
    with YtTestEnv() as yt_env:
        yt_env.create_table_with_fake_data()
        yt_api = yt_util.yt_util.YtAPI('test', yt_env.input_root, yt_env.output_root, yt_env.yt_client)
        processor = MockProcessor()
        loop = core.loop.Loop(yt_api, {'tellurium': processor})
        loop.loop(sleep_time=0, loop=BoolCounter(1))
        assert len(processor.send) == 1
        assert len(processor.status) == 1
        assert len(processor.cancel) == 0
        assert len(processor.write) == 1
        assert processor.send[0]


def test_simple_combine():
    with YtTestEnv() as yt_env1, YtTestEnv(input_api='//tmp/tellurium.api.in.test2', output_api='//tmp/tellurium.api.out.test2') as yt_env2:
        yt_env1.create_table_with_fake_data()
        yt_env2.create_table_with_fake_data()
        yt_api1 = yt_util.yt_util.YtAPI('test', yt_env1.input_root, yt_env1.output_root, yt_env1.yt_client)
        yt_api2 = yt_util.yt_util.YtAPI('test', yt_env2.input_root, yt_env2.output_root, yt_env2.yt_client)
        processor = MockProcessor()
        loop = core.loop.Loop(yt_util.yt_util.YtAPIComposite(yt_api1, yt_api2), {'tellurium': processor})
        loop.loop(sleep_time=0, loop=BoolCounter(1))
        assert len(processor.send) == 2
        assert len(processor.status) == 2
        assert len(processor.cancel) == 0
        assert len(processor.write) == 2


def test_request():
    with YtTestEnv() as yt_env:
        yt_env.create_table_with_fake_data()
        yt_api = yt_util.yt_util.YtAPI('test', yt_env.input_root, yt_env.output_root, yt_env.yt_client)
        scraper_api = MockScraperAPI()
        defaults = {
            processors.abstract_processor.AbstractProcessor.DEFAULT_USER: {
                processors.abstract_processor.AbstractProcessor.TELLURIUM_DEFAULTS: {
                    processors.abstract_processor.AbstractProcessor.ENGINE_TP: 'tellurium.chrome.v62',
                    processors.abstract_processor.AbstractProcessor.REGION_TP: 'yandex',
                    processors.abstract_processor.AbstractProcessor.LANGUAGE_TP: 'ru',
                    processors.abstract_processor.AbstractProcessor.USERAGENT_TP: 'Mozilla/5.0 (Linux; Android 7.0; SAMSUNG SM-A510F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.4 Chrome/51.0.2704.106 Mobile Safari/537.36',
                    processors.abstract_processor.AbstractProcessor.MOBILE_TP: True,
                    processors.abstract_processor.AbstractProcessor.WIDTH_TP: 360,
                    processors.abstract_processor.AbstractProcessor.HEIGHT_TP: 640,
                    processors.abstract_processor.AbstractProcessor.WAIT_TP: 5,
                    processors.abstract_processor.AbstractProcessor.WHOLEPAGE_TP: True,
                    processors.abstract_processor.AbstractProcessor.DOWNSCALE_TP: True,
                    processors.abstract_processor.AbstractProcessor.PIXELRATIO_TP: 3,
                    processors.abstract_processor.AbstractProcessor.RETRY_TP: 0,
                    processors.abstract_processor.AbstractProcessor.MAXRATE_TP: 0,
                },
                processors.abstract_processor.AbstractProcessor.SCRAPER_DEFAULTS: {
                    processors.abstract_processor.AbstractProcessor.PROCESSOR_SP: 'tellurium',
                    processors.abstract_processor.AbstractProcessor.QUOTA_SP: 'default',
                    processors.abstract_processor.AbstractProcessor.TTL_SP: 1
                }
            }
        }
        processor = processors.scraper_processor.ScreenshotScraperAPIProcessor(scraper_api, defaults)
        loop = core.loop.Loop(yt_api, {'tellurium': processor})
        loop.loop(sleep_time=0, loop=BoolCounter(1))
        assert len(scraper_api.create) == 1
        print(scraper_api.statuses)
        assert len(scraper_api.statuses) == 1
        assert len(scraper_api.results) == 1


def test_parse_engine():
    assert processors.abstract_processor.AbstractProcessor._parse_tellurium_engine('tellurium.chrome.v62') == ('CHROME', '62')
    assert processors.abstract_processor.AbstractProcessor._parse_tellurium_engine('tellurium.firefox.default') == ('FIREFOX', None)


def test_to_java8_duration():
    assert processors.scraper_processor.ScreenshotScraperAPIProcessor._to_java8_duration(days=1) == 'PH24H'
    assert processors.scraper_processor.ScreenshotScraperAPIProcessor._to_java8_duration(seconds=10) == 'PH10S'


def test_hack_accept_language():
    assert processors.scraper_processor.ScreenshotScraperAPIProcessor._hack_accept_language('kz') == 'kk'
    assert processors.scraper_processor.ScreenshotScraperAPIProcessor._hack_accept_language(None) is None
    assert processors.scraper_processor.ScreenshotScraperAPIProcessor._hack_accept_language('ru') == 'ru'
    assert processors.scraper_processor.ScreenshotScraperAPIProcessor._hack_accept_language('ua') == 'uk'


def test_sequense_to_histogram():
    result = to_histogram([0.1, 0.9, 1, 1.1, 2.2, 3.3, 5, 11.4])
    assert result[0] == [0, 1]
    assert result[1] == [1.0, 3]
    assert result[2] == [1.5, 1]
    assert result[3] == [2.25, 1]
    assert result[4] == [3.375, 1]
    assert result[5] == [5.0625, 0]
    assert result[6] == [7.59375, 0]
    assert result[7] == [11.390625, 1]


# def test_create_data():
#     tmp = YtTestEnv(input_api='//tmp/tellurium.in')
#     tmp.create_table_with_fake_data()

def test_cancel_by_tellurium_control():
    with YtTestEnv() as yt_env:
        yt_api = yt_util.yt_util.YtAPI('test', yt_env.input_root, yt_env.output_root, yt_env.yt_client)
        processor = MockProcessor(return_status=core.loop.Loop.STATUS_PROGRESS)
        create_fake_tables(yt_env, 1, pairs=True, in_progress=True, tellurium_control_cancel=True)
        loop = core.loop.Loop(yt_api, {'tellurium': processor})
        loop.loop(sleep_time=1, loop=BoolCounter(1))
        assert len(processor.cancel) == 1


def create_fake_tables(yt_env, n, pairs, in_progress=False, tellurium_control_cancel=False):
    created_tables = []
    for _ in range(n):
        fake_table_name = yt_env.create_table_with_fake_data()
        input_table_path = yt.ypath_join(yt_env.input_root, fake_table_name)
        created_tables.append(input_table_path)
        if tellurium_control_cancel:
            output_table_tellurium_control_attribute_path = yt.ypath_join(input_table_path, yt_util.yt_util.YtTask.TELLURIUM_CONTROL_ATTR)
            tellurium_control = {
                yt_util.yt_util.YtAPI.TELLURIUM_CONTROL_ACTION: yt_util.yt_util.YtAPI.TELLURIUM_CONTROL_CANCEL}
            yt_env.yt_client.set(output_table_tellurium_control_attribute_path, tellurium_control)
        if pairs:
            output_suffix = ".inprogress" if in_progress else ""
            output_table_path = yt.ypath_join(yt_env.output_root, fake_table_name + output_suffix)
            yt_env.copy_table(input_table_path, output_table_path)
            created_tables.append(output_table_path)
            output_table_batchid_attribute_path = yt.ypath_join(output_table_path, yt_util.yt_util.YtTask.SCRAPER_BATCHID_ATTR)
            yt_env.yt_client.set(output_table_batchid_attribute_path, "12345")
            output_table_status_attribute_path = yt.ypath_join(output_table_path, yt_util.yt_util.YtTask.SCRAPER_STATUS_ATTR)
            if in_progress:
                yt_env.yt_client.set(output_table_status_attribute_path, core.loop.Loop.STATUS_PROGRESS)
            else:
                yt_env.yt_client.set(output_table_status_attribute_path, core.loop.Loop.STATUS_COMPLETE)
    return created_tables


def test_automatic_quota_watch():
    with YtTestEnv(input_api="//home/cocainum/auto_tests/in", output_api="//home/cocainum/auto_tests/out") as yt_env:
        for table in yt_env.yt_client.list(yt_env.input_root, absolute=True) + \
                     yt_env.yt_client.list(yt_env.output_root, absolute=True):
            if yt_env.yt_client.exists(table):
                yt_env.yt_client.remove(table)

        node_count_limit = yt_env.yt_client.get("//sys/accounts/cocainum/@resource_limits")['node_count']
        node_count_usage = yt_env.yt_client.get("//sys/accounts/cocainum/@resource_usage")['node_count']

        old_finished_pairs = 10
        pairs_in_progress = 10
        unpaired_tables = 5
        new_finished_pairs = 10

        total_table_count = (old_finished_pairs + pairs_in_progress + new_finished_pairs) * 2 + unpaired_tables

        usage_threshold = 1.0 * (total_table_count - 2 * old_finished_pairs - new_finished_pairs + node_count_usage) / node_count_limit

        yt_api = yt_util.yt_util.YtAPI('test', yt_env.input_root, yt_env.output_root, yt_env.yt_client, usage_threshold=usage_threshold)

        create_fake_tables(yt_env, old_finished_pairs, pairs=True)

        assert yt_api.enough_free_quota()

        tables_to_be_kept = []  # e.g. tables in progress
        tables_to_be_kept.extend(create_fake_tables(yt_env, pairs_in_progress, pairs=True, in_progress=True))
        tables_to_be_kept.extend(create_fake_tables(yt_env, unpaired_tables, pairs=False))
        create_fake_tables(yt_env, new_finished_pairs, pairs=True)

        assert not yt_api.enough_free_quota()

        processor = MockProcessor(return_status=core.loop.Loop.STATUS_PROGRESS)
        # loop = t2s.Loop(yt_api, {'tellurium': processor})
        loop = core.loop.Loop(yt_util.yt_util.YtAPIComposite(yt_api), {'tellurium': processor})
        loop.loop(sleep_time=0, loop=BoolCounter(1))

        sleep(5)  # resource usage info might lag for a couple of seconds

        # print(yt_api.get_resource_usage())

        assert yt_api.enough_free_quota()

        remaining_tables = yt_env.yt_client.list(yt_env.input_root, absolute=True) + \
                           yt_env.yt_client.list(yt_env.output_root, absolute=True)

        assert all(t in remaining_tables for t in tables_to_be_kept)

        for table in remaining_tables:
            if yt_env.yt_client.exists(table):
                yt_env.yt_client.remove(table)


def get_unistat_metrics(queue):
    timeout = 3
    sleep(timeout)
    response_payload = requests.get("http://localhost:8080/unistat").json()
    queue.put(response_payload)


def test_errors_in_log_reporting():
    configure_logging()

    with YtTestEnv() as yt_env:
        yt_api = yt_util.yt_util.YtAPI('test', yt_env.input_root, yt_env.output_root, yt_env.yt_client)
        create_fake_tables(yt_env, 1, pairs=True, tellurium_control_cancel=True)
        # This input should yield a warning message

        scraper_api = MockScraperAPI()
        defaults = {
            processors.abstract_processor.AbstractProcessor.DEFAULT_USER: {
                processors.abstract_processor.AbstractProcessor.TELLURIUM_DEFAULTS: {
                    processors.abstract_processor.AbstractProcessor.ENGINE_TP: 'tellurium.chrome.v62',
                    processors.abstract_processor.AbstractProcessor.REGION_TP: 'yandex',
                    processors.abstract_processor.AbstractProcessor.LANGUAGE_TP: 'ru',
                    processors.abstract_processor.AbstractProcessor.USERAGENT_TP: 'Mozilla/5.0 (Linux; Android 7.0; SAMSUNG SM-A510F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.4 Chrome/51.0.2704.106 Mobile Safari/537.36',
                    processors.abstract_processor.AbstractProcessor.MOBILE_TP: True,
                    processors.abstract_processor.AbstractProcessor.WIDTH_TP: 360,
                    processors.abstract_processor.AbstractProcessor.HEIGHT_TP: 640,
                    processors.abstract_processor.AbstractProcessor.WAIT_TP: 5,
                    processors.abstract_processor.AbstractProcessor.WHOLEPAGE_TP: True,
                    processors.abstract_processor.AbstractProcessor.DOWNSCALE_TP: True,
                    processors.abstract_processor.AbstractProcessor.PIXELRATIO_TP: 3,
                    processors.abstract_processor.AbstractProcessor.RETRY_TP: 0,
                    processors.abstract_processor.AbstractProcessor.MAXRATE_TP: 0,
                },
                processors.abstract_processor.AbstractProcessor.SCRAPER_DEFAULTS: {
                    processors.abstract_processor.AbstractProcessor.PROCESSOR_SP: 'tellurium',
                    processors.abstract_processor.AbstractProcessor.QUOTA_SP: 'default',
                    processors.abstract_processor.AbstractProcessor.TTL_SP: 1
                }
            }
        }
        processor = processors.scraper_processor.ScreenshotScraperAPIProcessor(scraper_api, defaults)
        loop = core.loop.Loop(yt_api, {'tellurium': processor})
        queue = multiprocessing.Queue()
        p = multiprocessing.Process(target=get_unistat_metrics, args=(queue,))
        p.start()
        loop.loop(sleep_time=1, loop=BoolCounter(5))
        p.join()
        get = queue.get(timeout=5)
        keys = [m[0] for m in get]
        assert "warning_moving_sum_axxx" in keys


def test_yt_quota_data_in_unistat_report():
    with YtTestEnv() as yt_env:
        yt_api = yt_util.yt_util.YtAPI('test', yt_env.input_root, yt_env.output_root, yt_env.yt_client)
        create_fake_tables(yt_env, 1, pairs=True, tellurium_control_cancel=True)

        scraper_api = MockScraperAPI()
        processor = processors.scraper_processor.ScreenshotScraperAPIProcessor(scraper_api)
        loop = core.loop.Loop(yt_api, {'tellurium': processor})
        queue = multiprocessing.Queue()
        p = multiprocessing.Process(target=get_unistat_metrics, args=(queue,))
        p.start()
        loop.loop(sleep_time=1, loop=BoolCounter(5))
        p.join()
        get = queue.get(timeout=5)
        keys = [m[0] for m in get]
        assert "chunk_count_limit_freud_axxx" in keys
        assert "chunk_count_usage_freud_axxx" in keys
        assert "node_count_limit_freud_axxx" in keys
        assert "node_count_usage_freud_axxx" in keys
        assert "disk_space_limit_freud_axxx" in keys
        assert "disk_space_usage_freud_axxx" in keys


def to_histogram(timings):
    if len(timings) == 0:
        return [[0.0, 0]]
    hist = defaultdict(int)
    for timing in timings:
        bin = max(int(math.log(timing, 1.5)) + 1, 0)
        hist[bin] += 1
    result = []
    for i in range(0, max(hist, key=int) + 1):
        timing = math.pow(1.5, i - 1) if i > 0 else 0
        result.append([timing, hist[i]])
    return result
