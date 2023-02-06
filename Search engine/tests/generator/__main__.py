import os
import sys
import requests
import argparse
import subprocess
import random
import socket
import time
import base64
import logging
import retry
from search.idl import meta_pb2
from search.begemot.server.proto import begemot_pb2
from quality.personalization.big_rt.dynamic_table_http_proxy.proto import request_pb2 as dyntable_pb2
from quality.personalization.big_rt.rapid_clicks_common.proto import states_pb2 as rapidclicks_pb2
from search.daemons.models_proxy.tests.generator import urlid
import search.tools.devops.libs.nanny_services as nanny_services
from library.python import resource

_NANNY_HAMSTER_MMETA_WEB = "vla_jupiter_mmeta_hamster_yp"


@retry.retry(tries=3, delay=2)
def get_nanny_instances(service, need_container_hostname=True):
    instances = nanny_services.get_current_instances(service, requests.Session())
    instances_host_port = []
    for instance in instances:
        hostname_field = 'container_hostname' if need_container_hostname else 'hostname'
        instances_host_port.append({
            'hostname': instance[hostname_field],
            'port': instance['port']
        })
    random.shuffle(instances)
    print("trying to fetch mmeta config from one of:")
    for x in instances_host_port:
        print(x)
    return instances_host_port


def choose_alive_instance():
    instances = get_nanny_instances(_NANNY_HAMSTER_MMETA_WEB)
    for i in range(0, min(3, len(instances))):
        get_config_url = None
        try:
            random.shuffle(instances)
            target = 'http://{}:{}/yandsearch'.format(instances[0]['hostname'], instances[0]['port'])
            get_config_url = target + '?info=getconfig'
            print("get_config_url:", get_config_url)
            response = requests.get(get_config_url)
            return target, response.content
        except Exception as exc:
            logging.exception("Cannot get config from %s: %s", get_config_url, exc)
            del instances[0]
            continue
    raise Exception("Unable to get hamster cfg")


def fetch_mmeta_queries():
    # get mmeta requests from sandbox
    oauth_token = os.getenv('OAUTH_TOKEN')
    if not oauth_token:
        print('no OAUTH_TOKEN in environment', file=sys.stderr)
        exit(1)
    r = requests.get('https://sandbox.yandex-team.ru/api/v1.0/resource', params={
        'type': 'WEB_MIDDLESEARCH_PLAIN_TEXT_QUERIES',
        'state': 'READY',
        'attrs': '{"TE_web_production_mmeta_reqs":null}',
        'limit': 1,
    }, headers={'Authorization': 'OAuth ' + oauth_token})
    r.raise_for_status()
    resource = r.json()['items'][0]
    print("using resource id={} aka {}, filename={}, rbtorrent={}".format(resource['id'], resource['description'], resource['file_name'], resource['skynet_id']))
    subprocess.run(["sky", "get", "-uwp", resource['skynet_id']], check=True)
    return resource['file_name']


def start_models_proxy(models_proxy_binary, port, mmeta_config, use_bigrt_caches):
    os.makedirs('models_proxy_dir/ModelsProxy', exist_ok=True)
    os.makedirs('models_proxy_dir/FetchDocData', exist_ok=True)
    for name in ('/server.cfg', '/ModelsProxy/models_proxy.cfg', '/FetchDocData/fetch_doc_data.cfg'):
        with open('models_proxy_dir' + name, 'wb') as f:
            f.write(resource.find(name))
    if use_bigrt_caches:
        with open('models_proxy_dir/ModelsProxy/models_proxy.cfg', 'ab') as f:
            f.write(b"\nBigRtQueryCacheConfig { MaxCacheSize: 4000000 }\nBigRtHostCacheConfig { MaxCacheSize: 640000 }\nBigRtUrlCacheConfig { MaxCacheSize: 1500000 }\n")
    try:
        os.remove('models_proxy_dir/FetchDocData/main.cfg')
    except FileNotFoundError:
        pass
    os.symlink(os.path.abspath(mmeta_config), 'models_proxy_dir/FetchDocData/main.cfg')
    try:
        os.remove('models_proxy_dir/fetch-eventlog')
    except FileNotFoundError:
        pass
    p = subprocess.Popen([
        os.path.abspath(models_proxy_binary),
        '--port', str(port),
        '--data-dir', 'models_proxy_dir',
        'models_proxy_dir/server.cfg'])
    # wait a bit for the daemon to initialize
    while p.poll() is None:
        try:
            requests.get('http://localhost:{}/unistat'.format(port))
            return p
        except:
            time.sleep(0.1)
    assert False


