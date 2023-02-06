from core.testenv import testenv
from core.downloader import ForecasterDownloader

import os, subprocess
import pytest


@pytest.fixture(scope="module")
def yt_content(testenv):
    from yt.wrapper import YtClient

    def data():
        for i in xrange(100):
            yield {
                "sku": 1000 + i,
                "model_id": 2000 + i,
                "sku_count": 2,
                "white_median_price": 1000,
                "hid": 1,
                "hierarchy_hyper_ids": [1, 2, 3, 4, 5],
                "avg_price": 1000,
                "orders_sum": 100,
                "days_on_stock": 10,
                "ref_min_regular_price": 900,
                "white_min_regular_price": 800,
                "exp_smooth_demand": 1,
                "sales_total": 200,
                "clicks_sum": 300
            }

    client = YtClient(proxy=os.environ["YT_PROXY"])
    client.write_table("//msku_features", data())
    yield None
    client.remove("//msku_features", recursive=True, force=True)


@pytest.fixture(scope="module")
def downloader(testenv, yt_content):
    return ForecasterDownloader(env=testenv)


def test_downloader_somehow_works(downloader):
    """ Check that downloader download somewhat from YT
    """
    downloader.run(["MskuFeatures", "//msku_features", "msku-features.mmap"])
    assert os.path.getsize("msku-features.mmap") > 0


def test_downloader_fails_on_error(downloader):
    """ Check that downloader fails on absent table
    """
    with pytest.raises(subprocess.CalledProcessError):
        downloader.run(["MskuFeatures", "//msku_features-2", "msku-features.mmap"])
