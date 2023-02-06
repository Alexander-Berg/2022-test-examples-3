import collections
import json

import flask

from crypta.lib.python.test_utils import flask_mock_server


class MockMtsApi(flask_mock_server.FlaskMockServer):
    Event = collections.namedtuple("Event", ["ip", "port", "ts"])
    Match = collections.namedtuple("Match", ["ip", "port", "ts", "id"])

    def __init__(self, frozen_time):
        super(MockMtsApi, self).__init__("MTS")

        self.frozen_time = frozen_time

        @self.app.route("/", methods=["POST"])
        def get_matches():
            request = json.loads(flask.request.data.decode())
            events = [MockMtsApi._parse_event(event) for event in request]

            matches = [self._match_event(event)._asdict() for event in events]

            response_text = json.dumps(matches)

            response = flask.make_response(response_text, 200)
            response.mime_type = "application/json"
            return response

    @staticmethod
    def _parse_event(event):
        ip, port, ts = event["ip"], event["port"], event["ts"]
        return MockMtsApi.Event(ip, port, ts)

    def _is_matching(self, event):
        return event.ip != "0.0.0.0"

    def _match_event(self, event):
        return self.Match(event.ip, event.port, event.ts, self._get_user_id(event.ip))

    def _get_user_id(self, ip):
        if ip == "0.0.0.0":
            return "unknown"
        elif ip == "8.8.8.8":
            return "wrong_ip"
        return "mts_id_for_{}".format(ip)
