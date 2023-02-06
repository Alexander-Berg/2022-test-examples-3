import argparse
import yaml
import logging.config
from search.mon.tester.src.lib.config import Config


LOG_CONFIG = {
    'version': 1,
    'formatters': {
        'default': {
            'format': '[{asctime}] [{module} -> {pathname}:{lineno}] ({process} -> {thread}) [{levelname}] >> {'
                      'message}',
            'datefmt': '%Y-%m-%d %H:%M:%S',
            'style': '{',
        }
    },
    'handlers': {
        'default_file': {
            'level': 'DEBUG',
            'class': 'logging.handlers.WatchedFileHandler',
            'formatter': 'default',
            'filename': './app.log'
        }
    },
    'loggers': {
        'root': {
            'handlers': ['default_file'],
            'level': 'DEBUG',
        },
        'search': {
            'handlers': ['default_file'],
            'level': 'DEBUG',
        },
        'search.mon.iconostasis_validator': {
            'handlers': ['default_file'],
            'level': 'INFO',
            'propagate': False,
        },
    },
}


def arg_init():
    global PORT
    global DEBUG_PARAM

    parser = argparse.ArgumentParser()
    parser.add_argument('--port', action='store', type=int, help='application port', default=7000)
    parser.add_argument('--debug', action='store_true', help='debug parameters, set for debug,skip for false')
    parser.add_argument('--log_config', type=argparse.FileType('r', encoding='UTF-8'), dest='log_config')
    parser.add_argument('--config', '--tickets-ages-config', type=argparse.FileType('r', encoding='UTF-8'), dest='config')
    args, unknown = parser.parse_known_args()

    if args.log_config:
        log_config = yaml.load(args.log_config.read(), Loader=yaml.Loader)
        logging.config.dictConfig(log_config)
    else:
        logging.config.dictConfig(LOG_CONFIG)

    if args.config:
        config = Config.from_file(args.config)
        args.config.close()
    else:
        config = Config()

    logger = logging.getLogger(__name__)
    if 60000 > args.port > 1024:
        PORT = args.port
    else:
        logger.error('Wrong port, use range(1024-60000), set to default 7000')
        PORT = 7000

    DEBUG_PARAM = args.debug
    logger.error(f'Port is {PORT}, debug is {DEBUG_PARAM}')

    return PORT, DEBUG_PARAM, config
