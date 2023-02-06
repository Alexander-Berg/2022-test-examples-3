import yatest.common

import json
import os
import pytest
import urlparse

REARRANGE_DATA_PREFIX = os.path.join("extsearch", "fresh", "meta", "rearrange_data")
ALLOWED_DOMAINS = ["ru", "com.tr", "by", "kz", "ua", "uz"]
PRODUCTION_HOSTS_LISTS = [
    "news.hosts.exp.antispam.02.2019",
    "news.hosts.by",
    "news.hosts.kz",
    "news.hosts.tr",
    "news.hosts.uz",
]


def _parse_url(url):
    parsed_url = urlparse.urlparse(url)
    assert parsed_url.scheme == parsed_url.netloc == parsed_url.params == parsed_url.query == parsed_url.fragment == ""
    assert parsed_url.path == url


def _test_authority_boost_tsv(path):
    file_path = yatest.common.build_path(os.path.join(REARRANGE_DATA_PREFIX, *path))
    for line in open(file_path, "r"):
        assert len(line) > 0
        items = line.strip("\n").split("\t")
        assert len(items) == 2
        assert items[0] in ALLOWED_DOMAINS
        _parse_url(items[0])


def _json_valid(path):
    file_path = yatest.common.build_path(os.path.join(REARRANGE_DATA_PREFIX, *path))
    try:
        json.loads(open(file_path, "r").read(), encoding="utf-8")
    except:
        assert False


def test_authority_boost_allowed_tsv():
    _test_authority_boost_tsv(["authority_boost", "allowed.tsv"])

def test_authority_boost_allowed_news_vertical_tsv():
    _test_authority_boost_tsv(["authority_boost", "allowed_news_vertical.tsv"])


def test_authority_boost_authority_tsv():
    _test_authority_boost_tsv(["authority_boost", "authority.tsv"])


def test_news_wizard_titles_cfg():
    _json_valid(["news_wizard", "titles.cfg"])


def test_news_wizard_news_hosts():
    _json_valid(["news_wizard", "host_lists_config.json"])
    path_to_data = os.path.join(REARRANGE_DATA_PREFIX, "news_wizard")
    multi_exp_file = yatest.common.build_path(os.path.join(path_to_data, "news.hosts.exps"))
    assert os.path.isfile(multi_exp_file)
    exp_files = [line.rstrip() for line in open(multi_exp_file, 'r')]

    for production_host_file in PRODUCTION_HOSTS_LISTS:
        assert production_host_file in exp_files

    for filename in exp_files:
        file_path = yatest.common.build_path(os.path.join(path_to_data, filename))
        assert os.path.exists(file_path)
        for line in open(file_path, "r"):
            assert len(line) > 0
            url = line.strip("\n")
            _parse_url(url)


def test_saas_snippets_video_hosts():
    saas_snippets_dir = os.path.join(REARRANGE_DATA_PREFIX, "saas_snippets")
    had_youtube = False
    for line in open(yatest.common.build_path(os.path.join(saas_snippets_dir, "video_hosts.txt"))):
        line = line.rstrip()
        assert line
        _parse_url(line)
        if line == "youtube.com":
            had_youtube = True
    assert had_youtube


def test_news_wizard_news_logos():
    path_to_data = os.path.join(REARRANGE_DATA_PREFIX, "news_wizard")
    file_path = yatest.common.build_path(os.path.join(path_to_data, "news.logos"))
    line_num = 0
    for line in open(file_path, "r"):
        line_num += 1
        assert len(line) > 0
        items = line.strip("\n").split("\t")
        assert len(items) == 4, "line %i must have '<host>\\t<rect-pic>\\t<square-pic>\\t<original-pic>' format" % line_num
        _parse_url(items[0])
        picPathError = "picture path must have '<group-id>/<picture name>/' format"
        for picPath in items[1:]:
            parts = picPath.split("/")
            assert (len(parts) == 3) and (parts[0].isdigit()) and (parts[2] == ''), picPathError


def test_news_wizard_sports_hosts():
    file_path = yatest.common.build_path(os.path.join(REARRANGE_DATA_PREFIX, "news_wizard", "sports.hosts"))
    for line in open(file_path, "r"):
        assert line
        _parse_url(line)
