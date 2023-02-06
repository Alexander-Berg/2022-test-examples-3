#!/usr/bin/python

from extsearch.images.robot.scripts.cm.mrindex.ratedurls import add_urls_from_yt, SIMILAR_YT_PROXY, SIMILAR_YT_PATH, SIMILAR_YT_COLUMN

import yatest.common

TEST_TOKEN = 'TEST_TOKEN'


class TestYtUrlStorage:
    @staticmethod
    def urlopen_with_cert_none_mock(request):
        if SIMILAR_YT_PROXY in request.get_full_url():
            return open(yatest.common.source_path("extsearch/images/robot/scripts/cm/mrindex/ut/yt_responce.tsv"), 'r')
        return None

    def process_urls(self, res_file):
        wrong_format_urls = set()
        processed_urls = set()
        similar_urls = add_urls_from_yt(wrong_format_urls, processed_urls, TEST_TOKEN, SIMILAR_YT_PROXY, SIMILAR_YT_PATH, SIMILAR_YT_COLUMN, url_reader=TestYtUrlStorage.urlopen_with_cert_none_mock)
        for (url, crc) in similar_urls:
            res_file.write('{0}\t{1}\n'.format(url, crc))
        res_file.write('wrong_format_urls\n')
        for url in wrong_format_urls:
            res_file.write('{0}\n'.format(url))
        res_file.write('processed_urls\n')
        for url in processed_urls:
            res_file.write('{0}\n'.format(url))


def test_parce_yt_responce():
    test_yt_storage = TestYtUrlStorage()
    out_file_path = "yt_storage_result.result"
    with open(out_file_path, 'w') as res_file:
        test_yt_storage.process_urls(res_file)
        res_file.write('Done\n')
    return yatest.common.canonical_file(out_file_path)
