import json

import flask
import requests

from crypta.lib.python.test_utils.flask_mock_server import FlaskMockServer


EVENTS_REQUIRED_PARAMS = {"host", "service", "status", "description"}


class MockJugglerServer(FlaskMockServer):
    def __init__(self, name="MockJugglerServer"):
        super(MockJugglerServer, self).__init__(name)

        @self.app.route("/events", methods=["POST"])
        def events():
            events = flask.request.json["events"]

            for event in events:
                params = set(event.keys())
                if EVENTS_REQUIRED_PARAMS > params:
                    return flask.jsonify(
                        success=False,
                        message="Not all required params are present in the event: {}".format(EVENTS_REQUIRED_PARAMS - params)
                    ), requests.codes.bad_request

            return flask.jsonify(
                success=True,
                accepted_events=len(events),
                events=[dict(code=200)] * len(events)
            )

    @property
    def events_url(self):
        return "{}/{}".format(self.url_prefix, "events")

    def dump_events_requests(self):
        return [
            json.loads(request["request_data"])
            for request in self.dump_requests()
            if request["path"] == "/events"
        ]
