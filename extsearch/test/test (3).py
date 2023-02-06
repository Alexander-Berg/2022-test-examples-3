import yatest.common

FILE_NAMES = {
    'sideblock_old': 'user_sessions.txt',
    'sideblock_new': 'user_sessions_shows.txt',
    'sideblock_new2': 'user_sessions_shows2.txt',
    'exp_sideblock': 'user_sessions_exp_sideblock.txt',
    'desktop_manual': 'session_2017-04-19__13-57-32-519107.tsv',
}


# for uids like uu/0123456789abcdef
def escape(uid):
    return uid.replace('/', '_')


def unescape(uid):
    return uid.replace('_', '/')


def test_analyzer_output(mode, uid):
    exe = yatest.common.binary_path('extsearch/geo/tools/similar_orgs/analyze_sessions/analyze_sessions')
    args = ['-f', FILE_NAMES[mode], '-u', unescape(uid)]
    return yatest.common.canonical_execute(exe, args=args)


def read_uids(filename):
    uids = set()
    with open(filename) as fd:
        for line in fd:
            key, _ = line.split('\t', 1)
            uids.add(key)
    return uids


def pytest_generate_tests(metafunc):
    params = []
    for mode, fn in FILE_NAMES.iteritems():
        for uid in sorted(read_uids(fn)):
            params.append((mode, escape(uid)))

    metafunc.parametrize('mode,uid', params)
