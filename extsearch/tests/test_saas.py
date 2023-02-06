from extsearch.ymusic.indexer.rt_indexer.lib.saas import (
    Action,
    SaasActionBuilder,
)


def test_repeated_property():
    builder = SaasActionBuilder("url", Action.MODIFY)
    builder.add_property("test", ["v1", "v2"])
    message = builder.build_action()
    _assert_doc_equal_without_options(message, {
        "url": "url",
        "test": [
            {"type": "#p", "value": "v1"},
            {"type": "#p", "value": "v2"},
        ],
    })


def test_zone_added_multiple_times():
    builder = SaasActionBuilder('url', Action.MODIFY)
    builder.add_zone('z_test', 'test1')
    builder.add_zone('z_test', 'test2')
    document = builder.build_action()
    assert document['docs'][0]['z_test']['value'] == 'test1. test2'


def _assert_doc_equal_without_options(actual_message, expected_doc):
    actual = actual_message['docs'][0]
    actual.pop('options')
    assert expected_doc == actual
