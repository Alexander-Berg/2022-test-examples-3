from audioformat import AudioFormat

from cStringIO import StringIO
import numpy as np
import tornado.testing


def parse_wave_header(audio_content):
    buf = StringIO()
    buf.write(audio_content)
    audio_format = AudioFormat(buf)
    return audio_format, buf.tell()


def extract_audio(audio_data, header_size, dtype):
    data_pos = header_size + 4
    last_sample = (len(audio_data) - data_pos) / 4 * 4
    audio_data = audio_data[data_pos:data_pos + last_sample]
    audio = np.fromstring(audio_data, dtype=dtype).astype('float32')
    if dtype == 'int16':
        audio /= (2 ** 15)  # scale to -1..1
    return audio


def get_version():
    return 'test'


def build_spectrogram(data, sr=8000, fmin=300., fmax=3000., n_bins=64, hop_length=256):
    return np.zeros(shape=(52, 64))


class FingerprintsCalculator:
    def __init__(self, tfModelFile, stathandler, confidenceThreshold=None, thresholdParams=None):
        self.fingerprints = 'ZEAbI6og4HqbnTdcZET7q6tg6GuLnTdcRETTqytC6AGLnTfUQkyTAzoC4ECrnS3OYkwDI/4C4UyrnbzuZUQDI9qCwVmruLzuXEQTJ9KCwVkLKKXGbgDTLXtCgFEDKK1GdADzLVtCIFETJP9GdICxLZhTYFqbAP8kcgj1IYEHYmorEPoEchh3A6kGKkiyEH6EcRg3E7kEI0GyFGqEcRQ3IbAU40GyAeK0cRRHIaFWSUMKCeRERhSHIcHiCEcCKORGVASzIdCgCEeSNObOcATzMViSSE+bMeZWUoz3M3nSwEo7EX4GWoi3F61CokI7OHxGfIi3Nb0gsUO+MW9mfYyXIb82iUuOAX52fQzTI7L26AOOCW7GXQjDJQDm4AIWKG7GfEiFJdymYEeWLG5CfQjBJdyzaEufZV4iXQjXPxj36EsfYUoiWEhVFzn3oEEGYGiCfUgzN703oEEGVGiCf1w3N5Q3qEMGAEpyc9jXI5Dn6EIGAEpwUtiXJcDnKGIWAGpSUtiXNZjjImJWAGpSeNyXMZijGkPXAWJyWF3fMRj3XlPfAWJyWl3PFb13BFPvAkwCfl2LFfw1BFP2EkxC'  # noqa
        self.confidence = '///f/7////Z+//7////ff////89////X//+f3///////////+//f/67///vf///v6f////9//vv13/vv7/f///////7f7/9/7r//+W7//v////Z/779P/9///////v/////9///vn////////z//+/8O//////v9////9/fu99/f/3/97///////d/9///v9////////f////3f9//+/////ff/X////6//+///v3/9//nv//3/+/////7/f//9//3/b7//v19////9v1/////3///u///ff/7f79d/7//7f3//v9/+7//t/jfX//+4////72//f3//1/v7///+f7/p9z//v3+fv/3/r/+/v3/f////f/X+5/1////p/+v///r/5///9v97/5t/b/7vt57/+//9+/9+v37v55//7//3//9//27uZ339////e/v1//Lv7///r//3v3////bvf//vr/v9v/t19/7v/6/8/n/9//9///7+/+797f9///9/v//7/+7//198/vv/v1/7//3u/5v7/+//f3z733///++/v/d6//7//7b7/9e/u/9P//7/7/f7+/+//99//'    # noqa
        self.queryid = '5d149450-b5c6-11e5-971d-2a0206b8b010'
        self.expected_answer = '2381618'

    def calc_fingerprints(self, spectrum):
        return self.fingerprints, self.confidence


class MusicClassifier:
    def __init__(self, tfClassificationModelFile, sample_size):
        pass

    def predict(self, spectrum, hop_size):
        return [x/10. for x in range(10)]


class TestConnection:
    def __init__(self, calls):
        if calls % 2:
            self.SearchScript = 'http://vla1-0327-vla-misc-new-musicmic-mmeta-17006.gencfg-c.yandex.net:17006/yandsearch?'
        else:
            self.SearchScript = 'http:/localhost:{}/yandsearch?'.format(tornado.testing.get_unused_port())


class TestIterator:
    def __init__(self):
        self.calls = 0

    def Next(self):
        self.calls += 1
        return TestConnection(self.calls)


class TestOptions:
    def __init__(self):
        self.MaxAttempts = 5


class HttpSearchClient:
    def __init__(self, Script, Options, Descr='', ServiceDiscoveryOptions=''):
        self.Options = TestOptions()

    def PossibleConnections(self, int_param):
        return TestIterator()


classifier = MusicClassifier('', 0)
fingerprints_calculator = FingerprintsCalculator('', None)
