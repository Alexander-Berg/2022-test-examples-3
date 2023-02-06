from contextvars import copy_context
from typing import Optional

import pytest

from sendr_core.context import BaseCoreContext


class DefaultContext(BaseCoreContext):
    x: int
    y: Optional[int] = None
    z: int = 10


class TestBaseCoreContext:
    @pytest.fixture
    def context(self):
        return BaseCoreContext()

    def test_supports_getattr(self, context):
        assert getattr(context, 'request_id', 'test') == 'test'

    def test_request_id_undefined_by_default(self, context):
        with pytest.raises(AttributeError):
            context.request_id

    def test_keeps_value_on_context_enter(self, context):
        context.request_id = 'test-value'

        def some_function():
            assert context.request_id == 'test-value'

        ctx = copy_context()
        ctx.run(some_function)

    def test_resets_value_on_context_exit(self, context):
        context.request_id = 'test-value'

        def some_function():
            context.request_id = 'other-test-value'

        ctx = copy_context()
        ctx.run(some_function)
        assert context.request_id == 'test-value'


class TestDefaultValues:
    @pytest.fixture
    def context(self):
        return DefaultContext()

    def test_no_default(self, context):
        with pytest.raises(AttributeError):
            context.x

    def test_none_default(self, context):
        assert context.y is None

    def test_int_default(self, context):
        assert context.z == 10
