# coding: utf-8

import pytest
import six

from crypta.lib.python.statface.statface_sender import (
    get_masked_token,
    get_report_config_as_yaml,
)


def test_get_report_config_as_yaml():
    ref = six.u("""---
dimensions:
- fielddate: date
measures:
- metric_1: number
- metric_2: number
titles:
  fielddate: Дата
  metric_1: Метрика_1
  metric_2: Метрика_2
""")
    assert ref == get_report_config_as_yaml(
        measures=["metric_1", "metric_2"],
        additional_dimensions=[],
        titles_dict={
            "metric_1": six.u("Метрика_1"),
            "metric_2": six.u("Метрика_2"),
            "fielddate": six.u("Дата"),
        }
    )


dimension_types = [
    ("number", True, None),
    ("string", False, {"axis": "string"}),
]


@pytest.mark.parametrize("axis_ref_type,use_default,axis_type", dimension_types)
def test_get_report_config_as_yaml_with_dimensions(axis_ref_type, use_default, axis_type):
    ref = six.u("""---
dimensions:
- fielddate: date
- axis: {axis_type}
measures:
- metric_1: number
- metric_2: number
titles:
  fielddate: Дата
  axis: Ось
  metric_1: Метрика_1
  metric_2: Метрика_2
""").format(axis_type=axis_ref_type)

    kwargs = {} if use_default else {"additional_dimension_types": axis_type}

    assert ref == get_report_config_as_yaml(
        measures=["metric_1", "metric_2"],
        additional_dimensions=["axis"],
        titles_dict={
            "metric_1": six.u("Метрика_1"),
            "metric_2": six.u("Метрика_2"),
            "fielddate": six.u("Дата"),
            "axis": six.u("Ось"),
        },
        **kwargs
    )


def test_get_report_config_as_yaml_invalid_titles():
    with pytest.raises(Exception):
        get_report_config_as_yaml(
            measures=["metric_1", "metric_2"],
            additional_dimensions=[],
            titles_dict={
                "metric_1": six.u("Метрика_1"),
                "metric_3": six.u("Метрика_3"),
                "fielddate": six.u("Дата"),
            }
        )


def test_get_masked_token_default():
    assert "AQAD-qXXXXXXXXXXXXXXXXXX" == get_masked_token("AQAD-qlekjsfdlaksftdfdfw")


data = [
    ("", "", 100500, 100500/2),

    ("XXX", "123", 0, 0),
    ("12X", "123", 0, 2),
    ("123", "123", 0, 100500),

    ("XXX", "123", 3, 0),
    ("1XX", "123", 3, 1),
    ("12X", "123", 3, 2),
    ("123", "123", 3, 3),

    ("XX", "12", 3, 3),
]


@pytest.mark.parametrize("ref,token,min_len,visible_chars", data)
def test_get_masked_token(ref, token, min_len, visible_chars):
    assert ref == get_masked_token(token, min_len=min_len, visible_chars=visible_chars)
