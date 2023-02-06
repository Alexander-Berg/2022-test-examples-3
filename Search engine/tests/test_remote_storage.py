import pytest
import yatest.common
import yatest.common.network
import logging
import subprocess
import threading
import resource
import requests
import itertools
import select
import numpy as np

# import json
import tempfile
import os
import socket
from urllib3.util import parse_url
from http.client import HTTPConnection
import urllib
import time
import math

from google.protobuf import text_format
from search.base_search.server.protos.server_config_pb2 import TServerConfig
import search.base_search.common.protos.docs_tier_pb2 as EDocsTier
from search.base.blob_storage.config.protos.chunk_access_type_pb2 import EChunkAccessType
from search.base.blob_storage.config.protos.remote_chunked_blob_storage_index_config_pb2 import (
    TRemoteBlobStorageIndexConfig,
    TRemoteBlobStorageChunkConfig,
)
from search.base.blob_storage.protos.remote_chunked_blob_storage_request_pb2 import (
    TRemoteChunkedBlobStorageRequest,
    TBlobRequest,
)
from search.base.blob_storage.protos.remote_chunked_blob_storage_response_pb2 import TRemoteChunkedBlobStorageResponse

# from search.base.blob_storage.protos.blob_response_pb2 import TBlobResponse


log = logging.getLogger(__name__)
YA_KEEP_TEMPS = bool(os.environ.get('YA_KEEP_TEMPS', False))


def run(args, **kwargs):
    log.debug("run '%s'", "' '".join(args))
    return subprocess.run(args, **kwargs)


def write_protobuf_config(message):
    t = text_format.MessageToString(message)
    f = tempfile.NamedTemporaryFile(mode='w+t', delete=not YA_KEEP_TEMPS)
    f.write(t)
    f.flush()
    return f


def set_fd_limit(req=None, lim=None):
    soft, hard = resource.getrlimit(resource.RLIMIT_NOFILE)
    if req is not None and req > soft:
        soft = req
        hard = max(soft, hard)
    if lim is not None and lim < soft:
        soft = lim
    resource.setrlimit(resource.RLIMIT_NOFILE, (soft, hard))


def procstat(pid):
    res = {}

    for line in open('/proc/{}/status'.format(pid)):
        if ':' not in line:
            continue
        k, v = line.split(':', 1)
        if v.endswith('kB\n'):
            res[k] = int(v[:-3]) * 1024
        elif k in ['Threads', 'FDSize', 'voluntary_ctxt_switches', 'nonvoluntary_ctxt_switches']:
            res[k] = int(v)
        else:
            res[k] = v[1:-1]

    with open('/proc/{}/stat'.format(pid)) as f:
        text = f.read()
        # skip pid, comm and make index 1-based as in 'man proc'
        val = [0, 0, 0] + text[text.rindex(')') + 1 :].split()
        res.update(
            {
                'flags': int(val[9]),
                'minflt': int(val[10]),
                'cminflt': int(val[11]),
                'majflt': int(val[12]),
                'cmajflt': int(val[13]),
                'utime': int(val[14]),
                'stime': int(val[15]),
                'cutime': int(val[16]),
                'cstime': int(val[17]),
            }
        )

    return res


def fdinfo(pid):
    res = {}
    for fd in os.listdir('/proc/{}/fd'.format(pid)):
        try:
            info = {}
            info['path'] = os.readlink('/proc/{}/fd/{}'.format(pid, fd))
            for line in open('/proc/{}/fdinfo/{}'.format(pid, fd)):
                k, v = line.split(':', 1)
                info[k] = v[1:-1]
            res[int(fd)] = info
        except:
            pass
    return res


class Timer(object):
    def __init__(self):
        self.reset()

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, exc, value, tb):
        self.stop()

    def reset(self):
        self.sum = 0
        self.sum2 = 0
        self.min = math.inf
        self.max = -math.inf
        self.cnt = 0
        self.start_time = None
        self.finish_time = None

    def start(self):
        self.start_time = time.monotonic()

    def stop(self):
        self.stop_time = time.monotonic()
        self.last = self.stop_time - self.start_time
        self.cnt += 1
        self.sum += self.last
        self.sum2 += self.last * self.last
        if self.min > self.last:
            self.min = self.last
        if self.max < self.last:
            self.max = self.last

    @property
    def avg(self):
        return self.sum / self.cnt if self.cnt else 0

    @property
    def mdev(self):
        return math.sqrt(self.sum2 / self.cnt - self.avg ** 2) if self.cnt else 0

    @property
    def elapsed(self):
        return time.monotonic() - self.start_time

    def __repr__(self):
        return "<Timer cnt={} avg={} mdev={} min={} max={}>".format(self.cnt, self.avg, self.mdev, self.min, self.max)


