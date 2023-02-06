import pytest
from os.path import dirname, join
from django.conf import settings
from fan.utils.csv import csv_data_from_file


class MaillistData:
    def __init__(content, filename):
        self.content = content
        self.filename = filename


@pytest.fixture
def maillist_csv_content():
    with open(join(dirname(dirname(__file__)), "tests/data/xls/list-with-columns.xlsx"), "rb") as f:
        return csv_data_from_file(f)


@pytest.fixture
def maillist_xls_content():
    with open(join(dirname(dirname(__file__)), "tests/data/xls/list-with-columns.xlsx"), "rb") as f:
        return f.read()


@pytest.fixture
def maillist_content_with_missed_values():
    return """
email,a,b
a@ya.ru,a1,b1
b@ya.ru,a2
"""


@pytest.fixture
def too_large_maillist_content():
    return "line1@ya.ru\n" * (settings.MAX_MAILLIST_RECIPIENTS + 1)


@pytest.fixture
def maillist_content_with_user_template_variables_tab_separated():
    return """
email\ta\tb
a@ya.ru\ta1\tb1
b@ya.ru\ta2
"""


@pytest.fixture
def maillist_content_with_user_template_variables_without_header():
    return """
a@ya.ru,a1,b1
b@ya.ru,a2
"""


@pytest.fixture
def maillist_content_with_user_template_variables_with_empty_lines():
    return """
email,a,b
a@ya.ru,a1,b1


b@ya.ru,a2
"""


@pytest.fixture
def maillist_content_with_underscored_header():
    return """
email,_a,b
a@ya.ru,a1,b1
"""


@pytest.fixture
def maillist_content_with_capital_letters_in_headers():
    return """
email,Abc,cDE
a@ya.ru,a1,b1
"""


@pytest.fixture
def maillist_content_with_duplicated_headers_in_different_case():
    return """
email,Abc,aBC
a@ya.ru,a1,b1
"""


@pytest.fixture
def maillist_content_with_duplicated_headers():
    return """
email,abc,abc
a@ya.ru,a1,b1
"""


@pytest.fixture
def maillist_content_with_unicode():
    return """
;Имя;Фамилия;Компания;
a@b.ru;;;
c@d.ru;Алена;Х;Ко;
e@f.ru;;
"""


@pytest.fixture
def maillist_content_with_dirty_emails():
    return 'a@b.c\r\n"\t\na@b.c"'


@pytest.fixture
def bad_csv_content():
    return """
2a02:6b8:c0b:6e1a:0:1411:26e7:53bb
2a02:6b8:c08:d0a5:0:40b1:622b:eaa4
2a02:6b8:c08:7797:10e:cc13:0:4101
    """


@pytest.fixture
def empty_csv_content():
    return "\n"


@pytest.fixture
def corrupted_csv_content():
    return """
a@ya.ru
b@ya.ru
2a02:6b8:c08:d0a5:0:40b1:622b:eaa4
c@ya.ru
    """


@pytest.fixture
def missed_email_csv_content():
    return """
a@ya.ru
b@ya.ru

c@ya.ru
    """


@pytest.fixture
def csv_content_with_too_many_variables():
    return """
a@ya.ru,1,2,3,4,5,6
    """


@pytest.fixture
def csv_content_with_too_many_variables_but_some_are_hidden_variable():
    return """
email,a,b,c,d,e,_f
a@ya.ru,1,2,3,4,5,6
    """


@pytest.fixture
def csv_content_with_too_long_variable():
    return """
email,var1234567890123456
a@ya.ru,1
    """


@pytest.fixture
def csv_content_with_too_long_hidden_variable():
    return """
email,a,_var1234567890123456
a@ya.ru,1,2
    """


@pytest.fixture
def csv_content_with_too_long_variable_value():
    value = "a" * 129
    return """
a@ya.ru,1
b@ya.ru,{}
    """.format(
        value
    )


@pytest.fixture
def csv_content_with_too_long_hidden_variable_value():
    value = "b" * 129
    return """
email,a,_b
a@ya.ru,a1,b1
b@ya.ru,a2,{}
    """.format(
        value
    )


@pytest.fixture
def overriden_maillists_limit():
    stored = settings.MAILLISTS_LIMIT
    settings.MAILLISTS_LIMIT = 5
    yield settings.MAILLISTS_LIMIT
    settings.MAILLISTS_LIMIT = stored
