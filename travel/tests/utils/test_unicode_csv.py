# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import io

from common.utils.unicode_csv import UnicodeWriter


def test_unicodewriter():
    buffer = io.BytesIO()
    writer = UnicodeWriter(buffer, delimiter=b';', lineterminator=b'\n')
    writer.writerow(['Ляля', 'Hello'])
    writer.writerow(['Ляля2', 'Hello2'])
    writer.writerow(['Ляля3', 'Hello3'])

    assert buffer.getvalue().decode('utf8').strip() == """
Ляля;Hello
Ляля2;Hello2
Ляля3;Hello3
""".strip()
