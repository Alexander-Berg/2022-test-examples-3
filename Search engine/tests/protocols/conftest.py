import pytest

import protocols.const as const
from bot.modules.protocols import Protocols
from mocks.bot import temp_chats, chats


@pytest.fixture(scope='session')
def proto_module(get_context) -> Protocols:
    config = {
        'internal_martychat': const.INTERNAL_MARTYCHAT,  # TODO: add new chat
        'martychat': const.MARTYCHAT,
        'infra_chat': const.INFRACHAT,
        'managers_chat': const.MANAGERSCHAT,
        'seniormarty_chat': const.SENIORMARTYCHAT,
        'kbf_chat': const.KBFCHAT,
        'log_chat': const.LOGCHAT,
        'pr_crisis_chat': const.PR_CRISIS_CHAT,
        'important_protocols_chat': const.IMPORTANT_PROTOCOLS_CHAT,
        'feedback_chat': const.FEEDBACKCHAT,
    }

    with temp_chats(get_context, chats(*[config[key] for key in config if key.endswith('chat')])):
        proto = Protocols(get_context, config)
        proto.proto_context.cast._subs_future.set_result(None)
        get_context.modules.protocols = proto
        get_context.bot.register_modules(proto)
        yield proto


@pytest.fixture(scope='function')
def use_flag_enable_functionalities_duty(get_context, monkeypatch):
    _flags = get_context.flags.copy()
    get_context.flags['enable_functionalities_duty'] = True
    yield
    get_context.flags = _flags
