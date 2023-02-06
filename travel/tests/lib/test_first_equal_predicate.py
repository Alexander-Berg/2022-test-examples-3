# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.wizards.train_wizard_api.lib.first_equal_predicate import FirstEqualPredicate


def test_first_equal_predicate():
    pred = FirstEqualPredicate(3)
    assert map(pred, [1, 2, 3, 3, 4, 5]) == [False, False, True, False, False, False]
