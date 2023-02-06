# -*- coding: utf-8 -*-
import unittest

from mpfs.core.global_gallery.dao.source_id import SourceIdDAOItem
from mpfs.dao.base import BulkDeleteReqGenerator
from mpfs.metastorage.postgres.schema import source_ids


class BulkReqDeleteGeneratorTestCase(unittest.TestCase):

    def test_bulk_req_generator(self):
        items = [
            SourceIdDAOItem.build_by_params('123', '277f047191480c224b7585ed354ed2a9', '1234', False),
            SourceIdDAOItem.build_by_params('123', '277f047191480c224b7585ed354ed2b9', '1234', False),
            SourceIdDAOItem.build_by_params('123', '277f047191480c224b7585ed354ed2c9', '1234', True),
        ]
        columns = ['uid', 'source_id', 'storage_id', 'is_live_photo']
        bdrg = BulkDeleteReqGenerator(source_ids, items, columns)
        print bdrg.generate_tmpl()

        correct_request = (
                "DELETE FROM disk.source_ids\n" +
                "WHERE (is_live_photo = :is_live_photo_0 AND source_id = :source_id_0 AND storage_id = :storage_id_0 AND uid = :uid_0) OR\n" +
                "(is_live_photo = :is_live_photo_1 AND source_id = :source_id_1 AND storage_id = :storage_id_1 AND uid = :uid_1) OR\n" +
                "(is_live_photo = :is_live_photo_2 AND source_id = :source_id_2 AND storage_id = :storage_id_2 AND uid = :uid_2)"
        )

        assert bdrg.generate_tmpl() == correct_request
