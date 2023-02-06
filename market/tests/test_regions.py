from market.dynamic_pricing.pricing.library.utils import (
    make_rus_regions_set,
    make_rus_region_cast_dict,
    make_all_regions_dict,
)


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


records = [
    {
        'id': 1,
        'parents': [2, 3, 4],
        'country_id': 225,
        'type': 5
    },
    {
        'id': 5,
        'parents': [1, 2, 3, 4],
        'country_id': 225,
        'type': 4
    },
    {
        'id': 6,
        'parents': [3, 4],
        'country_id': 1,
        'type': 5
    }
]


def test_make_all_regions_dict():
    d = make_all_regions_dict(records)
    assert len(d) == 6
    assert d[1] == set([1, 2, 3, 4, 5])
    assert d[2] == set([1, 2, 3, 4, 5])
    assert d[3] == set([1, 2, 3, 4, 5, 6])
    assert d[4] == set([1, 2, 3, 4, 5, 6])
    assert d[5] == set([1, 2, 3, 4, 5])
    assert d[6] == set([3, 4, 6])


def test_make_rus_regions_set():
    d = make_rus_regions_set(records)
    assert len(d) == 1
    assert d.pop() == 1


def test_make_rus_region_cast_dict():
    all = make_all_regions_dict(records)
    rus = make_rus_regions_set(records)
    d = make_rus_region_cast_dict(records, all, rus)
    assert d == {1: [1], 5: [2]}  # 1 – регион из словаря по условию, 2 – это Питер из константы
