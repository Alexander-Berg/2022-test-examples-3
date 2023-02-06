import datetime

import iso8601


def test_storage_can_put_records(storage):
    storage.put("my key", "my value")
    timestamp, value = storage.get("my key")[0]
    now = datetime.datetime.utcnow().replace(tzinfo=iso8601.iso8601.UTC)
    assert abs(now - timestamp) < datetime.timedelta(seconds=1)
    assert value == "my value"


def test_storage_can_get_records(storage):
    storage.put("my key #0", "my value #0")
    storage.put("my key #1", "my value #1")
    storage.put("my key #0", "my value #2")
    threshold = storage.get("my key #0")[0][0]
    storage.put("my key #1", "my value #3")
    storage.put("my key #0", "my value #4")
    assert [value for _, value in storage.get("my key #0")] == [
        "my value #4",
        "my value #2",
        "my value #0",
    ]
    assert [value for _, value in storage.get("my key #1")] == [
        "my value #3",
        "my value #1",
    ]
    assert storage.get("my key #2") == []
    assert [value for _, value in storage.get("my key #0", limit=2)] == [
        "my value #4",
        "my value #2",
    ]
    assert [value for _, value in storage.get("my key #1", limit=1)] == [
        "my value #3",
    ]
    assert [value for _, value in storage.get("my key #0", limit=0)] == []
    assert [value for _, value in storage.get("my key #0", since=threshold)] == [
        "my value #4",
    ]
    assert [value for _, value in storage.get("my key #0", since=threshold, limit=1)] == [
        "my value #4",
    ]


def test_storage_can_delete_records(storage):
    then = datetime.datetime.utcnow().replace(tzinfo=iso8601.iso8601.UTC)
    storage.put("my key #0", "my value #0")
    storage.put("my key #1", "my value #1")
    storage.put("my key #0", "my value #2")
    now = datetime.datetime.utcnow().replace(tzinfo=iso8601.iso8601.UTC)
    storage.put("my key #1", "my value #3")
    storage.put("my key #0", "my value #4")
    storage.delete("my key #2")
    storage.delete("my key #0", til=then)
    assert len(storage.gather()) == 5
    storage.delete("my key #1", til=now)
    assert len(storage.gather()) == 4
    storage.delete("my key #0")
    assert len(storage.gather()) == 1
    storage.delete("my key #1")
    assert storage.gather() == []


def test_storage_can_gather_records(storage):
    storage.put("my key #0", "my value #0")
    storage.put("my key #1", "my value #1")
    storage.put("my key #0", "my value #2")
    storage.put("my key #1", "my value #3")
    storage.put("my key #0", "my value #4")
    assert {(key, value) for _, key, value in storage.gather()} == {
        ("my key #0", "my value #0"),
        ("my key #1", "my value #1"),
        ("my key #0", "my value #2"),
        ("my key #1", "my value #3"),
        ("my key #0", "my value #4"),
    }


def test_storage_can_clear_records(storage):
    storage.put("my key #0", "my value #0")
    storage.put("my key #1", "my value #1")
    storage.put("my key #0", "my value #2")
    storage.put("my key #1", "my value #3")
    storage.put("my key #0", "my value #4")
    assert len(storage.gather()) == 5
    storage.clear()
    assert storage.gather() == []
