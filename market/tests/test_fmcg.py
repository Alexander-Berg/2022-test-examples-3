# -*- coding: utf-8 -*-

import pytest
import yatest

from getter.service import fmcg


def gen_suggests_file(notes):
    suggest_file = yatest.common.test_output_path('suggest.tsv')
    with open(suggest_file, 'w') as f:
        for note in notes:
            f.write(note + '\n')
    return suggest_file


def check_fmcg_resource(suggest_file):
    resource = fmcg.create_fmcg_resources()['suggest-fmcg-categories.tsv']
    resource.check(suggest_file)
    return resource._checked


def test_fcgi_validation():
    suggest_file = gen_suggests_file(('Мобильные телефоны\t100\t{"entity": "category", "id": "91491"}',
                                      'Молочные продукты\t10\t{"entity": "category", "id": "100"}'))
    assert check_fmcg_resource(suggest_file)


@pytest.mark.parametrize('note, raise_msg', [
    ('Плохой вес\t0.25\t{"entity": "category", "id": "200"}', 'invalid literal for int() with base 10: \'0.25\''),
    ('Неправильное количество полей\t5', 'not enough fields'),
    ('Невалидный json\t50\t{"entity": "category", "id": "300}', 'Unterminated string'),
])
def test_bad_note(note, raise_msg):
    suggest_file = gen_suggests_file((note,))
    with pytest.raises(Exception) as e:
        check_fmcg_resource(suggest_file)
    assert raise_msg in str(e.value)
