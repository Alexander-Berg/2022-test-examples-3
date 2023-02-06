import {makeSuite, makeCase} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';

const COOKIE_LR = 'lr';
const COOKIE_MANUALLY_CHANGED = 'manuallyChangedRegion';
const COOKIE_CURRENT_REGION = 'currentRegionId';
const LOCAL_STORAGE_HISTORY = 'regionHistory';

/**
 * Тесты на виджет RegionSelector.
 * @param {PageObject.RegionSelector} regionSelector
 */
export default makeSuite('Попап выбора региона', {
    issue: 'MOBMARKET-11116',
    story: {
        async beforeEach() {
            await this.browser.yaOpenPage('touch:blank');
            await this.browser.localStorage('DELETE', LOCAL_STORAGE_HISTORY);
            await this.browser.deleteCookie(COOKIE_CURRENT_REGION);
            await this.browser.deleteCookie(COOKIE_LR);
        },

        'Кнопка автоматического определения региона.': {
            'При клике': {
                'происходит редирект на retPath': makeCase({
                    id: 'm-touch-2674',
                    async test() {
                        const {baseUrl} = this.browser.options;
                        const retPath = `${baseUrl}?lol=kek`;

                        await this.browser.yaOpenPage('touch:my-region', {retPath});

                        await this.regionSelector.buttonAutoDetectClick();
                        await this.browser.yaWaitForPageReady();
                        await this.browser.getUrl()
                            .should.eventually.to.be.equal(
                                retPath,
                                'URL соответствует ожидаемому'
                            );
                    },
                }),

                'происходит редирект на морду без retPath': makeCase({
                    id: 'm-touch-2675',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.buttonAutoDetectClick();
                        await this.browser.yaWaitForPageReady();
                        await this.browser.getUrl()
                            .should.eventually.to.be.equal(
                                this.browser.options.baseUrl,
                                'URL соответствует ожидаемому'
                            );
                    },
                }),

                'сбрасывает настройки региона': makeCase({
                    id: 'm-touch-2676',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.buttonAutoDetectClick();
                        await this.browser.yaWaitForPageReady();

                        await this.browser.getCookie(COOKIE_LR)
                            .should.eventually.to.be.equal(
                                null,
                                'Кука региона отсутствует'
                            );

                        await this.browser.getCookie(COOKIE_MANUALLY_CHANGED)
                            .should.eventually.to.be.equal(
                                null,
                                'Кука признака ручного выставления региона отсутствует'
                            );
                    },
                }),
            },
        },

        'Кнопка "Назад".': {
            'При клике': {
                'возвращает на предыдущую страницу из retPath': makeCase({
                    id: 'm-touch-2673',
                    async test() {
                        const {baseUrl} = this.browser.options;
                        const retPath = `${baseUrl}?lol=kek`;

                        await this.browser.yaOpenPage('touch:my-region', {retPath});
                        await this.regionSelector.back();
                        await this.browser.yaWaitForPageReady();
                        await this.browser.getUrl()
                            .should.eventually.to.be.equal(
                                retPath,
                                'URL соответствует ожидаемому'
                            );
                    },
                }),
                'возвращает на главную страницу без retPath': makeCase({
                    id: 'm-touch-2672',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.back();
                        await this.browser.yaWaitForPageReady();
                        await this.browser.getUrl()
                            .should.eventually.to.be.equal(
                                this.browser.options.baseUrl,
                                'URL соответствует ожидаемому'
                            );
                    },
                }),
            },
        },
        'Поле ввода.': {
            'В фокусе': {
                async beforeEach() {
                    await this.browser.yaOpenPage('touch:my-region');
                    await this.regionSelector.clickOnInput();
                },

                'скрывает кнопку "Назад"': makeCase({
                    id: 'm-touch-2684',
                    async test() {
                        await this.regionSelector.hasButtonBack()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Назад" не видна'
                            );
                    },
                }),
                'скрывает кнопку автоматического определения региона': makeCase({
                    id: 'm-touch-2685',
                    async test() {
                        await this.regionSelector.hasButtonAutoDetect()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка автоматического определения региона не видна'
                            );
                    },
                }),
                'отображает кнопку отмены ввода': makeCase({
                    id: 'm-touch-2683',
                    async test() {
                        await this.regionSelector.hasButtonCancel()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопка отмены ввода видна'
                            );
                    },
                }),
            },
            'При вводе текста': {
                async beforeEach() {
                    const regionConfig = routes.region.baseLocation;

                    await this.browser.yaOpenPage('touch:my-region', regionConfig);
                    await this.regionSelector.clickOnInput();
                },

                'отображает найденные населённые пункты': makeCase({
                    id: 'm-touch-2686',
                    async test() {
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.waitList();
                        await this.regionSelector.getListItemText(0)
                            .should.eventually.to.be.equal(
                                'Воронеж',
                                'Показан подходящий населённый пункт'
                            );
                    },
                }),

                'отображает регионы найденных населённых пунктов': makeCase({
                    id: 'm-touch-2690',
                    async test() {
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.waitList();
                        await this.regionSelector.getListItemRegionText(0)
                            .should.eventually.to.be.equal(
                                'Воронежская область, Россия',
                                'Показан подходящий регион'
                            );
                    },
                }),

                'отображает не больше восьми вариантов': makeCase({
                    id: 'm-touch-2687',
                    async test() {
                        await this.regionSelector.setInputValue('Ворон');
                        await this.regionSelector.waitList();
                        await this.regionSelector.getListItemsCount()
                            .should.eventually.to.be.equal(8, 'Показано 8 вариантов');
                    },
                }),
            },
        },
        'История выбора.': {
            async beforeEach() {
                const regionConfig = routes.region.baseLocation;

                await this.browser.yaOpenPage('touch:my-region', regionConfig);
            },

            'По умолчанию': {
                'отображает только текущий регион': makeCase({
                    id: 'm-touch-2668',
                    async test() {
                        await this.regionSelector.getListItemsCount()
                            .should.eventually.to.be.equal(1, 'В истории только текущий регион');

                        await this.regionSelector.getListItemText(0)
                            .should.eventually.to.be.equal('Москва', 'Текст соответствует ожидаемому');
                    },
                }),
                'отображает сохранённую историю вместе с текущим регионом': makeCase({
                    id: 'm-touch-2667',
                    async test() {
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.waitList();
                        await this.regionSelector.selectItem(0);
                        await this.browser.yaWaitForPageReady();
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.getListItemsCount()
                            .should.eventually.to.be.equal(
                                2,
                                'В истории текущий регион и выбранный на предыдущем этапе'
                            );
                        await this.regionSelector.getListItemText(0)
                            .should.eventually.to.be.equal('Воронеж', 'Первым указан пользовательский регион');
                        await this.regionSelector.getListItemText(1)
                            .should.eventually.to.be.equal('Москва', 'Вторым указан текущий регион');
                    },
                }),
            },
            'При выборе региона': {
                'устанавливает его в качестве текущего': makeCase({
                    id: 'm-touch-2671',
                    async test() {
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.waitList();
                        await this.regionSelector.selectItem(0);
                        await this.browser.yaWaitForPageReady();

                        await this.browser.getCookie(COOKIE_LR)
                            .should.eventually.to.be.equal(
                                null,
                                'Кука региона отсутствует'
                            );

                        await this.browser.getCookie(COOKIE_CURRENT_REGION)
                            .then(({value}) => value)
                            .should.eventually.to.be.equal('193', 'Регион сменился');
                    },
                }),
                'не создаёт дубли': makeCase({
                    id: 'm-touch-2669',
                    async test() {
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.waitList();
                        await this.regionSelector.selectItem(0);
                        await this.browser.yaWaitForPageReady();

                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.waitList();
                        await this.regionSelector.selectItem(0);
                        await this.browser.yaWaitForPageReady();

                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.getListItemsCount()
                            .should.eventually.to.be.equal(
                                2,
                                'В истории регионы не дублируются'
                            );

                        await this.regionSelector.getListItemText(0)
                            .should.eventually.to.be.equal('Воронеж', 'Первым указан пользовательский регион');
                        await this.regionSelector.getListItemText(1)
                            .should.eventually.to.be.equal('Москва', 'Вторым указан текущий регион');
                    },
                }),
                'сохраняет не более первых трёх регионов': makeCase({
                    id: 'm-touch-2670',
                    async test() {
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.waitList();
                        await this.regionSelector.selectItem(0);
                        await this.browser.yaWaitForPageReady();

                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.setInputValue('Волгоград');
                        await this.regionSelector.waitList();
                        await this.regionSelector.selectItem(0);
                        await this.browser.yaWaitForPageReady();

                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.setInputValue('Владивосток');
                        await this.regionSelector.waitList();
                        await this.regionSelector.selectItem(0);
                        await this.browser.yaWaitForPageReady();

                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.getListItemsCount()
                            .should.eventually.to.be.equal(
                                3,
                                'В истории сохранилось три региона'
                            );
                    },
                }),
            },
        },
        'Кнопка отмены ввода.': {
            'По умолчанию': {
                'скрыта': makeCase({
                    id: 'm-touch-2677',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.hasButtonCancel()
                            .should.eventually.to.be.equal(false, 'Кнопка отмены выбора не видна');
                    },
                }),
            },
            'При клике': {
                'скрывается': makeCase({
                    id: 'm-touch-2682',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.clickOnInput();
                        await this.regionSelector.clickOnButtonCancel();
                        await this.regionSelector.hasButtonCancel()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка отмены ввода не видна'
                            );
                    },
                }),
                'показывает кнопку "Назад"': makeCase({
                    id: 'm-touch-2680',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.clickOnInput();
                        await this.regionSelector.clickOnButtonCancel();
                        await this.regionSelector.hasButtonBack()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопка "Назад" видна'
                            );
                    },
                }),
                'показывает кнопку автоматического определения региона': makeCase({
                    id: 'm-touch-2681',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.clickOnInput();
                        await this.regionSelector.clickOnButtonCancel();
                        await this.regionSelector.hasButtonAutoDetect()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопка автоматического определения региона видна'
                            );
                    },
                }),
                'показывает историю выбора': makeCase({
                    id: 'm-touch-2679',
                    async test() {
                        const regionConfig = routes.region.baseLocation;

                        await this.browser.yaOpenPage('touch:my-region', regionConfig);
                        await this.regionSelector.clickOnInput();
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.clickOnButtonCancel();
                        await this.regionSelector.getListItemsCount()
                            .should.eventually.to.be.equal(
                                1,
                                'Показан один элемент истории'
                            );
                        await this.regionSelector.getListItemText(0)
                            .should.eventually.to.be.equal('Москва', 'Показан текущий регион');
                    },
                }),
                'очищает поле ввода': makeCase({
                    id: 'm-touch-2678',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.clickOnInput();
                        await this.regionSelector.setInputValue('Воронеж');
                        await this.regionSelector.clickOnButtonCancel();
                        await this.regionSelector.getInputValue()
                            .should.eventually.to.be.equal('', 'Поле ввода очистилось');
                    },
                }),
            },
        },

        'Сообщение о ненайденом регионе.': {
            async beforeEach() {
                await this.browser.yaOpenPage('touch:my-region');
            },

            'По умолчанию': {
                'скрыто': makeCase({
                    id: 'm-touch-2688',
                    async test() {
                        await this.regionSelector.hasNotFoundMessage()
                            .should.eventually.to.be.equal(false, 'Сообщение о ненайденом регионе скрыто.');
                    },
                }),
            },

            'При отсутствующих результатах поиска': {
                'отображается': makeCase({
                    id: 'm-touch-2689',
                    async test() {
                        await this.regionSelector.clickOnInput();
                        await this.regionSelector.setInputValue('вввв');
                        // eslint-disable-next-line market/ginny/no-pause
                        await this.browser.pause(300);
                        await this.regionSelector.hasNotFoundMessage()
                            .should.eventually.to.be.equal(true, 'Сообщение о ненайденом регионе отображается.');
                    },
                }),
            },
        },
    },
});
