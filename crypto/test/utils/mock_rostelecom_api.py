import collections

import flask

from crypta.ext_fp.matcher.bin.test.utils.ip_range import IpRange
from crypta.lib.python.test_utils import flask_mock_server


class MockRostelecomApi(flask_mock_server.FlaskMockServer):
    Event = collections.namedtuple("Event", ["ip", "port", "unixtime", "domain"])
    Match = collections.namedtuple("Match", ["ip", "port", "unixtime", "domain", "ext_id"])

    class InvalidBodyException(Exception):
        pass

    def __init__(self, ip_ranges, frozen_time):
        super(MockRostelecomApi, self).__init__("Rostelecom")

        self.ip_ranges = [IpRange(range_str) for range_str in ip_ranges]
        self.frozen_time = frozen_time

        @self.app.route("/", methods=["POST"])
        def get_matches():
            lines = flask.request.data.decode().rstrip().split("\n")
            events = [MockRostelecomApi._parse_event(line) for line in lines]

            matches = [self.Match(e.ip, e.port, e.unixtime, e.domain, self._get_user_id(e.ip)) for e in events if self._is_in_ranges(e.ip)]

            response_text = "\n".join([MockRostelecomApi._match_to_tsv(m) for m in matches])

            response = flask.make_response(response_text, 200)
            response.mime_type = "text/plain"
            return response

    @staticmethod
    def _parse_event(line):
        try:
            ip, port, unixtime, domain = line.rstrip().split("\t")
        except:
            raise MockRostelecomApi.InvalidBodyException("Couldn't parse line '{}'".format(line))

        unixtime = int(unixtime)
        return MockRostelecomApi.Event(ip, port, unixtime, domain)

    @staticmethod
    def _match_to_tsv(match):
        return "\t".join([str(v) for v in match])

    def _get_user_id(self, ip):
        return "fake_rostelecom_id_for_{}".format(ip)

    def _is_in_ranges(self, ip):
        return any([r.contains(ip) for r in self.ip_ranges])
