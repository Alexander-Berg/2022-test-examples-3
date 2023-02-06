import {makeSuite, makeCase} from 'ginny';

import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import AmountSelect from '@self/root/src/components/AmountSelect/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';

export default makeSuite('Отсутствие вызова актуализации при взаимодействии со сниппетом, в котором не установлена галочка.', {
    feature: 'Отсутствие вызова актуализации при взаимодействии со сниппетом, в котором не установлена галочка',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                firstAmountSelect: () => this.createPageObject(
                    AmountSelect,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${AmountSelect.root}`,
                    }
                ),

                secondAmountSelect: () => this.createPageObject(
                    AmountSelect,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(1)} ${AmountSelect.root}`,
                    }
                ),
            });
        },

        'Должен отсутствовать вызов актуализации при удалении/изменении количество товара у сниппета без галочки': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Открыть страницу корзины',
                    async () => {
                        await this.firstCartItem.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается сниппет первого добавленного товара'
                        );
                        await this.secondCartItem.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается сниппет второго добавленного товара'
                        );

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'В первом сниппете установлена галочка'
                        );
                        await this.secondCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'Во втором сниппете установлена галочка'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Убрать галочку из первого сниппета',
                    async () => {
                        await this.firstCheckbox.toggle();
                        await this.browser.yaScenario(this, waitForCartActualization);

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            false,
                            'В первом сниппете галочка не отображается'
                        );

                        await this.secondCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'Во втором сниппете галочка отображается'
                        );

                        await this.firstAmountSelect.buttonPlus.isEnabled()
                            .should.eventually.be.equal(
                                false,
                                'Кнопка "+" в первом сниппете блокирована'
                            );

                        await this.secondAmountSelect.buttonPlus.isEnabled()
                            .should.eventually.be.equal(
                                true,
                                'Кнопка "+" во втором сниппете не блокирована'
                            );
                    }
                );
            },
        }),

    },
});
