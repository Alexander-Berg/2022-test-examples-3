import jinja2
import pytest

from crypta.lib.python import templater


def test_render_template():
    assert templater.render_template("Hello {{ something }}", {"something": "world"}) == "Hello world"
    assert templater.render_template("Hello {{ something }}", {"something": "world"}, strict=True) == "Hello world"
    assert templater.render_template("Even numbers in [0, 10) are:{% for n in range(1,10) %}"
                                     "{% if n % 2 == 0 %} {{n}}{% endif %}{% endfor %}") == "Even numbers in [0, 10) are: 2 4 6 8"

    with pytest.raises(jinja2.UndefinedError):
        templater.render_template("{{ greeting }} {{ something }}", {"something": "world"}, strict=True)

    assert templater.render_template("{{ greeting }} {{ something }}", {"something": "world"}) == " world"


def test_trim():
    assert templater.render_template("{% for n in range(1,4) %}\n{{ n }}\n{% endfor %}") == "1\n2\n3\n"


def test_render_file(tmpdir):
    template = ("Hello {{ something }}\n"
                "Even numbers in [0, 10) are:{% for n in range(1,10) %}"
                "{% if n % 2 == 0 %} {{n}}{% endif %}{% endfor %}")

    input_file = tmpdir.join("tpl")
    input_file.write(template)

    output_file = tmpdir.join("output")
    templater.render_file(str(input_file), str(output_file), {"something": "world"})

    assert output_file.read() == "Hello world\nEven numbers in [0, 10) are: 2 4 6 8"

    with pytest.raises(jinja2.UndefinedError):
        templater.render_file(str(input_file), str(output_file), {}, strict=True)


def test_split_vars():
    assert templater.split_vars([]) == {}
    assert templater.split_vars(["a=b"]) == {"a": "b"}
    assert templater.split_vars(["a=b", "a=b"]) == {"a": "b"}
    assert templater.split_vars(["a=b", "a=c"]) == {"a": "c"}
    assert templater.split_vars(["a=c", "a=b"]) == {"a": "b"}
    assert templater.split_vars(["a=b=c"]) == {"a": "b=c"}
    assert templater.split_vars(["a-b=c"]) == {"a-b": "c"}
    assert templater.split_vars(["  = "]) == {"  ": " "}
