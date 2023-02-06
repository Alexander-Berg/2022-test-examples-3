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
    }
};

specs('Флаги для отключения и включения сценариев', () => {
    describe('Сценарий Афиши', function() {
        it.skip('Включенный по-умолчанию', async function() {
            const extraParams = { ...extraQueryParams };
            extraParams.exp_flags.OO_afisha_alice = 1;

            const result = await this.client.request('что в кино', extraParams);

            // Срабатывает сценарий Афиши
            this.asserts.yaCheckAnalyticsInfo(result, ANALYTICS_INFO.afisha);
        });

        it.skip('Принудительно отключенный через флаг', async function() {
            const extraParams = { ...extraQueryParams };
            extraParams.exp_flags.OO_afisha_alice = 1;
            extraParams.exp_flags.OO_afisha_alice_no_list_entry = 1;

            const result = await this.client.request('что в кино', extraParams);

            // Под флагом не должна срабатывать Афиша, срабатывает последующий сценарий объектов
            this.asserts.yaCheckAnalyticsInfo(result, ANALYTICS_INFO.objects);
        });
    });

    describe('Сценарий списка объектов', function() {
        it.skip('Включенный по-умолчанию', async function() {
            const result = await this.client.request('книги по психологии', extraQueryParams);

            // Срабатывает сценарий списка объектов
            this.asserts.yaCheckAnalyticsInfo(result, ANALYTICS_INFO.objects);
        });

        it.skip('Принудительно отключенный через флаг', async function() {
            const extraParams = { ...extraQueryParams };
            extraParams.exp_flags.OO_lists_alice_no_list_entry = 1;

            const result = await this.client.request('книги по психологии', extraParams);

            // Под флагом не должен срабатывать сценарий списка объектов
            this.asserts.yaCheckAnalyticsInfo(result, ANALYTICS_INFO.empty);
        });
    });
});
