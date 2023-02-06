from pytest_mock import MockFixture

from travel.avia.ad_feed.ad_feed.feed_generator.cutter import ByClickPriceCutter
from travel.avia.ad_feed.ad_feed.feed_generator.direction import DirectionFeedRow


def test_click_price_cutter(mocker: MockFixture):
    cutter = ByClickPriceCutter(threshold=1)
    data = [
        mocker.create_autospec(DirectionFeedRow),
        mocker.create_autospec(DirectionFeedRow),
        mocker.create_autospec(DirectionFeedRow),
    ]
    data[0].click_price = 0.5
    data[1].click_price = 2
    data[2].click_price = 1
    actual = list(cutter(data))
    assert len(actual) == 2
    assert actual[0].click_price == 2
    assert actual[1].click_price == 1
