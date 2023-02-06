const PARAMS = {
    // представиться станцией ОО бэкенду
    rearr: 'entsearch_experiment=alice_meta',
    // srcrwr: 'ENTITYSEARCH:afattakhov-vm-man.man.yp-c.yandex.net:4322:10000',
    flags: { 'mm_force_scenario=Wizard': 1 },
};

const EXP_FLAGS = {};

const extraQueryParams = {
    ...PARAMS,
    exp_flags: EXP_FLAGS,
};

const ANALYTICS_INFO = {
    empty: {
        intent: null,
        product_scenario_name: null,
    },
    objects: {
        intent: 'object_search_oo',
        product_scenario_name: 'object_search_oo',
    },
    afisha: {
        intent: 'afisha_oo',
        product_scenario_name: 'afisha_oo',
    },
};

specs('Флаги для отключения и включения сценариев (SEARCHAPP)', SURFACES.SEARCHAPP, () => {
    describe('Сценарий Афиши', function() {
        it.skip('Отключенный по-умолчанию', async function() {
            const extraParams = { ...extraQueryParams };
            extraParams.exp_flags.OO_afisha_alice = 1;

            const result = await this.client.request('что в кино', extraQueryParams);

            // Если в данных аналитики нет наших источников - значит срабатываний не было
            this.asserts.yaCheckAnalyticsInfo(result, ANALYTICS_INFO.empty);
        });

        it.skip('Принудительно включенный с помощью флага', async function() {
            const extraParams = { ...extraQueryParams };
            extraParams.exp_flags.OO_afisha_alice_enable_in_app = 1;
            extraParams.exp_flags.OO_afisha_alice = 1;

            const result = await this.client.request('что в кино', extraParams);

            // Если в данных аналитики наш источник - значит срабатывание было
            this.asserts.yaCheckAnalyticsInfo(result, ANALYTICS_INFO.afisha);
        });
    });

    describe('Сценарий списка объектов', function() {
        it.skip('Отключенный по-умолчанию', async function() {
            const result = await this.client.request('книги по психологии', extraQueryParams);

            // Если в данных аналитики нет наших источников - значит срабатываний не было
            this.asserts.yaCheckAnalyticsInfo(result, ANALYTICS_INFO.empty);
        });

        it.skip('Принудительно включенный с помощью флага', async function() {
            const extraParams = { ...extraQueryParams };
            extraParams.exp_flags.OO_lists_alice_enable_in_app = 1;

            const result = await this.client.request('книги по психологии', extraParams);

            // Если в данных аналитики наш источник - значит срабатывание было
            this.asserts.yaCheckAnalyticsInfo(result, ANALYTICS_INFO.objects);
        });
    });
});
