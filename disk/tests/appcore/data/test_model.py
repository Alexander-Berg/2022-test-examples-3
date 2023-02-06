# encoding: UTF-8

import unittest

from hamcrest import *

from appcore.data.model import Page, Pageable


class PageTestCase(unittest.TestCase):
    def test_page_properties(self):
        page = Page(
            items=[1, 2, 3],
            offset=1,
            size=3,
            total=10,
        )

        assert_that(
            page,
            has_properties(
                items=equal_to([1, 2, 3]),
                offset=equal_to(1),
                size=equal_to(3),
                total=equal_to(10),
            ),
        )


class PageableTestCase(unittest.TestCase):
    def test_oageable_properties(self):
        pageable = Pageable(
            offset=100,
            size=500,
        )

        assert_that(
            pageable,
            has_properties(
                offset=equal_to(100),
                size=equal_to(500),
                end_offset=equal_to(600),
            ),
        )
