import datetime
import io
import json
import logging
import uuid
from enum import Enum

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_qlog.logging.adapters.logger import LoggerContext
from sendr_qlog.logging.context import Context
from sendr_qlog.logging.formatters.deploy_meta import DeployMetaFormatter
from sendr_qlog.logging.formatters.lines import LinesFormatter
from sendr_qlog.logging.formatters.qloud import QloudFormatter
from sendr_qlog.logging.handlers.unified_agent.handler import UnifiedAgentHandler

from hamcrest import has_entries, match_equality, not_none


@pytest.fixture()
def init_context():
    return {'a': 1, 'b': 2}


@pytest.fixture
def logger_context(init_context):
    return LoggerContext(logging.getLogger(), init_context)


def test_initialization(init_context, logger_context):
    assert logger_context.get_context() == init_context


class TestContext:
    def test_sequence(self):
        ctx = Context()
        ctx.push(a=1)
        assert ctx.get_top_view() == {'a': 1}
        ctx.push(a=2)
        assert ctx.get_top_view() == {'a': 2}
        ctx.pop('a')
        assert ctx.get_top_view() == {'a': 1}
        with ctx:
            ctx.push(a=3)
            assert ctx.get_top_view() == {'a': 3}
        assert ctx.get_top_view() == {'a': 1}

    def test_pop_from_empty(self):
        ctx = Context()
        ctx.pop('a')
        assert ctx.get_top_view() == {}

    def test_clone_has_separate_state(self):
        context = Context(initial={'a': 1, 'b': 2})
        clone = context.clone()
        assert clone.get_top_view() == {'a': 1, 'b': 2}

        context.pop('a', 'b')
        assert clone.get_top_view() == {'a': 1, 'b': 2}


class TestLoggerContextClone:
    def test_can_manipulate_shared_context_through_both_adapters(self, logger_context):
        """
        Тестируем ожидаемое поведение, нежели one._context os two._context.
        """
        new_logger = logging.getLogger('another_logger')
        assert new_logger is not logger_context.logger

        cloned_adapter = logger_context.with_logger(new_logger)
        assert cloned_adapter.logger is new_logger

        assert logger_context.with_logger('another_logger').logger is new_logger

        cloned_adapter.context_push(something='something')
        assert logger_context.get_context()['something'] == 'something'

        logger_context.context_pop('something')
        assert 'something' not in cloned_adapter.get_context()

        logger_context.context_push(something='something')
        assert cloned_adapter.get_context()['something'] == 'something'

        cloned_adapter.context_pop('something')
        assert 'something' not in logger_context.get_context()


class TestLoggerContextPushPop:
    @pytest.fixture()
    def init_context(self):
        return {}

    @pytest.fixture()
    def push_context(self):
        return {'a': 'aa', 'b': 'bb'}

    def test_push(self, logger_context, push_context):
        with logger_context:
            logger_context.context_push(**push_context)
            assert logger_context.get_context() == push_context

    def test_context_manager_clears_context(self, logger_context, push_context):
        with logger_context:
            logger_context.context_push(**push_context)
        assert logger_context.get_context() == {}

    def test_pop(self, logger_context, push_context):
        with logger_context:
            logger_context.context_push(**push_context)
            for key in push_context:
                logger_context.context_pop(key)
            assert logger_context.get_context() == {}

    def test_pop_idempotent(self, logger_context, push_context):
        logger_context.context_push(test=1)
        with logger_context:
            logger_context.context_push(**push_context)
            for key in push_context:
                logger_context.context_pop(key)
            for key in push_context:
                logger_context.context_pop(key)
            assert logger_context.get_context() == {'test': 1}


@pytest.fixture(scope='function')
def stream():
    s = io.StringIO()
    yield s
    s.close()


@pytest.fixture
def get_message_from_stream(stream):
    def _inner():
        content = stream.getvalue()
        try:
            return json.loads(content)
        except ValueError as exc:
            raise ValueError(f'Stream contains invalid JSON string: {content}') from exc

    return _inner


