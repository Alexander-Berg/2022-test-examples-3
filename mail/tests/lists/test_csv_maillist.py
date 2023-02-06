import os

import pytest
from fan.lists.csv_maillist import (
    parse_csv_data,
    get_subscribers_number,
    get_preview,
    ReaderError,
    RecipientsCountExceeded,
    InvalidEmails,
    EmailColumnNotFound,
    UserTemplateVariableLengthExceeded,
    UserTemplateVariableValueLengthExceeded,
    UserTemplateVariablesCountExceeded,
)

N = 100000


def open_csv_file(relative_filename, mode="r"):
    csv_dir = os.path.join(os.path.dirname((os.path.dirname(__file__))), "data/csv")
    full_path = os.path.join(csv_dir, relative_filename)
    return open(full_path, mode)


@pytest.fixture
def valid_csv():
    return "\r\n".join(
        [
            "email,login",
            "lavrinenko@yandex-team.ru,lavrinenko",
            "kapp@yandex-team.ru,kapp ",
            " fedoseev88@yandex-team.ru,fedoseev88",
        ]
    )


@pytest.fixture
def parsed_valid_csv():
    return [
        {"email": "lavrinenko@yandex-team.ru", "login": "lavrinenko"},
        {"email": "kapp@yandex-team.ru", "login": "kapp "},
        {"email": " fedoseev88@yandex-team.ru", "login": "fedoseev88"},
    ]




@pytest.fixture
def valid_csv_without_headers():
    return "\r\n".join(
        [
            "0,+7916xxxxxxx@yandex.ru,1",
            "5,1992107445@yandex.com.tr,2",
            "6,101@yandex.com,3",
        ]
    )


@pytest.fixture
def parsed_valid_csv_without_headers():
    return [
        {"col3": "1", "col1": "0", "email": "+7916xxxxxxx@yandex.ru"},
        {"col3": "2", "col1": "5", "email": "1992107445@yandex.com.tr"},
        {"col3": "3", "col1": "6", "email": "101@yandex.com"},
    ]


@pytest.fixture
def invalid_email_csv():
    return "\n".join(
        [
            "forbio100@ya.ru",
            "forbio101@ya.ru",
            "forbio102@ya.ru",
            "654654",
            "",
            "",
            "",
        ]
    )


@pytest.fixture
def excel_tabs():
    return "\n".join(
        [
            "email\tvar1",
            "forbio100@ya.ru\t0",
            "forbio101@ya.ru\t1",
        ]
    )


@pytest.fixture
def parsed_excel_tabs():
    return [
        {"email": "forbio100@ya.ru", "var1": "0"},
        {"email": "forbio101@ya.ru", "var1": "1"},
    ]


@pytest.fixture
def csv_with_underscore():
    return "\n".join(
        [
            "Email,login,_enabled",
            "lavrinenko@yandex-team.ru,lavrinenko,0",
            "kapp@yandex-team.ru,kapp,0",
            "fedoseev88@yandex-team.ru,fedoseev88,0",
        ]
    )


@pytest.fixture
def parsed_csv_with_underscore():
    return [
        {"email": "lavrinenko@yandex-team.ru", "login": "lavrinenko"},
        {"email": "kapp@yandex-team.ru", "login": "kapp"},
        {"email": "fedoseev88@yandex-team.ru", "login": "fedoseev88"},
    ]




@pytest.fixture
def big_list_csv():
    emails = ["kapp+%s@yandex-team.ru" % i for i in range(N)]
    errors = ["kapp+%s" % i for i in range(N)]
    return "\r\n".join(emails + errors)


@pytest.fixture
def csv_with_empty_headers():
    return "\n".join(
        [
            "Email,login,,",
            "lavrinenko@yandex-team.ru,lavrinenko,0",
            "kapp@yandex-team.ru,kapp,0",
            "fedoseev88@yandex-team.ru,fedoseev88,0",
        ]
    )


@pytest.fixture
def invalid_emails_csv():
    return "\n".join(
        [
            "email",
            "without_at",
            "test@with space.example.com",
            "test@example.com какой-то мусор",
            "double@@at",
            "several@example.com emails@example.com",
            "email@bad-.domain",
        ]
    )


@pytest.fixture
def emails_without_errors_csv():
    return "\n".join(
        [
            "test@example.com",
            "test@a",
            "test@кириллический-домен.рф",
            "email@with-last-space ",
        ]
    )


@pytest.fixture
def parsed_emails_without_errors_csv():
    return [
        {"email": "test@example.com"},
        {"email": "test@a"},
        {"email": "test@кириллический-домен.рф"},
        {"email": "email@with-last-space "},
    ]


@pytest.fixture
def csv_with_string_without_email():
    return "\n".join(
        [
            "var1,email,var2",
            "val11,test@example.com,val12",
            "val21,,val22",
        ]
    )


@pytest.fixture
def parsed_csv_with_string_without_email():
    return [
        {"var1": "val11", "email": "test@example.com", "var2": "val12"},
    ]


@pytest.fixture
def big_valid_csv():
    strings = ["email,login"]
    for i in range(50):
        strings.append("email_{}@yandex.ru,login_{}".format(i, i))
    return "\n".join(strings)


@pytest.fixture
def preview_big_valid_csv():
    return [
        {"email": "email_0@yandex.ru", "login": "login_0"},
        {"email": "email_1@yandex.ru", "login": "login_1"},
        {"email": "email_2@yandex.ru", "login": "login_2"},
        {"email": "email_3@yandex.ru", "login": "login_3"},
        {"email": "email_4@yandex.ru", "login": "login_4"},
    ]


@pytest.fixture
def empty_csv_file():
    return ""


