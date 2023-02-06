import {makeCase, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import indexPageMock from './fixtures/index-page';
import questionsMock from './fixtures/questions';

/**
 * Тесты на блок ecom опросы
 * @param {PageObject.Question} widget
 * @param {PageObject.Options} options
 * @param {PageObject.InputPopup} inputPopup
 */
export default makeSuite('ECom профиль: опрос', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-71155',
    story: {
        'Пришёл опрос с бекенда.': {
            async beforeEach() {
                await this.browser.setState('Tarantino.data.result', [indexPageMock]);
                await this.browser.setState('djRecommender.ecomQuestions', questionsMock.questions);
                await this.browser.setState('djRecommender.ecomQuestionsMap', questionsMock.nextQuestionMap);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);
            },
            'Проверяем:': {
                'показывается ли виджет': makeCase({
                    id: 'm-touch-3939',
                    async test() {
                        await this.widget.waitForVisible();
                        await this.widget.title.getText().should.eventually
                            .to.be.equal('Советовать вам товары для детей?');
                    },
                }),
                'можно ли закрыть крестиком': makeCase({
                    id: 'm-touch-3937',
                    async test() {
                        await this.widget.waitForVisible(); // Виден
                        await this.widget.closeButton.click(); // Скрываем
                        await this.widget.waitForVisible(500, true); // Скрыт
                    },
                }),
                'нажатие на кнопку "Нет" закрывает опрос': makeCase({
                    id: 'm-touch-3938',
                    async test() {
                        await this.widget.waitForVisible();
                        await this.options.clickOptionByText('Закрыть');

                        await this.widget.title.getText().should.eventually.to.be.equal('Спасибо!');
                        await this.widget.subtitle.getText().should.eventually
                            .to.be.equal('Теперь рекомендации будут точнее, а нужные товары найдутся быстрее');

                        await this.widget.waitForTitleIsHidden(); // Текст пропадает через секунду
                    },
                }),
                'ввод кастомного варианта ответа': makeCase({
                    id: 'm-touch-3941',
                    async test() {
                        await this.widget.waitForVisible();
                        await this.options.clickOptionByText('Кастомный ответ');
                        await this.options.clickOptionByText('Другое');

                        await this.inputPopup.isVisible().should.eventually.to.be.equal(true);
                        await this.inputPopup.title.getText().should.eventually.to.be.equal('На чём вы играете?');

                        await this.inputPopup.isSubmitButtonDisabled().should.eventually.to.be.equal(true);
                        await this.inputPopup.input.setValue('новый ответ');
                        await this.inputPopup.isSubmitButtonDisabled().should.eventually.to.be.equal(false);
                        await this.inputPopup.submitButton.click();

                        await this.options.option(0).getText().should.eventually.to.be.equal('новый ответ');
                        await this.options.isOptionSelected(0, true);
                    },
                }),
                'выделяются опции в мультивыборе': makeCase({
                    id: 'm-touch-3940',
                    async test() {
                        await this.widget.waitForVisible();
                        await this.options.clickOptionByText('Мультивыбор');

                        await this.widget.title.getText().should.eventually.to.be.equal('Какого возраста?');
                        await this.widget.subtitle.getText().should.eventually
                            .to.be.equal('Можно выбрать несколько вариантов');

                        await this.widget.isNextButtonDisabled().should.eventually.to.be.equal(true);
                        await this.options.isOptionSelected(0, false);

                        await this.options.clickOptionByText('До 6 мес');

                        await this.widget.isNextButtonDisabled().should.eventually.to.be.equal(false);
                        await this.options.isOptionSelected(0, true);

                        // Отжимаем кнопку
                        await this.options.clickOptionByText('До 6 мес');

                        await this.widget.isNextButtonDisabled().should.eventually.to.be.equal(true);
                        await this.options.isOptionSelected(0, false);
                    },
                }),
                'вопрос с большими картинками': makeCase({
                    id: 'm-touch-3944',
                    async test() {
                        await this.widget.waitForVisible();
                        await this.options.clickOptionByText('Большие фото');

                        await this.widget.title.getText().should.eventually.to.be.equal('Для девочек или мальчиков?');
                        await this.options.clickOptionByText('Девочек');
                        await this.widget.title.getText().should.eventually.to.be.equal('Спасибо!');
                    },
                }),
                'проходим опрос до конца и видим Спасибо': makeCase({
                    id: 'm-touch-3943',
                    async test() {
                        await this.widget.waitForVisible();
                        await this.options.clickOptionByText('Мультивыбор');

                        await this.widget.title.getText().should.eventually.to.be.equal('Какого возраста?');
                        await this.options.clickOptionByText('До 6 мес');
                        await this.widget.nextButton.click(); // И жмём Готово

                        await this.widget.title.getText().should.eventually.to.be.equal('Спасибо!');
                        await this.widget.subtitle.getText().should.eventually
                            .to.be.equal('Теперь рекомендации будут точнее, а нужные товары найдутся быстрее');

                        await this.widget.waitForTitleIsHidden(); // Текст пропадает через секунду
                    },
                }),
            },
        },
    },
});

