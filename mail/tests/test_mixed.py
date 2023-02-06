# coding: utf-8
import pytest

from pymdb.types import MailLabelDef
from ora2pg.mixed import Mixed, build_mixed, expand_mixed


def mixed_only_flags(*flags):
    return Mixed(flags, [], [], False)


def system_label(name):
    return MailLabelDef(name=name, type='system')


def type_label(code):
    return MailLabelDef(name=str(code), type='type')


def label_keys(labels):
    return set((l.type, l.name) for l in labels)

MIXED_CASES = [
    (0, mixed_only_flags()),
    (32, mixed_only_flags('recent')),
    (2048, mixed_only_flags('seen')),
    (128, mixed_only_flags('deleted')),
    (128 + 2048, mixed_only_flags('deleted', 'seen')),
    (3648, Mixed(
        ['seen'],
        [],
        [system_label('answered'),
         system_label('draft'),
         system_label('forwarded')], False)),
    (4, Mixed([], ['spam'], [], False)),
    (4 + 2048, Mixed(['seen'], ['spam'], [], False)),
    (34342912, Mixed(
        ['seen'],
        [],
        [type_label(4), type_label(6)], False)),
    (153878528, Mixed(
        [], [], [type_label(18), type_label(22)], False)),
    (153878528 + 128 + 4, Mixed(
        ['deleted'],
        ['spam'],
        [type_label(18), type_label(22)],
        False)),
    (8192, Mixed([],[],[],True)),
]


@pytest.mark.parametrize(('raw_value', 'mixed'), MIXED_CASES)
def test_expand_mixed(raw_value, mixed):
    real = expand_mixed(raw_value)
    assert set(real.flags) == set(mixed.flags), 'Got different flags'
    assert set(real.attributes) == set(mixed.attributes), \
        'Got different attributes'
    assert label_keys(real.labels) == label_keys(mixed.labels), \
        'Got different labels'
    assert real.pop3_deleted == mixed.pop3_deleted, 'Got different pop3_deleted'


@pytest.mark.parametrize(('expected_raw_value', 'mixed'), MIXED_CASES)
def test_build_mixed(expected_raw_value, mixed, ):
    raw_value = build_mixed(
        flags=mixed.flags,
        attributes=mixed.attributes,
        labels=mixed.labels,
        pop3_deleted=mixed.pop3_deleted)
    assert raw_value == expected_raw_value
