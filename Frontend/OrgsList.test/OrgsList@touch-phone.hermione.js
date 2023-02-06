'use strict';

const PO = require('./OrgsList.page-object').touchPhone;

specs({
    feature: 'Список организаций',
}, function() {
    describe('Основные проверки', function() {
        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'кафе',
                data_filter: 'companies',
            }, PO.OrgsList());
            await browser.assertView('plain', PO.OrgsList.FirstItem());
        });

        it('Сайдблок', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'кафе',
                data_filter: 'companies',
            }, PO.OrgsList());
            await browser.yaOpenOverlayAjax(
                () => browser.yaCheckBaobabCounter(PO.OrgsList.FirstItem.OverlayHandler(), {
                    path: '/$page/$main/$result/composite/orgs-list/minibadge/item[@externalId@entity="organization"]',
                    behaviour: { type: 'dynamic' },
                }),
                PO.overlayOneOrg(),
                'Не открылся оверлей после клика в первый элемент списка',
            );
        });

        it('С аспектами', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'суши екб',
                srcparams: 'GEOV:experimental=add_snippet=ugc_aspects/1.x',
                data_filter: 'companies',
            }, PO.OrgsList());
            await browser.assertView('aspects', PO.OrgsList.FirstItem());
        });

        it('С кнопкой телефона', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'кафе зеленоград',
                data_filter: 'companies',
            }, PO.OrgsList());
            await browser.yaCheckBaobabCounter(PO.OrgsList.FirstItem.PhoneButton(), {
                path: '/$page/$main/$result/composite/orgs-list/minibadge/contacts[@action="phone" and @externalId@entity="organization" and @behaviour@type="dynamic"]',
            });

            await browser.yaCheckVacuum(
                { type: 'show', orgid: '145755125616', event: 'show_org' },
                'Не сработала метрика на показ организации',
            );
            await browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '145755125616', event: 'call', goal: 'make-call' },
                'Не сработала метрика на клик в кнопку телефон',
            );

            await browser.setBaseMetrics(metrics => metrics.concat([
                'feature.web.wizards_common.orgs_wiz_main.total.dynamic_clicks',
                'all.total_dynamic_click_count',
            ]));
        });

        it('Проверяем наличие доп. адреса в колдунщике оргмн', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: '382556587',
                data_filter: 'companies',
            }, PO.OrgsList());

            await browser.yaHaveVisibleText(
                PO.OrgsList.FirstItem.Content(),
                /Неглинная ул\., 10, Москва/,
                'Сломался адрес организации на первой карточке',
            );

            await browser.yaHaveVisibleText(
                PO.OrgsList.SecondItem.Content(),
                /Россия, Москва, Манежная площадь \(ТЦ Охотный ряд, -1 этаж\)/,
                'Сломался адрес организации на второй карточке',
            );
        });

        it('Большие фото', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'пиццерия',
                data_filter: 'companies',
                exp_flags: [
                    'GEO_orgs_list_thumb_size=100',
                ],
            }, PO.OrgsList());

            await browser.assertView('plain', PO.OrgsList.ThirdItem());
        });
    });

    it('Похожие запросы в середине списка', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе в москве',
            exp_flags: 'GEO_orgmn_discovery=inset',
            srcrwr: 'GEOV:rosetta.search.yandex.net:9904:1s',
            srcparams: 'GEOV:rearr=scheme_Local/Geo/DiscoveryAspects/Enabled=true',
            data_filter: 'companies',
        }, PO.OrgsList.orgRelated());
        await browser.yaAssertViewExtended('inset', PO.OrgsList.orgRelated());

        let relatedText = await browser.getText(PO.OrgsList.orgRelated.firstItem());

        relatedText = relatedText.replace('\n', ' ') + ' в Москве';

        await browser.yaWaitUntilSerpReloaded(
            () => browser.yaCheckLink2({
                selector: PO.OrgsList.orgRelated.firstItem.link(),
                target: '_self',
                url: {
                    href: 'https://yandex.ru/search/touch/?noredirect=1&text=...&lr=213&noreask=1&serp-reload-from=companies',
                    queryValidator: query => {
                        assert.equal(query.text, relatedText, 'Ошибка в параметре text');
                        assert.equal(query.noreask, '1', 'Ошибка в параметре noreask');
                        assert.equal(query['serp-reload-from'], 'companies', 'Ошибка в параметре serp-reload-from');

                        return true;
                    },
                    ignore: ['hostname'],
                },
                baobab: { path: '/$page/$main/$result/composite/orgs-list/org-related/scroller/link[@id]' },
                message: 'Ошибка в первой ссылке',
            }),
        );
    });

    it('Дозагрузка списка', async function() {
        const { browser } = this;
        const EXP_VAL = 5;

        await browser.yaOpenSerp({
            text: 'пиццерия в спб',
            data_filter: 'companies',
        }, PO.OrgsList());
        await browser.yaShouldBeVisible(PO.OrgsList.more(), 'Нет кнопки "Посмотреть ещё"');

        const itemsCountBefore = await browser.yaVisibleCount(PO.OrgsList.Item());

        await browser.yaWaitForItemsLoaded(
            PO.OrgsList.Item(),
            () => browser.yaCheckBaobabCounter(PO.OrgsList.more(), {
                path: '/$page/$main/$result/composite/orgs-list/more',
                behaviour: { type: 'dynamic' },
            }),
            'Не загрузились элементы после клика на кнопку "Посмотреть ещё"',
        );

        const itemsCountAfter = await browser.yaVisibleCount(PO.OrgsList.Item());

        assert.equal(
            itemsCountAfter - itemsCountBefore,
            EXP_VAL,
            'Количество добавленных элементов не совпадает со значением из флага эксперимнта',
        );

        await browser.assertView('loaded-and-more', [PO.OrgsList.more(), PO.OrgsList.lastItem()]);
        await browser.yaOpenOverlayAjax(
            () => browser.yaCheckBaobabCounter(PO.OrgsList.lastItem.OverlayHandler(), {
                path: '/$page/$main/$result/composite/orgs-list/minibadge/item[@externalId@entity="organization"]',
                behaviour: { type: 'dynamic' },
            }),
            PO.overlayOneOrg(),
            'Не открылся оверлей после клика в дозагруженный элемент',
        );
    });
});
