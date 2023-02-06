import yatest.common
import yatest.common.network
import requests
import requests.adapters
import urllib3.util.retry as retry
import time
import os
import gzip
import base64
import random
import json
from library.python import resource
from search.begemot.server.proto import begemot_pb2
from quality.relev_tools.bert_models.lib.model_descr_metadata import result_descr_pb2

rule_config = """
ModelsBundlePath: "cfg_models_bundle"
MaxBatchSize: {max_batch_size}
MaxJobSize: {max_job_size}
MaxPreprocessBatchSize: {max_batch_size}
"""

bundle_config = """
Model {
    Path: "pseudo_model.htxt"
    Version: "pseudo_version"
    SignalName: "pseudo_signal"
    Targets {
        Id: "test"
        FactorTargets {
            PredictName: "input_hash"
            TargetSlice: "pseudo_slice"
            TargetIndex: 1234
        }
        EmbeddingId: 4321
    }
}
"""

max_requests = 10


def prepare_data(data_dir, max_batch_size, max_job_size, bundle_config, is_first):
    if is_first:
        os.mkdir(data_dir)
        os.mkdir(data_dir + '/ConfigurableModels')
        os.mkdir(data_dir + '/ConfigurableModels/cfg_models_bundle')
    with open(data_dir + '/ConfigurableModels/configurable_models.cfg', 'w') as f:
        f.write(rule_config.format(max_batch_size=max_batch_size, max_job_size=max_job_size))
    with open(data_dir + '/ConfigurableModels/cfg_models_bundle/bundle.cfg', 'w') as f:
        f.write(bundle_config)
    with open(data_dir + '/ConfigurableModels/cfg_models_bundle/pseudo_model.htxt', 'wb') as f:
        f.write(resource.find('/pseudo_model.htxt'))


def start_daemon(daemon_port, data_dir):
    daemon = yatest.common.execute(
        [
            yatest.common.binary_path("search/daemons/begemot/configurable_models/configurable_models"),
            '--port', str(daemon_port),
            '--data', data_dir,
        ],
        wait=False
    )
    daemon_started = False
    while daemon.running:
        try:
            if requests.get('http://localhost:{}/admin?action=ping'.format(daemon_port)).content == b'pong\n':
                daemon_started = True
                break
        except:
            time.sleep(0.1)
    if not daemon_started:
        daemon.wait(check_exit_code=True)  # let the test machinery print a meaningful message
        assert False
    return daemon


def prepare_requests_session():
    # use keepalive requests
    s = requests.Session()
    # allow to retry all requests, POSTs are not retriable by default
    retries_policy = retry.Retry(method_whitelist=False, backoff_factor=1)
    s.mount('http://', requests.adapters.HTTPAdapter(max_retries=retries_policy))
    return s


