from nose.tools import assert_equals, assert_not_equal, assert_regexp_matches, assert_in, assert_not_in, assert_list_equal, assert_is_none, assert_is_not_none, assert_greater

import os
import sys
import psycopg2
import datetime
import time

STATE_0 = 0
STATE_1 = 1
STATE_2 = 2
STATE_3 = 3

LEFT_EDGE = 0
RIGHT_EDGE = 10
CENTER = RIGHT_EDGE / 2

def setup():
    global connection
    global cursor
    conninfo = os.environ['RESHARDDB_CONNINFO']
    connection = psycopg2.connect(conninfo)
    connection.set_isolation_level(0)
    cursor = connection.cursor()

def teardown():
    connection.close()

def run(sql, *args):
    bound_sql = cursor.mogrify(sql, args)
    cursor.execute(bound_sql)
    return cursor.fetchall()

def clear_all_migration_records():
    run('delete from resharding.migrations returning 0;')

def insert_migration(state, start_gid, end_gid):
    return run ('''
        insert into resharding.migrations (state, gid_range)
            values (%s, int8range(%s, %s, '[]'))
        returning 0;
    ''', state, start_gid, end_gid)


def set_migration_state(gid, allowed_states, state_to):
    return run('select end_gid, state from resharding.set_migration_state(%s, %s, %s);',
        gid, allowed_states, state_to)

def get_migrations_range():
    return run('select end_gid, state from resharding.get_migrations_range();')

class TestReshardDb():
    def setup(self):
        clear_all_migration_records()

    def check_single_migration(self, state_before, gid, allowed_states, state_to, expected_ret):
        for s in state_before:
            insert_migration(*s)
        rows = set_migration_state(gid, allowed_states, state_to)
        assert_equals(rows, expected_ret)

    def test_set_migration_state(self):
        cases = [
            # sketch the state transition set_migrations is expected to perform;
            # 0 denotes gids in state_0, 1 denotes gids in state_1
            # e.g [00] -> [1][0] means that gid on the left edge changes state from 0 to 1
            [ # [00] -> [1][0]
                # migrations to insert before testing: (state, start_gid, end_gid)
                [(STATE_0, LEFT_EDGE, RIGHT_EDGE)],
                # parameters passed to set_migration_state
                LEFT_EDGE, [STATE_0], STATE_1,
                # what set_migration_state is expected to return
                [(LEFT_EDGE, STATE_1), (RIGHT_EDGE, STATE_0)]
            ],
            [ # [00] -> [0][1]
                [(STATE_0, LEFT_EDGE, RIGHT_EDGE)],
                RIGHT_EDGE, [STATE_0], STATE_1,
                [(RIGHT_EDGE - 1, STATE_0), (RIGHT_EDGE, STATE_1)]
            ],
            [ # [000] -> [0][1][0]
                [(STATE_0, LEFT_EDGE, RIGHT_EDGE)],
                CENTER, [STATE_0], STATE_1,
                [(CENTER - 1, STATE_0), (CENTER, STATE_1), (RIGHT_EDGE, STATE_0)]
            ],
            [ # [0][1] -> [11]
                [(STATE_0, LEFT_EDGE, LEFT_EDGE), (STATE_1, LEFT_EDGE + 1, RIGHT_EDGE)],
                LEFT_EDGE, [STATE_0], STATE_1,
                [(RIGHT_EDGE, STATE_1)]
            ],
            [ # [1][0] -> [11]
                [(STATE_1, LEFT_EDGE, RIGHT_EDGE - 1), (STATE_0, RIGHT_EDGE, RIGHT_EDGE)],
                RIGHT_EDGE, [STATE_0], STATE_1,
                [(RIGHT_EDGE, STATE_1)]
            ],
            [ # [1][00] -> [11][0]
                [(STATE_1, LEFT_EDGE, LEFT_EDGE), (STATE_0, LEFT_EDGE + 1, RIGHT_EDGE)],
                LEFT_EDGE + 1, [STATE_0], STATE_1,
                [(LEFT_EDGE + 1, STATE_1), (RIGHT_EDGE, STATE_0)]
            ],
            [ # [00][1] -> [0][11]
                [(STATE_0, 0, RIGHT_EDGE - 1), (STATE_1, RIGHT_EDGE, RIGHT_EDGE)],
                RIGHT_EDGE - 1, [STATE_0], STATE_1,
                [(RIGHT_EDGE - 2, STATE_0), (RIGHT_EDGE, STATE_1)]
            ],
            [ # [1][0][1] -> [111]
                [
                    (STATE_1, LEFT_EDGE, CENTER - 1),
                    (STATE_0, CENTER, CENTER),
                    (STATE_1, CENTER + 1, RIGHT_EDGE)
                ],
                CENTER, [STATE_0], STATE_1,
                [(RIGHT_EDGE, STATE_1)]
            ],
            [ # state not allowed -> return special tuple
                [(STATE_0, LEFT_EDGE, RIGHT_EDGE)],
                LEFT_EDGE, [STATE_3], STATE_1,
                [(65536, 0), (RIGHT_EDGE, STATE_0)]
            ],
        ]
        for c in cases:
            yield self.check_single_migration, c[0], c[1], c[2], c[3], c[4]

    def test_get_migrations_range(self):
        insert_migration(STATE_0, 0, 10)
        rows = get_migrations_range()
        assert_equals(rows, [(10, STATE_0)])
