# -*- coding: utf-8 -*-
import commands

import pytest
import re
import os


@pytest.mark.long
def test_cmd(test_dir):
    status, output = commands.getstatusoutput(os.path.join(test_dir, 'commandline', 'scripts', 'tests.sh'))
    assert len(re.findall(":OK", output)) == 14, "Some of cmd tests failed: {}".format(output)

