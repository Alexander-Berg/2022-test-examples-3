import collections
import json

import flask

from crypta.lib.python.test_utils import flask_mock_server


class MockBeelineApi(flask_mock_server.FlaskMockServer):
    Event = collections.namedtuple("Event", ["ip", "port", "unixtime"])
    Match = collections.namedtuple("Match", ["ip", "port", "unixtime", "id"])

    def __init__(self, frozen_time):
        super(MockBeelineApi, self).__init__("Beeline")

        self.frozen_time = frozen_time

        @self.app.route("/", methods=["POST"])
        def get_matches():
            request = json.loads(flask.request.data.decode())
            events = [MockBeelineApi._parse_event(event) for event in request]

            matches = [self._match_event(event)._asdict() for event in events if self._is_matching(event)]

            response_text = json.dumps(matches)

            response = flask.make_response(response_text, 200)
            response.mime_type = "application/json"
            return response

    @staticmethod
    def _parse_event(event):
        ip, port, unixtime = event["ip"], event["port"], event["unixtime"]
        return MockBeelineApi.Event(ip, port, unixtime)

    def _is_matching(self, event):
        return event.ip != "0.0.0.0"

    def _match_event(self, event):
        return self.Match(event.ip, event.port, event.unixtime, self._get_user_id(event.ip))

    def _get_user_id(self, ip):
        return "beeline_id_for_{}".format(ip)