def do_test(max_batch_size, max_job_size, is_first):
    data_dir = yatest.common.work_path() + '/data'
    prepare_data(data_dir, max_batch_size, max_job_size, bundle_config, is_first)
    dumpfile = None
    if is_first:
        dumpfile = open('dump.json', 'w')
        dumpfile.write('[')
        dumpfile_empty = True

    results = []
    with yatest.common.network.PortManager() as pm:
        daemon_port = pm.get_port()
        daemon = start_daemon(daemon_port, data_dir)
        should_validate_exitcode = False
        try:
            s = prepare_requests_session()
            response = begemot_pb2.TBegemotResponse()
            embedding = result_descr_pb2.TExportableDocResult()
            num_requests = 0
            with gzip.open('cfg_models_sample.txt.gz', 'rb') as f:
                for line in f:
                    r = s.post('http://localhost:{}/cfg_models'.format(daemon_port), data=base64.b64decode(line.rstrip(b'\n')))
                    r.raise_for_status()
                    response.ParseFromString(r.content)
                    # print("got response: ", response)
                    assert len(response.Web.AppliedModels) == 1
                    assert response.Web.AppliedModels[0].Name == "pseudo_model"
                    assert response.Web.AppliedModels[0].Version == "pseudo_version"
                    assert response.Web.SlicedDataDescription.EmbeddingIds == [4321]
                    assert response.Web.SlicedDataDescription.DebugFeatureNames == ["input_hash"]
                    assert len(response.Web.SlicedDataDescription.Slice2Features) == 1
                    assert response.Web.SlicedDataDescription.Slice2Features[0].SliceName == "pseudo_slice"
                    assert response.Web.SlicedDataDescription.Slice2Features[0].FeaturesIds == [1234]
                    assert response.SuperMindMultiplier == 1
                    response.Web.SlicedCalculatedData.sort(key=lambda x: (x.DocHandle.Route, x.DocHandle.Hash))
                    results.append([])
                    if dumpfile is not None:
                        if dumpfile_empty:
                            dumpfile.write('{\n')
                            dumpfile_empty = False
                        else:
                            dumpfile.write('}, {\n')
                    first_doc = True
                    for doc in response.Web.SlicedCalculatedData:
                        assert len(doc.Embeddings) == 1
                        embedding.ParseFromString(doc.Embeddings[0])
                        assert embedding.ExportIdentifier == "pseudo_model"
                        assert len(embedding.DataParts) == 1
                        assert embedding.DataParts[0].DocumentDataLayerName == "pseudo_node_json"
                        assert embedding.DataParts[0].CompressionType == result_descr_pb2.EmbedCompression_ToOneByteWithMaxCoordRenorm
                        assert embedding.DataParts[0].FloatToOneByteCompressionBias == 0.0
                        assert embedding.DataParts[0].FloatToOneByteCompressionScale == 1.0
                        assert embedding.DataParts[0].FloatToOneByteCompressionResult.startswith(b'\0\xFF')
                        doc_inputs_json = embedding.DataParts[0].FloatToOneByteCompressionResult[2:].rstrip(b'\0')
                        expected_hash = 5381
                        for c in doc_inputs_json:
                            expected_hash = (expected_hash * 33 + c) & 0xFFFF
                        assert len(doc.SliceWithFeatures) == 1
                        assert doc.SliceWithFeatures[0].Features == [expected_hash]
                        assert doc.DebugFeatures == [expected_hash]
                        results[-1].append(expected_hash)
                        if dumpfile is not None:
                            if not first_doc:
                                dumpfile.write(',\n')
                            first_doc = False
                            dumpfile.write('"reqindex={}:doc={}/{}": '.format(num_requests, doc.DocHandle.Hash, doc.DocHandle.Route))
                            json.dump(json.loads(doc_inputs_json), dumpfile, ensure_ascii=False, indent=4, sort_keys=True)
                    num_requests += 1
                    if num_requests == max_requests:
                        break
            should_validate_exitcode = True
        finally:
            requests.get('http://localhost:{}/admin?action=shutdown'.format(daemon_port))
            daemon.wait(check_exit_code=should_validate_exitcode)
    if dumpfile is not None:
        if not dumpfile_empty:
            dumpfile.write('}')
        dumpfile.write(']\n')
        dumpfile.close()
    return results


def test_pseudo_model():
    notbatched = do_test(100500, 100500, True)
    batch_splitting = do_test(40, 100500, False)
    assert notbatched == batch_splitting
    job_splitting = do_test(40, 40, False)
    assert notbatched == job_splitting
    return yatest.common.canonical_file('dump.json')


bundle_cache_config = """
Model {
    Path: "pseudo_model.htxt"
    Version: "pseudo_version"
    SignalName: "pseudo_signal"
    Targets {
        Id: "test"
        FactorTargets {
            PredictName: "input_hash"
            TargetSlice: "pseudo_slice"
            TargetIndex: 1234
        }
    }
    SkipDocIfOmittedInputs: ["QueryBertNormed", "OmniUrl"]
    Cache {
        QueryKeys: ["QueryBertNormed"]
        DocKeys: ["OmniUrl"]
        MaxCacheSize: 300
    }
}
"""


class CacheSignalsKeeper():
    def __init__(self, daemon_port):
        self.stats = [0, 0, 0, 0]
        self.url = 'http://localhost:{}/admin?action=stat'.format(daemon_port)

    def get_since_last_call(self, s):
        r = s.get(self.url)
        r.raise_for_status()
        stats = [0, 0, 0, 0]
        prefix = 'WORKER-ConfigurableModels-EVENT-pseudo_signal_cachehit'
        # print(r.content)
        for stat_item in json.loads(r.content):
            if stat_item[0] == prefix + '_summ':
                stats[0] = stat_item[1]
            elif stat_item[0] == prefix + '-denom_summ':
                stats[1] = stat_item[1]
            elif stat_item[0] == prefix + '-full_summ':
                stats[2] = stat_item[1]
            elif stat_item[0] == prefix + '-full-denom_summ':
                stats[3] = stat_item[1]
        delta = [stats[i] - self.stats[i] for i in range(4)]
        self.stats = stats
        return delta


