# -*- coding: utf-8 -*-
from runtime_tests.util.stream.io.stream import SocketStream


class StreamManager(object):
    def __init__(self, resource_manager):
        super(StreamManager, self).__init__()
        self.__resource_manager = resource_manager

    def create(self, port, timeout=None, host='localhost'):
        sock = SocketStream.from_address(host, port, timeout=timeout)
        self.__resource_manager.register(sock)
        return sock
