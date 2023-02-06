from flask import Flask, Response, request, jsonify
from conf import Config
from cert_manager import SSLCertManager, SSLCertCache
from util import init_root_logger
from waitress import serve
from library.python import resource


context = None
app = Flask(__name__)
app.config['DEBUG'] = False


@app.route('/sign')
def sign():
    host = request.args.get('host', '')
    if not host:
        return jsonify(error='host is empty')
    global context
    certfile, privkeyfile = context.get_host_certificate(host)
    return Response('{}{}'.format(open(certfile).read(), open(privkeyfile).read()), mimetype='text/plain')


@app.route('/ping')
def ping():
    return Response('OK')


if __name__ == '__main__':
    init_root_logger()
    config = Config()
    ca_cert = resource.find("/fake_ca/cert.pem")
    ca_privkey = resource.find("/fake_ca/privkey.pem")
    open(config.casvc.cert_file, 'w').write(ca_cert)
    cache = SSLCertCache(config.casvc.cache_dir)
    context = SSLCertManager(ca_cert, ca_privkey, cache)
    serve(app, host='::1', port=config.casvc.server_port, threads=1)
