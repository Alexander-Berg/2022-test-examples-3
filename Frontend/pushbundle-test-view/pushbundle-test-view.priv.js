/**
 * Блок для тестов механизма PushBundle, см. TESTWEBREPORT-7512
 *
 * @param {GlobalData} data
 *
 * @returns {Bemjson|undefined}
 */
blocks['pushbundle-test-view'] = function(data) {
    if (!RequestCtx.GlobalContext.isYandexNet || !data.expFlags.pushbundle_test_view) return;

    return {
        block: 'pushbundle-test-view',
        mix: { block: 'serp', js: { uniqId: 'search' } },
        attrs: {
            'data-build': data.config.name,
            'data-main-bundle-id': data.bundles.getBundleId('pushbundle-test-view'),
            'data-ajax-bundle-id': data.bundles.getBundleId('pushbundle-test-view-ajax')
        },
        content: data.ajax ?
            blocks['pushbundle-test-view__content_type_ajax'](data) :
            blocks['pushbundle-test-view__content_type_main'](data)
    };
};

/**
 * Контент для первой загрузки, подгружает тестовый бандл для первой загрузки
 *
 * @param {GlobalData} data
 *
 * @returns {Bemjson}
 */
blocks['pushbundle-test-view__content_type_main'] = function(data) {
    data.pushBundle('pushbundle-test-view');

    return {
        block: 'pushbundle-test-view',
        elem: 'content',
        elemMods: { type: 'main' },
        content: [
            {
                block: 'pushbundle-test-view',
                elem: 'header',
                content: 'Тестовый блок для механизма PushBundle'
            },
            blocks['pushbundle-test-view__image'](),
            {
                block: 'pushbundle-test-view',
                elem: 'description',
                content: [
                    'Комплексное число, как следует из вышесказанного, последовательно. ',
                    'Точка перегиба искажает интеграл Гамильтона. ',
                    'К тому же непрерывная функция в принципе обуславливает степенной ряд.'
                ]
            }
        ]
    };
};

/**
 * Контент для загрузки по AJAX, подгружает тестовый бандл для аякса
 *
 * @param {GlobalData} data
 *
 * @returns {Bemjson}
 */
blocks['pushbundle-test-view__content_type_ajax'] = function(data) {
    data.pushBundle('pushbundle-test-view-ajax');

    return {
        block: 'pushbundle-test-view',
        elem: 'content',
        elemMods: { type: 'ajax' },
        content: [
            {
                block: 'pushbundle-test-view',
                elem: 'header',
                content: 'Тестовый блок для механизма PushBundle: AJAX'
            },
            blocks['pushbundle-test-view__image'](),
            {
                block: 'pushbundle-test-view',
                elem: 'description',
                content: [
                    'Подынтегральное выражение последовательно. ',
                    'Не доказано, что нормаль к поверхности отображает двойной интеграл. ',
                    'Эпсилон окрестность, как следует из вышесказанного, обуславливает интеграл ',
                    'по ориентированной области. Частная производная положительна.'
                ]
            }
        ]
    };
};

/**
 * Картинка с замечательной лошадью
 *
 * @returns {Bemjson}
 */
blocks['pushbundle-test-view__image'] = function() {
    return {
        block: 'image',
        mix: {
            block: 'pushbundle-test-view',
            elem: 'image'
        },
        url: 'borschik:link:pushbundle-test-view.png',
        width: 100,
        height: 100
    };
};
