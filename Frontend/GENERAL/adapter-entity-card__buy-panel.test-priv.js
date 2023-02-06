describeBlock('adapter-entity-card__buy-panel', function(block) {
    let context;
    let snpData;

    beforeEach(function() {
        context = {
            tld: 'ru',
            reqid: 1,
            expFlags: { },
            reportData: stubData('cgi')
        };
        context.pageUrl = context.reportData.cgi;
        context.pageUrl.p = sinon.stub().returns(1);
        context.isSearchApp = true;
        RequestCtx.GlobalContext.isLoggedIn = true;

        // SERP-138335: временно отключены кп-тумбы, с обратным флагом
        context.expFlags.QA_kp_cover_available_touch = 1;

        _.set(context, 'reportData.reqdata.passport.logged_in', false);

        snpData = {
            base_info: {
                legal: {
                    vh_licenses: {
                        watch_film_url: 'https://kinopoisk.ru'
                    }
                }
            },
            rich_info: {
                vh_meta: {
                    content_groups: [
                        { subscriptions: [] }
                    ],
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

    it('`undefined` if `tld` is not `ru`', function() {
        context.tld = 'tr';
        assert.isUndefined(block(context, snpData));
    });

    it('`undefined` if `vh_licenses` is `undefined`', function() {
        delete snpData.base_info.legal.vh_licenses;
        assert.isUndefined(block(context, snpData));
    });

    it('`undefined` if `watch_film_url` is `undefined`', function() {
        delete snpData.base_info.legal.vh_licenses.watch_film_url;
        assert.isUndefined(block(context, snpData));
    });

    it('`undefined` if no licenses', function() {
        assert.isUndefined(block(context, snpData));
    });

    it('`undefined` if subscribtion exists', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, {
            svod: {
                subscriptions: ['YA_PLUS']
            },
            user_subscription: 'YA_PLUS',
            uuid: 1
        });

        assert.isUndefined(block(context, snpData));
    });

    it('`undefined` if `avod`', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, { avod: true });

        assert.isUndefined(block(context, snpData));
    });

    it('`undefined` if EST was bought', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, {
            est: {
                EST: true
            }
        });

        assert.isUndefined(block(context, snpData));
    });

    it('`undefined` if TVOD was rented', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, {
            tvod: {
                TVOD: true
            }
        });

        assert.isUndefined(block(context, snpData));
    });

    it('plus subscription if content is TV_SERIES and plus subscription', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, { svod: true });
        Object.assign(snpData.rich_info.vh_meta.content_groups, [{
            subscriptions: [{ purchase_tag: 'kp-basic' }],
            content_type: 'TV_SERIES'
        }]);

        assert.equal(block(context, snpData).counter.logNodeData.name, 'plus');
    });

    it('premium subscription', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, { svod: true });
        Object.assign(snpData.rich_info.vh_meta.content_groups, [{
            subscriptions: [{ purchase_tag: 'kp-amediateka' }]
        }]);

        assert.equal(block(context, snpData).counter.logNodeData.name, 'premium');
    });

    it('kpbasic subscription', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, { svod: true });
        Object.assign(snpData.rich_info.vh_meta.content_groups, [{
            subscriptions: [{ purchase_tag: 'kp-basic' }]
        }]);

        assert.equal(block(context, snpData).counter.logNodeData.name, 'plus');
    });

    it('plus subscription', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, { svod: true });
        Object.assign(snpData.rich_info.vh_meta.content_groups, [{
            subscriptions: [{ purchase_tag: 'plus' }]
        }]);

        assert.equal(block(context, snpData).counter.logNodeData.name, 'plus');
    });

    it('discount tvod', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, {
            tvod: {
                price: 1,
                promocode: 1
            },
            est: {
                price: 2,
                promocode: 2
            }
        });
        _.set(context, 'reportData.reqdata.passport.logged_in', 1);

        assert.include(block(context, snpData).url, 'serpPromoForLogs=1');
    });

    it('discount est', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, {
            est: {
                price: 2,
                promocode: 2
            }
        });

        assert.equal(block(context, snpData).counter.logNodeData.name, 'discount');
    });

    it('dynamic: premium subscription', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, { svod: {} });
        snpData.rich_info.vh_meta.content_groups[0].subscriptions.push({
            purchase_tag: 'kp-amediateka'
        });

        assert.equal(block(context, snpData).counter.logNodeData.name, 'premium');
    });

    it('dynamic: plus subscription', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, { svod: {} });
        snpData.rich_info.vh_meta.content_groups[0].subscriptions.push({
            purchase_tag: 'plus'
        });

        assert.equal(block(context, snpData).counter.logNodeData.name, 'plus');
    });

    it('dynamic: discount tvod', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, {
            tvod: {
                price: 1,
                promocode: 1
            },
            est: {
                price: 2,
                promocode: 2
            }
        });
        _.set(context, 'reportData.reqdata.passport.logged_in', 1);

        assert.include(block(context, snpData).url, 'serpPromoForLogs=1');
    });

    it('dynamic: discount est', function() {
        Object.assign(snpData.base_info.legal.vh_licenses, {
            est: {
                price: 2,
                promocode: 2
            }
        });

        assert.equal(block(context, snpData).counter.logNodeData.name, 'discount');
    });
});
