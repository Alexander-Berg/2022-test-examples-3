# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from library.python import resource
from PIL import Image
from StringIO import StringIO

from travel.rasp.admin.lib.image import svg2image


def test_svg2image():
    image = svg2image(resource.find('lib/ref-image.svg'), (13, 13)).convert('RGBA')
    reference_image = Image.open(StringIO(resource.find('lib/ref-image.png')))
    assert image.mode == reference_image.mode
    assert image.size == reference_image.size
    assert image.category == reference_image.category
    assert image.getpalette() == reference_image.getpalette()
