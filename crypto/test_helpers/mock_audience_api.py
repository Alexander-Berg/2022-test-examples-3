import flask

from crypta.lib.python.test_utils import flask_mock_server


class MockAudienceApi(flask_mock_server.FlaskMockServer):

    def __init__(self):
        super(MockAudienceApi, self).__init__("Audience")

        @self.app.route("/v1/management/client/segments/upload_file/", methods=["POST"])
        def upload_segment_file():
            return flask.jsonify(segment={'id': 0})

        @self.app.route("/v1/management/client/segment/<segment_id>/confirm/", methods=["POST"])
        def confirm_segment(segment_id):
            return flask.jsonify({})
