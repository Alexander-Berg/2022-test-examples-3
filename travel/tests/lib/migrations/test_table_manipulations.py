# coding: utf-8

from __future__ import unicode_literals

from django.db import connection

import pytest

from travel.rasp.admin.lib.migrations import field_exists, drop_field, drop_field_if_exists, table_exists, drop_table, \
    drop_table_if_exists


TMP_TABLE_PREFIX = 'test.lib.migrations.'  # should be not too long


@pytest.mark.dbignore
def test_table_exists():
    table_name = TMP_TABLE_PREFIX + 'test_table_exists'
    schema_editor = connection.schema_editor()
    with connection.cursor() as cursor:
        cursor.execute("""
        CREATE TABLE {} ( id int );
        """.format(schema_editor.quote_name(table_name)))

    assert table_exists(schema_editor, table_name)

    with connection.cursor() as cursor:
        cursor.execute("""
        DROP TABLE {};
        """.format(schema_editor.quote_name(table_name)))

    assert not table_exists(schema_editor, table_name)


@pytest.mark.dbignore
def test_drop_table():
    table_name = TMP_TABLE_PREFIX + 'test_drop_table'
    schema_editor = connection.schema_editor()
    with connection.cursor() as cursor:
        cursor.execute("""
            CREATE TABLE {} ( id int );
        """.format(schema_editor.quote_name(table_name)))

    assert table_exists(schema_editor, table_name)

    drop_table(schema_editor, table_name)

    assert not table_exists(schema_editor, table_name)


@pytest.mark.dbignore
def test_drop_table_if_exists():
    table_name = TMP_TABLE_PREFIX + 'test_drop_table'
    schema_editor = connection.schema_editor()

    assert not drop_table_if_exists(schema_editor, table_name)

    with connection.cursor() as cursor:
        cursor.execute("""
            CREATE TABLE {} ( id int );
        """.format(schema_editor.quote_name(table_name)))

    assert table_exists(schema_editor, table_name)
    assert drop_table_if_exists(schema_editor, table_name)

    assert not table_exists(schema_editor, table_name)
    assert not drop_table_if_exists(schema_editor, table_name)

