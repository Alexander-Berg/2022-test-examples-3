function getExpectedBlockSkelet() {
    return {
        body: {
            log_id: 'direct_gallery',
            states: [
                {
                    div: {
                        items: [
                            {
                                action: {
                                    log_id: 'direct_gallery_whole_card_click_1',
                                },
                                visibility_action: {
                                    log_id: 'item/1/visibility',
                                },
                                items: [
                                    {
                                        action: {
                                            log_id: 'direct_gallery_title_click_1',
                                        },
                                    },
                                    {
                                        action: {
                                            log_id: 'direct_gallery_subtitle_click_1',
                                        },
                                    },
                                    {
                                        action: {
                                            log_id: 'direct_gallery_button_click_1',
                                        },
                                    },
                                ],
                            },
                            {
                                action: {
                                    log_id: 'direct_gallery_whole_card_click_2',
                                },
                                visibility_action: {
                                    log_id: 'item/2/visibility',
                                },
                                items: [
                                    {
                                        action: {
                                            log_id: 'direct_gallery_title_click_2',
                                        },
                                    },
                                    {
                                        action: {
                                            log_id: 'direct_gallery_subtitle_click_2',
                                        },
                                    },
                                    {
                                        action: {
                                            log_id: 'direct_gallery_button_click_2',
                                        },
                                    },
                                ],
                            },
                            {
                                action: {
                                    log_id: 'direct_gallery__serp',
                                },
                            },
                        ],
                    },
                    state_id: 1,
                },
            ],
        },
        has_borders: false,
        type: 'div2_card',
    };
}

specs('Реклама (SEARCHAPP) / Счетчики видимости', SURFACES.SEARCHAPP, () => {
    it('Общие проверки', async function() {
        const response = await this.client.request('окна пвх', {
            exp_flags: {
                direct_goodwin_visibility_count: 1,
                direct_confirm_hit: 1,
            },
        });

        this.asserts.yaCheckAnalyticsInfo(response, {
            intent: 'personal_assistant.scenarios.direct_gallery',
            product_scenario_name: 'search',
        });
        this.asserts.yaCheckCard(response, getExpectedBlockSkelet());
    });
});
