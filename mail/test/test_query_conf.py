# coding: utf-8

from io import StringIO as SIO

import pytest

from mail.pypg.pypg import query_conf


def load(s, *args, **kwargs):
    return query_conf.load(SIO(s), *args, **kwargs)


@pytest.mark.parametrize('empty_content', [
    u'',
    u'\n',
    u'--',
    u'''
    -- 11
    -- 22
    '''
])
def test_empty_parse(empty_content):
    assert not load(empty_content)


def test_broken_parse():
    with pytest.raises(query_conf.QueryConfSyntaxError):
        load(u'42')


def test_query_ovveride():
    with pytest.raises(query_conf.QueryOverrideError):
        load(u'''
        -- name: foo
        bar
        -- name: foo
        baz''')


def Q(query, args=None):
    return query_conf.Query(query, args, 42)

QH = query_conf.QueriesHolder


@pytest.mark.parametrize(('text', 'queries'), [
    (u'''
-- name: db_time
SELECT sysdate FROM dual''',
     {'db_time': Q('SELECT sysdate FROM dual')}),
    (u'''
-- name: backend_pid
SELECT pg_backend_pid()

-- name: current_query
SELECT current_query()''',
     {'backend_pid': Q('SELECT pg_backend_pid()'),
      'current_query': Q('SELECT current_query()')}),
])
def test_good_quries(text, queries):
    assert load(text) == QH(queries)


@pytest.mark.parametrize(('text', 'queries'), [
    (u'''
     -- name: echo
     SELECT :echo FROM dual''',
     {'echo': Q('SELECT :echo FROM dual', ['echo'])}),
    (u'''
     -- name: calc
     SELECT :x+:y-POW(:z,2) FROM dual''',
     {'calc': Q('SELECT :x+:y-POW(:z,2) FROM dual', ['x', 'y', 'z'])}),
    (u'''
     -- name: join_strings
     SELECT :x || :y FROM dual''',
     {'join_strings': Q('SELECT :x || :y FROM dual', ['x', 'y'])}),
])
def test_ora_vars_translate(text, queries):
    assert load(text, query_conf.ORACLE) == QH(queries)


def test_ora_dangerous_vars():
    with pytest.raises(query_conf.DangerousOraBind):
        load(u'''
-- name: its_ora_
SELECT * FROM folders WHERE uid=:uid''', query_conf.ORACLE)


def test_pg_vars_translate():
    PG_QUERIES = u'''
-- name: folders_with_limit
SELECT * FROM mail.folders WHERE uid=:uid LIMIT :limit
-- name: objects_in_schema
SELECT c.relname
  FROM pg_namespace ns
  JOIN pg_class c
    ON (ns.oid = c.relnamespace)
 WHERE ns.nspname = ANY(:schemas)
   AND c.relkind=:rel_type
'''
    folders_with_limit = Q(
        'SELECT * FROM mail.folders '
        'WHERE uid=%(uid)s LIMIT %(limit)s',
        ['uid', 'limit'])
    objects_in_schema = Q(
        'SELECT c.relname FROM pg_namespace ns '
        'JOIN pg_class c ON (ns.oid = c.relnamespace) '
        'WHERE ns.nspname = ANY(%(schemas)s) AND c.relkind=%(rel_type)s',
        ['schemas', 'rel_type'])
    assert load(PG_QUERIES) == QH(dict(
        folders_with_limit=folders_with_limit,
        objects_in_schema=objects_in_schema,
    ))


def test_query_holder():
    foo_query = Q('SELECT (:foo || :bar) = :baz')
    qh = query_conf.QueriesHolder(dict(foo=foo_query))
    with pytest.raises(query_conf.QueryNotFound):
        qh.xxx


def test_pg_query_with_cast():
    PG_QUERY = u'''
    --- name: fancy_time
    SELECT 'infinity'::timestamp
    '''
    real_fancy_query = load(PG_QUERY).fancy_time

    assert real_fancy_query == Q("SELECT 'infinity'::timestamp")
