#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main


class T(TestCase):
    def test_static_info(self):
        response = self.report.request_json('place=static_info')
        self.assertFragmentIn(
            response,
            {
                'knownThumbnails': [
                    {
                        'namespace': 'marketpic',
                        'thumbnails': [
                            {'height': 50, 'name': '50x50', 'width': 50},
                            {'height': 70, 'name': '55x70', 'width': 55},
                            {'height': 80, 'name': '60x80', 'width': 60},
                            {'height': 100, 'name': '74x100', 'width': 74},
                            {'height': 75, 'name': '75x75', 'width': 75},
                            {'height': 120, 'name': '90x120', 'width': 90},
                            {'height': 100, 'name': '100x100', 'width': 100},
                            {'height': 160, 'name': '120x160', 'width': 120},
                            {'height': 150, 'name': '150x150', 'width': 150},
                            {'height': 240, 'name': '180x240', 'width': 180},
                            {'height': 250, 'name': '190x250', 'width': 190},
                            {'height': 200, 'name': '200x200', 'width': 200},
                            {'height': 320, 'name': '240x320', 'width': 240},
                            {'height': 300, 'name': '300x300', 'width': 300},
                            {'height': 400, 'name': '300x400', 'width': 300},
                            {'height': 600, 'name': '600x600', 'width': 600},
                            {'height': 800, 'name': '600x800', 'width': 600},
                            {'height': 1200, 'name': '900x1200', 'width': 900},
                            {'height': 124, 'name': 'x124_trim', 'width': 166},
                            {'height': 166, 'name': 'x166_trim', 'width': 248},
                            {'height': 248, 'name': 'x248_trim', 'width': 332},
                            {'height': 332, 'name': 'x332_trim', 'width': 496},
                        ],
                    },
                    {
                        'namespace': 'marketpic_scaled',
                        'thumbnails': [
                            {'height': 50, 'name': '50x50', 'width': 50},
                            {'height': 70, 'name': '55x70', 'width': 55},
                            {'height': 80, 'name': '60x80', 'width': 60},
                            {'height': 100, 'name': '74x100', 'width': 74},
                            {'height': 75, 'name': '75x75', 'width': 75},
                            {'height': 120, 'name': '90x120', 'width': 90},
                            {'height': 100, 'name': '100x100', 'width': 100},
                            {'height': 160, 'name': '120x160', 'width': 120},
                            {'height': 150, 'name': '150x150', 'width': 150},
                            {'height': 240, 'name': '180x240', 'width': 180},
                            {'height': 250, 'name': '190x250', 'width': 190},
                            {'height': 200, 'name': '200x200', 'width': 200},
                            {'height': 320, 'name': '240x320', 'width': 240},
                            {'height': 300, 'name': '300x300', 'width': 300},
                            {'height': 400, 'name': '300x400', 'width': 300},
                            {'height': 600, 'name': '600x600', 'width': 600},
                            {'height': 800, 'name': '600x800', 'width': 600},
                            {'height': 1200, 'name': '900x1200', 'width': 900},
                            {'height': 124, 'name': 'x124_trim', 'width': 166},
                            {'height': 166, 'name': 'x166_trim', 'width': 248},
                            {'height': 248, 'name': 'x248_trim', 'width': 332},
                            {'height': 332, 'name': 'x332_trim', 'width': 496},
                        ],
                    },
                    {
                        'namespace': 'mpic',
                        'thumbnails': [
                            {'height': 50, 'name': '1hq', 'width': 50},
                            {'height': 100, 'name': '2hq', 'width': 100},
                            {'height': 75, 'name': '3hq', 'width': 75},
                            {'height': 150, 'name': '4hq', 'width': 150},
                            {'height': 200, 'name': '5hq', 'width': 200},
                            {'height': 250, 'name': '6hq', 'width': 250},
                            {'height': 120, 'name': '7hq', 'width': 120},
                            {'height': 240, 'name': '8hq', 'width': 240},
                            {'height': 500, 'name': '9hq', 'width': 500},
                            {'height': 124, 'name': 'x124_trim', 'width': 166},
                            {'height': 166, 'name': 'x166_trim', 'width': 248},
                            {'height': 248, 'name': 'x248_trim', 'width': 332},
                            {'height': 332, 'name': 'x332_trim', 'width': 496},
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
