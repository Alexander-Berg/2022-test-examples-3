const { expectedSnippetSkelet } = require('./data.js');

specs('Реклама (SEARCHAPP) / Добавление директ карточки', SURFACES.SEARCHAPP, () => {
    it('Общие проверки', async function() {
        const response = await this.client.request('окна пвх', {
            exp_flags: {
                direct_goodwin_direct_card: 1,
            },
        });

        this.asserts.yaCheckAnalyticsInfo(response, {
            intent: 'personal_assistant.scenarios.direct_gallery',
            product_scenario_name: 'search',
        });
        this.asserts.yaCheckCard(response, expectedSnippetSkelet);
    });
});