def make_reqid_unique(query, seen_reqids):
    pos = query.find('&reqid=')
    assert pos != -1
    pos += len('&reqid=')
    pos2 = query.find('&', pos)
    if pos2 == -1:
        pos2 = len(query)
    reqid = query[pos:pos2]
    suffix = ''
    while (reqid + suffix) in seen_reqids:
        suffix = '-uniq{}'.format(random.randrange(1000000))
    if suffix:
        reqid += suffix
        query = query[:pos] + reqid + query[pos2:]
    seen_reqids.add(reqid)
    return query


def fire_queries(mmeta_instance, queries_file, num_queries, models_proxy_port, use_bigrt_caches, self_hostname):
    seen_reqids = set()
    with open(queries_file, 'r') as f:
        s = requests.Session()
        for i in range(num_queries):
            query = f.readline()
            if not query:
                break
            query = query.rstrip('\r\n')
            # make sure reqid is unique, priemka queries contain many reasks
            query = make_reqid_unique(query, seen_reqids)
            query += '&pron=dont_need_snippets'
            query += '&metahost2=MODELS_PROXY:{}:{}'.format(self_hostname, models_proxy_port)
            query += '&rearr=models_proxy_othercache_passthrough=LogSubSourceRequestBody=1'
            query += '&rearr=models_proxy_othercache_passthrough=LogSubSourceResponse=1'
            query += '&rearr=models_proxy_othercache_passthrough=LogSelfRequestBody=1'
            query += '&rearr=models_proxy_othercache_passthrough=LogSelfResponse=1'
            query += '&reqinfo=models_proxy_tests_generator'
            if use_bigrt_caches:
                # we need to be stable, because otherwise cache tests will fail
                # webfresh documents tend to have unstable FetchDocData
                query += '&rearr=no_samohod_on_middle&rearr=no_webfresh_on_middle'
            try:
                s.get(mmeta_instance + query)  # ignore the response, we only care about models_proxy eventlog
            except:
                pass
            sys.stdout.write('.')
            sys.stdout.flush()
    print()


def doc_data_compatible(doc1, doc2):
    # sanity checks, fetch_doc_data returns only DocId, ArchiveInfo and BinaryData.DocSplitBertEmbedding
    for field, _ in doc1.ListFields():
        assert field.name in ('DocId', 'ArchiveInfo', 'BinaryData'), "unknown field in " + str(doc1)
    for field, _ in doc2.ListFields():
        assert field.name in ('DocId', 'ArchiveInfo', 'BinaryData'), "unknown field in " + str(doc2)
    assert len(doc1.ArchiveInfo.Passage) == len(doc2.ArchiveInfo.Passage) == 0
    assert len(doc1.ArchiveInfo.PassageAttr) == len(doc2.ArchiveInfo.PassageAttr) == 0
    assert len(doc1.ArchiveInfo.FloatRelatedAttribute) == len(doc2.ArchiveInfo.FloatRelatedAttribute) == 0
    if doc1.DocId != doc2.DocId:
        return False
    # in ArchiveInfo, only GtaRelatedAttribute can differ, bert requests have an extra attribute meta_descr
    if (doc1.ArchiveInfo.Title != doc2.ArchiveInfo.Title
            or doc1.ArchiveInfo.Headline != doc2.ArchiveInfo.Headline
            or doc1.ArchiveInfo.IndexGeneration != doc2.ArchiveInfo.IndexGeneration
            or doc1.ArchiveInfo.SourceTimestamp != doc2.ArchiveInfo.SourceTimestamp
            or doc1.ArchiveInfo.Url != doc2.ArchiveInfo.Url
            or doc1.ArchiveInfo.Size != doc2.ArchiveInfo.Size
            or doc1.ArchiveInfo.Charset != doc2.ArchiveInfo.Charset
            or doc1.ArchiveInfo.Mtime != doc2.ArchiveInfo.Mtime):
        return False
    doc1gta = {gta.Key : gta.Value for gta in doc1.ArchiveInfo.GtaRelatedAttribute}
    assert len(doc1gta) == len(doc1.ArchiveInfo.GtaRelatedAttribute)
    for gta in doc2.ArchiveInfo.GtaRelatedAttribute:
        old = doc1gta.get(gta.Key)
        if old is not None and old != gta.Value:
            return False
    if doc1.HasField('BinaryData') and doc2.HasField('BinaryData') and doc1.BinaryData != doc2.BinaryData:
        return False
    return True


