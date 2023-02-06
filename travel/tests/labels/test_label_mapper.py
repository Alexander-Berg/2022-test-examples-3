from travel.cpa.data_processing.lib.label_mapper import HotelsToGenericLabelMapper, TrainToGenericLabelMapper
# noinspection PyUnresolvedReferences
import travel.hotels.proto2.label_pb2 as label_hotels_pb2
# noinspection PyUnresolvedReferences
import travel.proto.trains.label_params_pb2 as label_trains_pb2


def test_hotels_to_generic_mapper():
    src = label_hotels_pb2.TLabel()

    # name mapping
    src.Source = 'utm_source'
    # auto mapping
    src.ICookie = 'icookie'
    # callable mapping
    src.IntTestIds.extend([1, 2, 3])
    src.IntTestBuckets.extend([4, 5, 6])

    mapper = HotelsToGenericLabelMapper()
    dst = mapper.get_mapped_proto(src)

    assert 'utm_source' == dst.UtmSource
    assert 'icookie' == dst.ICookie
    assert [1, 2, 3] == dst.TestIds
    assert [4, 5, 6] == dst.TestBuckets
    assert '1,0,4;2,0,5;3,0,6' == dst.ExpBoxes


def test_train_to_generic_mapper():
    src = label_trains_pb2.TLabelParams()

    # auto mapping
    src.ICookie = 'icookie'
    # callable mapping
    src.TestBuckets = '1,0,4;2,0,5;3,0,6'

    mapper = TrainToGenericLabelMapper()
    dst = mapper.get_mapped_proto(src)

    assert '1,0,4;2,0,5;3,0,6' == dst.ExpBoxes
    assert [1, 2, 3] == dst.TestIds
    assert [4, 5, 6] == dst.TestBuckets
