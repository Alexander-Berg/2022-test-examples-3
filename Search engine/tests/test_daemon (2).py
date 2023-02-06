import yatest.common
import yatest.common.network
import time
import requests
import requests.adapters
import urllib3.util.retry as retry
import os
import sys
import gzip
import base64
import shutil
from library.python import sanitizers
from search.begemot.server.proto import begemot_pb2
from search.daemons.models_proxy.tests.lib.diff_responses import check_responses
from search.daemons.models_proxy.tests.lib.diff_responses import ComparePrecisionContext


# In CI, even localhost is unreliable once per week.
# By default, scatter refuses to issue a fallback request to the instance that failed the initial request.
# So make three copies of every instance.
search_source_config_template = """
<SearchSource>
    ServerDescr WEB
    ServerGroup main@web
    CgiSearchPrefix http://localhost:{port}/FETCH_DOC_DATA/{client} http://localhost:{port}/FETCH_DOC_DATA/{client} http://localhost:{port}/FETCH_DOC_DATA/{client}
</SearchSource>
"""
aux_source_config_template = """
<AuxSource>
    ServerDescr {type}
    CgiSearchPrefix http://localhost:{port}/{type} http://localhost:{port}/{type} http://localhost:{port}/{type}
</AuxSource>
"""
models_proxy_config_template = """
GlobalTimeout: "1000s"
MaxDocsInRtModelsRequest: {rtmodels_max_docs}
MaxDocsInSplitRtModelsRequest: {rtmodels_max_docs}
MaxDocsInCfgModelsRequest: {rtmodels_max_docs}
MaxDocsInFPMRequest: {rtmodels_max_docs}
MaxDocsPerSource: {source_max_docs}
UseLightFetchRequests: {use_light_fetch_requests}
UseSplitModels: false
Eventlog: "fetch-eventlog"
"""
bigrt_caches_config_part = """
BigRtQueryCacheConfig {
    # avg size 3600, allocation size 4000
    #MaxCacheSize: 1000
    MaxCacheSize: 4000000
}
BigRtHostCacheConfig {
    # avg size 106, allocation size 128
    #MaxCacheSize: 5000
    MaxCacheSize: 640000
}
BigRtUrlCacheConfig {
    # avg size 84, allocation size 100
    #MaxCacheSize: 15000
    MaxCacheSize: 1500000
}
"""
aux_subsources = (
    'RTMODELS',
    'RTMODELS_PERSONAL_CACHED',
    'BERT_FEATURE_CALCER',
    'BERT_FEATURE_CALCER_PERSONAL',
    'DYNTABLE_HTTP_PROXY_SOURCE',
    'RTMODELS_FEATURES_PERSONAL_MODEL',
    'CFG_MODELS',
)


max_requests = 1000
if sanitizers.asan_is_on() or sanitizers.tsan_is_on() or sanitizers.msan_is_on():
    max_requests = 200


def wait_daemon_start(process, ruchka):
    # wait a bit for the daemon to initialize
    while process.running:
        try:
            requests.get(ruchka)
            return True
        except:
            time.sleep(0.1)
    return False


