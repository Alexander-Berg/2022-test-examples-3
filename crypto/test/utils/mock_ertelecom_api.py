import collections

import flask

from crypta.lib.python.test_utils import flask_mock_server


class MockErtelecomApi(flask_mock_server.FlaskMockServer):
    Event = collections.namedtuple("Event", ["ip", "port", "unixtime"])
    Match = collections.namedtuple("Match", ["code", "ip", "port", "unixtime", "ext_id"])

    class StatusCode(object):
        OK = 0
        TIMEOUT = 1
        NOT_FOUND = 2
        PARSING_ERROR = 3

    def __init__(self, frozen_time):
        super(MockErtelecomApi, self).__init__("Er-Telecom")

        self.frozen_time = frozen_time

        @self.app.route("/", methods=["POST"])
        def get_matches():
            lines = flask.request.data.decode().rstrip().split("\n")
            events = [MockErtelecomApi._parse_event(line) for line in lines]

            matches = [self._match_event(e) for e in events]

            response_text = "\n".join([MockErtelecomApi._match_to_tsv(m) for m in matches])

            response = flask.make_response(response_text, 200)
            response.mime_type = "text/plain"
            return response

    @staticmethod
    def _parse_event(line):
        ip, port, unixtime = line.rstrip().split("\t")
        unixtime = int(unixtime)
        return MockErtelecomApi.Event(ip, port, unixtime)

    def _match_event(self, event):
        if (self.frozen_time - event.unixtime) > 30:
            return self.Match(MockErtelecomApi.StatusCode.TIMEOUT, event.ip, event.port, event.unixtime, "null")

        return self.Match(MockErtelecomApi.StatusCode.OK, event.ip, event.port, event.unixtime, self._get_user_id(event.ip))

    @staticmethod
    def _match_to_tsv(match):
        return "\t".join([str(v) for v in match])

    def _get_user_id(self, ip):
        return "fake_ertelecom_id_for_{}".format(ip)
