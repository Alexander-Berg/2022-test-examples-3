import pytest
import yatest.common
import yatest.common.network
import os.path
import urllib
import time
import shard


@pytest.fixture(scope="session")
def shard_handle():
    cwd = yatest.common.work_path()
    return shard.Shard(0xdeadface, cwd)


class InvertedIndexServer:
    _process_handle = None
    _port_manager = None
    _port = None

    def start(self, shard_path, options={}):
        server_cfg = options["server_cfg"] if "server_cfg" in options else self._get_default_server_cfg()
        index_cfg = options["index_cfg"] if "index_cfg" in options else self._get_default_index_cfg()

        cwd = yatest.common.work_path()
        index_cfg_path = os.path.join(cwd, "index.cfg")
        server_cfg_path = os.path.join(cwd, "server.cfg")
        with open(server_cfg_path, "w") as f:
            f.write(server_cfg)
        with open(index_cfg_path, "w") as f:
            f.write(index_cfg)

        self._port_manager = yatest.common.network.PortManager()
        self._port = self._port_manager.get_port()

        exe = yatest.common.binary_path("search/base_search/daemons/inverted_index_storage/inverted_index_storage")
        assert os.path.isfile(exe) and os.access(exe, os.R_OK | os.X_OK)
        args = [exe, "-i", index_cfg_path, "-s", server_cfg_path, "-d", shard_path, "-p", str(self._port)]
        if "timestamp" in options:
            args.extend(["--db-timestamp", str(options["timestamp"])])
        self._process_handle = yatest.common.execute(args, wait=False)
        assert self._process_handle.running
        self._wait_port_used()
        os.unlink(index_cfg_path)
        os.unlink(server_cfg_path)

    def get_port(self):
        assert self._port is not None
        return self._port

    def stop(self):
        assert self._port_manager is not None
        self._port_manager.release()

        assert self._process_handle is not None
        assert self._process_handle.running
        res = urllib.urlopen("http://localhost:{}/admin?action=shutdown".format(self._port))
        assert res.getcode() == 200
        self._process_handle.wait(2)

    @staticmethod
    def _get_default_server_cfg():
        return "MaxQueueSize: 4\nThreads: 2"  # FIXME: use protobuf

    @staticmethod
    def _get_default_index_cfg():
        return "LockMemory: false"  # FIXME: use protobuf

    def _wait_port_used(self, timeout_seconds=20):
        assert self._port
        assert self._port_manager
        start = time.time()
        while self._port_manager.is_port_free(self._port):
            assert time.time() - start < timeout_seconds
            time.sleep(0.1)


@pytest.fixture
def server_handle(shard_handle, request):
    options = getattr(request.module, "MODULE_SERVER_OPTIONS", {})
    server = InvertedIndexServer()
    server.start(shard_handle.get_directory(), options)
    yield server
    server.stop()
