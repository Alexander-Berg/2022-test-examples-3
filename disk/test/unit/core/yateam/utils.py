# -*- coding: utf-8 -*-

import os
from test.unit.base import NoDBTestCase

from mpfs.core.yateam.logic import is_yateam_subtree, YATEAM_DIR_PATH


class IsYaTeamSubtreeTestCase(NoDBTestCase):

    @staticmethod
    def test_root_dir_success():
        assert is_yateam_subtree(YATEAM_DIR_PATH)

    @staticmethod
    def test_subtree_path_success():
        assert is_yateam_subtree(os.path.join(YATEAM_DIR_PATH, 'abc'))

    @staticmethod
    def test_dir_name_has_successive_chars_after_root():
        assert not is_yateam_subtree(YATEAM_DIR_PATH + 'abc')
