const params = {
    exp_flags: {
        goodwin_ydo_enabled: 1,
        goodwin_ydo_disable_rubric_filter: 1,
    },
};

const analyticsInfo = {
    intent: 'uslugi',
    product_scenario_name: 'uslugi',
};

specs('Услуги', () => {
    it.skip('Ремонт холодильника', async function() {
        const result = await this.client.request('услуги ремонт холодильника', params);

        const regexp = /Найден(о)? \d+ мастер(ов|а)? по ремонту холодильников в Москве, могу рассказать про цены или помочь вызвать исполнителя/;
        this.asserts.yaCheckAnalyticsInfo(result, analyticsInfo);
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckTextMatch(result, regexp);
    });

    it.skip('Выгул собак', async function() {
        const result = await this.client.request('выгуливание собак в москве', params);

        const regexp = /Найден(о)? \d+ выгульщик(ов|а)? собак в Москве, могу рассказать про цены или помочь вызвать исполнителя/;
        this.asserts.yaCheckAnalyticsInfo(result, analyticsInfo);
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckTextMatch(result, regexp);
    });

    it.skip('Грузоперевозки', async function() {
        const result = await this.client.request('грузоперевозки', params);

        const regexp = /Найден(о)? \d+ грузоперевозчик(ов|а)? в Москве, могу рассказать про цены или помочь вызвать исполнителя/;
        this.asserts.yaCheckAnalyticsInfo(result, analyticsInfo);
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckTextMatch(result, regexp);
    });

    it.skip('Курьер на час', async function() {
        const result = await this.client.request('курьер на час', params);

        const regexp = /Найден(о)? \d+ курьер(ов|а) в Москве, могу рассказать про цены или помочь вызвать исполнителя/;
        this.asserts.yaCheckAnalyticsInfo(result, analyticsInfo);
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckTextMatch(result, regexp);
    });
});
