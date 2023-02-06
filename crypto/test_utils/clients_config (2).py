from crypta.styx.services.api.lib.config.clients.permissions_pb2 import TPermissions
from crypta.styx.services.api.lib.config.clients.clients_config_pb2 import TClientsConfig


def _get_full_permissions():
    ret = TPermissions()
    ret.StatusAllowed = True
    ret.DeleteAllowed = True
    ret.PingAllowed = True
    ret.VersionAllowed = True
    return ret


class ClientsConfig(object):
    def __init__(self, tvm_ids):
        self.proto = TClientsConfig()
        self.proto.Anonymous.Name = "anonymous"
        self.proto.Anonymous.Permissions.CopyFrom(_get_full_permissions())

        client = self.proto.Clients[tvm_ids.full_permissions]
        client.Name = "full_permissions"
        client.Permissions.CopyFrom(_get_full_permissions())

        client = self.proto.Clients[tvm_ids.no_permissions]
        client.Name = "no_permissions"

        client = self.proto.Clients[tvm_ids.status_only]
        client.Name = "status_only"
        client.Permissions.StatusAllowed = True

        client = self.proto.Clients[tvm_ids.full_except_status]
        client.Name = "full_except_status"
        client.Permissions.CopyFrom(_get_full_permissions())
        client.Permissions.StatusAllowed = False

        client = self.proto.Clients[tvm_ids.delete_only]
        client.Name = "delete_only"
        client.Permissions.DeleteAllowed = True

        client = self.proto.Clients[tvm_ids.full_except_delete]
        client.Name = "full_except_delete"
        client.Permissions.CopyFrom(_get_full_permissions())
        client.Permissions.DeleteAllowed = False

        client = self.proto.Clients[tvm_ids.version_only]
        client.Name = "version_only"
        client.Permissions.VersionAllowed = True

        client = self.proto.Clients[tvm_ids.full_except_version]
        client.Name = "full_except_version"
        client.Permissions.CopyFrom(_get_full_permissions())
        client.Permissions.VersionAllowed = False

        client = self.proto.Clients[tvm_ids.ping_only]
        client.Name = "ping_only"
        client.Permissions.PingAllowed = True

        client = self.proto.Clients[tvm_ids.full_except_ping]
        client.Name = "full_except_ping"
        client.Permissions.CopyFrom(_get_full_permissions())
        client.Permissions.PingAllowed = False
