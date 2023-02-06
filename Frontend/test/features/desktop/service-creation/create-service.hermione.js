const PO = require('./PO');
const langs = ['ru', 'en'];

describe('Форма создания сервиса', () => {
    describe('Положительные', () => {
        langs.forEach(lang => {
            it(`Создание сервиса(${lang})`, async function() {
                // открыть форму создания сервиса тестового ABC (/create-service?lang=${lang})
                await this.browser.openIntranetPage({ pathname: '/create-service', query: { lang } });
                await this.browser.disableAnimations(':not(.Textinput-Box):not(.Textinput-Hint)');

                // появился первый шаг визарда
                await this.browser.waitForVisible(PO.serviceCreation.wizard(), 10000);
                await this.browser.assertView('first-step', PO.serviceCreation.wizard());

                // заполнить поле "Slug" значением "test-01"
                await this.browser.customSetValue(PO.serviceCreation.slugInput.control(), 'test-01');

                // нажать кнопку "Далее"
                await this.browser.click(PO.serviceCreation.wizard.nextButton());

                // появились ошибки валидации
                await this.browser.waitForVisible(PO.serviceCreation.nameInput.hint(), 5000);
                await this.browser.waitForVisible(PO.serviceCreation.englishNameInput.hint(), 5000);
                await this.browser.waitForVisible(PO.serviceCreation.slugInput.hint(), 5000);

                await this.browser.assertView('invalid-first-step', PO.serviceCreation.wizard());

                // заполнить поле "Название" значением "Тест-01"
                await this.browser.customSetValue(PO.serviceCreation.nameInput.control(), 'Тест-01');

                // заполнить поле "Название на английском языке" значением "Test-01"
                await this.browser.customSetValue(PO.serviceCreation.englishNameInput.control(), 'Test-01');

                // выбрать предложенное значение поля "Код"
                await this.browser.click(PO.serviceCreation.slugInput.control());
                await this.browser.waitForVisible(PO.serviceCreation.slugSuggestion.presetItem(), 5000);
                await this.browser.click(PO.serviceCreation.slugSuggestion.presetItem());

                // ввести в поле "Руководитель" текст "roma-vat"
                await this.browser.customSetValue(PO.serviceCreation.ownerInput.control(), 'roma-vat');

                // выбрать предложенное значение "Роман Ватютов"
                await this.browser.waitForVisible(PO.suggestPopup.suggestChoices.staffChoice(), 5000);
                await this.browser.click(PO.suggestPopup.suggestChoices.staffChoice());

                // ошибки валидации скрылись
                await this.browser.waitUntil(
                    () => this.browser.isVisible(PO.serviceCreation.nameInput.hint()).then(visible => !visible),
                    5000);
                await this.browser.waitUntil(
                    () => this.browser.isVisible(PO.serviceCreation.englishNameInput.hint()).then(visible => !visible),
                    5000);
                await this.browser.waitUntil(
                    () => this.browser.isVisible(PO.serviceCreation.slugInput.hint()).then(visible => !visible),
                    5000);

                // заполнить поле "Родитель" значением "orange" и выбрать его в списке саджеста
                await this.browser.customSetValue(PO.serviceCreation.parentInput.control(), 'orange');
                await this.browser.waitForVisible(PO.orangeParentChoice(), 10000);
                await this.browser.click(PO.orangeParentChoice());

                // заполнить поле "Теги" значением "MARTY" и выбрать его в списке саджеста
                await this.browser.customSetValue(PO.serviceCreation.tagsInput.control(), 'MARTY');
                await this.browser.waitForVisible(PO.martyTagChoice(), 10000);
                await this.browser.click(PO.martyTagChoice());
                await this.browser.click(PO.serviceCreation.title());
                // заполненный первый шаг
                await this.browser.assertView('completed-first-step', PO.serviceCreation.wizard());

                // нажать кнопку "Далее"
                await this.browser.click(PO.serviceCreation.wizard.nextButton());

                // появился второй шаг визарда
                await this.browser.waitForVisible(PO.serviceCreation.descriptionInput.control(), 5000);
                await this.browser.waitForVisible(PO.serviceCreation.englishDescriptionInput.control(), 5000);
                await this.browser.assertView('second-step', PO.serviceCreation.wizard());

                // заполнить поле "Описание" значением "Тестовое описание"
                await this.browser.customSetValue(PO.serviceCreation.descriptionInput.control(), '*Тестовое* **описание** `сервиса`');

                // заполнить поле "Описание на английском языке" значением "Test description"
                await this.browser.customSetValue(PO.serviceCreation.englishDescriptionInput.control(), '*Test* `service` **description**');

                // заполненный второй шаг
                await this.browser.assertView('completed-second-step', PO.serviceCreation.wizard());

                // нажать кнопку "Далее"
                await this.browser.click(PO.serviceCreation.wizard.nextButton());

                // предпросмотр
                await this.browser.waitForVisible(PO.serviceCreation.preview.description.wikiDocInited(), 10000);
                await this.browser.waitForVisible(PO.serviceCreation.preview.englishDescription.wikiDocInited(), 10000);
                await this.browser.assertView('preview', PO.serviceCreation.wizard());
            });
        });
        it('Создание сервиса без указания родительского сервиса', async function() {
            // открыть форму создания сервиса
            await this.browser.openIntranetPage({ pathname: '/create-service' }, { user: 'robot-abc-002' });

            // появился первый шаг визарда
            await this.browser.waitForVisible(PO.serviceCreation.wizard(), 10000);

            // ввести в поле "Название" уникальное название сервиса
            await this.browser.customSetValue(PO.serviceCreation.nameInput.control(), 'Тест фронтенда-14-09-11-45');

            // заполнить поле "Название на английском языке"
            await this.browser.customSetValue(PO.serviceCreation.englishNameInput.control(), 'Frontend test-14-09-11-45');

            // убрать курсор из поля "Название на английском"
            await this.browser.click(PO.serviceCreation.slugInput.control());
            // под полем "Slug" появилась надпись "Например" с предложенным слагом
            await this.browser.waitForVisible(PO.serviceCreation.slugSuggestion.presetItem());
            const slugHint = await this.browser.getText(PO.serviceCreation.slugSuggestion.presetItem());
            assert(slugHint === 'frontend_test_14_09_11_45',
                `в подсказке должен быть slug="frontend_test_14_09_11_45", а появился "${slugHint}"`,
            );

            // кликнуть на предложенный в строке "Например" слаг
            await this.browser.click(PO.serviceCreation.slugSuggestion.presetItem());

            // предложенное значение подставилось в поле "Slug"
            const slugInInput = await this.browser.getValue(PO.serviceCreation.slugInput.control());
            assert(slugInInput === slugHint,
                `в подсказке должен быть slug="${slugHint}", а появился "${slugInInput}"`,
            );

            // ввести в поле "Руководитель" текст "roma-vat"
            await this.browser.customSetValue(PO.serviceCreation.ownerInput.control(), 'roma-vat');
            // выбрать предложенное значение "Роман Ватютов"
            await this.browser.waitForVisible(PO.suggestPopup.suggestChoices.staffChoice(), 5000);
            await this.browser.click(PO.suggestPopup.suggestChoices.staffChoice());

            // вид заполненного первого шага формы
            await this.browser.assertView('first-step-no-parent', PO.serviceCreation.wizard());

            // нажать кнопку "Далее"
            await this.browser.click(PO.serviceCreation.wizard.nextButton());

            // произошёл переход на второй шаг визарда - "Описание"
            await this.browser.waitForVisible(PO.serviceCreation.descriptionInput.control(), 5000);
            await this.browser.waitForVisible(PO.serviceCreation.englishDescriptionInput.control(), 5000);

            // заполнить поле "Описание"
            await this.browser.customSetValue(PO.serviceCreation.descriptionInput.control(), '**Тест !!wiki!!-разметки** --test--');

            // заполнить поле "Описание на английском языке"
            await this.browser.customSetValue(PO.serviceCreation.englishDescriptionInput.control(), '((http://www.yandex.ru Яндекс))');

            // заполненный второй шаг
            await this.browser.assertView('completed-second-step-no-parent', PO.serviceCreation.wizard());

            // нажать кнопку "Далее"
            await this.browser.click(PO.serviceCreation.wizard.nextButton());

            // предпросмотр
            await this.browser.waitForVisible(PO.serviceCreation.preview.description.wikiDocInited(), 10000);
            await this.browser.waitForVisible(PO.serviceCreation.preview.englishDescription.wikiDocInited(), 10000);
            await this.browser.assertView('preview-no-parent', PO.serviceCreation.wizard());

            // кликнуть на кнопку "Отправить"
            await this.browser.click(PO.serviceCreation.wizard.submitButton());

            // произошёл переход на страницу сервиса
            await this.browser.waitForVisible(PO.service(), 20000);
            const url = await this.browser.yaGetParsedUrl();
            assert(url.pathname === '/services/frontend_test_14_09_11_45',
                `Не произошел редирект на страницу сервиса со слагом "${slugHint}"`,
            );
            // поле "Описание" заполнено
            await this.browser.waitForVisible(PO.serviceDescription(), 5000);
            await this.browser.assertView('service-description-field', PO.serviceDescription());
        });
        it('Подсказка потенциальных родителей нового сервиса', async function() {
            // открыть форму создания сервиса
            await this.browser.openIntranetPage({ pathname: '/create-service' }, { user: 'robot-abc-002' });

            // появился первый шаг визарда
            await this.browser.waitForVisible(PO.serviceCreation.wizard(), 10000);

            // ввести в поле "Руководитель" текст "zomb-prj-204"
            await this.browser.customSetValue(PO.serviceCreation.ownerInput.control(), 'zomb-prj-204');
            // выбрать предложенное значение "The Kitty"
            await this.browser.waitForVisible(PO.suggestPopup.suggestChoices.staffChoice(), 5000);
            await this.browser.click(PO.suggestPopup.suggestChoices.staffChoice());

            // под полем "Родитель" появились названия сервисов
            await this.browser.waitForVisible(PO.serviceCreation.parentSuggestion.presetItem(), 2000);
            await this.browser.assertView('suggested-parent', PO.serviceCreation.parentField());
        });
        it('При переключении между шагами визарда введённые данные сохраняются', async function() {
            // открыть форму создания сервиса
            await this.browser.openIntranetPage({ pathname: '/create-service' }, { user: 'robot-abc-002' });

            // появился первый шаг визарда
            await this.browser.waitForVisible(PO.serviceCreation.wizard(), 10000);

            // ввести в поле "Руководитель" текст "zomb-prj-204"
            await this.browser.customSetValue(PO.serviceCreation.ownerInput.control(), 'zomb-prj-204');
            // выбрать предложенное значение "The Kitty"
            await this.browser.waitForVisible(PO.suggestPopup.suggestChoices.staffChoice(), 5000);
            await this.browser.click(PO.suggestPopup.suggestChoices.staffChoice());

            // ввести в поле "Название" значение "Тест шагов визарда"
            await this.browser.customSetValue(PO.serviceCreation.nameInput.control(), 'Тест шагов визарда');

            // ввести в поле "Название на английском" значение "Wizard steps test"
            await this.browser.customSetValue(PO.serviceCreation.englishNameInput.control(), 'Wizard steps test');

            // заполнить поле "Slug" значением "wizard-steps-test"
            await this.browser.customSetValue(PO.serviceCreation.slugInput.control(), 'wizard-steps-test');

            // нажать кнопку "Далее"
            await this.browser.click(PO.serviceCreation.wizard.nextButton());

            // произошёл переход на второй шаг визарда - "Описание"
            await this.browser.waitForVisible(PO.serviceCreation.descriptionInput.control(), 5000);

            // заполнить поле "Описание"
            await this.browser.customSetValue(PO.serviceCreation.descriptionInput.control(), 'тестовое описание');

            // кликнуть на кнопку "Назад", расположенную в правом нижнем углу
            await this.browser.click(PO.serviceCreation.wizard.backButton());

            // произошёл переход на первый шаг визарда
            await this.browser.waitForVisible(PO.serviceCreation.nameInput(), 10000);

            // проверка того, что значения в полях сохранились
            await this.browser.assertView('back-to-first-step', PO.serviceCreation.wizard());

            // в боковом меню кликнуть на пункт "Описание"
            await this.browser.click(PO.serviceCreation.wizard.menu.description());

            // произошёл переход на второй шаг визарда - "Описание"
            await this.browser.waitForVisible(PO.serviceCreation.descriptionInput.control(), 5000);
            await this.browser.waitForVisible(PO.serviceCreation.englishDescriptionInput.control(), 5000);

            // поле "Описание" сохранило введённые значения, у поля "Описание на английском" появилась ошибка
            await this.browser.assertView('back-to-second-step', PO.serviceCreation.wizard());
        });
    });
});
