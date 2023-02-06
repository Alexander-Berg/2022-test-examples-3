from extsearch.video.robot.youtube_grabber.meta_grabber.lib.description_parser import parse_video_page


def test_new_lines():
    return parse_video_page('test.html')


def test_quotes_unesacpe():
    return parse_video_page('exc_case.html')


def test_ellipsis():
    return parse_video_page('out1.html')
