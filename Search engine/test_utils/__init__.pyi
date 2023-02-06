from search.martylib.types import *

import contextdecorator
import contextlib
import grpc
import logging
import unittest
# noinspection PyProtectedMember
from grpc._server import _Context
from sqlalchemy.orm.session import Session as _Session

from search.martylib.core.storage import Storage
from search.martylib.proto.structures import auth_pb2


GrpcStatusOptionalSequence = Union[grpc.StatusCode, Iterable[grpc.StatusCode]]

@contextlib.contextmanager
def fuzz(min_sleep: Number = ..., max_sleep: Number = ..., sleep_before: bool = ..., sleep_after: bool = ...) -> None: ...


def setup(additional_loggers: Optional[Set[String]] = ..., logger_class: Optional[Type[logging.Logger]] = ...) -> None: ...


class MockAuth(contextdecorator.ContextDecorator):
    _login: Optional[String]
    _roles: Optional[Iterable[String]]
    _previous_info: Optional[auth_pb2.AuthInfo]
    _auth_info = Optional[auth_pb2.AuthInfo]

    def __init__(self, login: Optional[String] = ..., roles: Optional[Iterable[String]] = ..., auth_info: Optional[auth_pb2.AuthInfo] = ...): ...

    def __enter__(self): ...

    def __exit__(self, exc_type: Type[BaseException], exc_value: Any, traceback: TracebackType): ...

    def __call__(self, *args, **kwargs): ...


class TestCase(unittest.TestCase):
    ADDITIONAL_LOGGERS: Optional[Set[String]]
    LOGGER_CLASS: Optional[Type[logging.Logger]]

    logger: logging.Logger
    mock_auth: MockAuth
    storage: Storage

    @classmethod
    def setUpClass(cls) -> None: ...

    @staticmethod
    @contextlib.contextmanager
    def mock_request(
        expected_grpc_status_code: Optional[GrpcStatusOptionalSequence] = ..., expected_grpc_details_regexp: Optional[String] = ...,
        *args, **kwargs,
    ): ...

    @staticmethod
    def require_postgres(target: Union[Type[TestCase], Callable]) -> Union[Type[TestCase], Callable]: ...

    @contextlib.contextmanager
    def assertRaisesWithMessage(self, exc_class: Type[BaseException], callable_obj: Optional[Callable] = ..., message: String = ..., *args, **kwargs): ...

    @contextlib.contextmanager
    def assertRaisesUnhandledMiddlewareException(
        self, exc_class: Type[BaseException], callable_obj: Optional[Callable] = ..., message: String = ..., expected_regexp: Optional[String] = ...,
    ): ...

    def assertGrpcStatusEquals(self, grpc_context: _Context, grpc_status_code: GrpcStatusOptionalSequence): ...


class ServerTestCase(TestCase):
    @classmethod
    def setUpClass(cls: Type[TestCase]) -> None: ...


class PytestCase(object):
    @classmethod
    def setup_class(cls) -> None: ...


class GrpcContextMock(_Context):
    _metadata: Iterable[Tuple[str, str]]

    # noinspection PyMissingConstructor
    def __init__(self, metadata: Optional[Union[dict, Iterable[Tuple[str, str]]]] = ...): ...

    def invocation_metadata(self) -> dict: ...