class TestWithQloudFormatter:
    """Проверяем, что вся цепочка работает и в stream летят корректные сообщения"""

    @pytest.fixture
    def stream_handler(self, stream):
        handler = logging.StreamHandler(stream=stream)
        handler.setLevel('DEBUG')
        handler.setFormatter(QloudFormatter())
        yield handler
        handler.close()

    @pytest.fixture
    def logger(self, stream_handler):
        # sick: не используем root логгер, т. к. тестовая среда тоже может писать в логгер
        lgr = logging.getLogger(str(uuid.uuid4().hex))
        lgr.addHandler(stream_handler)
        return lgr

    @pytest.fixture
    def logger_context(self, logger):
        return LoggerContext(logger)

    def test_message_format(self, logger_context: LoggerContext, get_message_from_stream):
        logger_context.context_push(a=1, b='two', c=None, d={'hello': 'hi'})
        logger_context.error('Message')
        message = get_message_from_stream()
        assert message['message'] == 'Message'
        assert message['level'] == 'ERROR'
        assert message['@fields']['context'] == {'a': 1, 'b': 'two', 'c': None, 'd': {'hello': 'hi'}}

    def test_datetime_serialization(self, logger_context: LoggerContext, get_message_from_stream):
        dt = datetime.datetime.utcnow()
        date = datetime.date.today()
        obj = object()
        logger_context.context_push(date=date, dt=dt, obj=obj)
        logger_context.error('Bad')
        message = get_message_from_stream()
        assert message['@fields']['context'] == {
            'dt': dt.isoformat(sep=' '),
            'date': date.isoformat(),
            'obj': repr(obj),
        }


class TestWithLinesFormatter:
    @pytest.fixture
    def stream_handler(self, stream):
        handler = logging.StreamHandler(stream=stream)
        handler.setLevel('DEBUG')
        handler.setFormatter(LinesFormatter())
        yield handler
        handler.close()

    @pytest.fixture
    def logger(self, stream_handler):
        lgr = logging.getLogger(str(uuid.uuid4().hex))
        lgr.addHandler(stream_handler)
        return lgr

    @pytest.fixture
    def logger_context(self, logger):
        return LoggerContext(logger)

    def test_message_format(self, logger_context: LoggerContext, stream):
        context = dict(
            a=1,
            b='two',
            c=None,
            d={'hello': 'hi'},
            date=datetime.datetime.today(),
            dt=datetime.datetime.utcnow(),
            obj=object(),
        )
        logger_context.context_push(**context)
        logger_context.error('Bad')
        message = stream.getvalue()

        pairs = {}
        for key_value in message.split('\t'):
            key, value = key_value.strip().split('=')
            if key in context:
                pairs[key] = value

        assert pairs == dict(
            a='1',
            b='two',
            c='None',
            d=repr(context['d']),
            date=repr(context['date']),
            dt=repr(context['dt']),
            obj=repr(context['obj']),
        )


LOGGER_NAME = str(uuid.uuid4().hex)


class TestWithDeployFormatter:
    @pytest.fixture
    def mock_session(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def handler(self, mock_session):
        handler = UnifiedAgentHandler(uri='x.test:65535', session=mock_session)
        handler.setLevel('DEBUG')
        handler.setFormatter(DeployMetaFormatter())
        yield handler
        handler.close()

    @pytest.fixture
    def logger(self, handler):
        lgr = logging.getLogger(LOGGER_NAME)
        lgr.addHandler(handler)
        return lgr

    @pytest.fixture
    def logger_context(self, logger):
        return LoggerContext(logger)

    def test_message_format(self, logger_context: LoggerContext, mock_session):

        class SomeEnum(Enum):
            CASE_1 = 'case_1'

        context = dict(
            a=1,
            b='two',
            c=None,
            d={'hello': 'hi'},
            some_enum=SomeEnum.CASE_1,
            date=datetime.date(2020, 1, 1),
            dt=datetime.datetime(2020, 1, 1, 10, 10, 10),
            obj=object(),
        )

        logger_context.context_push(**context)
        logger_context.error('Bad')

        mock_session.send.assert_called_once_with(
            'Bad',
            match_equality(not_none()),
            match_equality(
                has_entries({
                    'context': convert_then_match(
                        json.loads,
                        has_entries({
                            'context': has_entries({
                                'b': 'two',
                                'c': None,
                                'd': context['d'],
                                'some_enum': 'case_1',
                                'date': '2020-01-01',
                                'dt': '2020-01-01 10:10:10',
                                'obj': repr(context['obj']),
                            }),
                        })
                    )
                }),
            ),
        )
