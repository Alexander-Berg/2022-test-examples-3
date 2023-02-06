import ticket_parser2.api.v1 as tp2
import yatest

from mail.furita.tests.common.utils.env import get_env_type

X_YA_SERVICE_TICKET = 'X-Ya-Service-Ticket'
X_YA_USER_TICKET = 'X-Ya-User-Ticket'

_FURITA_TVM_ID = {'bigml': 2016039, 'corp': 2016041}

_SERVICES = {
    'bigml': {
        'akita': 2000430,
        'mops': 2000571,
        'sendbernar': 2000435,
        'wmi': 2000499
    },
    'corp': {
        'akita': 2000428,
        'mops': 2000575,
        'sendbernar': 2000434,
        'wmi': 2000500
    }
}


def _get_tvm_secret():
    secret_name = {'bigml': 'tvm_secret', 'corp': 'tvm_secret_corp'}
    secret_path = yatest.common.work_path(secret_name[get_env_type()])
    f = open(secret_path, 'r')
    return f.read()


def get_service_ticket(destination):
    env = get_env_type()
    src_id = _FURITA_TVM_ID[env]
    settings = tp2.TvmApiClientSettings(
        self_client_id=src_id,
        self_secret=_get_tvm_secret(),
        dsts=_SERVICES[env]
    )
    cli = tp2.TvmClient(settings)

    return cli.get_service_ticket_for(destination)