class RemoteStorageServer(object):
    def __init__(self, server_bin=None, base_path=None, port=8080, admin_port=8081, bind_unix=True, opts=None):
        if opts is None:
            opts = {}

        if server_bin is None:
            server_bin = yatest.common.build_path("search/base_search/daemons/remote_storage/remote_storage")
        self.server_bin = server_bin

        if base_path is None:
            base_path = os.getcwd()
        self.base_path = base_path

        self.port_manager = yatest.common.network.PortManager()
        self.port = self.port_manager.get_port(port)
        self.admin_port = self.port_manager.get_port(admin_port)

        opts['--port'] = self.port
        opts['--admin-port'] = self.admin_port

        self.unix_socket = None
        if bind_unix:
            self.unix_socket = tempfile.mktemp()
            opts['--unix-socket'] = self.unix_socket

        self.fd_count = 60000

        opts['--base-path'] = base_path
        opts.setdefault('--maxevents', 32)
        opts.setdefault('--queue-size', 300)
        opts.setdefault('--iteration-rate', 20)
        opts.setdefault('--poll-interval', '1s')
        opts.setdefault('--max-active-conns', 30000)
        opts.setdefault('--listener-backlog', 100)
        opts.setdefault('--inactive-timeout', '0.5s')
        opts.setdefault('--stale-timeout', '2s')
        # opts.setdefault('--buffers-limit', '100M')
        self.opts = opts

        retry = requests.packages.urllib3.Retry(total=5, connect=3, read=3, status_forcelist=[503], backoff_factor=1)

        adapter = requests.adapters.HTTPAdapter(max_retries=retry)
        self._session = requests.Session()
        self._session.mount('http://', adapter)

        self.storage_url = 'http://localhost:{}'.format(self.port)
        self.admin_url = 'http://localhost:{}/admin'.format(self.admin_port)
        self.unistat_url = 'http://localhost:{}/unistat'.format(self.admin_port)

        self.server_config = TServerConfig()
        self.server_config.Port = self.admin_port

        self.index_config = TRemoteBlobStorageIndexConfig()
        self.chunk_files = []

        self.unistat_prev = {}

    def _preexec(self):
        if self.fd_count is not None:
            set_fd_limit(self.fd_count)

    def add_chunk(
        self, path=None, size=None, tier=EDocsTier.PlatinumTier0, index='arc', shard=0, version=0, chunk=0, part=0, access_type=None
    ):
        if path is None:
            chunk_file = tempfile.NamedTemporaryFile(mode='w+b', delete=not YA_KEEP_TEMPS)
            chunk_file.write(b'0' * size)
            chunk_file.flush()
            self.chunk_files.append(chunk_file)
            path = chunk_file.name
        chunk = TRemoteBlobStorageChunkConfig(
            Tier=tier,
            Index=index.encode(),
            Shard=shard,
            Version=version,
            Id=chunk,
            Part=part,
            Path=os.path.relpath(path, self.base_path).encode(),
            AccessType=access_type
        )
        self.index_config.Chunks.extend([chunk])

    def make_request(
        self,
        from_config=None,
        tier=EDocsTier.PlatinumTier0,
        shard=0,
        version=0,
        index=b'arc',
        chunk=0,
        part=0,
        offset=0,
        size=0,
        repeat=1,
    ):
        if from_config is not None:
            cfg = self.index_config.Chunks[from_config]
            tier = cfg.Tier
            shard = cfg.Shard
            version = cfg.Version
            index = cfg.Index
            chunk = cfg.Id
            part = cfg.Part
        req = TRemoteChunkedBlobStorageRequest(Tier=tier, Shard=shard, Version=version)
        blob = TBlobRequest(Index=index, Chunk=chunk, Part=part, Offset=offset, Size=size)
        req.Blobs.extend([blob] * repeat)
        return req

    def make_connection(self):
        return HTTPConnection('localhost', port=self.port)

    def _wait_daemon(self, proc, expected=0):
        retcode = proc.wait()
        assert retcode == expected

    def start(self, wrapper=[]):
        self.server_config_file = write_protobuf_config(self.server_config)
        self.opts['--server-config'] = self.server_config_file.name

        self.index_config_file = write_protobuf_config(self.index_config)
        self.opts['--index-config'] = self.index_config_file.name

        args = wrapper + [self.server_bin]
        for k, v in self.opts.items():
            args += [k]
            if v is not None:
                args += [str(v)]

        log.info("Start '%s'", "' '".join(args))
        self._proc = subprocess.Popen(args, preexec_fn=self._preexec)
        self._waiter = threading.Thread(target=self._wait_daemon, args=(self._proc,), name='Waiter')
        self._waiter.start()
        self._session.get(self.unistat_url)
        self.pid = self._proc.pid

    def stop(self, timeout=60):
        if self._proc is None:
            return
        log.info("Stop")
        if self._proc.poll() is None:
            self._session.get(self.admin_url + '?action=shutdown', timeout=timeout)
        retcode = self._proc.wait(timeout=timeout)
        assert retcode == 0
        if self.unix_socket is not None:
            os.unlink(self.unix_socket)
        self._proc = None
        self._waiter.join()

    def unistat(self, params={}, timeout=1):
        raw = self._session.get(self.unistat_url, params=params).json()
        res = {}
        for v in raw:
            key, val = v
            if isinstance(val, list):
                num = 0
                den = 0
                for b, w in val:
                    num += b * w
                    den += w
                val = num / den if den else 0
            elif key.endswith('_dmmm'):
                diff = val - self.unistat_prev.get(key, 0)
                val, self.unistat_prev[key] = diff, val
            res[key] = val
        log.debug('unistat %s', res)
        return res

    def procstat(self):
        res = procstat(self.pid)
        log.debug('procstat %s', res)
        return res

    def fdinfo(self):
        res = fdinfo(self.pid)
        log.debug('fdinfo %s', res)
        return res