# doc1 = doc1 + doc2, called only if doc_data_compatible(doc1, doc2) returned True
def merge_doc_data(doc1, doc2):
    doc1gta = {gta.Key : gta.Value for gta in doc1.ArchiveInfo.GtaRelatedAttribute}
    for gta in doc2.ArchiveInfo.GtaRelatedAttribute:
        if gta.Key not in doc1gta:
            doc1.ArchiveInfo.GtaRelatedAttribute.extend([gta])
    if not doc1.HasField('BinaryData') and doc2.HasField('BinaryData'):
        doc1.BinaryData.CopyFrom(doc2.BinaryData)


# given full FetchDocData response, figure out what a light request would return
def drop_heavy_fetch_data(doc):
    lightdoc = meta_pb2.TDocument()
    lightdoc.DocId = doc.DocId
    lightdoc.ArchiveInfo.CopyFrom(doc.ArchiveInfo)
    del lightdoc.ArchiveInfo.GtaRelatedAttribute[:]
    for gta in doc.ArchiveInfo.GtaRelatedAttribute:
        if gta.Key == b'ya_music_for_alice_artist_name' or gta.Key == b'ya_music_for_alice_track_title':
            lightdoc.ArchiveInfo.GtaRelatedAttribute.append(gta)
    return lightdoc


fetch_doc_data_subsources = set(['WEB', 'PLATINUM', 'WEBFRESH_ON_MIDDLE', 'QUICK_SAMOHOD_ON_MIDDLE', 'QUICK_SAMOHOD_ON_MIDDLE_EXP', 'QUICK_SAMOHOD_ON_MIDDLE_EXP2'])
model_subsources = set(['RTMODELS_FEATURES_PERSONAL_MODEL', 'RTMODELS', 'RTMODELS_PERSONAL_CACHED', 'CFG_MODELS_SPLIT', 'CFG_MODELS_USERBODY', 'DYNTABLE_HTTP_PROXY_SOURCE', 'CFG_MODELS'])


