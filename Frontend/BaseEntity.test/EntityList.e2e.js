specs('Сценарий чтения списков ОО', () => {
    const PARAMS = {
        // представиться станцией ОО бэкенду
        rearr: 'entsearch_experiment=alice_meta',
        // srcrwr: 'ENTITYSEARCH:afattakhov-vm-man.man.yp-c.yandex.net:4322:10000',
        flags: { mm_wizard: 1 },
    };

    const EXP_FLAGS = {};

    const extraQueryParams = {
        ...PARAMS,
        exp_flags: EXP_FLAGS,
    };

    it.skip('Достопримечательности', async function() {
        let result = await this.client.request('достопримечательности парижа', extraQueryParams);

        this.asserts.yaCheckAnalyticsInfo(result, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });
        this.asserts.yaCheckVoiceInclude(result, 'Эйфелева башня -');
        this.asserts.yaCheckTextInclude(result, '«Эйфелева башня» -');
        this.asserts.yaCheckVoiceInclude(result, 'Лувр -');
        this.asserts.yaCheckTextInclude(result, '«Лувр» -');
        this.asserts.yaCheckVoiceInclude(result, 'Про что рассказать подробнее?');
        this.asserts.yaCheckTextInclude(result, 'Про что рассказать подробнее?');

        result = await this.client.request('Эйфелева башня', extraQueryParams);
        this.asserts.yaCheckVoiceInclude(result, 'Металлическая башня в центре Парижа');
        this.asserts.yaCheckTextInclude(result, 'Металлическая башня в центре Парижа');

        result = await this.client.request('назад', extraQueryParams);
        this.asserts.yaCheckVoiceInclude(result, 'Лувр -');
        this.asserts.yaCheckTextInclude(result, '«Лувр» -');

        result = await this.client.request('дальше', extraQueryParams);
        this.asserts.yaCheckVoiceInclude(result, 'Базилика Сакре-Кёр');
        this.asserts.yaCheckTextInclude(result, 'Базилика Сакре-Кёр');

        result = await this.client.request('пантеон', extraQueryParams);
        this.asserts.yaCheckVoiceInclude(result, 'Пантеон');
        this.asserts.yaCheckTextInclude(result, 'Пантеон');
    });

    it.skip('Связанный список книг', async function() {
        let result = await this.client.request('Борис Акунин', extraQueryParams);

        this.asserts.yaCheckAnalyticsInfo(result, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });
        this.asserts.yaCheckVoiceInclude(result, 'Борис Акунин');
        this.asserts.yaCheckTextInclude(result, 'Борис Акунин');

        result = await this.client.request('книги', extraQueryParams);
        this.asserts.yaCheckVoiceInclude(result, 'Борис Акунин: книги:');
        this.asserts.yaCheckTextInclude(result, 'Борис Акунин: книги:');
    });

    it.skip('Вики факт когда родился и голосовой тайтл', async function() {
        let result = await this.client.request('Александр 3', extraQueryParams);

        this.asserts.yaCheckAnalyticsInfo(result, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });
        this.asserts.yaCheckVoiceInclude(result, 'Александр III');
        this.asserts.yaCheckTextInclude(result, 'Александр III');

        result = await this.client.request('Когда родился', extraQueryParams);
        // this.asserts.yaCheckVoiceInclude(result, 'Александр III родился 10 марта 1845');
        // this.asserts.yaCheckTextInclude(result, 'Александр III родился 10 марта 1845');
    });

    it.skip('Связанный список актеров и фильмов актера', async function() {
        let result = await this.client.request('Любовь и голуби', extraQueryParams);

        this.asserts.yaCheckAnalyticsInfo(result, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });
        this.asserts.yaCheckVoiceInclude(result, 'Любовь и голуби');
        this.asserts.yaCheckTextInclude(result, 'Любовь и голуби');

        result = await this.client.request('Расскажи про актёров', extraQueryParams);
        this.asserts.yaCheckVoiceInclude(result, 'Любовь и голуби: актёры:');
        this.asserts.yaCheckTextInclude(result, 'Любовь и голуби: актёры:');
        // Роли перестали приходить
        // this.asserts.yaCheckVoiceInclude(result, ' в роли ');
        // this.asserts.yaCheckTextInclude(result, ' в роли ');

        result = await this.client.request('1', extraQueryParams); // нам не интересно какой актер на первой позиции
        result = await this.client.request('Где снимался', extraQueryParams);
        this.asserts.yaCheckVoiceInclude(result, ' фильмы: ');
        this.asserts.yaCheckTextInclude(result, ' фильмы: ');
    });

    it.skip('Источник описания и саджест связанных списков', async function() {
        let result = await this.client.request('Дэдпул 2', extraQueryParams);

        this.asserts.yaCheckAnalyticsInfo(result, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });
        this.asserts.yaCheckVoiceInclude(result, 'Дэдпул 2');
        this.asserts.yaCheckTextInclude(result, 'Дэдпул 2');

        result = await this.client.request('жанр', extraQueryParams);
        // this.asserts.yaCheckVoiceInclude(result, 'фантастика');
        // this.asserts.yaCheckTextInclude(result, 'фантастика');
        // this.asserts.yaCheckVoiceInclude(result, 'боевик');
        // this.asserts.yaCheckTextInclude(result, 'боевик');
        // this.asserts.yaCheckVoiceInclude(result, 'я могу рассказать про актёров и похожие фильмы');
        // this.asserts.yaCheckTextInclude(result, 'я могу рассказать про актёров и похожие фильмы');
    });

    it.skip('Отсутствие дублирования википедии', async function() {
        const queryParams = {
            ...extraQueryParams,
        };

        let result = await this.client.request('Фейхоа', queryParams);

        this.asserts.yaCheckTextInclude(result, 'По данным русской википедии');
        this.asserts.yaCheckTextNoInclude(result, 'Я узнала это на Википедии');
    });

    it.skip('Проверка тайтла связанных списков из данных', async function() {
        const queryParams = {
            ...extraQueryParams,
        };

        queryParams.srcrwr = 'ENTITYSEARCH:afattakhov-vm-man.man.yp-c.yandex.net:4322:10000';

        let result = await this.client.request('Испания', queryParams);

        /**
         * - рассказать еще?
         * - да
         */
        result = await this.client.request('да', queryParams);

        this.asserts.yaCheckTextInclude(result, 'Ещё я могу рассказать про достопримечательности этой страны и другие страны');
        this.asserts.yaCheckVoiceInclude(result, 'Ещё я могу рассказать про достопримечательности этой страны и другие страны');
    });

    it.skip('Отсутствие дублирования заголовка', async function() {
        const queryParams = {
            ...extraQueryParams,
        };

        let result = await this.client.request('Домовая мышь', queryParams);

        this.asserts.yaCheckTextIncludeOnce(result, 'Домовая мышь');
        this.asserts.yaCheckVoiceIncludeOnce(result, 'Домовая мышь');
    });

    it.skip('Проверка возможности рассказать о фактах в винительном падеже', async function() {
        const queryParams = {
            ...extraQueryParams,
        };

        queryParams.srcrwr = 'ENTITYSEARCH:afattakhov-vm-man.man.yp-c.yandex.net:4322:10000';

        let result = await this.client.request('Путы материнской любви', queryParams);
        result = await this.client.request('Какой жанр', queryParams);

        this.asserts.yaCheckTextInclude(result, 'количество страниц и автора'); // <- винительный падеж здесь
        this.asserts.yaCheckVoiceInclude(result, 'количество страниц и автора'); // <- винительный падеж здесь
    });

    it.skip('Голосовые аватарки в списке', async function() {
        const queryParams = { ...extraQueryParams };

        queryParams.srcrwr = 'ENTITYSEARCH:afattakhov-vm-man.man.yp-c.yandex.net:4332:10000';

        let result = await this.client.request('достопримечательности парижа', queryParams);

        this.asserts.yaCheckAnalyticsInfo(result, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });

        this.asserts.yaCheckVoiceInclude(result, 'Эйфелева башня - самая узнаваемая достопримечательность Парижа');
        this.asserts.yaCheckTextInclude(result, '«Эйфелева башня» - самая узнаваемая достопримечательность Парижа');
    });

    it.skip('Животные в связанных списках', async function() {
        let result = await this.client.request('бабуин', extraQueryParams);
        result = await this.client.request('расскажи', extraQueryParams);

        this.asserts.yaCheckAnalyticsInfo(result, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });

        this.asserts.yaCheckVoiceInclude(result, 'Ещё я могу рассказать про похожих животных');
        this.asserts.yaCheckTextInclude(result, 'Ещё я могу рассказать про похожих животных');
    });

    it.skip('Нет двойной точки в конце', async function() {
        const response = await this.client.request('расскажи про британник', {});

        this.asserts.yaCheckAnalyticsInfo(response, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });

        this.asserts.yaCheckTextMatch(response, /«Харланд энд Вольф»\.$/i);
    });

    it.skip('Постролл в музыку', async function() {
        const queryParams = { ...extraQueryParams };

        queryParams.exp_flags.OO_alice_music_postroll_enable = 1;
        delete queryParams.flags['mm_force_scenario=Wizard'];

        let result = await this.client.request('кто такие scorpions?', queryParams);
        result = await this.client.request('альбомы', queryParams);
        result = await this.client.request('blackout', queryParams);

        this.asserts.yaCheckAnalyticsInfo(result, {
            intent: 'object_search_oo',
            product_scenario_name: 'object_search_oo',
        });

        this.asserts.yaCheckVoiceInclude(result, 'Включить или рассказать ещё');
        this.asserts.yaCheckTextInclude(result, 'Включить или рассказать ещё');

        const musicScenarioResult = await this.client.request('окей', queryParams);

        this.asserts.yaCheckAnalyticsInfo(musicScenarioResult, {
            intent: 'music',
            product_scenario_name: 'music',
        });
    });
});