class RemoteStorageClient(object):
    def __init__(self, url, unix=None):
        self.url = url
        u = parse_url(url)
        self.addr = (u.host, u.port)
        self.unix = unix
        self.conn = None

    def MakeRequest(self, req):
        rsp = requests.post(self.url, data=req.SerializeToString())
        rsp = TRemoteChunkedBlobStorageResponse().FromString(rsp.content)
        return rsp

    def MakeStream(self, req):
        return requests.post(self.url, data=req.SerializeToString(), stream=True)

    def MakeSocket(self):
        sock = socket.socket(socket.AF_INET6)
        sock.connect(self.addr)
        return sock

    def MakeUnixSocket(self):
        sock = socket.socket(socket.AF_UNIX)
        sock.connect(self.unix)
        return sock

    def Connect(self):
        self.conn = HTTPConnection(self.addr[0], port=self.addr[1])
        self.conn.connect()
        self.sk = self.conn.sock.fileno()

    def SendRequest(self, req, params=None):
        self.start_time = time.monotonic()
        url = self.url
        if params is not None:
            url = url + '?' + urllib.parse.urlencode(params)
        self.conn.request('PUT', url, req)

    def RecvResponse(self):
        self.response = self.conn.getresponse()
        self.finish_time = time.monotonic()
        self.request_time = self.finish_time - self.start_time
        return self.response


@pytest.fixture
def rs():
    server = RemoteStorageServer()
    server.add_chunk(size=1 << 20)
    yield server
    server.stop()


