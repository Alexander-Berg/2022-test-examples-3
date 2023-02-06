import yatest.common


def test():
    res = []
    for tag in ['TG_L1', 'TG_L2', 'TG_L3']:
        with open('%s.txt' % tag, 'w') as fd:
            yatest.common.execute(
                [
                    yatest.common.binary_path(
                        'extsearch/geo/tools/generate_ignore_string_by_tag/generate_ignore_string_by_tag'
                    ),
                    '-t',
                    tag,
                    '-m',
                    '585',
                    '-s',
                ],
                stdout=fd,
            )
            res.append(yatest.common.canonical_file('%s.txt' % tag, local=True))
    return res


def test_remove():
    with open('TG_L1_r329.txt', 'w') as fd:
        yatest.common.execute(
            [
                yatest.common.binary_path(
                    'extsearch/geo/tools/generate_ignore_string_by_tag/generate_ignore_string_by_tag'
                ),
                '-t',
                'TG_L1',
                '-r',
                '329,316,176,177,178',
            ],
            stdout=fd,
        )
        return yatest.common.canonical_file('TG_L1_r329.txt', local=True)


def test_max():
    with open('TG_L1_m100.txt', 'w') as fd:
        yatest.common.execute(
            [
                yatest.common.binary_path(
                    'extsearch/geo/tools/generate_ignore_string_by_tag/generate_ignore_string_by_tag'
                ),
                '-t',
                'TG_L1',
                '-m',
                '100',
            ],
            stdout=fd,
        )
        return yatest.common.canonical_file('TG_L1_m100.txt', local=True)


def test_add():
    with open('TG_L1_m100_a0_25.txt', 'w') as fd:
        yatest.common.execute(
            [
                yatest.common.binary_path(
                    'extsearch/geo/tools/generate_ignore_string_by_tag/generate_ignore_string_by_tag'
                ),
                '-t',
                'TG_L1',
                '-m',
                '100',
                '-a',
                '0,25',
            ],
            stdout=fd,
        )
        return yatest.common.canonical_file('TG_L1_m100_a0_25.txt', local=True)
