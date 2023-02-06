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
                                ],
                            },
                            {
                                action: {
                                    log_id: 'direct_gallery_whole_card_click_2',
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
                                ],
                            },
                            {
                                action: {
                                    log_id: 'direct_gallery__serp',
                                },
                            },
                        ],
                    },
                },
            ],
        },
        has_borders: false,
        type: 'div2_card',
    };
}

specs('Реклама (SEARCHAPP) / Редизайн 4', SURFACES.SEARCHAPP, () => {
    it('Общие проверки', async function() {
        const response = await this.client.request('кредит наличными', {
            exp_flags: {
                direct_goodwin_design_4: 1,
            },
        });

        this.asserts.yaCheckAnalyticsInfo(response, {
            intent: 'personal_assistant.scenarios.direct_gallery',
            product_scenario_name: 'search',
        });
        this.asserts.yaCheckCard(response, getExpectedBlockSkelet());
    });
});
