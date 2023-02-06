# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.serialization.experiment_flags import parse_experiment_flags


@pytest.mark.parametrize('value, expected', (
    ('', frozenset()),
    (',junk', frozenset()),
    ('RASPWIZARDS-557,junk,', {ExperimentFlag.EXPERIMENTAL_SEARCH}),
))
def test_parse_experiment_flags(value, expected):
    assert parse_experiment_flags(value) == expected
