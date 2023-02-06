from io import StringIO

import pandas as pd

from library.python.resource import resfs_read


def test_read():
    settings = resfs_read("market/dynamic_pricing_parsing/regional_loading/settings/settings.csv").decode("utf-8")
    settings = pd.read_csv(StringIO(settings), sep=r'\s+;\s+', engine='python', comment='#')
    assert {'host', 'region_ids', 'timetable'} - set(settings.columns) == set()
