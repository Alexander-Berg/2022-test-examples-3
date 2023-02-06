specs({
    feature: 'Колдунщик оргмн',
    experiment: 'Дивная карточка для ПП',
}, SURFACES.SEARCHAPP, () => {
    it.skip('Дефолтный', async function() {
        const result = await this.client.request('кафе', {
            flags: {
                mm_find_poi: 1,
            },
            exp_flags: {
                GEO_orgmn_div_card: 1,
                GEO_enable_app: 1,
            },
        });
        const expected = /Вот варианты|Выбирайте|Нашла|Вот что я нашла|Вот, смотрите|Всё для вас|Смотрите, выбирайте/;

        this.asserts.yaCheckVoiceMatch(result, expected);
        this.asserts.yaCheckTextMatch(result, expected);
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'find_poi',
            intent: 'personal_assistant.scenarios.find_poi',
        });
    });
});
