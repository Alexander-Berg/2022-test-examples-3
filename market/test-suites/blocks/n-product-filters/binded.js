import {makeSuite, makeCase} from 'ginny';
import VisualFilter from '@self/platform/components/FilterVisual/__pageObject';

export default makeSuite('Связанные фильтры', {
    environment: 'kadavr',
    story: {
        'При клике на sku фильтр': {
            выставляются: makeCase({
                id: 'marketfront-5129',
                issue: 'MARKETFRONT-23296',
                async test() {
                    const {selectedPickerIndex, skuState} = this.params;

                    if (skuState) {
                        await this.browser.setState('report', skuState);
                    }

                    await this.browser.waitForVisible(VisualFilter.root);

                    await this.colorFilter.selectColor(selectedPickerIndex);

                    return this.allure.runStep('Проверяем что выставился связанный фильтр', () =>
                        this.browser.waitUntil(async () => {
                            const title = await this.browser.getText(VisualFilter.title);

                            return title === 'Enum filter:\nenumValue1';
                        }, 5000, 'Связанный фильтр не выставился'));
                },
            }),
        },
    },
});
