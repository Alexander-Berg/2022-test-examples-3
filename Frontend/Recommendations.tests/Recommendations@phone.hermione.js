specs({
    feature: 'Лента рекоммендаций',
}, () => {
    function mockItdItp(browser) {
        return browser.yaStartResourceWatcher(
            '/static/turbo/hermione/mock-external-resources.sw.js',
            [
                {
                    url: /^https:\/\/yandex\.ru\/search\/itditp/,
                    response: {
                        data: [
                            {
                                construct: {
                                    recommendations: [
                                        {
                                            annotation: 'В России с подозрением на коронавирусную инфекцию COVID-2019 были госпитализированы двое граждан Китая. Именно это стало первыми случаями коронавируса в России...',
                                            hash: 'ZAEC99F9759857F69',
                                            host: 'medvisor.ru',
                                            image: {
                                                height: 791,
                                                image_sizes: {
                                                    '244x122': '/max_g480_c6_r16x9_pd10',
                                                    '366x183': '/crop_g360_c6_r16x9_pd20',
                                                    '488x244': '/crop_g480_c12_r16x9_pd10',
                                                    '600x': '/crop_g360_c12_r16x9_pd20',
                                                    '732x366': '/lc_desktop_768px_r16x9_pd10',
                                                },
                                                image_source: 'image_snippet_turbo',
                                                project_id: 'get-turbo',
                                                raw_url: 'https://avatars.mds.yandex.net/get-turbo/2904219/rth88d1dacf4b86d6d3ab09665dcc26f2ac/orig',
                                                storage: 'avatar',
                                                url: 'rth88d1dacf4b86d6d3ab09665dcc26f2ac',
                                                width: 1181,
                                            },
                                            publication_ts: 1600173289,
                                            logurl: 'https://medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
                                            title: 'Первые два случая коронавируса в России',
                                            url: '/turbo?text=https%3A//medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
                                        },
                                        {
                                            annotation: 'Ажиотаж вокруг туалетной бумаги в супермаркетах оставляет нас в легком недоумении, а вот закупиться дезинфицирующими средствами от коронавируса – это на самом деле неплохая идея. Осталось выяснить, какие помогают, а какие не очень...',
                                            hash: 'ZB57F18DA553CBA6D',
                                            host: 'medvisor.ru',
                                            image: {
                                                height: 791,
                                                image_sizes: {
                                                    '244x122': '/max_g480_c6_r16x9_pd10',
                                                    '366x183': '/crop_g360_c6_r16x9_pd20',
                                                    '488x244': '/crop_g480_c12_r16x9_pd10',
                                                    '600x': '/crop_g360_c12_r16x9_pd20',
                                                    '732x366': '/lc_desktop_768px_r16x9_pd10',
                                                },
                                                image_source: 'image_snippet_turbo',
                                                project_id: 'get-turbo',
                                                raw_url: 'https://avatars.mds.yandex.net/get-turbo/2813055/rth9438acd2475489bd3e7951ba56371e8c/orig',
                                                storage: 'avatar',
                                                url: 'rth9438acd2475489bd3e7951ba56371e8c',
                                                width: 1181,
                                            },
                                            logurl: 'https://medvisor.ru/articles/lekarstva-i-protsedury/antiseptik-dlya-ruk-ot-koronavirusa/',
                                            title: 'Антисептик для рук от коронавируса: помогает или нет?',
                                            url: '/turbo?text=https%3A//medvisor.ru/articles/lekarstva-i-protsedury/antiseptik-dlya-ruk-ot-koronavirusa/',
                                        },
                                        {
                                            annotation: 'Магнитные губки — это отличное изобретение, и сделать себе такую очень просто. Для этого вам понадобятся: • два небольших магнита; • обычная губка; • острый нож; • иголка с ниткой. Как сделать магнитную губку.',
                                            categories: 'Сделай сам, Советы',
                                            factors: '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',
                                            hash: 'Z01DBEF840556576D',
                                            host: 'Lifehacker.ru',
                                            image: {
                                                group_id: '1040565',
                                                height: 768,
                                                image_sizes: {
                                                    '176x130': '/176x130',
                                                    '414x310': '/414x310',
                                                    '600x450': '/600x450',
                                                    '828x620': '/828x620',
                                                },
                                                image_source: 'turbo_mi_sn',
                                                main_color: '',
                                                project_id: 'get-snippets_images',
                                                raw_url: 'https://avatars.mds.yandex.net/get-snippets_images/1040565/30c73d4ee195c80df6f602f811f2a90a/orig',
                                                score: 0,
                                                storage: 'avatar',
                                                url: '30c73d4ee195c80df6f602f811f2a90a',
                                                width: 1366,
                                            },
                                            logurl: 'https://Lifehacker.ru/kak-myt-vysokie-stakany-i-vazy-iznutri/',
                                            omni_title: 'как мыть высокие стаканы и вазы изнутри лайфхакер',
                                            profile: 'EhFaMDFEQkVGODQwNTU2NTc2RBgAIgAq1wEQABrIATFTMz6RQwS+bt3TvfIjFrzl+yK+GeFEPl9fJ76ZSZG88TVfProzn70NFFW9ujOfvXlltLzRYR0+UuK/Par6p71OugI8OYHXvGpUrj0te0q+AwIKPSRPG7q57y4+m+6Ovh9CU75Cqcg9EsXrPam2Nz7l/Gc+ujOfvW3cDr2ZpVe+0oOVvqALrT4DAgo9TroCPCfo8DxjVXM9J+jwPCfo8DxqVK49bZljPjm0Qb6zF0a+AwIKPf1uVj5aG7c9mUmRvOTYn7uCxpw9IAA4AUAQSBdQATAQOAFKAA==',
                                            publication_ts: 1395176400,
                                            relevance: 0.6111216,
                                            shard: 'primus-WebTier1-0-1227-1598302821',
                                            source: 'basesearch',
                                            title: 'Как мыть высокие стаканы и вазы изнутри',
                                            url: 'https://lifehacker-ru.turbopages.org/s/lifehacker.ru/kak-myt-vysokie-stakany-i-vazy-iznutri/',
                                        },
                                        {
                                            annotation: 'Настоящий камин на дровах или даже его электронный вариант позволить себе может далеко не каждый, но сделать подобие с помощью экрана телевизора или компьютера — это более посильная задача.',
                                            categories: 'Технологии',
                                            factors: 'AcsAAABhbGxbMDsxOTA0KSBmb3JtdWxhWzA7MTU5NSkgd2ViWzA7MTEyOCkgd2ViX2l0ZGl0cFswOzc3NSkgd2ViX2l0ZGl0cF9zdGF0aWNfZmVhdHVyZXNbNzc1OzgwMykgd2ViX21ldGFfaXRkaXRwWzgwMzsxMTI4KSByYXBpZF9jbGlja3NbMTEyODsxNTE5KSByYXBpZF9wZXJzX2NsaWNrc1sxNTE5OzE1OTUpIGl0ZGl0cF91c2VyX2hpc3RvcnlbMTU5NTsxOTA0KaUHAADf//+aiIj4/73XQECAL5COzvFLSyuQjk4VBwePAP49UEzMXFVVTX9+vgwEBCheV9dVGBiohoKC1EBAgKykpKiBgACXoaCgpz09fSYiIvQT4N8/c3FxMhERoUpDQx3Wzc1Di4oa18zMIBER7///fzMQEBCAjwD+vUcA/14KAwPFzMmpEf38Uf/+1khIyMCwsC5ya+sXXFv7+5fi4OChRUWtmZiY95qJiRkwLm4NBASIsatrxsLC5Kura9XGxu5xysk5NzU1ay4ubkpDQ6UzM3NRGhrimpmZ0tBQaU5OXggFhRoJCZHi4GAfHBraf3hWVq6hoCAxc3JqRD9/1L+/xenoCAwLKyMhIQqop5fOzEx/IRUV////v////////////x3Jw0MZCAhwqqOj052dXYWBgWQgIKDH3NkZU0dHHPPyAOPifvefGqh8MhQUlImICIlubs5TU1NVR0dHAP+OR0ICsqcnDQQECMfCgoaCguyuoaAgN4iICHl3N25ZWcS2tk5lZCQzEBAQgKUwMHCvsbCwlIaGAomISGdmZnxTU1UcHEz18fFlICBADQUFgRIRkfLw8MKoqBDd3BxaVFSNhISgODgY1dHRWUhIYDCnsUA6QlIYGFgKAwP////v//////9Pd3b2e3ZycjwC+HcE8O8O6+bmHSokJKiRkSkODs5OTo5/opubX+Xh4cKxsAASEZH6+HgcFRXFOzmZ/Pr6YFBQyK6uTmVkJDozM7GWlgZjV1cpDAxEgH9/Avz789fXVxMREVUeHp6/vr4aCgrCSEgIVlJSqDAwkCojI4NAQKAzM7MUBgZSbWxsmpOTaywsDMXBwUwAAEBCTAAAQEJMAABAQvq7exMAAJAQEwAAkBATAACQEBMAAJAQEwAAkBATAACQEBMAAJAQEwAAkBATAACQEBMAAJAQEwAAkBATAACQEBMAAJAQEwAAkBATAACQEBMAAJAQEwAAkBATAACQkP//NwEAAAnhqKgoGBQUgzmNqYyMxFpaGhMAAJCQT2Rj4z93dXVj8O6ev76+11BQkMifn4vg3n4yKyufnZycBdPS+rhlZcXSz6+Kg4PVXFzc056evrCqqllJSRGflJQ45+amNzd3QTU1aywsTOLb2//f3wQAACTkJgAAICFYEFS+378KJL6bAACAhJgAAICEmAAAgISYAACAhPz//////////////////////////////////////78RyhPo/ONg+CAWC3HZ3d3daetPR1t/Onfa+tNp3Ry3Q3G2p84g8f+PqCoTb70fhr8A+w8hiNyp5m35VPO2TDVvy8u5KpPv/9KMuOH/////9wEAABwAAEAAAAABAAD4AAAAAgAAcAAAAAMAAAAAAAAAACAAAIABAAAMAABgAAAABwAA4AEAAAQAANADAAAIAAAgAACAAwAAuN8DAADwAwAAAAAAEAAAAAAAAAAAAAAAAAIAAPAAAAA8AAAADwAAwAEAAHgAAAAeAACABwAA4AEAAHgAAADeAwAA8AAAALwHAADg/////////////////////////////////////////////wAAAPwAAAAGAADgAQAA+B8AAMAAAAAGAAAwAACAAQAA+AMAAAAAAAAAAOAHAAAAAAAAAADADwAAAAAAAAAAgP9/uKqqYXC2BgAAAIOzNQAAAAAAADA4WwMAAAAAAAAAAAAGZ2uBm59BaDkAkOsCFWIGOD8LECoH6OBlAAAAAAAAYHC2BgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADTRRcAAACYLroAAAAAAAAAAAAAAAAAAAAAvCtRAAAAAAAAAAAAAAAAAAAAAAAAAJCHDeCCXmA8bITYpwQAAAAAAAB2H0F4BA3hETQEAAAAj6AhPIKG8AgawiNoCAAAAAAAADyChiAnIIAwmgETi4BH0BAAAIAQWchCZCELkYUsRBayEFnIQmQhC5qZKWhmJgAAACFddIV00RXSRVdIF10hXXSFdNEV0kVXKGogwBBeoM9NQOOWCOmiK6SL7v//////////////AwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAAIAAAgAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAABAAAEAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAABAAAEAAAAAAAgAACAAAAAAgAACAAAIAAAgAAAAAIAAAgAACAAAIAAAAACAAAIAAAgAACAAAAAAgAACAAAIAAAgAAAAAIAAAgAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAgAACAAAAAAAQAAAAAEAAAQAABAAAEAAAAABAAAEAAAQAABAAAAAAQAABAAAEAAAQAAAAAEAAAQAABAAAEAAAAABAAAEAAAQAACY////xAAAAAAAAAAA',
                                            hash: 'Z661EBE7118279772',
                                            host: 'Lifehacker.ru',
                                            image: {
                                                height: 791,
                                                image_sizes: {
                                                    '244x122': '/max_g480_c6_r16x9_pd10',
                                                    '366x183': '/crop_g360_c6_r16x9_pd20',
                                                    '488x244': '/crop_g480_c12_r16x9_pd10',
                                                    '600x': '/crop_g360_c12_r16x9_pd20',
                                                    '732x366': '/lc_desktop_768px_r16x9_pd10',
                                                },
                                                image_source: 'image_snippet_turbo',
                                                project_id: 'get-turbo',
                                                raw_url: 'https://avatars.mds.yandex.net/get-turbo/2813055/rth9438acd2475489bd3e7951ba56371e8c/orig',
                                                storage: 'avatar',
                                                url: 'rth9438acd2475489bd3e7951ba56371e8c',
                                                width: 1181,
                                            },
                                            logurl: 'https://Lifehacker.ru/fireplace-on-screen/',
                                            omni_title: 'как превратить экран пк или телевизора в камин лайфхакер',
                                            profile: 'EhFaNjYxRUJFNzExODI3OTc3MhgAIgAq1wEQABrIAaewqjwZnfq8hQtSPqewqjwIGZy+KjfaPUNfzL4Lpwi+OnDRPdnCK74kTxu6xmyWvWKk3L0kTxu6Ovn2vUXtOL6GT8K9TWogvZL/kz2NbGC+hQtSPtUveL1S4r89m+6OvuTYn7s9jCo+TroCPPIjFrzk2ao+5Nifu5lJkbwkTxu6d9EVvm2ZYz4Lpwi+DRRVvbOrPj09+DG95Nifu2pUrj2FC1I+PYwqPrJbXLynsKo8DvJIPHqNpT09+DG9C6cIvoLGnD09jCo+IAA4AUAQSBdQATAQOAFKAA==',
                                            publication_ts: 1517086800,
                                            relevance: 0.6047216,
                                            shard: 'primus-WebTier1-0-1462-1598302821',
                                            source: 'basesearch',
                                            title: 'Как превратить экран ПК или телевизора в камин',
                                            url: 'https://lifehacker-ru.turbopages.org/s/lifehacker.ru/fireplace-on-screen/',
                                        },
                                    ],
                                },
                            },
                        ],
                    },
                },
            ]
        );
    }

    async function openRecommendationFeed(browser, url) {
        await browser.url('/');
        await mockItdItp(browser);
        await browser.url(url);
        await browser.yaWaitForVisible(PO.page.container());
    }

    async function scrollToCard(browser) {
        await browser.yaScrollPage(PO.source());
        await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
        await browser.yaWaitForVisible(PO.recommendations());
        await browser.yaScrollPage(PO.recommendationsCard());
    }

    hermione.only.notIn('safari13');
    it('Внешний вид карточек в ленте', async function() {
        await this.browser.url('/');
        await mockItdItp(this.browser);
        await this.browser.url('/turbo?stub=recommendations/default.json');
        await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
        await this.browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
        await this.browser.yaMockImages({ shouldObserve: true });
        await this.browser.assertView('plain', PO.recommendations());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид нижней части статьи', async function() {
        await openRecommendationFeed(this.browser, '/turbo?text=bin%3Arecommendation-type-small-with-button');
        await this.browser.yaMockImages({ shouldObserve: true });
        await this.browser.yaScrollPage(PO.source(), 0.15);
        await this.browser.yaAssertViewportView('page-bottom');
    });

    hermione.only.notIn('safari13');
    it('Работает вместе с другим related-блоком', async function() {
        const browser = this.browser;

        await browser.url('/turbo?exp_flags=recommendation-feed%3D1&stub=recommendations%2Ftwo-related.json');
        await browser.yaWaitForVisible(PO.page.container());
        await browser.yaWaitForVisible(PO.related());
        await browser.yaWaitForVisible(PO.recommendations());
        await browser.yaAssertViewportView('two-related', { ignoreElements: PO.recommendationsCard.image() });
    });

    hermione.only.notIn('safari13');
    it('Скроллит к ленте рекомендаций, если переход произошёл с карточки', async function() {
        const browser = this.browser;

        await browser.url('/turbo/echo.msk.ru/s/news/2779528-echo.html');
        await browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
        await browser.yaScrollPage(PO.recommendations());
        await browser.click(PO.recommendationsCard());
        await browser.back();
        await browser.yaWaitForVisibleWithinViewport(PO.recommendations(), 'Не подскроллили к рекомендациям');

        await browser.yaScrollPage(0);
        await browser.reload();
        const isVisible = await browser.isVisibleWithinViewport(PO.recommendations());
        assert(!isVisible, 'Произошёл ненужный подскролл к рекомендациям');
    });

    hermione.only.notIn('safari13');
    it('Прячет плашку социальности', async function() {
        const browser = this.browser;

        await openRecommendationFeed(browser, '/turbo?stub=recommendations/social-panel.json&hermione_commentator=stub');
        await browser.yaWaitForVisible(PO.blocks.socialPanelVisible(), 'Плашка не показывается');
        await scrollToCard(browser);
        await browser.yaWaitForHidden(PO.blocks.socialPanelVisible(), 'Плашка не скрылась');
        await browser.yaScrollPage(0);
        await browser.yaWaitForVisible(PO.blocks.socialPanelVisible(), 'Плашка не показывается');
    });

    hermione.only.notIn('safari13');
    it('Ленивая загрузка ленты рекомендаций', async function() {
        const browser = this.browser;
        await browser.url('/turbo?stub=recommendations/lazy.json&exp_flags=recommendations-lazy-load=1');
        await browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');

        await this.browser.yaWaitForVisible(PO.blocks.recommendations(), 'Скелетоны не загрузились');

        await browser.execute(function(recClass) {
            document.querySelector(recClass).scrollIntoView();
        }, PO.blocks.recommendations());

        await this.browser.yaWaitForVisible('#recommendations', 'Рекоммендации не загрузились');
    });

    describe('Внешний вид с флагом позиции картинки', () => {
        hermione.only.notIn('safari13');
        it('Значение флага recommendation-card-img-pos=0', async function() {
            await this.browser.url('/');
            await mockItdItp(this.browser);
            await this.browser.url('/turbo?stub=recommendations/default.json&exp_flags=recommendation-card-img-pos=0');
            await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
            await this.browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
            await this.browser.yaMockImages({ shouldObserve: true });
            await this.browser.assertView('plain', PO.recommendations());
        });

        hermione.only.notIn('safari13');
        it('Значение флага recommendation-card-img-pos=1', async function() {
            await this.browser.url('/');
            await mockItdItp(this.browser);
            await this.browser.url('/turbo?stub=recommendations/default.json&exp_flags=recommendation-card-img-pos=1');
            await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
            await this.browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
            await this.browser.yaMockImages({ shouldObserve: true });
            await this.browser.assertView('plain', PO.recommendations());
        });

        hermione.only.notIn('safari13');
        it('Значение флага recommendation-card-img-pos=2', async function() {
            await this.browser.url('/');
            await mockItdItp(this.browser);
            await this.browser.url('/turbo?stub=recommendations/default.json&exp_flags=recommendation-card-img-pos=2');
            await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
            await this.browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
            await this.browser.yaMockImages({ shouldObserve: true });
            await this.browser.assertView('plain', PO.recommendations());
        });
    });

    describe('Реклама в ленте рекомендаций', function() {
        hermione.only.notIn('safari13');
        it('Проверка наличия рекламного блока в ленте', async function() {
            const browser = this.browser;

            await browser.url('/');
            await mockItdItp(this.browser);
            await browser.url('/turbo?exp_flags=adv-disabled%3D0&hermione_advert=stub&text=bin%3Arecommendations-advert');
            await browser.yaWaitForVisible(PO.page.container());
            await browser.yaScrollPage(PO.source());
            await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
            await browser.yaScrollPage(PO.reactAdvert(), 0.2);

            const { value: isAdvert } = await this.browser.execute(function() {
                return document.querySelectorAll('.turbo-recommendations .turbo-advert').length > 0;
            });

            assert.equal(isAdvert, true, 'Нет рекламных блоков в ленте');
        });

        hermione.only.notIn('safari13');
        it('Количество рекламных блоков', async function() {
            const browser = this.browser;
            const advertsCount = 4;

            await browser.url('/');
            await mockItdItp(browser);
            await browser.url('/turbo?exp_flags=adv-disabled%3D0&hermione_advert=stub&stub=recommendations%2Fwith-adverts.json');
            await browser.yaWaitForVisible(PO.pageJsInited());
            await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
            await browser.yaScrollPage(PO.recommendationsCard());

            const { value: count } = await browser.execute(function() {
                return document.getElementById('recommendations').querySelectorAll('.turbo-advert').length;
            });

            assert.equal(advertsCount, count, 'Неверное количество рекламы на странице');
        });

        hermione.only.notIn('safari13');
        it('Работа ленты без данных рекламы', async function() {
            const browser = this.browser;
            const advertsCount = 0;

            await browser.url('/');
            await mockItdItp(this.browser);
            await browser.url('/turbo?text=bin%3Arecommendations-withOutAds');
            await browser.yaWaitForVisible(PO.page.container());
            await browser.yaScrollPage(PO.source());
            await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
            await browser.yaWaitForVisible(PO.recommendations());

            const { value: adverts } = await this.browser.execute(function() {
                return document.querySelectorAll('.turbo-advert').length;
            });

            assert.equal(advertsCount, adverts, 'Неверное количество рекламных блоков на странице');
        });

        hermione.only.notIn('safari13');
        it('Работа ленты без данных о кол-ве повторяющихся блоков', async function() {
            const browser = this.browser;
            const advertsCount = 1;

            await browser.url('/');
            await mockItdItp(this.browser);
            await browser.url('/turbo?exp_flags=adv-disabled%3D0&hermione_advert=stub&text=bin%3Arecommendations-withOutRepeatedAds');
            await browser.yaWaitForVisible(PO.page.container());
            await browser.yaScrollPage(PO.source());
            await browser.yaScrollPage(PO.recommendationsCard(), 0.2);

            const { value: count } = await browser.execute(function() {
                return document.querySelectorAll('.turbo-advert').length;
            });
            assert.equal(advertsCount, count, 'Неверное количество рекламы на странице');
        });

        hermione.only.notIn('safari13');
        it('Проверка id повторяющихся рекламных блоков', async function() {
            const browser = this.browser;

            await browser.url('/');
            await mockItdItp(this.browser);
            await browser.url('/turbo?exp_flags=adv-disabled%3D0&hermione_advert=stub&text=bin%3Arecommendations-advert');
            await browser.yaWaitForVisible(PO.page.container());
            await browser.yaScrollPage(PO.source());
            await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');

            const { value: uniq1 } = await browser.execute(function() {
                return document
                    .querySelectorAll('.turbo-advert')[0]
                    .querySelector('.turbo-advert__item')
                    .getAttribute('id');
            });
            const { value: uniq2 } = await browser.execute(function() {
                return document
                    .querySelectorAll('.turbo-advert')[1]
                    .querySelector('.turbo-advert__item')
                    .getAttribute('id');
            });

            assert.notEqual(uniq1, uniq2, 'Одинаковые идентификаторы рекламных блоков');
        });

        hermione.only.notIn('safari13');
        it('Проверка шагов повтора рекламных блоков', async function() {
            const browser = this.browser;
            const advertsGap = 2;

            await browser.url('/');
            await mockItdItp(this.browser);
            await browser.url('/turbo?exp_flags=adv-disabled%3D0&hermione_advert=stub&text=bin%3Arecommendations-advert');
            await browser.yaWaitForVisible(PO.page.container());
            await browser.yaScrollPage(PO.source());
            await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
            await browser.yaScrollPage(PO.page.ending());
            await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Вторая порция карточек не загрузилась');

            const { value: index1 } = await browser.execute(function() {
                const advert1 = document.querySelectorAll('.turbo-advert')[1];
                return Array.prototype.indexOf.call(document.querySelector('.turbo-recommendations').children, advert1);
            });
            const { value: index2 } = await browser.execute(function() {
                const advert2 = document.querySelectorAll('.turbo-advert')[2];
                return Array.prototype.indexOf.call(document.querySelector('.turbo-recommendations').children, advert2);
            });

            assert.equal(advertsGap, index2 - index1 - 1, 'Неверное количество рекомендаций между повторяющимися блоками рекламы');
        });

        hermione.only.notIn('safari13');
        it('Проверка наличия рекламы перед карточками рекомендаций', async function() {
            const browser = this.browser;

            await browser.url('/');
            await mockItdItp(this.browser);
            await browser.url('/turbo?exp_flags=adv-disabled%3D0&hermione_advert=stub&stub=recommendations%2Fwith-adverts.json');
            await browser.yaWaitForVisible(PO.page.container());
            await browser.yaScrollPage(PO.source());
            await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');

            const { value: flag } = await browser.execute(function() {
                return document.querySelector('.turbo-recommendations').children[0].classList.contains('turbo-advert');
            });

            assert.equal(flag, true, 'Нет рекламы перед карточками в ленте рекомендаций');
        });

        hermione.only.notIn('safari13');
        it('Рендеринг нескольких рекламных блоков после карточки', async function() {
            const browser = this.browser;

            await browser.url('/');
            await mockItdItp(this.browser);
            await browser.url('/turbo?exp_flags=adv-disabled%3D0&hermione_advert=stub&stub=recommendations%2Fwith-adverts.json');
            await browser.yaWaitForVisible(PO.page.container());
            await browser.yaScrollPage(PO.source());
            await browser.yaWaitForHidden(PO.recommendationsCard.skeleton(), 'Карточки не загрузились');
            await browser.yaScrollPage(PO.reactAdvert(), 0.2);

            const { value: flag } = await browser.execute(function() {
                const isFirstAd = document.querySelector('.turbo-recommendations').children[2].classList.contains('turbo-advert');
                const isSecondAd = document.querySelector('.turbo-recommendations').children[3].classList.contains('turbo-advert');

                return isFirstAd && isSecondAd;
            });

            assert.equal(flag, true, 'Не отредерилось два рекламных блока подряд после первой карточки в ленте');
        });
    });

    describe('Типы карточек в ленте в зависимости от флага из данных', () => {
        hermione.only.notIn('safari13');
        it('small', async function() {
            const isButton = false;
            await openRecommendationFeed(this.browser, '/turbo?text=bin%3Arecommendation-type-small');
            await scrollToCard(this.browser);

            const { value: button } = await this.browser.execute(function() {
                return document.querySelector('.turbo-recommendations-card__action');
            });

            assert.equal(Boolean(button), isButton, 'Карточки рекомендаций с кнопками');
        });

        hermione.only.notIn('safari13');
        it('small-with-button', async function() {
            const isButton = true;
            await openRecommendationFeed(this.browser, '/turbo?text=bin%3Arecommendation-type-small-with-button');
            await scrollToCard(this.browser);

            const { value: button } = await this.browser.execute(function() {
                return document.querySelector('.turbo-recommendations-card__action');
            });

            assert.equal(Boolean(button), isButton, 'Карточки рекомендаций без кнопок');
        });

        async function openRelatedFeed(browser, url) {
            await browser.url('/');
            await mockItdItp(browser);
            await browser.url(url);
            await browser.yaWaitForVisible(PO.page.container());
            await browser.yaWaitForVisible(PO.related());
        }

        hermione.only.notIn('safari13');
        it('default', async function() {
            await openRelatedFeed(this.browser, '/turbo?text=bin%3Arecommendation-type-default');
        });

        hermione.only.notIn('safari13');
        it('Флаг отсутствует', async function() {
            await openRelatedFeed(this.browser, '/turbo?text=bin%3Arecommendations-type');
        });
    });

    describe('Дата публикации', () => {
        hermione.only.notIn('safari13');
        it('Показывает дату публикации по флагу recommendations-pub-date', async function() {
            const browser = this.browser;

            await openRecommendationFeed(browser, '/turbo?stub=recommendations/default.json&exp_flags=recommendations-pub-date=1');
            await browser.yaWaitForVisible(PO.recommendationsCard.dateString(), 'Не показали дату публикации');
            await browser.assertView('with-pub-date', PO.recommendationsCard());
        });

        hermione.only.notIn('safari13');
        it('Показывает дату публикации по признаку из данных ajax_recommendations_with_pub_date', async function() {
            const browser = this.browser;

            await openRecommendationFeed(browser, '/turbo?stub=recommendations/with-pub-date.json');
            await browser.yaWaitForVisible(PO.recommendationsCard.dateString(), 'Не показали дату публикации');
        });
    });

    describe('Тёмная тема', () => {
        hermione.only.notIn('safari13');
        it('Внешний вид', async function() {
            const browser = this.browser;

            await openRecommendationFeed(browser, '/turbo?stub=recommendations/default.json&patch=customTheme&exp_flags=page-custom-display-theme%3D1');
            /**
             * Форсируем тёмную тему в тесте таким способом,
             * т.к. кнопка переключения или флаг на момент написания теста не готов
             */
            await browser.execute(function() {
                document.body.classList.add('page_custom-display-theme_dark');
            });
            await browser.assertView('dark-theme', PO.recommendations());
        });
    });

    describe('Ограничение ленты', () => {
        hermione.only.notIn('safari13');
        it('Внешний вид кнопки "Показать ещё"', async function() {
            const browser = this.browser;

            await openRecommendationFeed(browser, '/turbo?stub=recommendations/limit-feed-button.json');

            await browser.assertView('button', PO.recommendations.loadMoreButton());
        });
    });
});
