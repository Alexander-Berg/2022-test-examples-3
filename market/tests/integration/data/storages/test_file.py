from yamarec1.beans import ytc
from yamarec1.data.codecs import DSVCodec
from yamarec1.data.storages import File


def test_file_can_store_iterable_data(random_yt_path, iterable_data):
    path = random_yt_path
    storage = File(path=path, codec=DSVCodec())
    storage.store(iterable_data)
    assert ytc.exists(path)
    assert list(storage.data) == list(iterable_data)


def test_file_can_store_streamable_data_without_decoding(random_yt_path, streamable_data):
    path = random_yt_path
    storage = File(path=path, codec=DSVCodec())
    streamable_data.__iter__ = NotImplemented
    storage.store(streamable_data)
    assert ytc.exists(path)
    assert len(list(storage.data)) == 2