def test_stat(rs):
    rs.start()
    print(rs.unistat().keys())

    known_signals = [
        # Connections
        'basesearch_daemon_accepted_conns_dmmm',
        'basesearch_daemon_conn_cache_size_ammm',

        # Errors
        'basesearch_daemon_5xx_errors_dmmm',
        'basesearch_daemon_service_unavailable_errors_dmmm',    # 503, also in 5xx
        'basesearch_daemon_requests_preempted_dmmm',            # also 503 and 5xx
        'remote_blob_storage_size_limit_violation_dmmm',        # request too large
        'remote_blob_storage_chunk_not_loaded_dmmm',            # absent file
        'remote_blob_storage_mapping_failed_dmmm',              # unknown file or read beyond eof
        'basesearch_daemon_unistat_errors_dmmm',

        # Queue
        'basesearch_daemon_requests_dmmm',                      # complete requests
        'basesearch_daemon_user_inflight_ahhh',                 # end request -> end I/O
        'remote_blob_storage_response_buffers_max',             # total size of all requests (end request -> end response)
        'basesearch_daemon_disk_inflight_ahhh',                 # start I/O -> end I/O
        'basesearch_daemon_disk_inflight_bytes_ahhh',           # start I/O -> end I/O
        'remote_blob_storage_request_total_size_dhhh',          # total size of one request

        # I/O by types
        'remote_blob_storage_bytes_dmmm',
        'remote_blob_storage_reads_dmmm',
        'remote_blob_storage_hedged_bytes_dmmm',
        'remote_blob_storage_hedged_reads_dmmm',
        'remote_blob_storage_recovery_bytes_dmmm',
        'remote_blob_storage_recovery_reads_dmmm',
        'remote_blob_storage_repeated_bytes_dmmm',
        'remote_blob_storage_repeated_reads_dmmm',

        # Backend
        'remote_blob_storage_absent_files_ammm',
        'remote_blob_storage_present_files_ammm',
        'remote_blob_storage_present_size_ammm',
        'remote_blob_storage_mapped_size_ammm',


        # Timings
        'self_request_time_mcs_dhhh',               # end request -> start response
        'self_total_request_time_mcs_dhhh',         # end request -> end response
        'self_succeeded_request_time_mcs_dhhh',     # end request -> start response
        'self_failed_request_time_mcs_dhhh',

        # Unused
        'self_queue_size_on_request_dhhh',
        'self_queue_wait_time_ms_dhhh',
        'self_failed_request_time_ms_dhhh',
        'self_request_time_ms_dhhh',
        'self_succeeded_request_time_ms_dhhh',
        'neh_transport_service_parse_errors_dmmm',  # FIXME
        'remote_blob_storage_empty_response_dmmm',
        'basesearch_daemon_404_errors_dmmm',
    ]

    present_signals = rs.unistat(params={'allholes': '1'}).keys()

    print(present_signals)
    print(known_signals)

    for k in known_signals:
        assert k in present_signals
    for k in present_signals:
        assert k in known_signals

    print(rs.procstat())


def test_request(rs):
    rs.start()

    client = RemoteStorageClient(rs.storage_url, rs.unix_socket)

    req = TRemoteChunkedBlobStorageRequest(
        Index=b'arc',
        Tier=EDocsTier.PlatinumTier0,
        Shard=0,
        Version=0,
    )
    blob = TBlobRequest(Chunk=0, Part=0, Offset=0, Size=1024)
    req.Blobs.extend([blob])

    print(rs.unistat())
    print(rs.procstat())

    tm = Timer()
    for i in range(1000):
        with tm:
            client.MakeRequest(req)

    print(tm)
    print(rs.unistat())
    print(rs.procstat())


def test_invalid_request(rs):
    rs.start()

    stat = rs.unistat()
    assert stat['basesearch_daemon_5xx_errors_dmmm'] == 0

    rsp = requests.post(rs.storage_url, data="AAAAAAAAAAA")
    print(rsp)
    assert rsp.status_code == 500

    stat = rs.unistat()
    assert stat['basesearch_daemon_5xx_errors_dmmm'] == 1


def test_stress(rs):
    rs.start()
    print(rs.unistat())
    print(rs.procstat())

    client = RemoteStorageClient(rs.storage_url, rs.unix_socket)
    socks = []
    tm = Timer()
    for x in range(1000):
        with tm:
            socks.append(client.MakeSocket())
        if tm.last > 0.5:
            print("Slow", x, tm.last)

    run(['ss', '-ltpnm', '( sport = {} )'.format(rs.port)])

    print(tm)
    print(rs.unistat())
    print(rs.procstat())


def test_non_recv(rs):
    rs.start()
    print(rs.unistat())
    print(rs.procstat())

    req = rs.make_request(from_config=0, size=1 << 20)

    print("OPEN")
    client = RemoteStorageClient(rs.storage_url)
    streams = []
    for x in range(1000):
        streams.append(client.MakeStream(req))

    print(rs.unistat())
    print(rs.procstat())

    print("CLOSE")
    for s in streams:
        s.close()

    print(rs.unistat())
    print(rs.procstat())


