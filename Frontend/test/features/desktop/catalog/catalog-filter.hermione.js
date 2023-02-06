const assert = require('assert');

const PO = require('./PO');

function openCatalogue(browser, query, keepLS) {
    return browser
        .openIntranetPage({ pathname: '/' }, { clearLS: !keepLS })
        .execute(keepLS => {
            // Нужно открывать каждый раз с чистым локалстораджем, чтобы не загружались сохранённые фильтры
            keepLS || window.localStorage.removeItem('catalogue.filters');
        }, keepLS)
        .openIntranetPage({ pathname: '/', query }, { clearLS: !keepLS })
        .waitForVisible(PO.catalogue.treeRow(), 10000);
}

describe('Фильтрация каталога сервисов', () => {
    describe('Положительные', () => {
        describe('Применение фильтров', () => {
            it('1. Комбинация фильтров', async function() {
                await openCatalogue(this.browser);

                // исходное состояние каталога
                await this.browser.assertView('initial', PO.catalogue());

                let parsedUrl = await this.browser.yaGetParsedUrl();

                // в URL отсуствуют параметры фильтрации
                assert(
                    [
                        'search',
                        'member',
                        'owner',
                        'department',
                        'states',
                        'isSuspicious',
                        'hasExternalMembers',
                        'tags',
                    ].every(param => !parsedUrl.searchParams.has(param)),
                    'В урле присутствуют параметры фильтрации'
                );

                await this.browser
                    // в фильтре "Участник" кликнуть на предложенный под полем вариант "robot-internal-003"
                    .click(PO.filters.groupTypeMember.presetItem())

                    // в фильтре "Идеальность" поставить галочку в пункте "С проблемами"
                    .click(PO.filters.groupTypeIsSuspicious.withProblems())

                    // ждём того как дерево начнёт обновляться и закончит
                    .waitForVisible(PO.treeDisabled(), 10000)
                    .waitForVisible(PO.treeDisabled(), 10000, true)

                    // клик (сбросить плавающий фокус)
                    .click(PO.catalogue.aside())

                    // отображаются только сервисы с проблемами, в которых пользователь является частью команды
                    .assertView('filtered', PO.catalogue());

                // в URL присутствует параметр isSuspicious=true
                await this.browser.yaAssertUrlParam('isSuspicious', 'true');

                // в URL присутствует параметр member=robot-internal-003
                await this.browser.yaAssertUrlParam('member', 'robot-internal-003');

                parsedUrl = await this.browser.yaGetParsedUrl();

                // в URL отсуствуют параметры кроме приведенных выше
                assert(
                    [
                        'search',
                        'owner',
                        'department',
                        'states',
                        'hasExternalMembers',
                        'tags',
                    ].every(param => !parsedUrl.searchParams.has(param)),
                    'В урле присутствуют лишние параметры фильтрации'
                );

                await this.browser
                    // обновить страницу
                    .refresh()
                    .waitForVisible(PO.catalogue.treeRow(), 10000)

                    // фильтрация после перехода по ссылке идентична простановке фильтров через интерфейс
                    .assertView('refreshed', PO.catalogue());
            });

            it('2. Фильтр по названию', async function() {
                await openCatalogue(this.browser);
                // в URL отсутствует параметр search=котики
                await this.browser.yaAssertUrlParam('search', 'котики', true);
                // в поле фильтра "Название" ввести "котики"
                await this.browser.setValue(PO.filters.groupTypeSearch.textinputControl(), 'котики');
                // в URL присутствует параметр search=котики
                await this.browser.yaAssertUrlParam('search', 'котики');
            });

            it('3. Фильтр по участнику', async function() {
                await openCatalogue(this.browser);
                // в URL отсутствует параметр member=robot-internal-003
                await this.browser.yaAssertUrlParam('member', 'robot-internal-003', true);

                await this.browser
                    // в поле фильтра "Участник" ввести "robot-internal-003"
                    .customSetValue(PO.filters.groupTypeMember.textinputControl(), 'robot-internal-003')
                    .waitForVisible(PO.suggestVisible.firstItem())
                    // выбрать в сажесте "robot-internal-003"
                    .click(PO.suggestVisible.firstItem());

                // в URL присутствует параметр member=robot-internal-003
                await this.browser.yaAssertUrlParam('member', 'robot-internal-003');
            });

            it('4. Фильтр по руководителю', async function() {
                await openCatalogue(this.browser);
                // в URL отсутствует параметр owner=robot-internal-003
                await this.browser.yaAssertUrlParam('owner', 'robot-internal-003', true);

                await this.browser
                    // в поле фильтра "Руководитель" ввести "robot-internal-003"
                    .customSetValue(PO.filters.groupTypeOwner.textinputControl(), 'robot-internal-003')
                    .waitForVisible(PO.suggestVisible.firstItem())
                    // выбрать в сажесте "robot-internal-003"
                    .click(PO.suggestVisible.firstItem());

                // в URL присутствует параметр owner=robot-internal-003
                await this.browser.yaAssertUrlParam('owner', 'robot-internal-003');
            });

            it('5. Фильтр по подразделению', async function() {
                await openCatalogue(this.browser);
                // в URL отсутствует параметр department=1
                await this.browser.yaAssertUrlParam('department', '1', true);

                await this.browser
                    // в поле фильтра "Подразделение" ввести "яндекс"
                    .customSetValue(PO.filters.groupTypeDepartment.textinputControl(), 'яндекс')
                    .waitForVisible(PO.suggestVisible.firstItem())
                    // выбрать в сажесте "Яндекс"
                    .click(PO.suggestVisible.firstItem());

                // в URL присутствует параметр department=1
                await this.browser.yaAssertUrlParam('department', '1');
            });

            it('6. Фильтр по статусу сервиса', async function() {
                await openCatalogue(this.browser);
                // в фильтре "Статус сервиса" снять галочку "Развивается"
                await this.browser.click(PO.filters.groupTypeStates.inDevelopment());
                // в URL отсутствует параметр states=develop
                await this.browser.yaAssertUrlParam('states', 'develop', true);
                // в URL присутствует параметр states=supported
                await this.browser.yaAssertUrlParam('states', 'supported');
                // в URL отсутствует параметр states=closed
                await this.browser.yaAssertUrlParam('states', 'closed', true);
                // в URL присутствует параметр states=needinfo
                await this.browser.yaAssertUrlParam('states', 'needinfo');
            });

            it('7. Фильтр по идеальности', async function() {
                await openCatalogue(this.browser);
                // в фильтре "Идеальность" установить галочку "Идеальный"
                await this.browser.click(PO.filters.groupTypeIsSuspicious.perfect());
                // в URL присутствует параметр isSuspicious=false
                await this.browser.yaAssertUrlParam('isSuspicious', 'false');
                // в URL отсутствует параметр isSuspicious=true
                await this.browser.yaAssertUrlParam('isSuspicious', 'true', true);
            });

            it('8. Фильтр по внешним участникам', async function() {
                await openCatalogue(this.browser);
                // в фильтре "Внешние участники" установить галочку "С внешними"
                await this.browser.click(PO.filters.groupTypeExternals.withExternals());
                // в URL присутствует параметр hasExternalMembers=true
                await this.browser.yaAssertUrlParam('hasExternalMembers', 'true');
                // в URL отсутствует параметр hasExternalMembers=false
                await this.browser.yaAssertUrlParam('hasExternalMembers', 'false', true);
            });

            it('9. Фильтр по тегу', async function() {
                await openCatalogue(this.browser);
                // в URL отсутствует параметр tags=63
                await this.browser.yaAssertUrlParam('tags', '63', true);

                await this.browser
                    // в поле фильтра "Теги" ввести "GDPR"
                    .customSetValue(PO.filters.groupTypeTags.textinputControl(), 'GDPR')
                    .waitForVisible(PO.suggestVisible.firstItem())
                    // выбрать GDPR в саджесте
                    .click(PO.suggestVisible.firstItem());

                // в URL присутствует параметр tags=63
                await this.browser.yaAssertUrlParam('tags', '63');
            });
        });

        describe('Состояния каталога', () => {
            it('10. Плашка отсутствия результатов поиска', async function() {
                return this.browser
                    // открыть главную страницу тестового ABC с фильтрацией по названию "catshug" (/?search=catshug)
                    .openIntranetPage({ pathname: '/', query: { search: 'catshug' } })
                    .waitForVisible(PO.tree.message(), 10000)
                    // отображается сообщение "Ни один сервис не попадает под выбранные фильтры"
                    .assertView('no-results', PO.tree());
            });
        });

        describe('Сохранение фильтров', () => {
            it('11. Сохранение фильтров', async function() {
                // открыть главную страницу тестового ABC (/)
                await openCatalogue(this.browser);
                // в URL отсуствует параметр member
                await this.browser.yaAssertUrlParam('member', null, true);
                // в фильтре "Участник" кликнуть на предложенный под полем вариант "robot-internal-003"
                await this.browser.click(PO.filters.groupTypeMember.presetItem());
                // открыть главную страницу тестового ABC (/)
                await openCatalogue(this.browser, null, true);
                // в URL присутствует параметр member=robot-internal-003
                await this.browser.yaAssertUrlParam('member', 'robot-internal-003');
            });
        });
    });
});
