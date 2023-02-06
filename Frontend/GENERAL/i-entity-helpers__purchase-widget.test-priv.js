describeBlock('i-entity-helpers__purchase-widget-enframed-url', function(block) {
    let context;
    let snpData;

    beforeEach(function() {
        context = {
            expFlags: {
                test_tool: ''
            }
        };
        snpData = {
            rich_info: {
                vh_meta: {
                    subscriptions: [
                        {
                            billing_product_id: 'ru.yandex.plus.kinopoisk.amediateka.1month.autorenewable.native.web.7days.trial',
                            currency: 'RUB',
                            name: 'Кинопоиск + Амедиатека',
                            offer_sub_text: 'JSON {"bo": "Вы экономите 219{RUB}","default": "по подписке Кинопоиск + Амедиатека","touch": "затем 649{RUB} / мес. по подписке Кинопоиск + Амедиатека", "widget": "затем 649 ₽ в месяц"}',
                            offer_text: 'JSON{"default": "649{RUB} в месяц","touch": "Смотреть 7 дней бесплатно", "widget": "7 дней бесплатно", "button": "7 дней бесплатно", "boHeader": "Смотрите по подписке", "boSubHeader": "Кинопоиск + Амедиатека: 5300 фильмов и сериалов в FullHD"}',
                            price: 649,
                            price_period: 'P1M',
                            purchase_tag: 'kp-amediateka',
                            trial_duration: 'P7D'
                        },
                        {
                            billing_product_id: 'ru.yandex.web.kinopoisk.native.1month.autorenewable.7days.trial.1month.intro.499.until31december.kinopoisk_amediateka.649',
                            currency: 'RUB',
                            name: 'Кинопоиск + Амедиатека',
                            offer_sub_text: 'затем 649 ₽ в месяц. Первые 7 дней — бесплатно.',
                            offer_text: '499 ₽ в месяц до конца года',
                            price: 499,
                            price_period: 'P1M',
                            purchase_tag: 'kp-amediateka',
                            trial_duration: 'P7D'
                        },
                        {
                            billing_product_id: 'ru.yandex.web.kinopoisk.native.1month.autorenewable.30days.trial.1month.intro.99.until1sept.basic_kinopoisk.269',
                            currency: 'RUB',
                            name: 'Кинопоиск',
                            offer_sub_text: 'JSON{ "bo": "Далее 269{RUB} в месяц. Все бонусы Яндекс.Плюс: скидка на Яндекс.Такси, полный доступ к Яндекс.Музыке, бесплатная доставка на Беру и другое", "default": "далее 269{RUB} / мес. по подписке Кинопоиск", "touch": "затем 99{RUB} / мес. до конца лета, далее 269{RUB} / мес.", "widget": "затем 99₽ в месяц до конца лета, далее 269₽ в месяц"}',
                            offer_text: 'JSON{"default": "99{RUB} в месяц до конца лета","touch": "30 дней бесплатно", "widget": "30 дней бесплатно", "button": "30 дней бесплатно", "boHeader": "Смотрите по подписке Кинопоиск", "boSubHeader": "4800 фильмов и сериалов в Full HD ∙ Эксклюзивы Кинопоиска ∙ Без рекламы ∙ Яндекс.Плюс в подарок"}',
                            price: 99,
                            price_period: 'P1M',
                            purchase_tag: 'kp-basic',
                            trial_duration: 'P1M'
                        },
                        {
                            billing_product_id: 'ru.yandex.plus.1month.autorenewable.native.web.1month.trial.kz',
                            currency: 'KZT',
                            name: 'Яндекс.Плюс',
                            offer_sub_text: 'JSON{"default": "по подписке Яндекс.Плюс", "touch": "затем 849 тенге в месяц по подписке Яндекс.Плюс"}',
                            offer_text: 'JSON{"default": "849 тенге в месяц","touch": "Смотреть 30 дней бесплатно"}',
                            price: 849,
                            price_period: 'P1M',
                            purchase_tag: 'plus',
                            trial_duration: 'P1M'
                        }
                    ]
                }
            }
        };
    });

    it('not a svod case', function() {
        const result = block(context, snpData, { isSVOD: false, place: 'RIGHT_CARD' });

        assert.isString(result);
        assert.notInclude(result, '&subscriptionType=plus', 'subscriptionType=plus not found');
        assert.include(result, '&kpId=&', 'empty kpId not found');
        assert.include(result, '', 'right_card not found');
        assert.isTrue(/serp_entity_(pad|desktop|touch)_right_card/.test(result), 'right_card not found');
    });

    it('ban_kinopoisk_hd with dynamic-offers and service kinopoisk_hd', function() {
        const result = block(context, snpData, { service: 'kinopoisk_hd', place: 'RIGHT_CARD' });

        assert.isString(result);
        assert.include(result, '&subscriptionType=plus', 'subscriptionType=plus not found');
        assert.include(result, '&kpId=&', 'empty kpId not found');
        assert.isTrue(/serp_entity_(pad|desktop|touch)_right_card/.test(result), 'right_card not found');
    });

    it('dynamic-offers and service kinopoisk_hd', function() {
        const result = block(context, snpData, { service: 'kinopoisk_amedia', place: 'BIG_ANSWER' });

        assert.isString(result);
        assert.include(result, '&subscriptionType=kp-amediateka', 'subscriptionType=kp-amediateka not found');
        assert.include(result, '&kpId=&', 'empty kpId not found');
        assert.isTrue(/serp_entity_(pad|desktop|touch)_bo/.test(result), 'bo not found');
    });

    it('does not empty kpId when defined in params', function() {
        const result = block(
            context, snpData,
            { service: 'kinopoisk_amedia', place: 'CENTER_CARD' },
            { kpId: 111 }
        );

        assert.isString(result);
        assert.isTrue(/kinopoisk_amedia_serp_entity_(pad|desktop|touch)_central_card_111/.test(result), 'central_card not found');
        assert.include(result, '&kpId=111&', 'kpId=111 not found');
    });
});