def test_poll_files(rs):
    fn = tempfile.mktemp()
    assert not os.path.exists(fn)
    rs.add_chunk(part=1, path=fn)
    print(rs.index_config)
    assert len(rs.index_config.Chunks) == 2
    rs.start()

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 0
    assert stat['remote_blob_storage_absent_files_ammm'] == 1
    assert stat['remote_blob_storage_present_files_ammm'] == 1
    assert stat['remote_blob_storage_present_size_ammm'] == 1 << 20

    client = RemoteStorageClient(rs.storage_url, rs.unix_socket)

    req = rs.make_request(from_config=1, size=1024)
    rsp = client.MakeRequest(req)
    print(rsp)

    stat = rs.unistat()
    print(stat)
    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 1

    assert not rsp.ErrorInfo.HasError
    assert len(rsp.BlobResponses) == 1
    assert rsp.BlobResponses[0].Failed

    f = open(fn, mode='w+b')
    f.write(b'0' * 1024)
    f.flush()

    time.sleep(2)

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 0
    assert stat['remote_blob_storage_absent_files_ammm'] == 0
    assert stat['remote_blob_storage_present_files_ammm'] == 2
    assert stat['remote_blob_storage_present_size_ammm'] == (1 << 20) + 1024

    rsp = client.MakeRequest(req)
    print(rsp)

    assert not rsp.ErrorInfo.HasError
    assert len(rsp.BlobResponses) == 1
    assert not rsp.BlobResponses[0].Failed

    os.unlink(fn)

    time.sleep(2)

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 0
    assert stat['remote_blob_storage_absent_files_ammm'] == 0
    assert stat['remote_blob_storage_present_files_ammm'] == 2
    assert stat['remote_blob_storage_present_size_ammm'] == (1 << 20) + 1024

    rsp = client.MakeRequest(req)
    print(rsp)

    assert not rsp.ErrorInfo.HasError
    assert len(rsp.BlobResponses) == 1
    assert not rsp.BlobResponses[0].Failed


def test_close_unlinked_files(rs):
    rs.opts['--close-unlinked-files'] = None
    fn = tempfile.mktemp()
    assert not os.path.exists(fn)
    rs.add_chunk(part=1, path=fn)
    print(rs.index_config)
    assert len(rs.index_config.Chunks) == 2
    rs.start()

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 0
    assert stat['remote_blob_storage_absent_files_ammm'] == 1
    assert stat['remote_blob_storage_present_files_ammm'] == 1
    assert stat['remote_blob_storage_present_size_ammm'] == 1 << 20

    client = RemoteStorageClient(rs.storage_url, rs.unix_socket)

    req = rs.make_request(from_config=1, size=1024)
    rsp = client.MakeRequest(req)
    print(rsp)

    stat = rs.unistat()
    print(stat)
    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 1

    assert not rsp.ErrorInfo.HasError
    assert len(rsp.BlobResponses) == 1
    assert rsp.BlobResponses[0].Failed

    f = open(fn, mode='w+b')
    f.write(b'0' * 1024)
    f.flush()

    time.sleep(2)

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 0
    assert stat['remote_blob_storage_absent_files_ammm'] == 0
    assert stat['remote_blob_storage_present_files_ammm'] == 2
    assert stat['remote_blob_storage_present_size_ammm'] == (1 << 20) + 1024

    rsp = client.MakeRequest(req)
    print(rsp)

    assert not rsp.ErrorInfo.HasError
    assert len(rsp.BlobResponses) == 1
    assert not rsp.BlobResponses[0].Failed

    os.unlink(fn)

    time.sleep(2)

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 0
    assert stat['remote_blob_storage_absent_files_ammm'] == 1
    assert stat['remote_blob_storage_present_files_ammm'] == 1
    assert stat['remote_blob_storage_present_size_ammm'] == 1 << 20

    rsp = client.MakeRequest(req)
    print(rsp)

    assert not rsp.ErrorInfo.HasError
    assert len(rsp.BlobResponses) == 1
    assert rsp.BlobResponses[0].Failed

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 1
    assert stat['remote_blob_storage_absent_files_ammm'] == 1
    assert stat['remote_blob_storage_present_files_ammm'] == 1
    assert stat['remote_blob_storage_present_size_ammm'] == 1 << 20