def test_cache_in_pseudo_model():
    data_dir = yatest.common.work_path('withcache')
    os.mkdir(data_dir)
    data_dir += '/data'
    prepare_data(data_dir, 40, 40, bundle_cache_config, True)
    with yatest.common.network.PortManager() as pm:
        daemon_port = pm.get_port()
        daemon = start_daemon(daemon_port, data_dir)
        should_validate_exitcode = False
        try:
            s = prepare_requests_session()
            request = begemot_pb2.TBegemotRequest()
            request2 = begemot_pb2.TBegemotRequest()
            response = begemot_pb2.TBegemotResponse()
            url = 'http://localhost:{}/cfg_models'.format(daemon_port)
            num_requests = 0
            random.seed(1234)
            signals_keeper = CacheSignalsKeeper(daemon_port)
            with gzip.open('cfg_models_sample.txt.gz', 'rb') as f:
                for line in f:
                    # first request: get "true" values with disabled cache
                    request.ParseFromString(base64.b64decode(line.rstrip(b'\n')))
                    request.Web.ModelsToUse[:] = ["pseudo_model:test"]  # drop "debug"
                    request.Web.NoCacheRead = True
                    request.Web.NoCacheWrite = True
                    r = s.post(url, data=request.SerializeToString())
                    r.raise_for_status()
                    response.ParseFromString(r.content)
                    assert len(response.Web.SlicedDataDescription.Slice2Features) == 1
                    assert response.Web.SlicedDataDescription.Slice2Features[0].SliceName == "pseudo_slice"
                    assert response.Web.SlicedDataDescription.Slice2Features[0].FeaturesIds == [1234]
                    assert len(response.Web.SlicedCalculatedData) == len(request.Web.FetchedDocData.DocData)
                    canonical_result = {}
                    for x in response.Web.SlicedCalculatedData:
                        assert len(x.SliceWithFeatures) == 1
                        assert len(x.SliceWithFeatures[0].Features) == 1
                        canonical_result[(x.DocHandle.Route, x.DocHandle.Hash)] = x.SliceWithFeatures[0].Features[0]
                    assert signals_keeper.get_since_last_call(s) == [0, 0, 0, 0]  # nocache requests should not influence cachehit signals
                    # second and third request: replace a random half of documents with empty docs
                    # fourth and fifth request: ask the full request again
                    should_be_cached = set()
                    request.Web.NoCacheRead = False
                    request.Web.NoCacheWrite = False
                    for idx in range(4):
                        # generate request
                        skipped_docs = set()
                        if idx < 2:
                            expected_cached_docs = 0
                            request2.CopyFrom(request)
                            for i in range(len(request2.Web.FetchedDocData.DocData)):
                                if random.getrandbits(1):
                                    request2.Web.FetchedDocData.DocData[i].Clear()
                                    doc = (-2, random.getrandbits(64))
                                    assert doc not in skipped_docs
                                    skipped_docs.add(doc)
                                    request2.Web.FetchedDocData.DocData[i].DocHandle.Route = doc[0]
                                    request2.Web.FetchedDocData.DocData[i].DocHandle.Hash = doc[1]
                                elif i in should_be_cached:
                                    expected_cached_docs += 1
                                else:
                                    should_be_cached.add(i)
                            r = s.post(url, data=request2.SerializeToString())
                        else:
                            expected_cached_docs = len(should_be_cached)
                            should_be_cached = set(range(len(request2.Web.FetchedDocData.DocData)))
                            r = s.post(url, data=request.SerializeToString())
                        # validate response
                        r.raise_for_status()
                        response.ParseFromString(r.content)
                        # print(idx, response)
                        assert response.Web.FailedDocuments == 0
                        assert response.Web.SkippedDocuments == len(skipped_docs)
                        assert len(response.Web.SlicedDataDescription.Slice2Features) == 1
                        assert response.Web.SlicedDataDescription.Slice2Features[0].SliceName == "pseudo_slice"
                        assert response.Web.SlicedDataDescription.Slice2Features[0].FeaturesIds == [1234]
                        num_validated = 0
                        for x in response.Web.SlicedCalculatedData:
                            assert len(x.SliceWithFeatures) == 1
                            assert len(x.SliceWithFeatures[0].Features) == 1
                            doc = (x.DocHandle.Route, x.DocHandle.Hash)
                            if doc in skipped_docs:
                                assert x.SliceWithFeatures[0].Features[0] == 0
                            else:
                                assert doc in canonical_result
                                assert x.SliceWithFeatures[0].Features[0] == canonical_result[doc]
                                num_validated += 1
                        assert num_validated == len(request.Web.FetchedDocData.DocData) - len(skipped_docs)
                        # validate cachehit signals
                        cachehit, cachehit_denom, cachehit_full, cachehit_full_denom = signals_keeper.get_since_last_call(s)
                        if num_validated == 0:
                            # no cachehit signals if everything is skipped
                            assert cachehit == cachehit_denom == cachehit_full == cachehit_full_denom == 0
                        else:
                            assert cachehit_denom == num_validated
                            assert cachehit == expected_cached_docs
                            assert cachehit_full_denom == 1
                            assert cachehit_full == (cachehit == cachehit_denom)
                    num_requests += 1
                    if num_requests == max_requests:
                        break
            should_validate_exitcode = True
        finally:
            requests.get('http://localhost:{}/admin?action=shutdown'.format(daemon_port))
            daemon.wait(check_exit_code=should_validate_exitcode)


