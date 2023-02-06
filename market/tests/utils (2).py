import yt.yson as yson


def string_regions_to_list(regions):
    return list(sorted(frozenset([yson.YsonUint64(int(num)) for num in regions.split() if num])))