@pytest.fixture
def csv_without_email():
    return "\n".join(
        [
            "login,name,mobile",
            "login_1,name_1,1",
            "login_2,name_2,2",
        ]
    )


@pytest.fixture
def csv_with_empty_string():
    return "\n".join(
        [
            "test@example.com",
            "",
        ]
    )


@pytest.fixture
def csv_with_empty_string_and_headers():
    return "\n".join(
        [
            "email,name",
            "test@example.com,Nikita",
            "",
        ]
    )


def test_parse_valid_csv(valid_csv, parsed_valid_csv):
    csv_maillist = parse_csv_data(valid_csv)
    assert csv_maillist == parsed_valid_csv
    assert get_subscribers_number(csv_maillist) == 3


def test_parse_valid_csv_without_headers(
    valid_csv_without_headers, parsed_valid_csv_without_headers
):
    csv_maillist = parse_csv_data(valid_csv_without_headers)
    assert csv_maillist == parsed_valid_csv_without_headers
    assert get_subscribers_number(csv_maillist) == 3


def test_parse_excel_tabular(excel_tabs, parsed_excel_tabs):
    csv_maillist = parse_csv_data(excel_tabs)
    assert csv_maillist == parsed_excel_tabs
    assert get_subscribers_number(csv_maillist) == 2


def test_parse_with_underscore_removal(csv_with_underscore, parsed_csv_with_underscore):
    csv_maillist = parse_csv_data(csv_with_underscore)
    assert csv_maillist == parsed_csv_with_underscore
    assert get_subscribers_number(csv_maillist) == 3


def test_parse_headers(valid_csv):
    csv_maillist = parse_csv_data(valid_csv)
    headers = set(csv_maillist[0].keys())
    assert headers == {"email", "login"}


def test_parse_generate_headers(valid_csv_without_headers):
    csv_maillist = parse_csv_data(valid_csv_without_headers)
    headers = set(csv_maillist[0].keys())
    assert headers == {"col1", "email", "col3"}


def test_parce_empty_csv_header(csv_with_empty_headers):
    csv_maillist = parse_csv_data(csv_with_empty_headers)
    headers = set(csv_maillist[0].keys())
    assert headers == {"email", "login", "col3", "col4"}


def test_correct_parce_double_quoted():
    with open_csv_file("quotes.csv") as f:
        file_data = f.read()

    csv_maillist = parse_csv_data(file_data)
    row = csv_maillist[0]
    assert row["text"] == 'This text has "quoted" word, indeed.'


def test_do_not_parse_strings_without_email(
    csv_with_string_without_email, parsed_csv_with_string_without_email
):
    csv_maillist = parse_csv_data(csv_with_string_without_email)
    assert csv_maillist == parsed_csv_with_string_without_email
    assert get_subscribers_number(csv_maillist) == 1


def test_empty_csv_file(empty_csv_file):
    with pytest.raises(ReaderError):
        parse_csv_data(empty_csv_file)


def test_do_not_count_empty_string(csv_with_empty_string):
    csv_maillist = parse_csv_data(csv_with_empty_string)
    assert get_subscribers_number(csv_maillist) == 1


def test_do_not_count_empty_string_with_headers(csv_with_empty_string_and_headers):
    csv_maillist = parse_csv_data(csv_with_empty_string_and_headers)
    assert get_subscribers_number(csv_maillist) == 1


def test_parse_csv_with_too_many_recipients(big_list_csv):
    with pytest.raises(RecipientsCountExceeded):
        parse_csv_data(big_list_csv)


def test_parse_csv_with_invalid_email(invalid_email_csv):
    expected_message = "invalid emails: 654654"
    with pytest.raises(InvalidEmails, match=expected_message):
        parse_csv_data(invalid_email_csv)


def test_parse_csv_with_invalid_emails(invalid_emails_csv):
    expected_invalid_emails = [x for x in invalid_emails_csv.split("\n")]
    expected_invalid_emails = expected_invalid_emails[1:]
    expected_message = " ".join(["invalid emails:"] + expected_invalid_emails)
    with pytest.raises(InvalidEmails, match=expected_message):
        parse_csv_data(invalid_emails_csv)


def test_parse_emails_without_errors(emails_without_errors_csv, parsed_emails_without_errors_csv):
    csv_maillist = parse_csv_data(emails_without_errors_csv)
    assert csv_maillist == parsed_emails_without_errors_csv


def test_parse_csv_with_too_long_variable(csv_content_with_too_long_variable):
    with pytest.raises(UserTemplateVariableLengthExceeded):
        parse_csv_data(csv_content_with_too_long_variable)


def test_parse_csv_with_too_long_variable_value(csv_content_with_too_long_variable_value):
    with pytest.raises(UserTemplateVariableValueLengthExceeded):
        parse_csv_data(csv_content_with_too_long_variable_value)


def test_parse_csv_with_too_many_variables(csv_content_with_too_many_variables):
    with pytest.raises(UserTemplateVariablesCountExceeded):
        parse_csv_data(csv_content_with_too_many_variables)


def test_parse_csv_without_email(csv_without_email):
    with pytest.raises(EmailColumnNotFound):
        parse_csv_data(csv_without_email)


def test_get_preview_small_csv(valid_csv, parsed_valid_csv):
    csv_maillist = parse_csv_data(valid_csv)
    preview = get_preview(csv_maillist)
    assert preview == parsed_valid_csv


def test_get_preview_big_csv(big_valid_csv, preview_big_valid_csv):
    csv_maillist = parse_csv_data(big_valid_csv)
    preview = get_preview(csv_maillist)
    assert preview == preview_big_valid_csv
