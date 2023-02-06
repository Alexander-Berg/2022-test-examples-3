# -*- coding: utf-8 -*-

import os
import sys

from travel.avia.admin.lib.logs import stdstream_file_capturing, remove_secrets


def test_stdstream_file_capturing(tmpdir):
    capture = tmpdir.join('capture.txt')
    stdout = tmpdir.join('stdout.txt')
    stderr = tmpdir.join('stderr.txt')

    pid = os.fork()
    if not pid:
        sys.stdout.flush()
        sys.stderr.flush()

        os.close(1)
        os.close(2)
        with stdout.open('wt') as stdout_file, stderr.open('wt') as stderr_file, capture.open('wt') as capture_file:
            os.dup2(stdout_file.fileno(), 1)
            os.dup2(stderr_file.fileno(), 2)

            os.write(1, 'before stdout\n')
            os.write(2, 'before stderr\n')

            with stdstream_file_capturing(capture_file):
                os.write(1, 'ssss ')
                os.write(2, 'yyyy')

            os.write(1, 'after stdout')
            os.write(2, 'after stderr')

        os._exit(0)

    os.waitpid(pid, 0)

    assert capture.open().read() == 'ssss yyyy'
    assert stdout.open().read() == 'before stdout\nafter stdout'
    assert stderr.open().read() == 'before stderr\nafter stderr'


def test_remove_secrets():
    input_dict = {
        'url': 'shtp://abc/bcd.php',
        'login': 'logmein',
        'password': 'aaa bbb ccc ddd',
        'passwd': 'iamroot',
        'hash': 'abcd345efbc654',
    }
    input_copy = input_dict.copy()
    output_dict = remove_secrets(input_dict)

    assert input_dict == input_copy
    assert input_dict != output_dict
    for key in input_dict:
        assert key in output_dict
    assert output_dict['password'] != input_dict['password']
    assert output_dict['passwd'] != input_dict['passwd']
    assert output_dict['hash'] != input_dict['hash']
