from search.wizard.entitysearch.py.entref import EntRef


def test_encode():
    assert EntRef().decode("0oCglydXcyMjcyNjLpiG2m").to_dict() == {"ObjectId": "ruw227262"}


def test_decode():
    assert EntRef().from_dict({"ObjectId": "ruw227262"}).encode() == "0oCglydXcyMjcyNjLpiG2m"


def test_kwargs_decode():
    assert EntRef.from_kwargs(ObjectId="ruw227262").encode() == "0oCglydXcyMjcyNjLpiG2m"


def test_type_of_encoded_value():
    assert isinstance(EntRef.from_kwargs(ObjectId="ruw227262").encode(), str)