def test_poll_mmap_files(rs):
    fn = tempfile.mktemp()
    fn2 = tempfile.mktemp()
    fn3 = tempfile.mktemp()

    assert not os.path.exists(fn)
    rs.add_chunk(part=1, path=fn, access_type=EChunkAccessType.Mmap)
    rs.add_chunk(part=2, path=fn2, access_type='Mmap')
    rs.add_chunk(part=3, path=fn3, access_type='Disk')

    print(rs.index_config)
    chunks = len(rs.index_config.Chunks)
    assert chunks == 4
    rs.start()

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 0
    assert stat['remote_blob_storage_absent_files_ammm'] == 3
    assert stat['remote_blob_storage_present_files_ammm'] == 1
    assert stat['remote_blob_storage_present_size_ammm'] == 1 << 20

    client = RemoteStorageClient(rs.storage_url, rs.unix_socket)

    for i in range(1, chunks):
        req = rs.make_request(from_config=i, size=1024)
        rsp = client.MakeRequest(req)
        print(rsp)

    stat = rs.unistat()
    print(stat)
    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 3

    assert not rsp.ErrorInfo.HasError
    assert len(rsp.BlobResponses) == 1
    assert rsp.BlobResponses[0].Failed

    f = open(fn, mode='w+b')
    f.write(b'0' * 1024)
    f.flush()

    os.link(os.path.abspath(fn), fn2)
    open(fn2).read()
    os.link(os.path.abspath(fn), fn3)
    open(fn3).read()

    time.sleep(2)

    stat = rs.unistat()
    print(stat)

    assert stat['remote_blob_storage_chunk_not_loaded_dmmm'] == 0
    assert stat['remote_blob_storage_absent_files_ammm'] == 0
    assert stat['remote_blob_storage_present_files_ammm'] == 4
    assert stat['remote_blob_storage_present_size_ammm'] == (1 << 20) + 3*1024
    assert stat['remote_blob_storage_mapped_size_ammm'] == 1024

    rsp = client.MakeRequest(req)
    print(rsp)

    assert not rsp.ErrorInfo.HasError
    assert len(rsp.BlobResponses) == 1
    assert not rsp.BlobResponses[0].Failed


def test_buffers_limit(rs):
    rs.opts['--buffers-limit'] = '4M'
    rs.start()
    req = rs.make_request(from_config=0, size=1 << 20)

    stat0, proc0 = rs.unistat(), rs.procstat()

    print(stat0, proc0)

    responses = []
    for i in range(1, 5):
        rsp = requests.post(rs.storage_url, data=req.SerializeToString(), stream=True)
        print(rsp)
        assert rsp.status_code == 200
        responses.append(rsp)

        stat2 = rs.unistat()
        assert stat2['basesearch_daemon_5xx_errors_dmmm'] == 0
        assert stat2['basesearch_daemon_service_unavailable_errors_dmmm'] == 0
        assert stat2['basesearch_daemon_accepted_conns_dmmm'] == 1
        assert stat2['basesearch_daemon_conn_cache_size_ammm'] == i
        assert stat2['basesearch_daemon_requests_dmmm'] == 1
        assert stat2['remote_blob_storage_response_buffers_max'] == i << 20

    rsp = requests.post(rs.storage_url, data=req.SerializeToString(), stream=True)
    print(rsp)
    assert rsp.status_code == 503

    stat2 = rs.unistat()
    assert stat2['basesearch_daemon_5xx_errors_dmmm'] == 1
    assert stat2['basesearch_daemon_service_unavailable_errors_dmmm'] == 1
    assert stat2['basesearch_daemon_accepted_conns_dmmm'] == 1
    assert stat2['basesearch_daemon_conn_cache_size_ammm'] == 5
    assert stat2['basesearch_daemon_requests_dmmm'] == 1
    assert stat2['remote_blob_storage_response_buffers_max'] == 4 << 20

    for i, rsp in enumerate(responses):
        data = rsp.content
        print(rsp, len(data))

        stat2 = rs.unistat()
        assert stat2['basesearch_daemon_5xx_errors_dmmm'] == 0
        assert stat2['basesearch_daemon_service_unavailable_errors_dmmm'] == 0
        assert stat2['basesearch_daemon_accepted_conns_dmmm'] == 0
        assert stat2['basesearch_daemon_conn_cache_size_ammm'] == 5
        assert stat2['basesearch_daemon_requests_dmmm'] == 0
        assert stat2['remote_blob_storage_response_buffers_max'] == (3 - i) << 20

    # fdinfo = rs.fdinfo()


