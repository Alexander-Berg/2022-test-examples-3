# coding=utf-8
"""
This script created supometr test data.
"""

import difflib
import json
import os
import sys

import yt.wrapper as yt

from supometr import get_args
from supometr import do_supometriya

__mydir__ = os.path.dirname(os.path.realpath(__file__))

# Local data
TEST_DATA_FOLDER = "./test_data"

TEST_EDGES_DATA_PATH = os.path.join(__mydir__, TEST_DATA_FOLDER, "test_data_supometr.json")
REFERENCE_SUPOMETRIYA_RESULT_PATH = os.path.join(__mydir__, TEST_DATA_FOLDER, "test_processing_result.json")

# YT
YT_CLUSTER = "hahn"
DEFAULT_YT_PROXY = "{}.yt.yandex.net".format(YT_CLUSTER)

TEST_DATA_YT_FOLDER = "//home/crypta/team/ikutukov/test_soup_tmp"
SUPOMETRIYA_OUTPUT_PATH = TEST_DATA_YT_FOLDER.rstrip("/") + "/test_soup.out"
YT_PROXY_PUBLIC_PATH = "https://yt.yandex-team.ru/{}/#page=navigation&path=".format(YT_CLUSTER)


def are_jsons_equal(json1, json2):
    json1_text = json.dumps(json1, sort_keys=True, indent=2)
    json2_text = json.dumps(json2, sort_keys=True, indent=2)
    print "Diff:"
    print ''.join(
        difflib.ndiff(json1_text.splitlines(1), json2_text.splitlines(1))
    )
    return json1_text == json2_text


def load_test_data(test_edges_data_path, yt_folder):
    sys.stdout.write("Loading test data from:\n{}\n".format(test_edges_data_path))
    with open(test_edges_data_path, "rb") as f:
        test_data = json.load(f)
    sys.stdout.write("...done\n")
    sys.stdout.write("Output folder: {}\n".format(yt_folder))
    if yt.exists(yt_folder):
        sys.stdout.write("Removing existing folder {}\n".format(yt_folder))
        yt.remove(yt_folder, force=True, recursive=True)

    sys.stdout.write("Creating folder {}\n".format(yt_folder))
    yt.mkdir(yt_folder, recursive=True)

    out_folders = []
    for k, v in test_data.iteritems():
        table_path = "/".join([yt_folder, k])
        yt.create_table(table_path)
        sys.stdout.write("Filling {} with data...\n".format(table_path))
        out_folders.append(table_path)
        yt.write_table(
            table_path,
            v,
            format='json',
            raw=False
        )

    sys.stdout.write("...writing done\n")
    sys.stdout.write(
        "Everything is done right!\nResulting tables:\n    {}\n".format(
            "\n    ".join([YT_PROXY_PUBLIC_PATH + fp for fp in out_folders])
        )
    )

if __name__ == '__main__':
    sys.stdout.write("Using YT proxy: {}\n".format(DEFAULT_YT_PROXY))
    yt.update_config({"proxy": {"url": DEFAULT_YT_PROXY}})

    load_test_data(TEST_EDGES_DATA_PATH, TEST_DATA_YT_FOLDER)

    sys.stdout.write("Running supometriya")

    # Override args
    args = get_args()
    args.skip_cleanup = True
    args.source = TEST_DATA_YT_FOLDER
    args.target = SUPOMETRIYA_OUTPUT_PATH

    # Main run
    print "Running supometr..."
    do_supometriya(args)
    print "...done!"

    with open(REFERENCE_SUPOMETRIYA_RESULT_PATH, "rb") as f:
        reference_result = json.load(f)
    reference_result = sorted(reference_result, key=lambda x: x["topohash"])

    result = [
        record for record
        in yt.read_table(SUPOMETRIYA_OUTPUT_PATH, format=yt.JsonFormat(), raw=False)
    ]
    result = sorted(result, key=lambda x: x["topohash"])

    print "Processing result:"
    print json.dumps(result, indent=2)
    print "Reference result:"
    print json.dumps(reference_result, indent=2)
    print "Comparing..."
    are_equal = are_jsons_equal(
        result,
        reference_result
    )
    print "Success" if are_equal else "Fail"