bundle_cache_degrade_config = """
Model {
    Path: "pseudo_model.htxt"
    Version: "pseudo_version"
    SignalName: "pseudo_signal"
    Targets {
        Id: "test"
        FactorTargets {
            PredictName: "input_hash"
            TargetSlice: "pseudo_slice"
            TargetIndex: 1234
        }
    }
    SkipDocIfOmittedInputs: ["QueryBertNormed", "OmniUrl"]
    Cache {
        QueryKeys: ["QueryBertNormed"]
        DocKeys: ["OmniUrl"]
        MaxCacheSize: 300
    }
    Degrade {
        Gpu {
            StartDegradationF: 0.1
            EndDegradationF: 0.5
            MultLowerBound: 0.1
            AutoModeType: "exp"
        }
    }
}
"""


def test_cache_degrade_in_pseudo_model():
    data_dir = yatest.common.work_path('withcache_degrade')
    os.mkdir(data_dir)
    data_dir += '/data'
    prepare_data(data_dir, 40, 40, bundle_cache_degrade_config, True)
    with yatest.common.network.PortManager() as pm:
        daemon_port = pm.get_port()
        daemon = start_daemon(daemon_port, data_dir)
        should_validate_exitcode = False
        try:
            s = prepare_requests_session()
            request = begemot_pb2.TBegemotRequest()
            response = begemot_pb2.TBegemotResponse()
            url = 'http://localhost:{}/cfg_models'.format(daemon_port)
            with gzip.open('cfg_models_sample.txt.gz', 'rb') as f:
                request.ParseFromString(base64.b64decode(f.readline().rstrip(b'\n')))
            request.Web.ModelsToUse[:] = ["pseudo_model:test"]  # drop "debug"
            request.Web.NoCacheRead = False
            request.Web.NoCacheWrite = False
            # initialization: cache results for degraded request
            request.Web.DegradeOverride = 0.5
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.CachedDocuments == response.Web.SkippedDocuments == 0
            # check 1: cache works for same degrade level
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == response.Web.CachedDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.SkippedDocuments == 0
            # check 2: cache works for greater degrade level
            request.Web.DegradeOverride = 0.6
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == response.Web.CachedDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.SkippedDocuments == 0
            # check 3: cache is disabled for lesser degrade level
            request.Web.DegradeOverride = 0
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.CachedDocuments == response.Web.SkippedDocuments == 0
            # check 4: ...but cache is properly updated with lesser degrade level
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == response.Web.CachedDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.SkippedDocuments == 0
            should_validate_exitcode = True
        finally:
            requests.get('http://localhost:{}/admin?action=shutdown'.format(daemon_port))
            daemon.wait(check_exit_code=should_validate_exitcode)


def test_degrade_override_in_pseudo_model():
    # this time, do not enable degradation in config but still ask for degraded results
    # with this config, the daemon is not prepared to cache degraded results,
    # but can still load a normal cache for consistency with the previous case
    # (can always be disabled with nocache=da)
    data_dir = yatest.common.work_path('withcache_degrade_noconfig')
    os.mkdir(data_dir)
    data_dir += '/data'
    prepare_data(data_dir, 40, 40, bundle_cache_config, True)
    with yatest.common.network.PortManager() as pm:
        daemon_port = pm.get_port()
        daemon = start_daemon(daemon_port, data_dir)
        should_validate_exitcode = False
        try:
            s = prepare_requests_session()
            request = begemot_pb2.TBegemotRequest()
            response = begemot_pb2.TBegemotResponse()
            url = 'http://localhost:{}/cfg_models'.format(daemon_port)
            with gzip.open('cfg_models_sample.txt.gz', 'rb') as f:
                request.ParseFromString(base64.b64decode(f.readline().rstrip(b'\n')))
            request.Web.ModelsToUse[:] = ["pseudo_model:test"]  # drop "debug"
            request.Web.NoCacheRead = False
            request.Web.NoCacheWrite = False
            # initialization: ask degraded request
            request.Web.DegradeOverride = 0.5
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.CachedDocuments == response.Web.SkippedDocuments == 0
            # check 1: results from degraded request cannot be cached
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.CachedDocuments == response.Web.SkippedDocuments == 0
            # ask normal request
            request.Web.DegradeOverride = 0
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.CachedDocuments == response.Web.SkippedDocuments == 0
            # check 2: results from degraded request can be loaded from normal cache
            request.Web.DegradeOverride = 0.5
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert response.Web.NumDocuments == response.Web.CachedDocuments == len(request.Web.FetchedDocData.DocData)
            assert response.Web.FailedDocuments == response.Web.SkippedDocuments == 0
            should_validate_exitcode = True
        finally:
            requests.get('http://localhost:{}/admin?action=shutdown'.format(daemon_port))
            daemon.wait(check_exit_code=should_validate_exitcode)


