cdef extern from "search/base_search/daemons/inverted_index_storage/tests/shard_util/shard_util.h":
    void CreateShard(object, const char*);
    object DeserializeHits(const char*, size_t);

def create_shard(description, out_dir):
    if isinstance(out_dir, unicode):
        out_dir = out_dir.encode('utf-8')
    CreateShard(description, out_dir)

def deserialize_hits(serialized):
    return DeserializeHits(serialized, len(serialized))
