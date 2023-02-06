specs('Колдунщик 1орг', () => {
    it.skip('Дефолтный', async function() {
        const result = await this.client.request('кафе Пушкин');
        const expected = 'Пушкинъ';

        this.asserts.yaCheckVoiceInclude(result, expected);
        this.asserts.yaCheckTextInclude(result, expected);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });

    it.skip('Адрес', async function() {
        const result = await this.client.request('адрес кафе Пушкин');

        this.asserts.yaCheckVoiceInclude(result, 'Тверской бульвар 26А');
        this.asserts.yaCheckTextInclude(result, 'Тверской бульвар 26А');
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });

    it.skip('Телефон', async function() {
        const result = await this.client.request('телефон кафе Пушкин');
        const answer = 'Номер телефона «Пушкинъ» +7 (495) 739-00-33.';

        this.asserts.yaCheckVoiceInclude(result, answer);
        this.asserts.yaCheckTextInclude(result, answer);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });

    it.skip('Время работы', async function() {
        const result = await this.client.request('часы работы кафе Пушкин');
        const answer = 'Работает';

        this.asserts.yaCheckVoiceInclude(result, answer);
        this.asserts.yaCheckTextInclude(result, answer);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });

    it.skip('Отзывы', async function() {
        let result = await this.client.request('кафе Пушкин отзывы');
        const answer = /По моим данным «Пушкинъ» имеет рейтинг (\d*[.])?\d+ баллов из 10/;

        this.asserts.yaCheckVoiceMatch(result, answer);
        this.asserts.yaCheckTextMatch(result, answer);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'ersonal_assistant.scenarios.find_poi',
        });
    });
});
