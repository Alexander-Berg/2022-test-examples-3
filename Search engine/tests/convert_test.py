from converter import convert
from testing_utils import read_json_test_data, check_fields_diff


def check_component(actual, expected):
    check_fields_diff(actual, expected,
                      ["title", "page-url", "view-url", "title", ["serp-component-debug-dump", "server-descr"],
                       "snippet", ["raw-data", "previews"], ["raw-data", "serpData"], "thumb-urls", "rank",
                       "wizard-type", "type", "alignment", "duration", "video-id", "video-player-html",
                       ["video", "video-id"], ["video", "video-hd-flag"], ["video", "video-player-html"],
                       ["video", "thmb-href"], ["video", "has-video-player"], ["video", "frames-thumbs"]])


def check_same_fields_for_both_format(actual, expected):
    check_fields_diff(actual, expected,
                      ["class", "serpRequestExplained", "serp-request-explained",
                       "status", ["serp-page", "serp-page-attempts"],
                       ["serp-page", "parser-results", "documents-found"],
                       ["serp-page", "parser-results", "url"]])


def check_components(actual, expected):
    if len(actual) != len(expected):
        raise ValueError(f"Incorrect len: expected {expected}, but actual {actual} components")
    for i in range(len(actual)):
        check_component(actual[i], expected[i])


def check_all_json(input_json_path, expected_json_path):
    actual = convert(read_json_test_data(input_json_path))
    expected = read_json_test_data(expected_json_path)

    check_same_fields_for_both_format(actual, expected)
    check_components(actual["serp-page"]["parser-results"]["components"],
                     expected["serp-page"]["parser-results"]["components"])


def test_simple_convert():
    check_all_json("simple_parsed_content.json", "expected_simple_parsed_content.json")
