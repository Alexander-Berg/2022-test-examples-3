from test_common import _test_target


TARGETS = [
    ['authors.metrics.start'],
    ['authors.metrics.api_freshness_calc'],
    ['authors.metrics.api_freshness_merge'],
    ['authors.metrics.author_api_count'],
    ['authors.metrics.author_api_merge'],
    ['authors.metrics.media_attributes_extract'],
    ['authors.metrics.media_attributes_count'],
    ['authors.metrics.media_attributes_merge'],
    ['authors.metrics.finish'],

    ['authors.portions.start'],
    ['authors.portions.convert'],
    ['authors.portions.finish'],

    ['authors.thumbs.start'],
    ['authors.thumbs.run_yql'],
    ['authors.thumbs.make_portion'],
    ['authors.thumbs.finish'],
]


def test_dry_run():
    for target_with_args in TARGETS:
        _test_target(target_with_args)