def test_request_types(rs):
    rs.start()

    rd = 1
    sz = 1 << 10

    req = rs.make_request(from_config=0, size=sz)

    stat0, proc0 = rs.unistat(), rs.procstat()

    print(stat0, proc0)

    for re, ry, he in itertools.product(['', '0', '1', 'N', 'Y'], repeat=3):
        params = []
        if re:
            params += ["repeated=" + re]
        if ry:
            params += ["recovery=" + ry]
        if he:
            params += ["hedged=" + he]

        re = 1 if re in ['1', 'Y'] else 0
        ry = 1 if ry in ['1', 'Y'] else 0
        he = 1 if he in ['1', 'Y'] else 0

        url = rs.storage_url + '?' + '&'.join(params)
        rsp = requests.post(url, data=req.SerializeToString())

        assert rsp.status_code == 200

        stat2 = rs.unistat()

        # print(url, stat2)

        assert stat2['basesearch_daemon_requests_dmmm'] == 1
        assert stat2['remote_blob_storage_reads_dmmm'] == rd
        assert stat2['remote_blob_storage_bytes_dmmm'] == sz
        assert stat2['remote_blob_storage_repeated_reads_dmmm'] == re * rd
        assert stat2['remote_blob_storage_repeated_bytes_dmmm'] == re * sz
        assert stat2['remote_blob_storage_recovery_reads_dmmm'] == ry * rd
        assert stat2['remote_blob_storage_recovery_bytes_dmmm'] == ry * sz
        assert stat2['remote_blob_storage_hedged_reads_dmmm'] == he * rd
        assert stat2['remote_blob_storage_hedged_bytes_dmmm'] == he * sz


def test_queue_order(rs):
    wrapper = []
    # wrapper += ['systemd-run', '--pipe', '--user', '--property=IOReadBandwidthMax=/ 100M']
    # wrapper += ['/usr/bin/strace', '-T', '-o' '/tmp/rs.log']
    rs.opts['--rate-limit'] = 100
    rs.opts['--queue-size'] = 300
    rs.opts['--lifo-order'] = None
    rs.opts['--priorities'] = None
    rs.opts['--preemptive'] = None

    rs.start(wrapper=wrapper)

    req = rs.make_request(from_config=0, size=1 << 10, repeat=4).SerializeToString()

    nr_flows = 1000
    nr_prios = 3
    flows = [RemoteStorageClient(rs.storage_url) for i in range(nr_flows)]

    socket_index = {}
    for i, f in enumerate(flows):
        f.Connect()
        socket_index[f.sk] = i

    epoll = select.epoll()
    finish_order = []
    start_time = time.monotonic()

    def flow_priority(i):
        if i % 50 == 0:
            return 2
        if i % 10 == 0:
            return 1
        return 0

    for i, f in enumerate(flows):
        f.SendRequest(req, params={'priority': flow_priority(i)})
        epoll.register(f.sk, select.EPOLLIN)

        for sk, _ in epoll.poll(timeout=1./200):
            epoll.unregister(sk)
            i = socket_index[sk]
            flows[i].select_time = time.monotonic()
            finish_order.append(i)

    while len(finish_order) < nr_flows:
        for sk, _ in epoll.poll():
            epoll.unregister(sk)
            i = socket_index[sk]
            flows[i].select_time = time.monotonic()
            finish_order.append(i)

    for f in flows:
        f.RecvResponse()

    stat = rs.unistat()

    prio = [([], []) for i in range(nr_prios)]

    finish_order = finish_order[:-300]

    for i in finish_order:
        f = flows[i]
        p = flow_priority(i)
        t = f.select_time - f.start_time
        print(i, p, f.response.status, f.response.length, f.start_time - start_time, t)
        prio[p][0 if f.response.status == 200 else 1].append(t)

    for k, v in stat.items():
        print(k, v, sep='\t')

    perc = [50, 90, 95, 98, 99, 99.9]
    def fmt(v):
        return '{:.2f}ms'.format(v*1000)

    print('result', 'count', 'avg', *['p{}'.format(p) for p in perc], sep='\t')

    for p, prio_stat in enumerate(prio):
        for r, res in enumerate(['succ', 'fail']):
            stat = prio_stat[r]
            if stat:
                print('prio' + str(p), res, len(stat), fmt(np.average(stat)), *[fmt(v) for v in np.percentile(stat, perc)], sep='\t')
