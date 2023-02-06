import os

import flask
import yatest.common

from crypta.lib.python.test_utils import flask_mock_server


def mock_sandbox_server_with_resource(resources_path, resource_type, released):
    class MockSandboxServer(flask_mock_server.FlaskMockServer):
        def __init__(self):
            super(MockSandboxServer, self).__init__("Sandbox")
            self.commands = []

            @self.app.route("/last/{}/<file_name>".format(resource_type))
            def get_resource(file_name):
                return flask.send_file(yatest.common.build_path(os.path.join(resources_path, resource_type, file_name)))

        def get_resource_url(self, file_name):
            return 'http://localhost:{port}/last/{resource_type}/{file_name}?attrs={{"released":"{released}"}}'.format(
                port=self.port,
                resource_type=resource_type,
                file_name=file_name,
                released=released,
            )

    return MockSandboxServer()
