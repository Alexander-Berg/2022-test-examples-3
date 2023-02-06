import unittest


def skip_unless_has_tags(obj, test_tags):
    if obj is None or test_tags is None:
        return unittest.skip('Test has no tags or tags is not in args')
    if all(item in test_tags for item in obj.tags):
        return lambda func: func
    return unittest.skip(f'Test has {test_tags} tags, but needed {obj.tags} tags')
