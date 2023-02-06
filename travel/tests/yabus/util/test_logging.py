# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import logging

from yabus.util.logging import GenericFilter


class TestGenericFilter(object):
    def test_filter(self, caplog):
        logger = logging.getLogger(__name__)

        with caplog.at_level('DEBUG', logger=__name__):
            logger.debug('foo')

            assert caplog.records
            caplog.clear()

            logger.addFilter(GenericFilter(lambda r: r.getMessage() != 'foo'))
            logger.debug('foo')

            assert not caplog.records
