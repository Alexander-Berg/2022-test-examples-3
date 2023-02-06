from crypta.lib.python.yt.dyntables import(
    kv_schema,
    kv_setup,
)


class YtReplicatedDyntables(object):
    def __init__(self, yt):
        schema = kv_schema.get()
        pivot_keys = kv_schema.create_pivot_keys(1)

        self.yt = yt
        self.yt_client = yt.get_yt_client()
        self.master, replicas = kv_setup.kv_setup(self.yt_client, [(yt.yt_id, self.yt_client)], "//master", "//replica", schema, pivot_keys, sync=True)
        self.replica = replicas[0]
