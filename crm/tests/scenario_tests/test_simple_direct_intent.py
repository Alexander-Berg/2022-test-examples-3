from crm.supskills.direct_skill.src.intents.general.simple_direct_intent import SimpleDirectIntent


class TestSimpleDirectIntent:
    async def test___get_data_from_ids_no_net(self, fake_bunker, conversation_with_login, direct5_net_error,
                                              direct4_net_error):
        sdi = SimpleDirectIntent(fake_bunker)
        intent, state = await sdi.check_direct_data(conversation_with_login,
                                                    direct5_net_error,
                                                    direct4_net_error)
        assert intent == 'intent_name', state == 'intent_state'

    async def test_get_data_no_ids(self, fake_bunker, conversation_with_login, direct5_key_error, direct4_ok):
        sdi = SimpleDirectIntent(fake_bunker)
        intent, state = await sdi.check_direct_data(conversation_with_login,
                                                    direct5_key_error,
                                                    direct4_ok)
        assert intent == 'intent_name', state == 'intent_state'

    async def test___get_data_from_ids_empty_answer(self, fake_bunker, conversation_with_login, direct5_key_error,
                                                    direct4_ok):
        sdi = SimpleDirectIntent(fake_bunker)
        intent, state = await sdi.check_direct_data(conversation_with_login,
                                                    direct5_key_error,
                                                    direct4_ok)
        assert intent == 'intent_name', state == 'intent_state'

    async def test___get_data_from_ids_ok(self, fake_bunker, conversation_with_login, direct5_ok, direct4_ok):
        sdi = SimpleDirectIntent(fake_bunker)
        intent, state = await sdi.check_direct_data(conversation_with_login,
                                                    direct5_ok,
                                                    direct4_ok)
        assert intent == 'intent_name', state == 'intent_state'

    async def test___get_data_from_ids_bad_login(self, fake_bunker, conversation_with_login, direct5_api_error,
                                                 direct4_ok):
        sdi = SimpleDirectIntent(fake_bunker)
        intent, state = await sdi.check_direct_data(conversation_with_login,
                                                    direct5_api_error,
                                                    direct4_ok)
        assert intent == 'intent_name', state == 'intent_state'

    async def test_get_login_no_login(self, fake_bunker, SimpleDirectIntent_no_login, conversation_no_login, direct5_ok,
                                      direct4_ok):
        sdi = SimpleDirectIntent(fake_bunker)
        intent, state = await sdi.check_direct_data(conversation_no_login,
                                                    direct5_ok,
                                                    direct4_ok)
        assert intent == 'call_operator' and state == ''
