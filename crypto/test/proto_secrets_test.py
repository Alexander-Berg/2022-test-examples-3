from crypta.lib.python.proto_secrets import proto_secrets
from crypta.lib.native.proto_secrets.test.proto.secrets_pb2 import TUser


def test_get_copy_without_secrets():
    user = TUser()
    user.Login = "login"
    user.Password = "password"
    user.Description = "description"

    return str(proto_secrets.get_copy_without_secrets(user))
