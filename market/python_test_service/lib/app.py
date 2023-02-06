# coding: utf-8

import argparse
import logging
import os
import socket
import time

from library.python.svn_version import svn_revision
import gunicorn.app.base
import flask

from lib.settings import Settings
import lib.state


BLUEPRINTS = [
    'ping',
]

logger = logging.getLogger()


def register_blueprints(app, blueprints):
    for name in blueprints:
        fromlist = 'lib.blueprints'
        module_name = '.'.join([fromlist, name])
        logger.debug('register {}'.format(module_name))
        module = __import__(module_name, fromlist=fromlist)
        app.register_blueprint(getattr(module, 'page'))


class FlaskApp(flask.Flask):
    def __init__(self, settings):
        super(FlaskApp, self).__init__('python_app')
        self.settings = settings

    @property
    def state(self):
        return lib.state.State(self.settings.statefile)


def create_flask_app(settings):
    app = FlaskApp(settings)
    app.version = svn_revision()

    register_blueprints(app, BLUEPRINTS)
    fqdn = os.getenv('NODE_NAME', socket.getfqdn())

    @app.before_request
    def before():
        try:
            flask.request.start_time = time.time()
        except Exception as e:
            logger.exception(e)

    @app.after_request
    def after(response):
        try:
            response.headers['X-HOST'] = fqdn
            stime_in_ms = str(int(1000 * (time.time() - flask.request.start_time)))
            response.headers['X-TIME'] = stime_in_ms
        except Exception as e:
            logger.exception(e)
        return response

    return app


class GunicornApp(gunicorn.app.base.BaseApplication):
    __slots__ = ('flask_app', 'host', 'port', 'workers', 'errorlog', 'accesslog')

    def __init__(self, flask_app, host, port, workers, errorlog=None, accesslog=None):
        self.flask_app = flask_app
        self.host = host
        self.port = port
        self.workers = workers
        self.errorlog = errorlog
        self.accesslog = accesslog
        super(GunicornApp, self).__init__()

    def load_config(self):
        self.cfg.set('workers', self.workers)
        self.cfg.set('bind', '{}:{}'.format(self.host, self.port))
        # if self.errorlog is not None:
        #     self.cfg.set('errorlog', self.errorlog)
        self.cfg.set('errorlog', None)
        if self.accesslog is not None:
            self.cfg.set('accesslog', self.accesslog)
        self.cfg.set('worker_class', 'gunicorn.workers.ggevent.GeventWorker')

    def load(self):
        return self.flask_app


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', '-H', dest='host', default='[::1]')
    parser.add_argument('-p', '--port', type=int, required=True)
    parser.add_argument('-w', '--listen-threads', type=int, default=1)  # multiprocessing.cpu_count())
    parser.add_argument('-l', '--log-path')
    parser.add_argument('-s', '--statefile', default='./state.json')
    parser.add_argument('--env')
    parser.add_argument('--dc')
    parser.add_argument('--debug', action='store_true')
    parser.add_argument('--root-path')
    parser.add_argument('--config')
    args = parser.parse_args()

    if args.host == "::":
        args.host = "[::]"

    handlers = [logging.StreamHandler()]
    if args.log_path:
        open(args.log_path, 'a').close()
        handlers.append(logging.FileHandler(args.log_path))
    formatter = logging.Formatter('%(asctime)s %(process)6d | %(levelname)-5s | %(message)s', datefmt='%Y-%m-%d %H:%M:%S')
    for handler in handlers:
        handler.setFormatter(formatter)
        logger.addHandler(handler)
        logging.getLogger('gunicorn.error').addHandler(handler)
        logger.setLevel(logging.DEBUG)
    logger.info('starting')

    accesslog = None
    if args.debug:
        accesslog = '-'

    if os.path.exists(args.statefile):
        os.remove(args.statefile)

    for key, val in sorted(args.__dict__.iteritems()):
        logger.debug('args {}: {}'.format(key, val))
    logger.info('http://localhost:{}/ping'.format(args.port))

    try:
        settings = Settings(args)
        flask_app = create_flask_app(settings)
        app = GunicornApp(
            flask_app,
            host=args.host,
            port=args.port,
            workers=args.listen_threads,
            errorlog=args.log_path,
            accesslog=accesslog,
        )
        app.run()
    except Exception as e:
        logger.exception(e)
        raise
