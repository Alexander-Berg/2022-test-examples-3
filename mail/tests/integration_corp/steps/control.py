import xmlrpc.client as xmlrpclib

DOBBY_HOSTS = [
    'doberman',
    'doberman2',
]


def get_supervisor(doberman_host):
    server = xmlrpclib.Server('http://%s:9001/RPC2' % doberman_host)
    return server.supervisor


def get_doberman_info_from_supervisor(doberman_host):
    return get_supervisor(doberman_host).getProcessInfo('doberman')


def start_doberman_by_supervisor(doberman_host):
    if not get_supervisor(doberman_host).startProcess('doberman'):
        raise RuntimeError('Doberman fail while start')


def stop_doberman_by_supervisor(doberman_host):
    if not get_supervisor(doberman_host).stopProcess('doberman'):
        raise RuntimeError('Doberman fail while stop')


def start_dobermans():
    for doberman_host in DOBBY_HOSTS:
        start_doberman_by_supervisor(doberman_host)


def dobermans_should_be_stopped():
    def impl():
        for doberman_host in DOBBY_HOSTS:
            state = get_doberman_info_from_supervisor(doberman_host)['statename']
            if state == 'STOPPED':
                yield doberman_host
            else:
                stop_doberman_by_supervisor(doberman_host)
                yield doberman_host, state
    return list(impl())
