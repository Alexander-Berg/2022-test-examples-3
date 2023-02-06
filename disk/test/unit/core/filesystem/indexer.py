# -*- coding: utf-8 -*-

from mpfs.core.filesystem.indexer import DiskDataIndexer
from mpfs.core.user.constants import PHOTOUNLIM_AREA_PATH


def test_notes_not_in_get_search_reindex_supported_areas():
    assert '/notes' not in DiskDataIndexer.get_search_reindex_supported_areas()


def test_photounlim_in_get_search_reindex_supported_areas():
    assert PHOTOUNLIM_AREA_PATH in DiskDataIndexer.get_search_reindex_supported_areas()
