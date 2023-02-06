from crm.supskills.direct_skill.src.intents.campaign.campaign_parameters.campaign_status import \
    CampaignStatusNumberGeneralTopic, extract_feeds_ids


class TestCampaignStatus:
    async def test_no_login(self, fake_bunker, conversation_no_login, direct5_ok, direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_no_login,
                                                                       direct5_ok,
                                                                       direct4_ok)
        assert intent == 'call_operator' and state == ''

    async def test_status_on(self, fake_bunker, conversation_with_login, direct5_campaign_status_on, direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_on,
                                                                       direct4_ok)
        assert intent == 'campaign_status_going_shows' and state == ''

    async def test_status_archived(self, fake_bunker, conversation_with_login, direct5_campaign_status_archived,
                                   direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_archived,
                                                                       direct4_ok)
        assert intent == 'campaign_status_archive' and state == ''

    async def test_status_draft(self, fake_bunker, conversation_with_login, direct5_campaign_status_draft, direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_draft,
                                                                       direct4_ok)
        assert intent == 'campaign_status_draft' and state == ''

    async def test_status_suspended(self, fake_bunker, conversation_with_login, direct5_campaign_status_suspended,
                                    direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_suspended,
                                                                       direct4_ok)
        assert intent == 'campaign_status_stopped' and state == ''

    async def test_status_moderation(self, fake_bunker, conversation_with_login, direct5_campaign_status_moderation,
                                     direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_moderation,
                                                                       direct4_ok)
        assert intent == 'campaign_status_under_moderation' and state == ''

    async def test_status_rejected(self, fake_bunker, conversation_with_login, direct5_campaign_status_rejected,
                                   direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_rejected,
                                                                       direct4_ok)
        assert intent == 'campaign_status_problem_moderation_refusal' and state == ''

    async def test_status_no_going_shows(self, fake_bunker, conversation_with_login, direct5_campaign_status_no_shows,
                                         direct4_campaign_status_no_shows):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_no_shows,
                                                                       direct4_campaign_status_no_shows)
        assert intent == 'campaign_status_no_going_shows' and state == ''

    async def test_status_rarely_served(self, fake_bunker, conversation_with_login,
                                        direct5_campaign_status_rarely_served, direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_rarely_served,
                                                                       direct4_ok)
        assert intent == 'campaign_status_problem_small_targeting_groups' and state == ''

    async def test_status_ads_not_created(self, fake_bunker, conversation_with_login,
                                          direct5_campaign_status_not_created, direct4_ok):
        campaign_status_intent = CampaignStatusNumberGeneralTopic(fake_bunker)
        intent, state = await campaign_status_intent.check_direct_data(conversation_with_login,
                                                                       direct5_campaign_status_not_created,
                                                                       direct4_ok)
        assert intent == 'campaign_status_problem_in_processing_or_not_created' and state == ''

    async def test_extract_feeds_ids(self, direct5_campaign_status_not_created):
        ad_groups = await direct5_campaign_status_not_created.get_ad_groups()
        assert sorted(extract_feeds_ids(ad_groups)) == [115, 120]
