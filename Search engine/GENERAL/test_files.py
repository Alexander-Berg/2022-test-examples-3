# coding: utf-8

import os
import magic
import validators

from yatest import common


HOSTS_FILTER_PATH = 'search/runtime_archives/middle_search/resources/rearrange/ungroup_vital/hosts_filter'


def artifacts(func):
    """
    Return all artifacts from project
    """
    def walker():
        # Taking directory inside arcadia
        arcadia_path = common.binary_path(HOSTS_FILTER_PATH)

        # Listing sub path to get artifacts only and skip package files
        artifacts_path = os.path.join(arcadia_path, HOSTS_FILTER_PATH)
        for root, dirs, files in os.walk(artifacts_path, followlinks=True):
            for filename in files:
                func(os.path.join(artifacts_path, filename))

    return walker


@artifacts
def test_file_type(filepath: str):
    """
    Tests file type

    :param filepath: Path to file with hosts
    """
    assert magic.from_file(filepath) == 'ASCII text'


@artifacts
def test_file_content(filepath: str):
    """
    Domain validation test

    :param filepath: Path to file with hosts
    """
    with open(filepath, 'r') as file:
        for record in file:
            url = record.rstrip('\n')
            assert validators.domain(url) is True
