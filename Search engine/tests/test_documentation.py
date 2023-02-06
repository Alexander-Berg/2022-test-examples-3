# coding=utf-8

import commands

import pytest
import os
from os import path

ROOT_DIR = path.realpath(path.join(path.dirname(__file__), '..'))


@pytest.mark.skip(reason="no way to checkout bb from sandbox")
@pytest.mark.smoke
@pytest.mark.parametrize("script", [path.join(ROOT_DIR, 'docs', 'cookbook', 'examples', 'clone_repo_ssh.sh'),
                                    path.join(ROOT_DIR, 'docs', 'cookbook', 'examples', 'create_virtualenv.sh')])
def test_clone_repo(tmpdir, script):
    cwd = os.getcwd()
    try:
        os.chdir(tmpdir.strpath)
        status, output = commands.getstatusoutput(script)
        assert status == 0, output
    finally:
        os.chdir(cwd)


@pytest.mark.skip(reason="no way to checkout bb from sandbox")
@pytest.mark.smoke
@pytest.mark.parametrize("script", [path.join(ROOT_DIR, 'docs', 'cookbook', 'examples', 'request_json.sh'),
                                    path.join(ROOT_DIR, 'docs', 'cookbook', 'examples', 'generate_example.sh')])
def test_doc_scripts(script):
    status, output = commands.getstatusoutput(script)
    assert status == 0, output
