from datetime import datetime
from unittest.mock import patch

from crm.supskills.direct_skill.src.intents.show_conditions.show_absent import ShowAbsentGeneral


class TestShowAbsent:
    async def test_no_login(self, fake_bunker, conversation_no_login, direct5_ok, direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_no_login,
                                                                   direct5_ok,
                                                                   direct4_ok)
        assert intent == 'call_operator' and state == ''

    async def test_not_one_campaign(self, fake_bunker, conversation_with_login, direct5_key_error, direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_key_error,
                                                                   direct4_ok)
        assert intent == 'show_absent' and state == ''

    async def test_one_group_no_ads(self, fake_bunker, conversation_with_login, direct5_show_absent_one_group_no_ads,
                                    direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_show_absent_one_group_no_ads,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_archived_or_converted' and state == ''

    async def test_many_group_no_ads(self, fake_bunker, conversation_with_login, direct5_show_absent_many_groups_no_ads,
                                     direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_show_absent_many_groups_no_ads,
                                                                   direct4_ok)
        assert intent == 'show_absent' and state == ''

    async def test_many_group_one_ad(self, fake_bunker, conversation_with_login, direct5_show_absent_many_groups_one_ad,
                                     direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_show_absent_many_groups_one_ad,
                                                                   direct4_ok)
        assert intent == 'show_absent' and state == ''

    async def test_one_group_one_ad_same(self, fake_bunker, conversation_with_login,
                                         direct5_show_absent_one_group_same_ad,
                                         direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_show_absent_one_group_same_ad,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_archived_or_converted' and state == ''

    async def test_one_group_one_ad_different(self, fake_bunker, conversation_with_login,
                                              direct5_show_absent_one_group_different_ad,
                                              direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_show_absent_one_group_different_ad,
                                                                   direct4_ok)
        assert intent == 'show_absent' and state == ''

    async def test_no_groups_one_ad(self, fake_bunker, conversation_with_login,
                                    direct5_show_absent_no_group_one_ad,
                                    direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_show_absent_no_group_one_ad,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_archived_or_converted' and state == ''

    async def test_no_groups_many_ads(self, fake_bunker, conversation_with_login,
                                      direct5_show_absent_no_group_many_ads,
                                      direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_show_absent_no_group_many_ads,
                                                                   direct4_ok)
        assert intent == 'show_absent' and state == ''

    async def test_status_archived(self, fake_bunker, conversation_with_login,
                                   direct5_campaign_status_archived,
                                   direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_campaign_status_archived,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_archived_or_converted' and state == ''

    async def test_status_converted(self, fake_bunker, conversation_with_login,
                                    direct5_campaign_status_archived,
                                    direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_campaign_status_archived,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_archived_or_converted' and state == ''

    async def test_status_suspended(self, fake_bunker, conversation_with_login,
                                    direct5_campaign_status_suspended,
                                    direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_campaign_status_suspended,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_suspended' and state == ''

    async def test_status_moderation(self, fake_bunker, conversation_with_login,
                                     direct5_campaign_status_moderation,
                                     direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_campaign_status_moderation,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_off_moderation' and state == ''

    async def test_status_draft(self, fake_bunker, conversation_with_login,
                                direct5_campaign_status_draft,
                                direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_campaign_status_draft,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_off_draft' and state == ''

    async def test_status_rejected(self, fake_bunker, conversation_with_login,
                                   direct5_campaign_status_rejected,
                                   direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_campaign_status_rejected,
                                                                   direct4_ok)
        assert intent == 'show_absent_campaign_off_rejected' and state == ''

    async def test_start_later(self, fake_bunker, conversation_with_login,
                               direct5_show_absent_start_later,
                               direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                   direct5_show_absent_start_later,
                                                                   direct4_ok)
        assert intent == 'show_absent_start_date_later_than_now' and state == ''

    async def test_not_work_time(self, fake_bunker, conversation_with_login,
                                 direct5_show_absent_not_work_time,
                                 direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        with patch('crm.supskills.direct_skill.src.intents.show_conditions.show_absent.datetime') as dt_mock:
            dt_mock.utcnow.return_value = datetime(2021, 7, 30, 10, 7, 3)
            dt_mock.strptime.return_value = datetime(2021, 7, 30)
            intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                       direct5_show_absent_not_work_time,
                                                                       direct4_ok)
        assert intent == 'show_absent_time_targeting' and state == ''

    async def test_ads_rarely_served(self, fake_bunker, conversation_with_login,
                                     direct5_show_absent_not_rarely_served,
                                     direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        with patch('crm.supskills.direct_skill.src.intents.show_conditions.show_absent.datetime') as dt_mock:
            dt_mock.utcnow.return_value = datetime(2021, 7, 30, 10, 7, 3)
            dt_mock.strptime.return_value = datetime(2021, 7, 30)
            intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                       direct5_show_absent_not_rarely_served,
                                                                       direct4_ok)
        assert intent == 'show_absent_ads_rarely_served' and state == ''

    async def test_is_search(self, fake_bunker, conversation_with_login,
                             direct5_show_absent_is_search,
                             direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        with patch('crm.supskills.direct_skill.src.intents.show_conditions.show_absent.datetime') as dt_mock:
            dt_mock.utcnow.return_value = datetime(2021, 7, 30, 10, 7, 3)
            dt_mock.strptime.return_value = datetime(2021, 7, 30)
            intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                       direct5_show_absent_is_search,
                                                                       direct4_ok)
        assert intent == 'show_absent_check_shows_algorithm' and state == 'if_found_on_search'

    async def test_is_network(self, fake_bunker, conversation_with_login,
                              direct5_show_absent_is_network,
                              direct4_ok):
        show_absent_intent = ShowAbsentGeneral(fake_bunker)
        with patch('crm.supskills.direct_skill.src.intents.show_conditions.show_absent.datetime') as dt_mock:
            dt_mock.utcnow.return_value = datetime(2021, 7, 30, 10, 7, 3)
            dt_mock.strptime.return_value = datetime(2021, 7, 29)
            intent, state = await show_absent_intent.check_direct_data(conversation_with_login,
                                                                       direct5_show_absent_is_network,
                                                                       direct4_ok)
        assert intent == 'YANDEX_REJECT' and state == 'if_found_on_search'
