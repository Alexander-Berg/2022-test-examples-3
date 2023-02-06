import {makeSuite, makeCase} from 'ginny';

import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';

/**
 * Тесты на виджет GainExpertise
 */

export default makeSuite('Попап экспертизы.', {
    feature: 'Страница Экспертизы',
    issue: 'MARKETFRONT-16111',
    id: 'marketfront-4115',
    params: {
        expectedBadgeText: 'Ожидаемый текст бейджа',
    },
    story: {
        'По умолчанию': {
            'Должен содержать верное сообщение': makeCase({
                async test() {
                    await this.setPageObjects({
                        gainedExpertise: () => this.createPageObject(GainedExpertise),
                    });

                    await this.gainedExpertise.waitForModalVisible(5000);

                    return this.expect(this.gainedExpertise.getBadgeText())
                        .to.be.equal(this.params.expectedBadgeText, 'Получили новый уровень экспертизы');
                },
            }),
        },
    },
});
