describeBlock('i-entity-helpers__get-thumb-offer-text', function(block) {
    let context;
    let snpData;
    let license;

    beforeEach(function() {
        context = {
            expFlags: {
                test_tool: false
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
                            offer_text: 'JSON{"default": "649 в месяц","touch": "Смотреть 7 дней бесплатно", "widget": "7 дней бесплатно", "button": "7 дней бесплатно", "boHeader": "Смотрите по подписке", "boSubHeader": "Кинопоиск + Амедиатека: 5300 фильмов и сериалов в FullHD"}',
                            price: 649,
                            price_period: 'P1M',
                            purchase_tag: 'kp-amediateka',
                            trial_duration: 'P7D'
                        }
                    ]
                }
            }
        };
        license = '';
    });

    it('dynamic-offers text validation', function() {
        license = 'kp-amediateka';
        const result = block(context, snpData, license);

        assert.isDefined(result);
        assert.isArray(result.content);
        assert.lengthOf(result.content, 2);
        assert.ownInclude(
            result.content[0],
            { text: '649 в месяц' }
        );
    });
});
