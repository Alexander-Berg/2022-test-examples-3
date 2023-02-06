specs({
    feature: 'Колдунщик оргмн',
    type: 'Оргмн',
}, () => {
    it.skip('Дефолтный', async function() {
        const expected = new RegExp('^((.+Сейчас ((открыто)|(закрыто))\.)|(.+Работает круглосуточно\.))');

        const result = await this.client.request('кафе');

        this.asserts.yaCheckVoiceMatch(result, expected);
        this.asserts.yaCheckVoiceMatch(result, expected);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });
});
