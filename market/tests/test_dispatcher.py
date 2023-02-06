import pytest

from yamarec_metarouter.exceptions import DispatchError
from yamarec_metarouter.pattern import Pattern


def test_dispatcher_dispatches_correctly(dispatcher):
    assert dispatcher.dispatch("/product/123") == ("model card", Pattern("/product/([0-9]+)(\?|$)"))
    assert dispatcher.dispatch("/product/?id=123&track=weird") == ("weird model card", Pattern("/product//?\?id=([0-9]+)($|&|#)"))
    assert dispatcher.dispatch("/search?text=test") == ("search", Pattern("/search(\?|$)"))
    assert dispatcher.dispatch("/summertime???") == ("summertime", Pattern("/summertime(\?|$)"))


def test_dispatcher_raises_error_if_cannot_handle(dispatcher):
    with pytest.raises(DispatchError):
        dispatcher.dispatch("/wintertime")
    with pytest.raises(DispatchError):
        dispatcher.dispatch("/product/abc")
    with pytest.raises(DispatchError):
        dispatcher.dispatch("/search/")
    with pytest.raises(DispatchError):
        dispatcher.dispatch("/searc")
    with pytest.raises(DispatchError):
        dispatcher.dispatch("")
