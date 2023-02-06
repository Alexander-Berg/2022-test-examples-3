from __future__ import print_function, absolute_import

import os
import random
import socket
import gevent
import pytest
import logging

import common.utils

from common.joint.client import RPCClientGevent as RPCClient
from common.joint.server import Server, RPC
from common.joint import errors
from common.joint.tests import context

ctx = context.Context(context.Context.Config({
    'rpc': {
        'connection_magic': 0xDEADBEAF,
        'handshake_send_timeout': 1,
        'handshake_receive_timeout': 1,
        'idle_timeout': 6,                  # then no requests are processed - close connection after 10 secs
        'socket_nodelay': True,             # we will send a lot of small packets
        'socket_nodelay_cutoff': 2097152,   # 2mb
        'socket_receive_buffer': 131072,    # 128kb
        'socket_send_buffer': 131072,       # 128kb
        'socket_timeout': 6,                # raw socket timeout, should be bigger than any other
        'receive_buffer': 16384,            # 16kb
        'uid_generation_tries': 10,
        'uid_generation_retry_sleep': 0.1,
        'pingpong_sleep_seconds': 0,

        'client': {
            'connect_timeout': 1,
            'socket_nodelay': True,
            'socket_receive_buffer': 131072,
            'socket_send_buffer': 131072,
            'socket_timeout': 6,
            'idle_timeout': 1,
            'ping_tick_time': 1,            # ping every 10 seconds
            'ping_wait_time': 1,
            'receive_buffer': 16384,
            'job_registration_timeout': 2,
        },
    },

    'server': {
        'host': '',
        'port': 0,
        'unix': None,
        # 'host': None,
        # 'port': None,
        # 'unix': '/tmp/serviceq.%d.rpc.test.sock',

        'backlog': 10,
        'magic_receive_timeout': 10,
        'max_connections': 32,
    },
}))

ctx.stalled_jobs_timeout = .5


class ServerSideException(errors.ServerError):
    pass


class LocalServer(RPC):
    class LocalServerSideException(BaseException):
        pass

    def __init__(self, ctx):
        super(LocalServer, self).__init__(ctx)

        self.server = Server(ctx)
        # Register server connection handlers
        self.server.register_connection_handler(self.get_connection_handler())

    def start(self):
        super(LocalServer, self).start()
        self.server.start()
        return self

    def stop(self):
        self.server.stop()
        self.server.join()
        super(LocalServer, self).stop()
        super(LocalServer, self).join()
        return self

    @RPC.simple
    def hang(self, timeout, terminate_timeout):
        try:
            gevent.sleep(timeout)
        except gevent.GreenletExit:
            gevent.sleep(terminate_timeout)

    def on_stalled_jobs(self, stalled):
        self.ctx.stalled = stalled

    @RPC.simple
    def ping(self, magic):
        return magic

    @RPC.simple
    def exception(self, serializable=True):
        raise (ServerSideException if serializable else self.LocalServerSideException)('Something wrong!', 42)

    @RPC.full
    def range(self, job, a, b):
        for i in range(a, b):
            gevent.sleep(0.01)
            job.state(i)
        return a + b

    @RPC.generator(name='range2')
    def __theSameAsRangeMethodButWithYieldingAndWithoutSleeping(self, a, b):
        for i in range(a, b):
            yield i
        raise StopIteration(a + b)

    @RPC.generator
    def emptyGen(self):
        if False:
            yield

    @RPC.dupgenerator
    def duplex_gen(self, a, b):
        for i in xrange(a, b):
            if (yield i) is True:
                break


