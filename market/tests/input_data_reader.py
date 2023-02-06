import pytest
import tempfile
import os
import random

from market.yamarec.yamarec.bin.yamarec_dt_uploader.uploader import InputDataReader


@pytest.yield_fixture(scope="module")
def input_file():
    fd, file_path = tempfile.mkstemp(dir="/tmp", text=True)
    os.close(fd)
    with open(file_path, "w") as f:
        for index in xrange(100):
            data = [str(random.randint(0, 9)) for _ in xrange(1020)]
            f.write("%s:%s\n" % (str(index).zfill(3), "".join(data)))
        f.write("  \n")
    try:
        yield file_path
    finally:
        os.remove(file_path)


def test_get_all_chunks(input_file):
    """ Check sequential reading of all chunks. """
    reader = InputDataReader(input_file, 1024, 1)
    assert reader.has_unlocked_chunks()
    for x in range(100):
        chunk = reader.get_chunk()
        assert x == chunk.id
        assert 1024 == chunk.size
        reader.unlock_chunk(chunk.id, release=True)
    assert reader.get_chunk().id == -1
    assert not reader.has_unlocked_chunks()


def test_get_chunk_size(input_file):
    """ Check sequential reading of all chunks with bigger chunk size. """
    reader = InputDataReader(input_file, 1024 * 4 + 10, 80000)
    assert reader.has_unlocked_chunks()
    for x in range(20):
        chunk = reader.get_chunk()
        assert x == chunk.id
        assert 1024 * 5 == chunk.size
        reader.unlock_chunk(chunk.id, release=True)
    assert reader.get_chunk().id == -1
    assert not reader.has_unlocked_chunks()


def test_get_chunk_size_raw_limit(input_file):
    """ Check sequential reading of all chunks with raw limit. """
    reader = InputDataReader(input_file, 1024 * 10, 5)
    assert reader.has_unlocked_chunks()
    for x in range(20):
        chunk = reader.get_chunk()
        assert x == chunk.id
        assert 1024 * 5 == chunk.size
        reader.unlock_chunk(chunk.id, release=True)
    assert reader.get_chunk().id == -1
    assert not reader.has_unlocked_chunks()


def test_get_lock_unlock(input_file):
    """ Check get chunk with lock/unlock functionality """
    reader = InputDataReader(input_file, 1024 * 50, 80000)
    chunk1 = reader.get_chunk(lock=False)
    chunk2 = reader.get_chunk(lock=False)
    assert chunk1 == chunk2
    chunk3 = reader.get_chunk(lock=True)
    assert chunk1 == chunk3
    assert reader.has_unlocked_chunks()
    chunk4 = reader.get_chunk(lock=True)
    assert chunk1 != chunk4
    assert not reader.has_unlocked_chunks()


def test_lock_unlock(input_file):
    """ Check lock/unlock functionality """
    reader = InputDataReader(input_file, 1024 * 50, 80000)
    chunk1 = reader.get_chunk(lock=False)
    reader.lock_chunk(chunk1.id)
    chunk2 = reader.get_chunk(lock=True)
    assert chunk1 != chunk2
    assert not reader.has_unlocked_chunks()
    reader.unlock_chunk(chunk1.id)
    assert reader.has_unlocked_chunks()
    chunk3 = reader.get_chunk(lock=True)
    assert chunk1 == chunk3
    assert not reader.has_unlocked_chunks()


def test_release_chunk(input_file):
    """ Test of releasing chunk """
    reader = InputDataReader(input_file, 1024 * 50, 80000)
    chunk1 = reader.get_chunk(lock=False)
    reader.lock_chunk(chunk1.id)
    chunk2 = reader.get_chunk(lock=True)
    assert chunk1 != chunk2
    assert not reader.has_unlocked_chunks()
    reader.unlock_chunk(chunk1.id, release=True)
    assert not reader.has_unlocked_chunks()
    chunk3 = reader.get_chunk(lock=True)
    assert chunk3.id == -1
