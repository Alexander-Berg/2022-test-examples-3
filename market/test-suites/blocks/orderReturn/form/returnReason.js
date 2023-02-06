import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';
import {
    fillReturnsForm,
    checkReturnReasonOptionsValidation,
} from '@self/root/src/spec/hermione/scenarios/returns';
import {RETURN_ITEM_REASON_OPTIONS_MAP} from '@self/root/src/entities/returnCandidateItem/constants';

/**
 * Тесты на взаимодействие компонентов формы на экране причины возврата.
 */

export default makeSuite('Экран причины возврата', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    returnItemsScreen: () => this.createPageObject(
                        ReturnItems,
                        {parent: this.returnsPage}
                    ),
                    reasonsChooseScreen: () => this.createPageObject(Reason, {parent: this.returnsPage}),
                });

                await this.browser.yaScenario(this, fillReturnsForm, {
                    itemsIndexes: [3],
                    itemsCount: 5,
                });

                await this.reasonsChooseScreen.isButtonClickable().should.eventually.to.be
                    .equal(false, 'Кнопка Продолжить должна быть заблокирована');

                await this.reasonsChooseScreen.isReasonInputVisible(1).should.eventually.to.be
                    .equal(false, 'Поле ввода причины возврата должно быть скрыто');

                await this.reasonsChooseScreen.isPhotoUploadHeaderVisible(1).should.eventually.to.be
                    .equal(false, 'Загрузчик изображений должен быть скрыт');
            },
        },

        makeSuite('Выбор опции возврата.', {
            id: 'marketfront-5701',
            issue: 'MARKETPROJECT-8485',
            story: {
                'Опция "Есть недостатки"': makeCase({
                    async test() {
                        await this.reasonsChooseScreen
                            .setReasonForItem(1, RETURN_ITEM_REASON_OPTIONS_MAP.BAD_QUALITY);
                    },
                }),
                'Опция "Не подошел"': makeCase({
                    async test() {
                        await this.reasonsChooseScreen
                            .setReasonForItem(1, RETURN_ITEM_REASON_OPTIONS_MAP.DO_NOT_FIT);
                    },
                }),
                'Опция "Привезли не то"': makeCase({
                    async test() {
                        await this.reasonsChooseScreen
                            .setReasonForItem(1, RETURN_ITEM_REASON_OPTIONS_MAP.WRONG_ITEM);
                    },
                }),
                async afterEach() {
                    await this.reasonsChooseScreen.waitForReasonInputVisible(1);

                    await this.reasonsChooseScreen.isButtonClickable().should.eventually.to.be
                        .equal(true, 'Кнопка Продолжить должна быть разблокирована');

                    await this.reasonsChooseScreen.isReasonInputVisible(1).should.eventually.to.be
                        .equal(true, 'Поле ввода причины возврата должно быть видно');

                    await this.reasonsChooseScreen.isPhotoUploadHeaderVisible(1).should.eventually.to.be
                        .equal(true, 'Загрузчик изображений должен быть виден');
                },
            },
        }),

        makeSuite('Валидация выбора опций возврата.', {
            id: 'marketfront-5702',
            issue: 'MARKETPROJECT-8485',
            story: {
                'Опция "Есть недостатки"': makeCase({
                    async test() {
                        await this.reasonsChooseScreen
                            .setReasonForItem(1, RETURN_ITEM_REASON_OPTIONS_MAP.BAD_QUALITY);
                        return this.browser.yaScenario(this, checkReturnReasonOptionsValidation, {});
                    },
                }),
                'Опция "Не подошел"': makeCase({
                    async test() {
                        await this.reasonsChooseScreen
                            .setReasonForItem(1, RETURN_ITEM_REASON_OPTIONS_MAP.DO_NOT_FIT);
                        return this.browser.yaScenario(this, checkReturnReasonOptionsValidation, {
                            withPhotoUpload: false,
                        });
                    },
                }),
                'Опция "Привезли не то"': makeCase({
                    async test() {
                        await this.reasonsChooseScreen
                            .setReasonForItem(1, RETURN_ITEM_REASON_OPTIONS_MAP.WRONG_ITEM);
                        return this.browser.yaScenario(this, checkReturnReasonOptionsValidation, {});
                    },
                }),
            },
        })
    ),
});
