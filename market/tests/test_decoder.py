import pytest
from market.robotics.cv.library.py.ya_barcode_reader import code_info
from market.robotics.cv.library.py.ya_barcode_reader import barcode_decoder


@pytest.fixture
def empty_info():
    return code_info.CodeInfo()


@pytest.fixture
def data():
    return {
        "CodeType": "QR",
        "Text": "PLT123456"
    }


def test_empty_code(empty_info: code_info.CodeInfo):
    assert not empty_info
    assert not empty_info.value
    assert not empty_info.type


def test_code_info_init():
    info = code_info.CodeInfo("Code39", "LOC123456")
    assert info
    assert info.type == "Code39"
    assert info.value == "LOC123456"


def test_code_info_from_dict(data: dict):
    info = code_info.CodeInfo.from_dict(data)
    assert info
    assert info.type == "QR"
    assert info.value == "PLT123456"


def test_decoder_import():
    decoder = barcode_decoder.YaBarcodeDecoder()
    assert hasattr(decoder, "decode")
