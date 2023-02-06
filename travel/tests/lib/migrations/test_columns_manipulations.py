# coding: utf-8

from __future__ import unicode_literals

from django.db import connection

import pytest

from travel.rasp.admin.lib.migrations import field_exists, drop_field, drop_field_if_exists


TMP_TABLE_NAME = __name__


@pytest.yield_fixture(autouse=True)
def tmp_table():
    schema_editor = connection.schema_editor()
    with connection.cursor() as cursor:
        cursor.execute("""
            DROP TABLE IF EXISTS {};
        """.format(schema_editor.quote_name(TMP_TABLE_NAME)))
        cursor.execute("""
            CREATE TABLE {} (id int);
        """.format(schema_editor.quote_name(TMP_TABLE_NAME)))

    yield

    with connection.cursor() as cursor:
        cursor.execute("""
            DROP TABLE {};
        """.format(schema_editor.quote_name(TMP_TABLE_NAME)))


@pytest.mark.dbignore
def test_field_exists():
    schema_editor = connection.schema_editor()
    with connection.cursor() as cursor:
        cursor.execute("""
        ALTER TABLE {} ADD COLUMN aaaaa varchar(20);
        """.format(schema_editor.quote_name(TMP_TABLE_NAME)))

    assert field_exists(schema_editor, TMP_TABLE_NAME, 'aaaaa')
    assert not field_exists(schema_editor, TMP_TABLE_NAME, 'bbbbb')


@pytest.mark.dbignore
def test_drop_field():
    schema_editor = connection.schema_editor()
    with connection.cursor() as cursor:
        cursor.execute("""
        ALTER TABLE {} ADD COLUMN aaaaa varchar(20);
        """.format(schema_editor.quote_name(TMP_TABLE_NAME)))

    assert field_exists(schema_editor, TMP_TABLE_NAME, 'aaaaa')

    drop_field(schema_editor, TMP_TABLE_NAME, 'aaaaa')

    assert not field_exists(schema_editor, TMP_TABLE_NAME, 'aaaaa')


@pytest.mark.dbignore
def test_drop_field_if_exists():
    schema_editor = connection.schema_editor()

    assert not drop_field_if_exists(schema_editor, TMP_TABLE_NAME, 'aaaaa')

    with connection.cursor() as cursor:
        cursor.execute("""
        ALTER TABLE {} ADD COLUMN aaaaa varchar(20);
        """.format(schema_editor.quote_name(TMP_TABLE_NAME)))

    assert field_exists(schema_editor, TMP_TABLE_NAME, 'aaaaa')

    assert drop_field_if_exists(schema_editor, TMP_TABLE_NAME, 'aaaaa')
    assert not drop_field_if_exists(schema_editor, TMP_TABLE_NAME, 'aaaaa')

    assert not field_exists(schema_editor, TMP_TABLE_NAME, 'aaaaa')
