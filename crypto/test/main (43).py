from crypta.lib.proto.user_data import user_data_pb2
from crypta.lib.python import proto


def test_read_proto_from_dict():
    dictionary = {
        'Attributes': b'\x08\x03\x18\x01(\x02@\x01',
        'Segments': None,
    }

    user_data = proto.read_proto_from_dict(user_data_pb2.TUserData(), dictionary)

    assert not user_data.HasField('Segments')
    assert user_data.Attributes.Age == 3
    assert user_data.Attributes.Gender == 1
    assert user_data.Attributes.Income == 2
    assert user_data.Attributes.HasCryptaID
