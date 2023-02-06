import logging
import json
import sys
from os import environ
from socket import gethostname
from conf import SANDBOX_PREFIX
try:
    from urllib.parse import unquote
except ImportError:
    from urllib import unquote


def init_root_logger():
    info_handler = logging.StreamHandler(sys.stdout)
    info_handler.setLevel(logging.INFO)
    info_handler.setFormatter(logging.Formatter('%(asctime)s %(name)s [%(levelname)s] %(message)s'))
    err_handler = logging.StreamHandler(sys.stderr)
    err_handler.setLevel(logging.ERROR)
    logger = logging.getLogger()
    logger.setLevel(logging.INFO)
    logger.addHandler(info_handler)
    logger.addHandler(err_handler)


def get_node_fqdn():
    return environ.get('DEPLOY_POD_PERSISTENT_FQDN', gethostname())


def get_node_dc():
    return environ.get('DEPLOY_NODE_DC', 'unk')


def eval_player_id(url):
    player_id = None
    if not url.startswith(SANDBOX_PREFIX):
        return player_id
    cgi = 'playerData='
    pos = url.find(cgi)
    if pos == -1:
        return player_id
    try:
        player_id = json.loads(unquote(url[pos + len(cgi):]))['PlayerId']
    except:
        pass
    return player_id
