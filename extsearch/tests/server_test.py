import sys
import json
import time
import argparse
import requests
import tempfile
import threading

from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler


class YabsServer(BaseHTTPRequestHandler):
    def __init__(self, answer, *args, **kwargs):
        self.answer = answer
        BaseHTTPRequestHandler.__init__(self, *args, **kwargs)

    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/json')
        self.end_headers()
        self.wfile.write(json.dumps(self.answer))


class YabsServerFactory(object):
    def __init__(self, answer):
        self.answer = answer

    def __call__(self, *args, **kwargs):
        YabsServer(self.answer, *args, **kwargs)


class YabsProxyServer:
    def __init__(self, port, yabs_port=0, is_external_server=False):
        self.port = port
        self.yabs_port = yabs_port
        self.is_external_server = is_external_server

    def start_server(self):
        import yatest.common

        server_path = yatest.common.binary_path("extsearch/wizards/yabs/yabs")
        config_file = yatest.common.source_path("extsearch/wizards/yabs/config.cfg")
        if self.yabs_port != 0:
            with tempfile.NamedTemporaryFile(delete=False, mode='w', suffix="config.cfg") as tmp_config_file:
                tmp_config_file_name = tmp_config_file.name
                for line in open(config_file):
                    line = line.replace("yabs.yandex.ru", "localhost:{}".format(self.yabs_port))
                    line = line.replace("an.yandex.ru", "localhost:{}".format(self.yabs_port))
                    line = line.replace("yandex.ru", "localhost:{}".format(self.yabs_port))
                    if "Host:" in line:
                        line = "    Host: localhost\n"
                    tmp_config_file.write(line)
                    tmp_config_file.flush()
            config_file = tmp_config_file_name

        sys.stderr.write("config_file: {}\n".format(config_file))

        self.server = yatest.common.execute((
            server_path,
            '-V', "Port={}".format(self.port),
            config_file
        ), wait=False)

        has_started = False
        for i in xrange(10):
            try:
                req = requests.get(self.get_status_url())
                if req.status_code != 200:
                    raise RuntimeError("Server /stats returned status_code != 200")
            except requests.ConnectionError:
                time.sleep(2)
                continue

            has_started = True
            break

        if not has_started:
            raise RuntimeError("Server start has timed out")

    def __enter__(self):
        self.host = "http://localhost:{}".format(self.port)
        if not self.is_external_server:
            try:
                self.start_server()
            except:
                # In case if exception thrown before the creation of the server
                if hasattr(self, "server"):
                    self.server.kill()
                raise
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if not self.is_external_server:
            self.server.kill()

    def get_status_url(self):
        return self.host + "/yandsearch?info=getconfig"

    def get_url(self, url_path):
        return self.host + url_path


def run_test_server_status(port, yabs_port=0, is_external_server=False):
    with YabsProxyServer(port, yabs_port=yabs_port, is_external_server=is_external_server) as server:
        sys.stderr.write("status_url: {}\n".format(server.get_status_url()))
        req = requests.get(server.get_status_url())
        assert req.status_code == 200


def test_server_status():
    from yatest.common.network import PortManager

    with PortManager() as port_manager:
        run_test_server_status(port_manager.get_port(), port_manager.get_port())


def json_replace_item(obj, keys):
    if isinstance(obj, dict):
        for k in obj:
            if k in keys:
                obj[k] = None
            else:
                json_replace_item(obj[k], keys)
    if isinstance(obj, list):
        for o in obj:
            json_replace_item(o, keys)


def prepare_result(req):
    req_json = json.loads(req.text)

    result = {}
    if "Grouping" in req_json:
        result["Grouping"] = req_json["Grouping"]
    json_replace_item(req_json, ["Mtime"])

    # json_replace_item(req_json, ["Mtime", "Head", "DebugInfo", "ErrorInfo"])
    if "SearcherProp" in req_json:
        for p in req_json["SearcherProp"]:
            if p["Key"] in ["AppHostMode", "YabsDirectZenUrl", "YabsDirectZenSecondUrl", "YabsProxyUrl", "BannerExtraUrl"]:
                result[p["Key"]] = p["Value"]

    return json.dumps(result, indent=4)


def run_test_server_request(port, url_path, yabs_port=0, is_external_server=False):
    with YabsProxyServer(port, yabs_port=yabs_port, is_external_server=is_external_server) as server:
        req = requests.get(server.get_url(url_path))
        return prepare_result(req)


def do_server_request(url_path, answer):
    from yatest.common.network import PortManager

    with PortManager() as port_manager:
        yabs_port = port_manager.get_port()
        sys.stderr.write("yabs_port: {}\n".format(yabs_port))
        httpd = HTTPServer(("localhost", yabs_port), YabsServerFactory(answer))

        def serve():
            sys.stderr.write("server daemon - starting server\n")
            httpd.serve_forever()
            sys.stderr.write("server daemon - server stopped\n")

        thread_type = threading.Thread(target=serve)
        thread_type.start()

        sys.stderr.write("check server status\n")
        while True:
            req = requests.get("http://localhost:{}/status".format(yabs_port))
            if req.status_code == 200:
                break
        sys.stderr.write("server status ok\n")

        proxy_port = port_manager.get_port()
        sys.stderr.write('proxy_port: {}\n'.format(proxy_port))
        result = run_test_server_request(proxy_port, url_path, yabs_port=yabs_port)
        httpd.shutdown()
        return result


def test_morda_smart_request():
    import yatest.common
    req_path = yatest.common.runtime.work_path("yabs/morda_smart_request.txt")
    req_json = json.load(open(req_path))
    return do_server_request(req_json["request"], req_json["answer"])


# def test_vcards_vhome_request():
#     import yatest.common
#     req_path = yatest.common.runtime.work_path("yabs/vcards_vhome_request.txt")
#     req_json = json.load(open(req_path))
#     return do_server_request(req_json["request"], req_json["answer"])


def test_direct_request():
    import yatest.common
    req_path = yatest.common.runtime.work_path("yabs/direct_request.txt")
    req_json = json.load(open(req_path))
    return do_server_request(req_json["request"], req_json["answer"])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--port', dest='port', required=True,
        help="External server port on localhost:<port>"
    )

    args = parser.parse_args()

    run_test_server_status(args.port, is_external_server=True)


if __name__ == "__main__":
    main()
