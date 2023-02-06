from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender import SignatureMaker


def test_get_signatures_data(signature_maker: SignatureMaker, signature_response, mock_signatures_data):
    data = signature_maker.get_signatures_data('test_sig_valid')
    assert data == mock_signatures_data


def test_get_signatures_http_err(signature_maker: SignatureMaker):
    data = signature_maker.get_signatures_data('test_sig_err')
    assert data == {}


def test_get_signatures_decode_err(signature_maker: SignatureMaker):
    data = signature_maker.get_signatures_data('test_sig_wrong')
    assert data == {}


def test_get_signatures_none_video(signature_maker: SignatureMaker):
    data = signature_maker.get_signatures(None)
    assert data == {}


def test_get_signatures_video_signatures_url(signature_maker: SignatureMaker, signature_mock_video_info):
    signature_mock_video_info.transcoder_info = {
        'SignaturesStatusStr': 'ESSDone',
        'SignaturesUrl': 'test_sig_valid'
    }
    data = signature_maker.get_signatures(signature_mock_video_info)
    assert 'VisWord64v2' in data


def test_get_signatures_video_signatures_url_no_url(signature_maker: SignatureMaker, signature_mock_video_info):
    signature_mock_video_info.transcoder_info = {
        'SignaturesStatusStr': 'ESSDone'
    }
    data = signature_maker.get_signatures(signature_mock_video_info)
    assert data == {}


def test_get_signatures_video_speech_to_text_url(signature_maker: SignatureMaker,
                                                 signature_response, signature_mock_video_info):
    signature_mock_video_info.transcoder_info = {
        'SpeechToTextStatusStr': 'ES2TSDone',
        'SpeechToTextUrl': 'test_sig_valid'
    }
    data = signature_maker.get_signatures(signature_mock_video_info)
    assert 'VisWord64v2' in data


def test_get_signatures_video_speech_to_text_url_no_url(signature_maker: SignatureMaker, signature_mock_video_info):
    signature_mock_video_info.transcoder_info = {
        'SpeechToTextStatusStr': 'ES2TSDone',
    }
    data = signature_maker.get_signatures(signature_mock_video_info)
    assert data == {}
