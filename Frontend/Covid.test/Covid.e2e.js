specs('Колдунщик коронавируса', () => {
    it('Общий запрос', async function() {
        const result = await this.client.request('ковид');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        const regexp = /По данным Роспотребнадзора в России:\n- подтвержден(о)? \d+ новы(х|й) случа(я|й|ев) заражения \(всего \d+\)\n- выздоровел(о)? \d+ человек(а)? \(всего \d+\)\n- умер(ло)? \d+ человек(а)? \(всего \d+\)\nМогу рассказать про статистику в регионах России и странах мира\./;
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckTextMatch(result, regexp);
    });

    it('Статистика', async function() {
        let result = await this.client.request('Статистика коронавирус');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        const regexp = /По данным Роспотребнадзора в России:\n- подтвержден(о)? \d+ новы(х|й) случа(я|й|ев) заражения \(всего \d+\)\n- выздоровел(о)? \d+ человек(а)? \(всего \d+\)\n- умер(ло)? \d+ человек(а)? \(всего \d+\)\nМогу рассказать про статистику в регионах России и странах мира\./;
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckTextMatch(result, regexp);
    });

    it('Симптомы', async function() {
        const result = await this.client.request('Коронавирус симптомы');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });

        this.asserts.yaCheckVoiceInclude(result, 'К наиболее распространенным симптомам COVID-19');
        this.asserts.yaCheckTextInclude(result, 'К наиболее распространенным симптомам COVID-19');
    });

    it('Cтатистика в мире', async function() {
        const result = await this.client.request('Коронавирус статистика в мире');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });

        const regexp = /По данным университета Джонса Хопкинса в мире:\n- подтвержден(о)? \d+ новы(х|й) случа(я|й|ев) заражения \(всего \d+\)\n- выздоровел(о)? \d+ человек(а)? \(всего \d+\)\n- умер(ло)? \d+ человек(а)? \(всего \d+\)\nМогу рассказать о статистике по всей России, по ее конкретному региону или по другой стране мира\./;
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckTextMatch(result, regexp);
    });

    it('Как передается', async function() {
        const result = await this.client.request('коронавирус как передается');

        const answer = 'Пути передачи COVID-19';
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, answer);
        this.asserts.yaCheckTextInclude(result, answer);
    });

    it.skip('Статистика в регионах/странах', async function() {
        let result = await this.client.request('коронавирус');
        result = await this.client.request('Статистика в Башкирии');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, 'По данным Роспотребнадзора в регионе Республика Башкортостан');
        this.asserts.yaCheckTextInclude(result, 'По данным Роспотребнадзора в регионе Республика Башкортостан');

        result = await this.client.request('Статистика в Папуа—Новой Гвинеи');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, 'По данным университета Джонса Хопкинса в стране Папуа-Новая Гвинея');
        this.asserts.yaCheckTextInclude(result, 'По данным университета Джонса Хопкинса в стране Папуа-Новая Гвинея');

        result = await this.client.request('Статистика в Санкт-Петербурге');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, 'По данным Роспотребнадзора в городе Санкт-Петербург');
        this.asserts.yaCheckTextInclude(result, 'По данным Роспотребнадзора в городе Санкт-Петербург');
    });

    it('Отключение колдунщика не для России', async function() {
        this.client.setLocation(this.locations.MINSK);
        const result = await this.client.request('коронавирус');

        // Проверяем что ответил не сценарий covid
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        }, { equal: false });
    });
});
