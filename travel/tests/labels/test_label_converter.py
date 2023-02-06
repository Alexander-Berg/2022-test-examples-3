
from travel.cpa.data_processing.lib.label_converter import LABEL_CONVERTERS
from travel.cpa.data_processing.lib.protobuf_utils import camel_to_snake


def test_label_generic():
    f = LABEL_CONVERTERS['generic'].modified_key_func
    assert 'label_utm_source' == f('UtmSource')
    assert 'label_hotel_id' == f('HotelId')


def test_label_avia():
    f = LABEL_CONVERTERS['avia'].modified_key_func
    assert 'label_national_version' == f('NationalVersion')
    assert 'label_pp' == f('Pp')


def test_label_hotels():
    f = LABEL_CONVERTERS['hotels'].modified_key_func
    assert 'label_source' == f('Source')
    assert 'label_yandex_uid' == f('YandexUid')


def test_label_train():
    f = LABEL_CONVERTERS['train'].modified_key_func
    assert 'source_utm_source' == f('UtmSource')
    assert 'label_ip' == f('Ip')


def test_camel_to_snake():
    assert '' == camel_to_snake('')
    assert 'l' == camel_to_snake('l')
    assert 'u' == camel_to_snake('U')
    assert 'ul' == camel_to_snake('Ul')
    assert 'l_u' == camel_to_snake('lU')
    assert 'll_u' == camel_to_snake('llU')
    assert 'l_uu' == camel_to_snake('lUU')
    assert 'l_ul_u' == camel_to_snake('lUlU')
    assert 'ul_ul' == camel_to_snake('UlUl')