def do_test(testid, rtmodels_max_docs, source_max_docs, use_light_fetch_requests, use_early_requests, use_bigrt_caches):
    tests_data_dir = 'bigrt_caches_tests_data/bigrtcache_' if use_bigrt_caches else 'tests_data/'
    # print("max_requests = {}".format(max_requests))
    with yatest.common.network.PortManager() as pm:
        # prepare&start mock subsources
        mocker_port = pm.get_port()
        mocker = yatest.common.execute(
            [
                yatest.common.binary_path("search/daemons/models_proxy/tests/mock_subsource/mock_subsource"),
                '--port', str(mocker_port),
                '--fetched-doc-data', tests_data_dir + 'fetched_doc_data.tsv.gz',
                '--aux-sources-data', tests_data_dir + 'subsources.tskv.gz',
                # '--threads', '1',
            ],
            wait=False
        )
        if not wait_daemon_start(mocker, 'http://localhost:{}/admin?action=ping'.format(mocker_port)):
            mocker.wait(check_exit_code=True)  # in case daemon has failed to start
            assert False  # if something went wrong
        should_validate_mocker = False
        try:
            # create mmeta config
            mmeta_mock_config = '<Collection autostart="must" meta="yes" id="yandsearch">\n'
            for i in range(50):  # should be more than enough
                mmeta_mock_config += search_source_config_template.format(port=mocker_port, client=i)
            for subsource in aux_subsources:
                mmeta_mock_config += aux_source_config_template.format(type=subsource, port=mocker_port)
            mmeta_mock_config += '</Collection>\n'
            # start the daemon
            basedir = yatest.common.work_path() + '/' + testid
            os.mkdir(basedir)
            os.mkdir(basedir + '/FetchDocData')
            os.mkdir(basedir + '/ModelsProxy')
            with open(basedir + '/FetchDocData/main.cfg', 'w') as g:
                g.write(mmeta_mock_config)
            with open(yatest.common.test_source_path('fetch_doc_data.cfg'), 'r') as f:
                with open(basedir + '/FetchDocData/fetch_doc_data.cfg', 'w') as g:
                    g.write(f.read())
            with open(basedir + '/ModelsProxy/models_proxy.cfg', 'w') as g:
                g.write(models_proxy_config_template.format(
                    rtmodels_max_docs=rtmodels_max_docs,
                    source_max_docs=source_max_docs,
                    use_light_fetch_requests=('true' if use_light_fetch_requests else 'false')
                ))
                if use_bigrt_caches:
                    g.write(bigrt_caches_config_part)
            daemon_port = pm.get_port()
            daemon = yatest.common.execute(
                [
                    yatest.common.binary_path("search/daemons/models_proxy/models_proxy"),
                    "--port", str(daemon_port),
                    "--data-dir", basedir,
                    yatest.common.test_source_path('server.cfg'),
                ],
                wait=False
            )
            daemon_url = 'http://localhost:{}'.format(daemon_port)
            if not wait_daemon_start(daemon, daemon_url + '/unistat'):
                daemon.wait(check_exit_code=True)  # in case daemon has failed to start
                assert False  # if something went wrong

            should_validate_exitcode = False
            try:
                # send requests and compare results with canonical
                test_response = begemot_pb2.TBegemotResponse()
                canonical_response = begemot_pb2.TBegemotResponse()
                # use keepalive requests
                s = requests.Session()
                # allow to retry all requests, POSTs are not retriable by default
                retries_policy = retry.Retry(method_whitelist=False)
                s.mount('http://', requests.adapters.HTTPAdapter(max_retries=retries_policy))
                num_requests = 0
                with gzip.open(tests_data_dir + 'requests_responses.tsv.gz', 'rb') as f:
                    for line in f:
                        reqid, request, response = line.rstrip(b'\n').split(b'\t')
                        r = s.post(daemon_url + '/models_proxy', data=base64.b64decode(request), headers={'Accept-Encoding': 'identity'})
                        r.raise_for_status()
                        test_response.ParseFromString(r.content)
                        canonical_response.ParseFromString(base64.b64decode(response))
                        assert check_responses(canonical_response, test_response, sys.stderr, ComparePrecisionContext(["NBg.NProto.TBegemotResponse.SubSourceStats"]))
                        num_requests += 1
                        if num_requests >= max_requests:
                            break

                should_validate_exitcode = True
            finally:
                # shutdown the daemon
                requests.get(daemon_url + '/admin?action=shutdown')
                daemon.wait(check_exit_code=should_validate_exitcode)
                should_validate_mocker = should_validate_exitcode
                if not should_validate_exitcode:
                    shutil.copyfile("fetch-eventlog", yatest.common.output_path('eventlog'))
        finally:
            requests.get('http://localhost:{}/admin?action=shutdown'.format(mocker_port))
            mocker.wait(check_exit_code=should_validate_mocker)


def test_base_config():
    do_test("base", 100500, 100500, False, False, False)


def test_light_fetch_requests():
    do_test("light", 100500, 100500, True, False, False)


def test_splitdocs_requests():
    do_test("splitdocs", 25, 25, False, False, False)


def test_splitdocs_light_fetch_requests():
    do_test("lightsplitdocs", 25, 25, True, False, False)


def test_early_requests():
    do_test("early", 25, 25, True, True, False)


def test_bigrt_caches():
    do_test("base", 100500, 100500, False, False, True)
