from pycommon import *

class TestPing:
    def setup(self):
        self.__host = Testing.host()
        self.__port = Testing.port()
        self.__raw = RawClient(self.__host, self.__port)
    def teardown(self):
        pass
    def test_ping(self):
        response = self.__raw.get("/ping")
        check(response, 200, "pong")
