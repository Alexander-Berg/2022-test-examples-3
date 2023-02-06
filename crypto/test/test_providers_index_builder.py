#!/usr/bin/env python

from providers_index_builder import Index
import pytest


def test_index_inconsistent_fields():
    rows = [
        {
            "id": 1,
            "name": "foo"
        },
        {
            "id": 2,
            "name": "foo",
            "status": "disabled"
        }
    ]

    with pytest.raises(Exception):
        Index(rows)


def test_index_eq():
    rows = [
        {
            "id": 1,
            "name": "foo",
            "status": "enabled"
        },
        {
            "id": 2,
            "name": "foo",
            "status": "disabled"
        }
    ]

    rows2 = list(rows)

    index = Index(rows)
    index2 = Index(rows2)

    assert index == index2
    assert index.rows == index2.rows
    assert index.all_keys == index2.all_keys