def process_eventlog(evlogdump, eventlog_filename, max_queries):
    print("processing eventlog:", eventlog_filename)
    tasks_mapping = {}
    fetched_doc_data = {}
    p = subprocess.Popen([
        evlogdump,
        '-i', 'ContextCreated,Base64PostBody,SubSourceInit,SubSourceRequest,SubSourceResponseDump,SubSourceOk,DebugMessage',
        eventlog_filename],
        stdout=subprocess.PIPE,
        encoding='utf-8')
    last_requests = []
    report = meta_pb2.TReport()
    curframe = None
    reqid = None
    models_proxy_request_string = None
    models_proxy_request = begemot_pb2.TBegemotRequest()
    notfetched = set()
    requests = {}
    responses = {}
    bad_frame = False
    subsources_file = open('subsources.tskv', 'w')
    requests_file = open('requests_responses.tsv', 'w')
    numreqs = 0
    dyntable_task2key = {}
    dyntable_request = dyntable_pb2.TUserDataRequest()
    dyntable_keys = rapidclicks_pb2.TKeys()
    pending_dyntable_response = None
    for line in p.stdout:
        if not line:
            break
        if numreqs >= max_queries:
            continue
        event = line.rstrip('\n').split('\t')
        if event[2] == 'ContextCreated':
            assert curframe is None
            curframe = event[1]
            reqid = event[4]
            models_proxy_request_string = None
            notfetched = set()
            requests = {}
            responses = {}
            bad_frame = False
            dyntable_task2key = {}
            pending_dyntable_response = None
            tasks_mapping = {}
        elif event[2] == 'Base64PostBody':
            assert curframe == event[1]
            assert models_proxy_request_string is None
            models_proxy_request_string = event[3]
            models_proxy_request.ParseFromString(base64.b64decode(models_proxy_request_string))
            assert reqid == models_proxy_request.Web.ReqId
            reqid += ['', ':ytier', ':itditp'][models_proxy_request.ModelsProxyRequest.Tier]
            models_proxy_request.Web.ReqId = reqid
        elif event[2] == 'SubSourceInit':
            assert curframe == event[1]
            taskNum = int(event[5])
            clientName = event[6]
            assert clientName in fetch_doc_data_subsources or clientName in model_subsources, ("unknown clientName: %s" %clientName)
            assert taskNum not in tasks_mapping
            tasks_mapping[taskNum] = clientName
        elif event[2] == 'SubSourceRequest':
            assert curframe == event[1]
            clientNum = int(event[3])
            taskNum = int(event[8])
            url = event[7]
            body = event[9]
            if tasks_mapping[taskNum] in fetch_doc_data_subsources:
                assert not body
                assert '&fetch_doc_data=da' in url
                if len(last_requests) <= clientNum:
                    last_requests += [None] * (clientNum + 1 - len(last_requests))
                last_requests[clientNum] = url
            else:
                subsrc = tasks_mapping[taskNum]
                if subsrc == 'DYNTABLE_HTTP_PROXY_SOURCE':
                    dyntable_request.ParseFromString(base64.b64decode(body))
                    assert len(dyntable_request.Keys) >= 1
                    dyntable_keys.ParseFromString(dyntable_request.Keys[0])
                    assert len(dyntable_keys.Keys) == 1
                    reqtype = dyntable_keys.Keys[0].KeyType
                    for i in range(1, len(dyntable_request.Keys)):
                        dyntable_keys.ParseFromString(dyntable_request.Keys[i])
                        assert len(dyntable_keys.Keys) == 1
                        assert reqtype == dyntable_keys.Keys[0].KeyType
                    subsrc += ':{}'.format(reqtype)
                    task = int(event[8])
                    assert task not in dyntable_task2key or dyntable_task2key[task] == reqtype
                    dyntable_task2key[task] = reqtype
                assert subsrc not in requests or requests[subsrc] == body
                requests[subsrc] = body
        elif event[2] == 'SubSourceResponseDump':
            clientNum = int(event[3])
            clientDescr = event[4]
            content = event[5]
            assert pending_dyntable_response is None
            if clientDescr in fetch_doc_data_subsources:
                assert last_requests[clientNum] is not None
                if 'bert_embedding' in last_requests[clientNum]:
                    is_v1_embedding = '&pron=fetch_doc_data_opts%3Dbert_embedding&' in last_requests[clientNum]
                    is_v2_embedding = '&pron=fetch_doc_data_opts%3Dbert_embedding_v2&' in last_requests[clientNum]
                    assert is_v1_embedding + is_v2_embedding == 1
                else:
                    is_v1_embedding = is_v2_embedding = False
                report.ParseFromString(base64.b64decode(content))
                assert len(report.Grouping) == 1
                assert report.Grouping[0].IsFlat == meta_pb2.TGrouping.TIsFlat.YES
                for group in report.Grouping[0].Group:
                    assert len(group.Document) == 1
                    doc = group.Document[0]
                    if doc.HasField("DocHash"):
                        assert not doc.HasField("DocId")
                        doc.DocId = urlid.BinaryDocIdToString(doc.DocHash, doc.Route).encode('ascii')
                        doc.ClearField("DocHash")
                        doc.ClearField("Route")
                    if not doc.ArchiveInfo.Url:
                        # empty response instead of information, often happens in WEBFRESH_ON_MIDDLE
                        # models_proxy treats this as "not fetched", follow the lead
                        docid = doc.DocId.decode('ascii')
                        assert (clientNum, docid) not in notfetched
                        notfetched.add((clientNum, docid))
                        continue
                    subsrc_docid = doc.DocId.decode('ascii')
                    docid = '{}-{}'.format(clientNum, subsrc_docid)
                    pos = last_requests[clientNum].find('&dh=' + subsrc_docid)
                    assert pos != -1
                    pos += len('&dh=') + len(subsrc_docid)
                    if pos < len(last_requests[clientNum]) and last_requests[clientNum][pos] == ':':
                        light_fetch_data = drop_heavy_fetch_data(doc)
                        if docid in fetched_doc_data:
                            if doc_data_compatible(fetched_doc_data[docid], light_fetch_data):
                                merge_doc_data(fetched_doc_data[docid], light_fetch_data)
                            else:
                                print('note: dropping reqid {} due to incompatible light FetchDocData results for docid {}'.format(reqid, docid))
                                print('[old doc]\n{}[new doc]\n{}'.format(fetched_doc_data[docid], light_fetch_data))
                                bad_frame = True
                                continue
                        else:
                            fetched_doc_data[docid] = light_fetch_data
                        pos2 = last_requests[clientNum].find('&', pos)
                        if pos2 == -1:
                            pos2 = len(last_requests[clientNum])
                        docid += last_requests[clientNum][pos:pos2]
                    if is_v1_embedding:
                        docid = docid + ':v1'
                    if docid in fetched_doc_data:
                        if doc_data_compatible(fetched_doc_data[docid], doc):
                            merge_doc_data(fetched_doc_data[docid], doc)
                        else:
                            print('note: dropping reqid {} due to incompatible FetchDocData results for docid {}'.format(reqid, docid))
                            print('[old doc]\n{}[new doc]\n{}'.format(fetched_doc_data[docid], doc))
                            bad_frame = True
                    else:
                        fetched_doc_data[docid] = doc
            elif clientDescr == 'DYNTABLE_HTTP_PROXY_SOURCE':
                pending_dyntable_response = content
            else:
                assert clientDescr not in responses
                responses[clientDescr] = content
        elif event[2] == 'SubSourceOk':
            assert curframe == event[1]
            if pending_dyntable_response is not None:
                taskNum = int(event[9])
                clientDescr = tasks_mapping[taskNum]
                assert clientDescr == 'DYNTABLE_HTTP_PROXY_SOURCE'
                clientDescr += ':{}'.format(dyntable_task2key[taskNum])
                assert clientDescr not in responses
                responses[clientDescr] = content
                pending_dyntable_response = None
        elif event[2] == 'DebugMessage' and event[3].startswith('done: '):
            assert curframe == event[1]
            curframe = None
            if bad_frame:
                continue
            assert pending_dyntable_response is None
            assert models_proxy_request_string is not None
            models_proxy_response = event[3][len('done: '):]
            requested_sources = set(requests.keys())
            responded_sources = set(responses.keys())
            assert len(requested_sources) == len(requests.keys())
            assert len(responded_sources) == len(responses.keys())
            if requested_sources != responded_sources:
                assert responded_sources - requested_sources == set()
                print("model unanswers: {}, skipping frame".format(';'.join(requested_sources - responded_sources)))
                continue
            if notfetched:
                # pretend that not-fetched documents never were in the request
                removed = set()
                for subsrc in models_proxy_request.Web.SubSources:
                    filtered_docs = []
                    filtered_owners = []
                    filtered_relevsents = []
                    for i, doc in enumerate(subsrc.DocHandles):
                        docid = urlid.BinaryDocIdToString(doc.Hash, doc.Route)
                        if (subsrc.SourceId, docid) in notfetched:
                            assert (subsrc.SourceId, docid) not in removed
                            removed.add((subsrc.SourceId, docid))
                        else:
                            filtered_docs.append(doc)
                            if len(subsrc.DocOwners) > i:
                                filtered_owners.append(subsrc.DocOwners[i])
                            if len(subsrc.RelevantSentences) > i:
                                filtered_relevsents.append(subsrc.RelevantSentences[i])
                    if len(subsrc.DocHandles) != len(filtered_docs):
                        del subsrc.DocHandles[:]
                        subsrc.DocHandles.extend(filtered_docs)
                        if subsrc.DocOwners:
                            del subsrc.DocOwners[:]
                            subsrc.DocOwners.extend(filtered_owners)
                        if subsrc.RelevantSentences:
                            del subsrc.RelevantSentences[:]
                            subsrc.RelevantSentences.extend(filtered_relevsents)
                assert len(removed) == len(notfetched)
            if notfetched or models_proxy_request.ModelsProxyRequest.Tier:
                models_proxy_request_string = base64.b64encode(models_proxy_request.SerializeToString()).decode('ascii')
            requests_file.write(reqid)
            requests_file.write('\t')
            requests_file.write(models_proxy_request_string)
            requests_file.write('\t')
            requests_file.write(models_proxy_response)
            requests_file.write('\n')
            subsources_file.write('reqid=')
            subsources_file.write(reqid)
            for k in sorted(requests.keys()):
                subsources_file.write('\t{}:request='.format(k))
                subsources_file.write(requests[k])
                subsources_file.write('\t{}:response='.format(k))
                subsources_file.write(responses[k])
            subsources_file.write('\n')
            numreqs += 1
    requests_file.close()
    subsources_file.close()
    print('written {} requests into requests_responses.tsv + subsources.tskv'.format(numreqs))
    with open('fetched_doc_data.tsv', 'w') as f:
        for docid, doc in fetched_doc_data.items():
            serialized = base64.b64encode(doc.SerializeToString()).decode('ascii')
            f.write('{}\t{}\n'.format(docid, serialized))
    print('written {} docdatas into fetched_doc_data.tsv'.format(len(fetched_doc_data)))
    exitcode = p.wait()
    assert exitcode == 0
    assert numreqs >= 0.8 * max_queries


