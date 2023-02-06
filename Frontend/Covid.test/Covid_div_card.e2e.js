function getExpectedCard(text, buttonText = 'ПОДРОБНЕЕ') {
    const textBlock = {
        text_style: 'text_s',
        type: 'div-universal-block',
    };

    if (text) {
        textBlock.text = text;
    }

    return {
        body: {
            background: [
                {
                    color: '#FFFFFF',
                    type: 'div-solid-background',
                },
            ],
            states: [
                {
                    action: {
                        log_id: 'whole_card',
                        url: 'https://yandex.ru/covid19',
                    },
                    blocks: [
                        {
                            size: 'xs',
                            type: 'div-separator-block',
                        },
                        textBlock,
                        {
                            has_delimiter: 1,
                            size: 'xs',
                            type: 'div-separator-block',
                        },
                        {
                            action: {
                                log_id: 'whole_card',
                                url: 'https://yandex.ru/covid19',
                            },
                            text: `<font color='#0A4DC3'>${buttonText}</font>`,
                            type: 'div-footer-block',
                        },
                    ],
                    state_id: 1,
                },
            ],
        },
    };
}

specs('Колдунщик коронавируса (SEARCHAPP)', SURFACES.SEARCHAPP, () => {
    it('Общий запрос', async function() {
        const expectedCard = getExpectedCard();

        const result = await this.client.request('ковид');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        const regexp = /По данным Роспотребнадзора в России:\n- подтвержден(о)? \d+ новы(х|й) случа(я|й|ев) заражения \(всего \d+\)\n- выздоровел(о)? \d+ человек(а)? \(всего \d+\)\n- умер(ло)? \d+ человек(а)? \(всего \d+\)\nМогу рассказать про статистику в регионах России и странах мира\./;
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckCard(result, expectedCard);
    });

    it('Статистика', async function() {
        let expectedCard = getExpectedCard();

        let result = await this.client.request('Статистика коронавирус');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        const regexp = /По данным Роспотребнадзора в России:\n- подтвержден(о)? \d+ новы(х|й) случа(я|й|ев) заражения \(всего \d+\)\n- выздоровел(о)? \d+ человек(а)? \(всего \d+\)\n- умер(ло)? \d+ человек(а)? \(всего \d+\)\nМогу рассказать про статистику в регионах России и странах мира\./;
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckCard(result, expectedCard);
    });

    it('Симптомы', async function() {
        const expectedCard = getExpectedCard("<font color='#000000'>К наиболее распространенным симптомам COVID-19 относятся:<br/>— повышение температуры тела;<br/>— сухой кашель;<br/>— утомляемость.<br/>К более редким симптомам относятся боли в суставах и мышцах, заложенность носа, головная боль, конъюнктивит, боль в горле, диарея, потеря вкусовых ощущений или обоняния, сыпь и изменение цвета кожи на пальцах рук и ног.<br/>Немедленно обратитесь к врачу, если у вас повысилась температура, появились одышка, кашель, боль в грудной клетке, нарушения речи или движения.<br/><br/>Есть еще кое-что:<br/>— как передается вирус<br/>— статистика в регионах России и странах мира<br/>— симптомы</font>", 'Симптомы коронавируса');

        const result = await this.client.request('Коронавирус симптомы');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, 'К наиболее распространенным симптомам COVID-19');
        this.asserts.yaCheckCard(result, expectedCard);
    });

    it('Cтатистика в мире', async function() {
        let expectedCard = getExpectedCard();

        const result = await this.client.request('Коронавирус статистика в мире');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        const regexp = /По данным университета Джонса Хопкинса в мире:\n- подтвержден(о)? \d+ новы(х|й) случа(я|й|ев) заражения \(всего \d+\)\n- выздоровел(о)? \d+ человек(а)? \(всего \d+\)\n- умер(ло)? \d+ человек(а)? \(всего \d+\)\nМогу рассказать о статистике по всей России, по ее конкретному региону или по другой стране мира\./;
        this.asserts.yaCheckVoiceMatch(result, regexp);
        this.asserts.yaCheckCard(result, expectedCard);
    });

    it('Как передается', async function() {
        const expectedCard = getExpectedCard("<font color='#000000'>Пути передачи COVID-19:<br/>1. Воздушно-капельный (выделение вируса происходит при кашле, чихании, разговоре)<br/>2. Контактно-бытовой (через поверхности, предметы обихода)<br/><br/>Есть еще кое-что:<br/>— как передается вирус<br/>— статистика в регионах России и странах мира<br/>— симптомы</font>", 'Как передается коронавирус');
        const result = await this.client.request('коронавирус как передается');

        const answer = 'Пути передачи COVID-19';
        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, answer);
        this.asserts.yaCheckCard(result, expectedCard);
    });

    it.skip('Статистика в регионах/странах', async function() {
        const expectedCard = getExpectedCard();

        let result = await this.client.request('Статистика коронавирус');
        result = await this.client.request('Статистика в Башкирии');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, 'По данным Роспотребнадзора в регионе Республика Башкортостан');
        this.asserts.yaCheckCard(result, expectedCard);

        result = await this.client.request('Статистика в Папуа—Новой Гвинеи');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, 'По данным университета Джонса Хопкинса в стране Папуа-Новая Гвинея');
        this.asserts.yaCheckCard(result, expectedCard);

        result = await this.client.request('Статистика в Санкт-Петербурге');

        this.asserts.yaCheckAnalyticsInfo(result, {
            product_scenario_name: 'covid',
            intent: 'covid',
        });
        this.asserts.yaCheckVoiceInclude(result, 'По данным Роспотребнадзора в городе Санкт-Петербург');
        this.asserts.yaCheckCard(result, expectedCard);
    });
});
