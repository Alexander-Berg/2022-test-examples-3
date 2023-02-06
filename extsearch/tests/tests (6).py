import json
import yatest.common


def probe(video):
    ffprobe = yatest.common.binary_path('contrib/libs/ffmpeg-3.4.1/bin/ffprobe/ffprobe')

    probe_out = 'probe.json'
    with open(probe_out, 'w') as f:
        yatest.common.execute([ffprobe, '-i', video, '-print_format', 'json', '-show_format', '-show_streams'], stdout=f)

    with open(probe_out, 'r') as f:
        return json.load(f)


def test_mp4():
    binary = yatest.common.binary_path('extsearch/video/robot/rt_transcoder/chup/tool/tool')

    yatest.common.execute([binary, '-i', '2.mp4'])
    for i in range(1, 48):
        info = probe('segment-%d.mp4' % i)

        assert len(info['streams']) == 1

        s = info['streams'][0]
        assert s['width'] == 1280
        assert s['height'] == 720

        duration = float(s['duration'])
        assert 10 < duration and duration < 40
