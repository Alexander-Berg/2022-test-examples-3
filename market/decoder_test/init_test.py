from lib.decoder.label_decoder import LabelDecoder


def test_decoding(config, image):
    decoder = LabelDecoder(config)
    ret, decoding_result = decoder.decode(image)
    print(decoding_result)
    assert ret
    print(decoding_result)
