from hamcrest import assert_that, is_not, has_item, contains_string


def assert_log_contains(logger_mock, expected_string):
    assert_that(_flattern_log(logger_mock), has_item(contains_string(expected_string)))


def assert_log_not_contains(logger_mock, expected_string):
    assert_that(_flattern_log(logger_mock), is_not(has_item(contains_string(expected_string))))


def _flattern_log(logger_mock):
    return map(lambda raw_log_row: raw_log_row[0][0] + str(raw_log_row[1]), logger_mock.call_args_list)
