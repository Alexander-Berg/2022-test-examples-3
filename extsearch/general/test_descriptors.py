# -*- coding: utf-8 -*-
# TODO(epinkovskii): use fake descriptors.json here
import pytest

import extsearch.ymusic.scripts.reindex.gendocs as gd


NON_EXISTING_TYPE = 1000


@pytest.fixture()
def descriptors_generator(descriptors_file):
    yield gd.DescriptorsGenerator(descriptors_file)


def test__generate_descriptors__without_default_type_descriptors(descriptors_generator):
    descriptors = descriptors_generator.generate_descriptors(gd.EntityDescriptorsInfo(
        type_=1,
        soundtrack=False,
        album_type="audiobook",
    ))
    assert len(descriptors) == 4


def test__generate_descriptors__with_soundtrack(descriptors_generator):
    descriptors = descriptors_generator.generate_descriptors(gd.EntityDescriptorsInfo(
        type_=NON_EXISTING_TYPE,
        soundtrack=True,
        album_type=None,
    ))
    assert list(descriptors) == ['саундтрек', 'ost', 'theme']
