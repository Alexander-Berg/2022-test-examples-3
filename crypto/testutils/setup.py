from crypta.lib.python.yt.dyntables import kv_client
from crypta.utils.rtmr_resource_service.lib.db_client import DbClient
import six


def setup(yt_kv, cluster_envs, public_resources):
    db_client = DbClient(kv_client.make_kv_client(yt_kv.master.yt_client.config["proxy"]["url"], yt_kv.master.path, token="FAKE"))

    db_client.set_public_resources(public_resources)

    for resource, env, version in public_resources:
        db_client.set_latest_resource_version(env, resource, version)

    for dc, env in six.iteritems(cluster_envs):
        db_client.set_env(dc, env)
