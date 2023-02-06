from contextlib import contextmanager
import mock

import psycopg2.extensions as PGE


def mocked_conn(cursor=mock.Mock(spec=PGE.cursor)):
    retval = mock.MagicMock(spec=PGE.connection)
    retval.cursor.return_value = cursor
    return retval


@contextmanager
def mocked_qexec(queries2results, qexec_module_path):
    def side_effects(self, query, **kwargs):  # pylint: disable=W0613
        assert query in queries2results, (
            'Try execute unknown query %r, '
            'known: %r' % (query, queries2results)
        )
        return queries2results[query]

    with mock.patch('%s.qexec' % qexec_module_path) as mocked:
        mocked.side_effect = side_effects
        yield mocked
