#!/skynet/python/bin/python

import os
import sys
import api.kqueue
from kernel.util.errors import formatException


class RunOnHosts:
    def __init__(self):
        pass

    def run(self):
#        raise Exception, "Testing exception"
        return True


if __name__ == '__main__':
    timeout = sys.argv[1]
    hosts = sys.argv[2:]

    client = api.kqueue.Client('c')
    skynet_iterator = client.run(hosts, RunOnHosts()).wait(200)

    success_hosts = []
    failure_hosts = []
    for host, result, failure in skynet_iterator:
        if failure != None:
            print "Host %s Error message <%s>" % (host, formatException(failure))
            failure_hosts.append(host)
        else:
            success_hosts.append(host)

    print "%d success hosts, %d failure hosts, %d timeout hosts" % (len(success_hosts), len(failure_hosts), len(hosts) - len(success_hosts) - len(failure_hosts))



