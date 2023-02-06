from lib.decoder.label_decoder import LabelDecoder
from lib.types.bbox2d import Bbox2D


def test_image(image):
    box = Bbox2D(0,0, 20, 20)
    image.crop(box)
    print(type(image.data))
