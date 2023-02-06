def test_remtagger_can_set_tag(remtagger, tag1):
    assert not remtagger.is_set(tag1, 1)
    version = remtagger.set(tag1)
    assert remtagger.is_set(tag1, version)


def test_remtagger_check_existence_of_tag(remtagger, tag2):
    version = remtagger.set(tag2)
    assert not remtagger.is_set(tag2, version + 1)
    assert remtagger.is_set(tag2, version)