def setup_function(func):
    print("")  # Avoid log entries printing on the same line as the result test

    global ctx
    func.ctx = ctx
    port = func.ctx.cfg.server.port
    host = func.ctx.cfg.server.host if port is not None else func.ctx.cfg.server.unix
    if port is None:
        host %= os.getpid()
        func.ctx.cfg.server.unix = host
        if os.path.exists(host):
            os.unlink(host)
    else:
        for i in range(0, 3):
            port = random.randrange(10000, 30000)
            try:
                RPCClient(cfg=func.ctx.cfg.rpc, host="::1", port=port).connect()
            except socket.error:
                break
        func.ctx.cfg.server.port = port

    func.srv = LocalServer(func.ctx).start()
    # Connect to the local server
    func.client = RPCClient(cfg=func.ctx.cfg.rpc, host="::1", port=port)


def teardown_function(func):
    print("")  # Avoid log entries printing on the same line as the result test
    # Stop both client and server
    gevent.sleep(0.1)  # Take time for server to cleanup a bit
    func.client.stop()
    func.srv.stop()


def test_stalled_jobs():
    ctx.stalled = None
    client = test_stalled_jobs.client
    client.call('hang', .1, ctx.stalled_jobs_timeout / 5.).wait(1)
    assert ctx.stalled is None

    with pytest.raises(errors.CallTimeout):
        client.call('hang', 1, ctx.stalled_jobs_timeout / 5.).wait(.1)
    gevent.sleep(ctx.stalled_jobs_timeout * 2)
    assert ctx.stalled is None

    with pytest.raises(errors.CallTimeout):
        client.call('hang', 1, ctx.stalled_jobs_timeout * 2).wait(.1)
    assert ctx.stalled is None
    gevent.sleep(ctx.stalled_jobs_timeout * 2)
    assert ctx.stalled is not None


def test_basic():
    ctx = test_basic.ctx
    client = test_basic.client

    # Attempt to connect to non-listening port
    try:
        port = client.port
        host = client.host
        if port is None:
            host += '.notexistingsocket'
        else:
            port += 1
        RPCClient(cfg=ctx.cfg.rpc, host=host, port=port).ping()
        assert False and "This point should not be reached - the operation should fail."
    except socket.error:
        pass

    # Perform server ping
    client.ping()

    # Check that call to a method, which will raise an exception will not be raised on client-side,
    # if the client will immediately discard the method call result.
    call = client.call('exception')
    gevent.sleep(0.1)  # Get some sleep to take a chance for server to perform the actual call

    # Check that call to a non-existing method and discarding the result immediately
    # will not raise any exceptions on client-side.
    call = client.call('noSuchMethod')
    gevent.sleep(0.5)  # Get some sleep to take a chance for server to perform the actual call

    # Check that server-side exception correctly restored on client-side.
    call = client.call('exception')
    try:
        call.wait()
    except errors.ServerError as ex:
        assert isinstance(ex, ServerSideException)
        assert ex.args[0] == 'Something wrong!'
        assert ex.args[1] == 42
        assert ex.sid == call.sid
        assert ex.jid == call.jid
        assert len(ex.tb)

    try:
        client.call('exception', False).wait()
    except errors.ServerError as ex:
        assert ex.__class__ is errors.ServerError
        assert ex.args[0] == 'Something wrong!'
        assert ex.args[1] == 42
        assert len(ex.tb)

    # Check regular method processing
    for i in range(0, 2):
        magic = random.random()
        assert client.call('ping', magic).wait(timeout=0.1) == magic
        # Check client will resurrect after reactor stop
        client.stop()

    # Check regular method processing with intermediate server pinging
    call = client.call('ping', 42)
    client.ping()
    assert call.wait() == 42

    call = client.call('range2', 2, 8)
    assert call.next() == 2
    client.ping()
    assert call.next() == 3
    client.ping()
    assert call.wait() == 10

    call = client.call('duplex_gen', 2, 10)
    gen = call.generator
    sample = iter(xrange(2, 10))
    for i in gen:
        assert i == sample.next()

    call = client.call('duplex_gen', 2, 10)
    gen = call.generator
    sample = iter(xrange(2, 5))
    i = None
    for i in gen:
        if i == 5:
            try:
                gen.send(True)
            except StopIteration:
                pass
        else:
            assert i == sample.next()
    assert i == 5

    # Check methods with intermediate results returning
    for meth in ['range', 'range2']:
        call = client.call(meth, 8, 20)
        for i in call:
            assert 8 <= i < 20
        assert call.wait(timeout=0) == 28

    # Check non-existing methods call
    call = client.call('noSuchMethod', 'foo')
    try:
        call.wait()
        assert False and "This point should not be reached - the operation should fail."
    except errors.CallError as ex:
        # Check also that the exception provides session and job IDs
        assert ex.args[1] == call.sid
        assert ex.args[2] == call.jid

    # Check timeout with intermediate results fetch
    try:
        call = client.call('range', 8, 20)
        for i in call.iter(timeout=0.1):
            assert 8 <= i < 20
        assert False and "This point should not be reached - the operation should time out."
    except errors.CallTimeout:
        pass

    # Check timeout on simple call form
    try:
        client.call('range', 8, 20).wait(timeout=0.1)
        assert False and "This point should not be reached - the operation should time out."
    except errors.CallTimeout:
        pass

    # Check empty generator call
    call = client.call('emptyGen')
    for i in call:
        assert False
    assert call.wait(timeout=0) is None

    # Check the reactor will correctly handle just-created-and-immediately-dropped job
    client.call('ping', 42).__del__()

    # Check reactor abort
    assert client.call('ping', 0xC01CDE).wait(timeout=0.1) == 0xC01CDE
    c = client.call('ping', 42)
    old_rsid = c.reactor.sid
    original_get = c.job.queue.get

    def patched_getter(*args, **kwargs):
        data = original_get(*args, **kwargs)
        if data[0] == "REGISTERED":
            data[0] = "COMPLETE"
        return data

    c.job.queue.get = patched_getter
    try:
        c.wait()
        assert "This point should never be reached!"
    except errors.ProtocolError:
        pass

    # Here reactor should be restarted finely
    c = client.call('ping', 0xC01CDE)
    assert c.wait(timeout=0.1) == 0xC01CDE
    assert c.reactor.sid != old_rsid

    # Let's perform several jobs simultaneously
    def grn(client):
        for meth in ['range', 'range2']:
            call = client.call(meth, 8, 20)
            for i in call:
                assert 8 <= i < 20
            assert call.wait(timeout=0) == 28

    grns = [gevent.spawn(grn, client) for i in range(0, 3)]
    for g in grns:
        g.get()


