import json
import os

import six
import tvmauth
import yatest

from crypta.lib.python.tvm import helpers


class TvmApiRecipe(object):
    secrets_path = "library/recipes/tvmapi/clients/clients.json"

    def __init__(self):
        self.host = "localhost"

        with open("./tvmapi.port") as f:
            self.port = int(f.read())

        with open(yatest.common.source_path(self.secrets_path)) as f:
            self.secrets = {k: v["secret"] for k, v in six.iteritems(json.load(f))}

        self.all_ids = set(int(x) for x in six.iterkeys(self.secrets))
        self.free_ids = set(self.all_ids)

        self.clients = {}

    def __enter__(self):
        os.environ[helpers.TVM_TEST_PORT_ENV_VAR] = str(self.port)
        return self

    def __exit__(self, *args):
        del os.environ[helpers.TVM_TEST_PORT_ENV_VAR]
        self.stop()

    def stop(self):
        for client in six.itervalues(self.clients):
            client.stop()

    def get_secret(self, tvm_id):
        return self.secrets[str(tvm_id)]

    @property
    def address(self):
        return "http://{}:{}".format(self.host, self.port)

    def issue_id(self):
        return self.free_ids.pop()

    def get_service_ticket_headers(self, tvm_src_id, tvm_dst_id):
        return helpers.get_tvm_headers(self.get_service_ticket(tvm_src_id, tvm_dst_id))

    def get_service_ticket(self, tvm_src_id, tvm_dst_id):
        client = self.get_tvm_client(tvm_src_id)
        return client.get_service_ticket_for(tvm_id=tvm_dst_id)

    def check_service_ticket(self, tvm_dst_id, service_ticket):
        client = self.get_tvm_client(tvm_dst_id)
        return client.check_service_ticket(service_ticket)

    def get_tvm_client(self, tvm_src_id):
        if tvm_src_id not in self.clients:
            settings = tvmauth.TvmApiClientSettings(
                self_tvm_id=tvm_src_id,
                self_secret=self.secrets[str(tvm_src_id)],
                dsts=list(self.all_ids),
                enable_service_ticket_checking=True,
                enable_user_ticket_checking=True,
                localhost_port=self.port,
            )

            self.clients[tvm_src_id] = tvmauth.TvmClient(settings)

        return self.clients[tvm_src_id]
