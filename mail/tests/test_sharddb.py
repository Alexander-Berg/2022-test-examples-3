from mail.devpack.lib.components.sharddb import ShardDb


def test_sharddb(coordinator):
    sharddb = coordinator.components[ShardDb]
    sharddb.execute("insert into shards.shards (shard_id, name) values(123, 'shard123');")
    result = sharddb.query("select shard_id, name from shards.shards where shard_id = 123;")
    assert result == [(123, 'shard123')]
