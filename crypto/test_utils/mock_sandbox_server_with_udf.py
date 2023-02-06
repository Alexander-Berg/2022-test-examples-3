import json

import flask
import yatest.common

from crypta.lib.python.test_utils import flask_mock_server


def mock_sandbox_server_with_udf(udf_resource_type, udf_so_path):
    class MockSandboxServer(flask_mock_server.FlaskMockServer):
        def __init__(self):
            super(MockSandboxServer, self).__init__("Sandbox")
            self.commands = []

            @self.app.route("/last/{}".format(udf_resource_type))
            def get_udf():
                assert "salt" in flask.request.args
                assert 1 == len(flask.request.args.getlist("attrs"))
                attrs = json.loads(flask.request.args["attrs"])
                assert attrs["released"] == "stable"

                return flask.send_file(yatest.common.binary_path(udf_so_path))

        def get_udf_url(self):
            return 'http://localhost:{port}/last/{udf_resource_type}?attrs={{"released":"stable"}}&salt=1'.format(
                port=self.port,
                udf_resource_type=udf_resource_type,
            )

    return MockSandboxServer()
