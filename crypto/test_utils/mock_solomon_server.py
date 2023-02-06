import json

from crypta.lib.python.test_utils.flask_mock_server import FlaskMockServer


class MockSolomonServer(FlaskMockServer):
    def __init__(self, name="MockSolomonServer"):
        super(MockSolomonServer, self).__init__(name)

        @self.app.route("/push", methods=["POST"])
        def push():
            return "OK"

        @self.app.route("/api/v2/push", methods=["POST"])
        def push_v2():
            return "OK"

    def dump_push_requests(self):
        result = []

        for request in self.dump_requests():
            if request["path"] in ("/push", "/api/v2/push"):
                item = request["args"].copy()
                item.update(json.loads(request["request_data"]))

                for sensor in item["sensors"]:
                    del sensor["ts"]
                    if isinstance(sensor["value"], float):
                        sensor["value"] = "{:.6g}".format(sensor["value"])

                result.append(item)

        return result
