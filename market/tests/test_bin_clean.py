# coding: utf-8

"""Integration tests for the ghoul library.
"""

import os
import subprocess
import time
import six
import yatest.common

import fixtures
from fixtures import cores_dir, ghoul_confs  # noqa


def run_ghoul_clean(cores_dir, ghoul_confs, dry_run=False, keep_count=None):  # noqa
    devnull = open(os.devnull, 'w')
    cmd = [
        yatest.common.binary_path('market/idx/admin/ghoul/bin/clean/market-ghoul-cleaner'),
        '--cores', cores_dir,
        '--conf-dir', ghoul_confs,
        '--core-len', str(fixtures.CORE_LEN),
        '--clean-time', str(int(time.time()) - fixtures.NOW + 3 * 24 * 60 * 60),
        '--verbose',
    ]
    if dry_run:
        cmd.append('--dry-run')
    if keep_count:
        cmd.extend(['--keep-count', str(keep_count)])
    return six.ensure_str(subprocess.check_output(cmd, stderr=devnull))


def test_ghoul_clean(cores_dir, ghoul_confs):  # noqa
    ''' Проверям логику удаления корок + режим dry-run.
    '''
    cores_before_remove = os.listdir(cores_dir)

    # Сперва проверим в режиме dry-run (имена корок к удалению просто пишутся)
    output = run_ghoul_clean(cores_dir, ghoul_confs, True)
    to_remove = [os.path.basename(line) for line in output.splitlines()]

    expected_to_remove = [
        'core.20180109130531.market-feedpars.6.ps01ht.923682.hash.disabled',
        'core.20180110125127.market-feedpars.6.ps01ht.290615.hash.disabled',
        'core.20180110154857.bash.6.ps01ht.49782.hash.disabled',
        'core.20180110224213.stats-convert.6.ps01ht.718996.hash.disabled',
        'core.20180110224941.stats-convert.6.ps01ht.844657.hash.disabled',
        'core.20180110225020.stats-convert.6.ps01ht.847392.hash.disabled',
        'core.20180110225550.stats-convert.6.ps01ht.940832.hash.disabled',
        'core.20180110225922.stats-convert.6.ps01ht.978225.hash.disabled',
        'core.20180110230342.stats-convert.6.ps01ht.9717.hash.disabled',
    ]

    assert sorted(to_remove) == expected_to_remove
    # Проверим, что правда ничего не удалили
    assert os.listdir(cores_dir) == cores_before_remove

    # Теперь запустим без dry-run и проверим, что все нужные корки удалены.
    run_ghoul_clean(cores_dir, ghoul_confs, False)
    cores_after_remove = os.listdir(cores_dir)
    assert set(cores_before_remove) - set(cores_after_remove) == set(to_remove)


def test_ghoul_clean_with_count_threshold(cores_dir, ghoul_confs):  # noqa
    '''
        Проверяем работу клинера в режиме с троешхолдом по количеству корок одного и того же бинаря
    '''
    expected = [
        'core.20180114154503.stats-convert.6.ps01ht.890912.hash.disabled',
        'core.20180116154325.stats-convert.6.ps01ht.43044.hash.disabled',
        'core.20180116154438.market-feedpars.6.ps01ht.33929.hash.disabled',
        'core.20180116154857.bash.6.ps01ht.49782.hash.disabled',
        'core.20180116154857.market-feedpars.6.ps01ht.49782.hash.disabled',
    ]

    run_ghoul_clean(cores_dir, ghoul_confs, False, 2)
    cores_after_remove = os.listdir(cores_dir)
    assert set(cores_after_remove) == set(expected)
