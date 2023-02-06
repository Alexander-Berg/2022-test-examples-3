# -*- coding: utf-8 -*-
from runtime_tests.util.proto.handler.client.http import HTTPConnection


class HTTPConnectionManager(object):
    def __init__(self, resource_manager, stream_manager):
        super(HTTPConnectionManager, self).__init__()
        self.__resource_manager = resource_manager
        self.__stream_manager = stream_manager

    def __create(self, sock, newline='\r\n'):
        conn = HTTPConnection(sock, newline=newline)
        self.__resource_manager.register(conn)
        return conn

    def create(self, port, timeout=None, host='localhost', newline='\r\n'):
        """
        :rtype: HTTPConnection
        """
        return self.__create(self.__stream_manager.create(port, timeout=timeout, host=host), newline=newline)

    def create_ssl(self, port, ssl_options=None, host='localhost', check_closed=True):
        """
        :rtype: HTTPConnection
        """
        return self.__create(self.__stream_manager.create_ssl(port, ssl_options, host=host, check_closed=check_closed))
