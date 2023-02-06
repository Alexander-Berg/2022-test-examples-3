# -*- coding: utf-8 -*-
import ctypes
import multiprocessing as m
import threading as t
import time
import socket
import errno

from runtime_tests.util.backend import socket_server
from runtime_tests.util.stream.io import stream
from runtime_tests.util.resource import AbstractResource

from runtime_tests.util.proto.handler.server import ConfigServerHandlerFactory


class PythonBackendException(Exception):
    pass


class BackendConfig(object):
    def __init__(self, handler_factory, port, address_family):
        super(BackendConfig, self).__init__()
        self.port = port
        self.handler_factory = handler_factory
        self.address_family = address_family


class ReuseServer(socket_server.ThreadingTCPServer):
    allow_reuse_address = True

    def __init__(self, config):
        self.__config = config
        self.address_family = config.address_family
        if config.address_family is None:
            addrs = [(socket.AF_INET6, 'localhost', config.port), (socket.AF_INET, 'localhost', config.port)]
        else:
            addrs = [(config.address_family, '', config.port)]
        socket_server.ThreadingTCPServer.__init__(
            self, addrs,
            config.handler_factory
        )

    def get_request(self, sock):
        conn, address = socket_server.ThreadingTCPServer.get_request(self, sock)
        self.__config.handler_factory.state.accepted.inc()
        self.__config.handler_factory.state.conn_addrs.put((address[0], address[1]))
        conn = stream.SocketStream(conn)
        return conn, address


class PythonBackend(AbstractResource):
    __HOST = 'localhost'
    __START_TIME = 5
    __SLEEP_TIMEOUT = 0.01
    __JOIN_TIMEOUT = 10

    def __init__(self, server_config, connect_func, logger):
        super(PythonBackend, self).__init__()
        self.__name = server_config.handler_factory.name

        self.__connect_func = connect_func
        self.__logger = logger

        self.__server_config = server_config
        self.__stop_flag = m.Value(ctypes.c_bool, False)
        self.__stop_condition = m.Condition()
        self.__server_process = m.Process(target=self.__start_server)

    @property
    def server_config(self):
        """
        :rtype: BackendConfig
        """
        return self.__server_config

    @property
    def state(self):
        """
        :rtype: AbstractState
        """
        return self.__server_config.handler_factory.state

    def start(self, check_timeout=None):
        if self.__stop_flag.value:
            raise PythonBackendException('backend already running')
        self.__stop_flag.value = False
        self.__server_process.start()

        if check_timeout:
            time.sleep(check_timeout)

        self.__check_process()

    stop = AbstractResource.finish

    def _finish(self):
        self.__stop_condition.acquire()
        self.__stop_flag.value = True
        self.__stop_condition.notify_all()
        self.__stop_condition.release()
        self.state.finish()
        self.__server_process.join(self.__JOIN_TIMEOUT)
        if self.__server_process.is_alive():
            self.__logger.error('backend process not finished')
            self.__server_process.terminate()

    def __start_server(self):
        server_ = ReuseServer(self.__server_config)
        server_thread = t.Thread(target=server_.serve_forever)
        server_thread.start()
        self.__stop_condition.acquire()
        while not self.__stop_flag.value:
            self.__stop_condition.wait()
        server_.shutdown()
        server_.server_close()
        server_thread.join(self.__JOIN_TIMEOUT)
        if server_thread.is_alive():
            self.__logger.error('backend thread not finished')
            server_thread.terminate()
        self.__stop_condition.release()

    # TODO check backend with condition variable
    def __check_process(self):
        start_time = time.time()

        def check_time():
            if time.time() - start_time > self.__START_TIME:
                raise PythonBackendException('%s timed out' % self.__name)

        while True:
            try:
                sock = self.__connect_func(self.__server_config.port)
                sock.close()
                while self.state.accepted.value == 0:
                    time.sleep(self.__SLEEP_TIMEOUT)
                    check_time()
                self.state.accepted.reset()
                self.state.conn_addrs.get()
                break
            except socket.error, err:
                if err.errno == errno.ECONNREFUSED:
                    pass
                else:
                    raise PythonBackendException('%s exception: %s' % (self.__name, str(err)))

            check_time()
            time.sleep(self.__SLEEP_TIMEOUT)

    def get_name(self):
        return self.__name


class BackendManager(object):
    def __init__(self, logger, resource_manager, stream_manager):
        super(BackendManager, self).__init__()
        self.__logger = logger
        self.__stream_manager = stream_manager

        self.__resource_manager = resource_manager

    def start(self, handler_config, port, address_family):
        """
        :param Config handler_config: backend handler config

        :rtype: PythonBackend
        """
        state = handler_config.STATE_TYPE(handler_config)
        handler_factory = ConfigServerHandlerFactory(state, handler_config)
        server_config = BackendConfig(handler_factory, port, address_family)
        return self.start_generic(server_config)

    def start_generic(self, server_config):
        """
        :param BackendConfig server_config: backend config
        :rtype: PythonBackend
        """
        connect_func = self.__stream_manager.create
        backend = PythonBackend(server_config, connect_func, self.__logger)
        self.__resource_manager.register(backend)
        backend.start()
        return backend
