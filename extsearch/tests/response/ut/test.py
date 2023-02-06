import pytest
from google.protobuf import text_format
import yatest.common
import yandex.maps.proto.common2.response_pb2 as pb_response
from extsearch.geo.meta.tests.response.pb_response_wrapper import PbSearchResult


def test_business_pb():
    with open(yatest.common.test_source_path('data/1679543455.pb.txt')) as fd:
        blob = text_format.Parse(fd.read(), pb_response.Response()).SerializeToString()

    r = PbSearchResult(blob)
    assert r.is_non_empty()
    assert r.is_business_result()
    assert r.doc_count == 1
    assert r.first_doc.name == 'Лига джентльменов'
    assert r.first_doc.permalinks() == ['1679543455', '59717497794', '148150514054']
    assert r.unpack_context().AbsThresh == pytest.approx(0.05)
