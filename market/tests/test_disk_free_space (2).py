# -*- coding: utf-8 -*-
from __future__ import absolute_import


import os
import pytest
import checks.disk_free_space as disk_free_space_check

from utils import CLOUD_TYPE_VAR


BSCONFIG_IDIR = "/tmp/"
CUSTOM_PARTITIONS = ["/var/log/yandex"]


@pytest.mark.parametrize(
    argnames="cloud_type,result",
    argvalues=(
        ("nanny", set(["/", BSCONFIG_IDIR, "/logs", "/cores", "/persistent-data"] + CUSTOM_PARTITIONS)),
        ("deploy", set(["/"] + CUSTOM_PARTITIONS)),
    )
)
def test_get_partitions(cloud_type, result):
    os.environ["BSCONFIG_IDIR"] = BSCONFIG_IDIR
    os.environ[CLOUD_TYPE_VAR] = cloud_type

    assert disk_free_space_check.get_partitions(CUSTOM_PARTITIONS) == result
