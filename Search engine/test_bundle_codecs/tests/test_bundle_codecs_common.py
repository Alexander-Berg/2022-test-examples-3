import logging
import pytest
import yatest.common
import os
import sys

bin_path = yatest.common.binary_path("search/lingboost/saas/codecs/test_bundle_codecs/test_bundle_codecs")

input_basic_bundles = "./test_bundles.tsv"
input_reg_bundles = "./test_reg_bundles.tsv"

class TBundleCodecChecker:
    def __init__(self, codec, num, bundles_path = input_basic_bundles):
        self.codec = codec
        self.num = num
        self.bundles_path = bundles_path

    def run(self):
        cmd = "head -n %(num)d %(bundles)s | %(binary)s -c %(codec)s >output_%(codec)s.tsv" \
            % {'binary' : bin_path, 'codec' : self.codec, 'num' : self.num, 'bundles' : self.bundles_path}

        sys.stderr.write("Execute: {}\n".format(cmd))
        os.system(cmd)

        num_lines = 0
        for line in open("output_%(codec)s.tsv" % {'codec' : self.codec}):
            num_lines += 1

        assert 0 == num_lines, "codec $(codec)s failed on %(num)d bundles" \
            % {'codec' : self.codec, 'num' : num_lines / 2}

