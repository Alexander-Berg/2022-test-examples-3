import pytest

import extsearch.video.sepe.metrics_new.launch as launch


class ResponseMock(object):
    @property
    def text(self):
        return "Raw text"

    def json(self):
        raise ValueError("Error parsing json")


@launch._catch_parsing_error
def risingerror(someparameter, x=18):
    assert someparameter == 42
    assert x == 18
    return ResponseMock()


def test_something():
    with pytest.raises(launch.InternalError) as exc_info:
        risingerror(42)
    assert "Raw text" in str(exc_info.value)