@pytest.mark.xfail(run=False)  # FIXME: SANDBOX-4242
def test_performance():
    client = test_performance.client

    # Reduce amount of log entries produced on performance test
    test_performance.ctx.log.setLevel(logging.WARNING)
    # Let's count which amount of time will take processing of 1K requests.
    # Its doesn't matter I will do it in parallel or not - both client and server are greenlets.
    # So the actual speed can theoretically be almost two times bigger for couple of different processes,
    # but it should not be scalable for more CPUs - server will always process messages with one thread.
    REQUESTS = 5000
    with common.utils.Timer() as timer:
        for i in range(0, REQUESTS):
            assert client.call('ping', 42).wait() == 42
        print('%d requests processed in %s. Rate is %.4f' % (REQUESTS, timer, REQUESTS / float(timer)))
        # KORUM: Any ideas, how to keep acceptable rate without asserting on absolute numbers?
        # Actually, we can calculate some approximate CPU performance number and check the rate
        # _relatively_ against the calculated number and some hard-coded value..
        # assert rate > 3000

    # Check the speed of intermediate results processing
    REQUESTS = 100000
    with common.utils.Timer() as timer:
        call = client.call('range2', 1, REQUESTS)
        for i in call:
            pass
        call.wait()
        print(
            '%d intermediate result iterations processed in %s. Rate is %.4f'
            % (REQUESTS, timer, REQUESTS / float(timer))
        )
        # KORUM: Any other ideas?
        # assert rate > 60000
