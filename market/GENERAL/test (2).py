import report_to_metrics_format
import json


def make_json_diff_msg(left, right, msg=''):
    import difflib
    left = json.dumps(left, indent=4)
    right = json.dumps(right, indent=4)
    lines = difflib.context_diff(left.splitlines(keepends=True), right.splitlines(keepends=True))
    lines = ''.join(lines)
    return msg + '\n' + lines


def run_report_to_metrics_format_test():
    report_output = json.load(open('./test_data/report_output.json'))
    basket = json.load(open('./test_data/basket.json'))
    out = report_to_metrics_format.make_metrics_serpset(report_output, basket)
    expected = json.load(open('./test_data/expected.json'))
    assert expected == out, make_json_diff_msg(out, expected)


def run_tests():
    run_report_to_metrics_format_test()


if __name__ == '__main__':
    run_tests()
