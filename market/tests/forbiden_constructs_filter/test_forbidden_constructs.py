# -*- coding: utf-8 -*-

import os
from tempfile import NamedTemporaryFile

import pytest
from yatest.common import source_path

FORBIDDEN = ['Y_FAIL(', 'Y_UNREACHABLE(', 'Y_VERIFY(']
ALLOWED = ['Y_ASSERT(', 'Y_VERIFY_DEBUG(', 'Y_ENSURE(']
CONTENT_TEMPLATE = '#define FLATBUFFERS_ASSERT(condition) {}condition)'
EXTENSIONS = ['.cpp', '.h']
MESSAGE = f'https://st.yandex-team.ru/MARKETOUT-36888 : \
Please do not use {FORBIDDEN} construct. Use {ALLOWED} instead'


def get_forbidden_usage(line: str):
    sub = ' Y_'
    start = line.find(sub)
    while start != -1:
        start += 1
        for forbidden in FORBIDDEN:
            if line.startswith(forbidden, start):
                return f"{line}\n{'^'.rjust(start + 1, '-')}"

        start = line.find(sub, start)


def get_forbdden_construct_in_file(file_path: str):
    filename, file_extension = os.path.splitext(file_path)
    if file_extension.lower() not in EXTENSIONS:
        return

    with open(file_path, 'r') as file:
        forbidden_usages = map(get_forbidden_usage, file)
        for place in filter(None.__ne__, forbidden_usages):
            yield f'{place}\nin {file_path}\n\n'


class TestForbiddenConstructs:
    @pytest.mark.parametrize("construct", FORBIDDEN)
    def test_contains_forbidden(self, construct):
        message = f"Method should recognize {construct}"
        with NamedTemporaryFile('w+', suffix='.h') as ntf:
            ntf.write(CONTENT_TEMPLATE.format(construct))
            ntf.flush()
            assert list(get_forbdden_construct_in_file(ntf.name)), message

    @pytest.mark.parametrize("construct", ALLOWED)
    def test_not_contains_forbidden(self, construct):
        message = f'Method should allow {construct}'
        with NamedTemporaryFile('w+', suffix='.h') as ntf:
            ntf.write(CONTENT_TEMPLATE.format(construct))
            ntf.flush()
            assert not list(get_forbdden_construct_in_file(ntf.name)), message

    def test_report_does_not_contain_forbidden_construct(self):
        root_path = source_path(os.path.join('market', 'report'))
        places = []
        for top_dir, _, file_names in os.walk(root_path):
            if 'arcadia/market/report/test' in top_dir:
                continue
            for file_name in file_names:
                path = os.path.join(top_dir, file_name)
                places.extend(get_forbdden_construct_in_file(path))

        places_count = len(places)

        assert places_count == 0, f'{MESSAGE}\n\n{"".join(places)}'
