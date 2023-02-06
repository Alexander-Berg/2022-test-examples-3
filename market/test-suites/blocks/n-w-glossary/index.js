import {makeCase, makeSuite} from 'ginny';
import _ from 'lodash';
import Glossary from '@self/platform/spec/page-objects/glossary';

/**
 * Тест на блок n-w-glossary.
 */
export default makeSuite('Блок терминов.', {
    feature: 'Контент страницы',
    story: {
        'По умолчанию': {
            'каждый термин содержит определение и описание': makeCase({
                id: 'marketfront-1049',
                issue: 'MARKETVERSTKA-25097',
                async test() {
                    const glossaries = await this.browser.allure.runStep(
                        'Получаем все блоки терминов',
                        () => this.browser.elements(Glossary.root)
                            .then(({value}) => _.times(
                                value.length,
                                index => this.createPageObject(
                                    Glossary,
                                    {
                                        root: `${Glossary.root}:nth-child(${index + 1})`,
                                    }
                                )
                            ))
                    );

                    return this.browser.allure.runStep(
                        'Проверяем, что каждый блок термина содержит заголовок и описание',
                        () => Promise.resolve(
                            glossaries.map(async glossary => {
                                await glossary.hasTerm();
                                await glossary.hasDescription();

                                return true;
                            })
                        )
                    );
                },
            }),
        },
    },
});
