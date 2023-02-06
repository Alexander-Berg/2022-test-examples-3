import collections
import json

import flask

from crypta.lib.python.test_utils import flask_mock_server


class MockIntentaiApi(flask_mock_server.FlaskMockServer):
    Event = collections.namedtuple("Event", ["ip", "port", "timestamp"])
    Match = collections.namedtuple("Match", ["ip", "port", "timestamp", "user_id"])

    def __init__(self, frozen_time):
        super(MockIntentaiApi, self).__init__("INTENTAI")

        self.frozen_time = frozen_time

        @self.app.route("/", methods=["POST"])
        def get_matches():
            request = json.loads(flask.request.data.decode())
            events = [MockIntentaiApi._parse_event(event) for event in request]

            matches = [self._match_event(event)._asdict() for event in events]

            response_text = json.dumps(matches)

            response = flask.make_response(response_text, 200)
            response.mime_type = "application/json"
            return response

    @staticmethod
    def _parse_event(event):
        ip, port, ts = event["ip"], event["port"], event["timestamp"]
        return MockIntentaiApi.Event(ip, port, ts)

    def _match_event(self, event):
        return self.Match(event.ip, event.port, event.timestamp, self._get_user_id(event.ip))

    def _get_user_id(self, ip):
        if ip == "9.9.9.9":
            return None
        return "intentai_id_for_{}".format(ip)
