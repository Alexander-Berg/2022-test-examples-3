from threading import Thread

import flask
import ipaddr
import requests
import retry
import six
import yatest.common.network


MOCK_PATH_PREFIX = "/_mock"


def mock_path(path):
    return MOCK_PATH_PREFIX + path


PING_PATH = mock_path("/ping")
SHUTDOWN_PATH = mock_path("/shutdown")


def is_ipv6(host):
    try:
        ipaddr.IPv6Address(six.ensure_str(host, "utf-8"))
        return True
    except ipaddr.AddressValueError:
        return False


def create_app(name):
    app = flask.Flask(name)
    app._requests = []

    @app.route(SHUTDOWN_PATH)
    def shutdown():
        if "werkzeug.server.shutdown" not in flask.request.environ:
            raise RuntimeError("Not running the development server")
        flask.request.environ["werkzeug.server.shutdown"]()
        return "Server shutting down..."

    @app.route(PING_PATH)
    def ping():
        return flask.make_response("OK", 200)

    @app.errorhandler(404)
    def page_not_found(_):
        return flask.make_response("Not found", 404)

    @app.errorhandler(500)
    def internal_error(_):
        return flask.make_response("Internal server error", 500)

    @app.errorhandler(400)
    def bad_request(_):
        return flask.make_response("Bad request", 400)

    @app.before_first_request
    def cleanup_db():
        app._requests = []

    @app.after_request
    def save_response(response):
        if not flask.request.path.startswith(MOCK_PATH_PREFIX):
            request_params = dict(
                method=flask.request.method,
                path=flask.request.path,
                args=dict(flask.request.args.lists()),
                request_data=flask.request.data,
                status_code=response.status_code,
            )

            if not response.direct_passthrough:
                request_params["response_data"] = response.data

            if flask.request.files:
                files = {}
                for k, v in six.iteritems(flask.request.files):
                    v.stream.seek(0)
                    files[k] = v.read()
                request_params["files"] = files

            app._requests.append(request_params)

        return response

    return app


class FlaskMockServer(object):
    def __init__(self, name):
        self.app = create_app(name)
        self.thread = None
        self._host = "::1"
        self.port_manager = yatest.common.network.PortManager()
        self.port = None

    def _ping_exc(self):
        self.get(PING_PATH, timeout=1).raise_for_status()

    def is_up(self):
        try:
            self._ping_exc()
        except Exception:
            return False

        return True

    @property
    def host(self):
        return "[{}]".format(self._host) if is_ipv6(self._host) else self._host

    @property
    def address(self):
        assert self.port
        return "{}:{}".format(self.host, self.port)

    @property
    def url_prefix(self):
        return "http://{}".format(self.address)

    def _run(self):
        self.app.run(self._host, self.port)

    def get(self, path, *args, **kwargs):
        return requests.get(self.url_prefix + path, *args, **kwargs)

    def post(self, path, *args, **kwargs):
        return requests.post(self.url_prefix + path, *args, **kwargs)

    def start(self):
        if self.is_up() or self.thread:
            raise Exception("Tried to start an already running server")

        self.port = self.port_manager.get_port()

        self.thread = Thread(target=self._run)
        self.thread.setDaemon(True)
        self.thread.start()

        retry.retry_call(self._ping_exc, tries=30, delay=1)

    def stop(self):
        self.get(SHUTDOWN_PATH, timeout=5)
        self.thread.join(timeout=5)

        if self.thread.is_alive():
            raise Exception("Failed to stop server thread")

        self.thread = None
        self.port_manager.release()

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.stop()

    def dump_requests(self):
        return self.app._requests
