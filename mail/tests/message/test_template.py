import pytest
from fan.message.template import Template


@pytest.mark.parametrize(
    "test_name,a,b,expect",
    (
        ("gt", 1, 2, "False"),
        ("gt", 2, 1, "True"),
        ("lt", 2, 1, "False"),
        ("lt", 1, 2, "True"),
        ("gte", 1, 2, "False"),
        ("gte", 2, 1, "True"),
        ("gte", 2, 2, "True"),
        ("lte", 2, 1, "False"),
        ("lte", 1, 2, "True"),
        ("lte", 2, 2, "True"),
        ("eq", 2, 2, "True"),
        ("eq", 1, 2, "False"),
        ("ne", 1, 2, "True"),
        ("ne", 2, 2, "False"),
    ),
)
def test_custom_tests(test_name, a, b, expect):
    template = "{{% if a is {0} b %}}True{{% else %}}False{{% endif %}}".format(test_name)
    result = Template(template).render(**{"a": a, "b": b})
    assert result == expect


def test_user_template_variables(template_with_variables):
    actual_user_variables = sorted(Template(template_with_variables).user_template_variables)
    expected_user_variables = sorted(["some_user_varable_1", "some_user_varable_2"])
    assert actual_user_variables == expected_user_variables


def test_builtin_template_variables(template_with_variables):
    actual_builtin_variables = sorted(Template(template_with_variables).builtin_template_variables)
    expected_builtin_variables = sorted(["unsubscribe_link", "sender_letter_id"])
    assert actual_builtin_variables == expected_builtin_variables


@pytest.fixture
def template_with_variables():
    return """<html>
    <head></head>
    <body>
        <!-- some builin variables -->
        {{ unsubscribe_link }}
        {{ unsubscribe_link }}
        {{ sender_letter_id }}

        <!-- some user variables -->
        {{ some_user_varable_1 }}
        {{ some_user_varable_2 }}
        {{ some_user_varable_2 }}
    </body>
    </html>"""
