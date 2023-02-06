# -*- coding: utf-8 -*-

import os

FP_THRESHOLD = float(1e-8)
DT_THRESHOLD_PERCENT = 10


class TestConfig:

    def __init__(self, arc_src_path, arc_tests_data_path):
        self.src_path = arc_src_path
        self.data_path = arc_tests_data_path

    def get_test_data_path(self):
        return os.path.join(self.data_path, "pers", "features_test")

    def get_out_file(self, formula):
        return os.path.join(self.get_test_data_path(), formula + ".out")

    def get_mlfeatures(self):
        return os.path.join(self.src_path, "ysite", "yandex", "pers", "feat", "decl", "mlfeatures_gen.in")

    def get_context(self, name):
        return os.path.join(self.get_test_data_path(), name + ".context.txt")

    def get_rearr_ctx(self):
        return self.get_context("rearr")

    def get_user_ctx(self):
        return self.get_context("user")

    def get_cp_trie(self):
        return os.path.join(self.get_test_data_path(), "clustprofiles.trie")