def fill_fake_request(request):
    request.Web.UserRequest = 'fake user request'
    doc = request.Web.FetchedDocData.DocData.add()
    doc.DocHandle.Hash = 1234
    doc.DocHandle.Route = -2
    doc.OmniUrl = 'https://example.com/url'
    doc.OmniTitle = 'example title'


def test_nonexisting_model():
    data_dir = yatest.common.work_path('nonexisting_model')
    os.mkdir(data_dir)
    data_dir += '/data'
    prepare_data(data_dir, 40, 40, bundle_config, True)
    with yatest.common.network.PortManager() as pm:
        daemon_port = pm.get_port()
        daemon = start_daemon(daemon_port, data_dir)
        should_validate_exitcode = False
        try:
            s = prepare_requests_session()
            url = 'http://localhost:{}/cfg_models'.format(daemon_port)
            request = begemot_pb2.TBegemotRequest()
            response = begemot_pb2.TBegemotResponse()
            fill_fake_request(request)
            request.Web.ReqId = 'fake-reqid1'
            request.Web.ModelsToUse[:] = ['nonexisting_model:test']
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert any('nonexisting_model' in x for x in response.Web.DebugMessages)
            assert response.Web.SlicedDataDescription == begemot_pb2.TWebResponse.TSlicedDataDescription()

            request.Web.ReqId = 'fake-reqid2'
            request.Web.ModelsToUse[:] = ['nonexisting_model:-test']
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            assert len(response.Web.DebugMessages) == 0
            assert response.Web.SlicedDataDescription == begemot_pb2.TWebResponse.TSlicedDataDescription()
            should_validate_exitcode = True
        finally:
            requests.get('http://localhost:{}/admin?action=shutdown'.format(daemon_port))
            daemon.wait(check_exit_code=should_validate_exitcode)


def test_multiple_names_same_model():
    data_dir = yatest.common.work_path('multiple_names_same_model')
    os.mkdir(data_dir)
    data_dir += '/data'
    prepare_data(data_dir, 40, 40, bundle_cache_config, True)
    with yatest.common.network.PortManager() as pm:
        daemon_port = pm.get_port()
        daemon = start_daemon(daemon_port, data_dir)
        should_validate_exitcode = False
        try:
            s = prepare_requests_session()
            url = 'http://localhost:{}/cfg_models'.format(daemon_port)
            request = begemot_pb2.TBegemotRequest()
            response = begemot_pb2.TBegemotResponse()
            fill_fake_request(request)
            request.Web.ReqId = 'fake-reqid1'
            request.Web.ModelsToUse[:] = ['pseudo_model:test', 'pseudo_signal:test']
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response.ParseFromString(r.content)
            # model is calculated twice
            assert len(response.Web.SlicedDataDescription.Slice2Features) == 2
            assert response.Web.SlicedDataDescription.Slice2Features[1] == response.Web.SlicedDataDescription.Slice2Features[0]
            assert len(response.Web.SlicedCalculatedData) == 1
            assert len(response.Web.SlicedCalculatedData[0].SliceWithFeatures) == 2
            assert response.Web.SlicedCalculatedData[0].SliceWithFeatures[0] == response.Web.SlicedCalculatedData[0].SliceWithFeatures[1]
            # and the cache should work for both names
            request.Web.ReqId = 'fake-reqid2'
            r = s.post(url, data=request.SerializeToString())
            r.raise_for_status()
            response2 = begemot_pb2.TBegemotResponse()
            response2.ParseFromString(r.content)
            assert response2.Web.CachedDocuments == response2.Web.NumDocuments == 2  # one document calculated twice
            response2.Web.CachedDocuments = 0
            assert response == response2
            should_validate_exitcode = True
        finally:
            requests.get('http://localhost:{}/admin?action=shutdown'.format(daemon_port))
            daemon.wait(check_exit_code=should_validate_exitcode)
