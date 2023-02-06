describe('Колдунщик оргмн / Фильтры', () => {
    it.skip('Фильтр ближайшие', async function() {
        this.client.setLocation(this.locations.MOSCOW);

        let result = await this.client.request('пиццерия в Екатеринбурге виз');
        const firstOrgExpected = new RegExp('^((.+Сейчас ((открыто)|(закрыто))\.)|(.+Работает круглосуточно\.))');

        this.asserts.yaCheckVoiceMatch(result, firstOrgExpected);
        this.asserts.yaCheckTextMatch(result, firstOrgExpected);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });

    it.skip('Фильтр открытые', async function() {
        this.client.setLocation(this.locations.MOSCOW);

        let result = await this.client.request('открытые пиццерии в Екатеринбурге виз');
        let expected = new RegExp('^((.+Сейчас ((открыто)|(закрыто))\.)|(.+Работает круглосуточно\.))');

        this.asserts.yaCheckVoiceMatch(result, expected);
        this.asserts.yaCheckTextMatch(result, expected);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });

    it.skip('Фильтр хорошие', async function() {
        this.client.setLocation(this.locations.MOSCOW);

        let result = await this.client.request('пиццерия в Екатеринбурге виз');
        const expected = new RegExp('^((.+Сейчас ((открыто)|(закрыто))\.)|(.+Работает круглосуточно\.))');

        this.asserts.yaCheckVoiceMatch(result, expected);
        this.asserts.yaCheckTextMatch(result, expected);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });

        result = await this.client.request('кафе в Екатеринбурге с хорошими отзывами');
        const ratingOrgexpected = new RegExp('^((.+Сейчас ((открыто)|(закрыто))\.)|(.+Работает круглосуточно\.))');

        this.asserts.yaCheckVoiceMatch(result, ratingOrgexpected);
        this.asserts.yaCheckTextMatch(result, ratingOrgexpected);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });
});
