from conf import Config
from sys import argv,exit
from time import time
from os.path import exists
import logging


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    svc = 'unknown'
    res = 1
    try:
        svc = argv[1]
        config = Config()
        item = getattr(config, svc)
        res = not (exists(item.hb_file) and int(open(item.hb_file).read()) + item.hb_timeout >= int(time()))
    except:
        pass
    logging.info('Service [{}] {}'.format(svc, 'OK' if not res else 'failed'))
    exit(res)
