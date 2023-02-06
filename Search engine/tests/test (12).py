import yatest.common
from os.path import isfile, join, split, splitext
from os import listdir
import re
import library.python.resource as rs


def test_profile_consistency():
    json_pattern = re.compile(r'.*\.json$', re.IGNORECASE)
    path = yatest.common.test_source_path('..')
    profiles_from_files = [splitext(filename)[0] for filename in listdir(path) if isfile(join(path, filename)) and json_pattern.match(filename)]
    profiles_from_resources = [split(key)[1] for key in rs.iterkeys('/profile/')]
    profiles_from_files.sort()
    profiles_from_resources.sort()
    diff_files = list(set(profiles_from_files) - set(profiles_from_resources))
    diff_res = list(set(profiles_from_resources) - set(profiles_from_files))
    ref_link = "https://a.yandex-team.ru/arc/trunk/arcadia/search/scraper/profile/readme.md"
    assert not diff_files, "There are some profile(s) not added to resources: {}. See {} for details".format(diff_files, ref_link)
    assert not diff_res, "There are some non-existing profile(s) in resources: {}. See {} for details".format(diff_res, ref_link)
    # assert profiles_from_files == profiles_from_resources, 'Please take a look at https://a.yandex-team.ru/arc/trunk/arcadia/search/scraper/profile/readme.md'