def main():
    parser = argparse.ArgumentParser(description='Collects requests&responses for models_proxy tests from mmeta queries')
    parser.add_argument('-m', '--models-proxy', required=True, help='compiled models_proxy binary')
    parser.add_argument('-e', '--evlogdump', required=True, help='compiled evlogdump binary')
    parser.add_argument('-p', '--port', type=int, default=8371, help='port to run models_proxy on')
    parser.add_argument('-n', '--num-queries', type=int, default=1000, help='limit number of queries')
    parser.add_argument('-c', '--mmeta-config', help='mmeta config, default=load from hamster')
    parser.add_argument('-q', '--queries-file', help='mmeta queries, default=locate&load from sandbox')
    parser.add_argument('-i', '--mmeta-instance', help='mmeta instance, default=random from hamster')
    parser.add_argument('--just-process-eventlog', help='generate test data from existing models_proxy eventlog')
    parser.add_argument('--with-bigrt-caches', help='enables bigrt caches', action='store_true')
    parser.add_argument('--hostname', help='set hostname explicitly', default=socket.gethostname())
    args = parser.parse_args()

    if args.just_process_eventlog:
        process_eventlog(args.evlogdump, args.just_process_eventlog, args.num_queries)
        return

    mmeta_instance = args.mmeta_instance
    fetched_config = None
    if not mmeta_instance:
        mmeta_instance, fetched_config = choose_alive_instance()

    mmeta_config = args.mmeta_config
    if not mmeta_config:
        if not fetched_config:
            fetched_config = requests.get(mmeta_instance + '?info=getconfig').content
        mmeta_config = 'mmeta.cfg'
        with open(mmeta_config, 'wb') as f:
            f.write(fetched_config)
        print("fetched mmeta config into", mmeta_config)

    queries_file = args.queries_file
    if not queries_file:
        queries_file = fetch_mmeta_queries()

    daemon = start_models_proxy(args.models_proxy, args.port, mmeta_config, args.with_bigrt_caches)

    fire_queries(mmeta_instance, queries_file, args.num_queries, args.port, args.with_bigrt_caches, args.hostname)

    requests.get('http://localhost:{}/admin?action=shutdown'.format(args.port))
    exitcode = daemon.wait()
    assert exitcode == 0

    process_eventlog(args.evlogdump, 'models_proxy_dir/fetch-eventlog', args.num_queries)

if __name__ == '__main__':
    main()
