from crypta.siberia.bin.core.lib.configs.proto.clients.clients_config_pb2 import TClientsConfig


class ClientsConfig(object):
    def __init__(self, tvm_ids):
        self.proto = TClientsConfig()
        self.proto.Anonymous.Name = "anonymous"
        self.proto.Anonymous.Permissions.Allowed = True

        client = self.proto.Clients[tvm_ids.full_permissions]
        client.Name = "full_permissions"
        client.Permissions.Allowed = True

        client = self.proto.Clients[tvm_ids.version_only]
        client.Name = "version_only"
        client.Permissions.Allowed = False
