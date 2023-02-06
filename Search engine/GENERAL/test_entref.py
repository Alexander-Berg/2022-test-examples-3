from search.wizard.entitysearch.py.entref import EntRef


def test_encode():
    assert EntRef().decode(b"0oCglydXcyMjcyNjLpiG2m").to_dict() == {"ObjectId": "ruw227262"}


def test_decode():
    assert EntRef().from_dict({"ObjectId": "ruw227262"}).encode() == b"0oCglydXcyMjcyNjLpiG2m"


def test_kwargs_decode():
    assert EntRef.from_kwargs(ObjectId="ruw227262").encode() == b"0oCglydXcyMjcyNjLpiG2m"


def test_type_of_encoded_value():
    assert not isinstance(EntRef.from_kwargs(ObjectId="ruw227262").encode(), str)
    assert isinstance(EntRef.from_kwargs(ObjectId="ruw227262").encode(), bytes)
