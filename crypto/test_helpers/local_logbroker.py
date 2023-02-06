import os


def get_port():
    return int(os.getenv('LOGBROKER_PORT'))
