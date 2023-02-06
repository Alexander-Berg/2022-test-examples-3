import pytest

from crypta.graph.soup.config.python import (ID_TYPE, SOURCE_TYPE, LOG_SOURCE, EDGE_TYPE)

import crypta.graph.soup.config.proto.source_type_pb2 as source_type_proto
import crypta.graph.soup.config.proto.log_source_pb2 as log_source_proto
import crypta.lib.proto.identifiers.id_type_pb2 as id_type_proto


class TestParseYuidWithEal(object):

    def test_enums(self):
        assert ID_TYPE.YANDEXUID.Name == 'yandexuid'
        assert ID_TYPE.YANDEXUID.Type == id_type_proto.YANDEXUID

        assert SOURCE_TYPE.PROBABILISTIC2.Name == 'fuzzy2'
        assert SOURCE_TYPE.PROBABILISTIC2.Type == source_type_proto.PROBABILISTIC2

        assert LOG_SOURCE.WATCH_LOG.Name == 'wl'
        assert LOG_SOURCE.WATCH_LOG.Type == log_source_proto.WATCH_LOG

    def test_deserialize(self):
        assert len(EDGE_TYPE.values()) > 0

    @pytest.mark.parametrize('id1_type, id2_type, source_type', (
        (ID_TYPE.EMAIL, ID_TYPE.EMAIL_MD5, SOURCE_TYPE.MD5_HASH),
        (ID_TYPE.EMAIL, ID_TYPE.EMAIL_SHA256, SOURCE_TYPE.SHA256_HASH),
        (ID_TYPE.PHONE, ID_TYPE.PHONE_MD5, SOURCE_TYPE.MD5_HASH),
        (ID_TYPE.PHONE, ID_TYPE.PHONE_SHA256, SOURCE_TYPE.SHA256_HASH),
    ))
    def test_edges(self, id1_type, id2_type, source_type):
        edge = EDGE_TYPE.get_edge_type(id1_type, id2_type, source_type, LOG_SOURCE.SOUP_PREPROCESSING)
        assert edge.Props.EdgeStrength == edge.Props.ARTIFICIAL

    def test_more_edges(self):
        yuid_edges = [e for e in EDGE_TYPE.values() if e.Id1Type == ID_TYPE.YANDEXUID]
        assert len(yuid_edges) > 0

    @pytest.mark.parametrize('enum, ignored', (
        (ID_TYPE, {'DEFAULT': 0, 'MD5': -1, 'SHA256': -2, 'IDFA_GAID': -3, 'DIRECT_CLIENT_ID': -4, }),
        (SOURCE_TYPE, {'DEFAULT': 0, }),
        (LOG_SOURCE, {'DEFAULT': 0, }),
    ))
    def test_no_syntetic_fields(self, enum, ignored):
        """
        Should check is ProtoEnum ignore fields without additional props
        common case - ignore IdTypes DEFAULT, UNKNOWN, MD5, SHA256, etc
        """
        assert enum._ignored == ignored
        for value in enum.values():
            assert value.Name not in ignored
            assert value.Type > 0
        assert len(enum.values()) > 0

    def test_edge_usage(self):
        edge = EDGE_TYPE.get_edge_type(
            ID_TYPE.YANDEXUID,
            ID_TYPE.UUID,
            SOURCE_TYPE.SEARCH_APP_MOB_REPORT,
            LOG_SOURCE.MOBILE_REPORT_LOG)

        assert (not edge.Usage.SoupUpdate)
        assert edge.Usage.HumanMatching
        assert edge.Usage.YandexSafe
        assert edge.Usage.SoupyIndevice
