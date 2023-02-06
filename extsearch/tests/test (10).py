#!/usr/bin/env python

# local imports
import common

# global imports (yandex tools)
import yatest.common

# global imports
import os
import shutil
import unittest


MR_SERVER = "arnold.yt.yandex.net"
INDEX_PREFIX = "images"
INDEX_STATE = "21181332-246161"
SHARD_NUMBER = "152"

TEST_SHARDWRITER = "extsearch/images/robot/index/index_download/lib/ut/mock_shardwriter/mock_shardwriter"
MOCK_SHARDWRITER_OUTPUT_FILE_NAME = "test_output.txt"  # this name is also hardcoded in mock_shardwriter


def simplified_shardwriter_download(mode, tier_name="primary"):
    common.call_shardwriter_download(
        MR_SERVER,
        INDEX_PREFIX,
        INDEX_STATE,  # index state was intentionally set to invalid to avoid problems in prod
        SHARD_NUMBER,
        yatest.common.work_path(),
        mode,
        True,
        tier_name
    )


def check_and_clear_output(expected_args):
    with open(MOCK_SHARDWRITER_OUTPUT_FILE_NAME, 'r') as content_file:
        content = content_file.read()
        content = content.strip()
        if content != expected_args:
            raise Exception('Failed to check content, expected content: "' + expected_args +
                            '", actual content: "' + content + '"')
    os.remove(MOCK_SHARDWRITER_OUTPUT_FILE_NAME)


def simplified_check_and_clear_output(mode, tier_name="primary"):
    expected_content = \
        "DownloadShard " + \
        "--index-prefix " + INDEX_PREFIX + \
        " --index-state " + INDEX_STATE + \
        " --shard-number " + SHARD_NUMBER + \
        " --out-dir " + yatest.common.work_path() + \
        " --mode " + mode + \
        " --tier-name " + tier_name + \
        " --server " + MR_SERVER
    check_and_clear_output(expected_content)


class SmokeTest(unittest.TestCase):

    def test_shardwriter_download(self):
        try:
            os.environ['PATH'] = '{0}:{1}'.format(os.environ.get('PATH', ''), os.getcwd())
            shutil.copy2(yatest.common.binary_path(TEST_SHARDWRITER), os.path.join(os.getcwd(), "shardwriter"))
            simplified_shardwriter_download("keyinvindex")
            simplified_check_and_clear_output("keyinvindex")
            simplified_shardwriter_download("userdoc")
            simplified_check_and_clear_output("userdoc")
        except Exception, e:
            raise Exception('Exception in unittests: "' + str(e) + '"')
        except:
            raise Exception('Unhandled exception during unittests')
        finally:
            os.remove(os.path.join(os.getcwd(), "shardwriter"))
