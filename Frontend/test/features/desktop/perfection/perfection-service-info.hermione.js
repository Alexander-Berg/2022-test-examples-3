const PO = require('./PO');

describe('Светофоры идеальности', function() {
    describe('Положительные', function() {
        it('1. Пользователь с ограниченной ролью не видит светофор идеальности', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, {
                user: 'robot-abc-003',
            });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeam.scope(), 20000);
            // в правой части блока с заголовком сервиса нет информера "Состояние сервиса"
            // и розовой плашки с найденными проблемами
            await this.browser.assertView('service-header-by-robot-abc-003', PO.serviceHeader());
        });
        it('2. Пользователь с сильно ограниченной ролью не видит светофор идеальности', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, {
                user: 'robot-abc-004',
            });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeam.scope(), 20000);
            // в правой части блока с заголовком сервиса нет информера "Состояние сервиса"
            // и розовой плашки с найденными проблемами
            await this.browser.assertView('service-header-by-robot-abc-004', PO.serviceHeader());
        });
        it('3. Пользователь с базовой ролью может открыть модалку светофора через "Состояние сервиса"', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, {
                user: 'robot-abc-001',
            });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeam.scope(), 20000);
            // в правой части блока с заголовком сервиса есть информер "Состояние сервиса"
            // и розовая плашка с найденными проблемами
            await this.browser.waitForVisible(PO.serviceHeader.perfectionInfo(), 500);
            await this.browser.assertView('service-header-by-robot-abc-001', PO.serviceHeader());
            // навести курсор на информер
            await this.browser.moveToObject(PO.serviceHeader.perfectionInfo.status.trafficLights.criticalIcon());
            // появился попап с подробной информацией о состоянии сервиса
            await this.browser.waitForVisible(PO.visiblePopup2(), 5000);
            // кликнуть на второй слева квадратик в информере
            await this.browser.click(PO.serviceHeader.perfectionInfo.status.trafficLights.criticalIcon());
            // открылась модалка "Состояние сервиса Автотестовый сервис"
            await this.browser.waitForVisible(PO.perfectionModal.wikiText(), 5000);
            // в урле появились параметры "perfection_details=3838" и "issuesGroup=clarity"
            await this.browser.yaAssertUrlParam('perfection_details', '3838');
            await this.browser.yaAssertUrlParam('issuesGroup', 'clarity');
            // модалка "Состояние сервиса Автотестовый сервис", выбран таб "Понятность"
            await this.browser.assertPopupView(PO.perfectionModal(), 'clarity-tab-in-modal', PO.perfectionModal.wrapper(), {
                animationDisabled: true,
                redrawElements: ['.Modal_hasAnimation .Modal-Content'],
                redrawMode: 'hard',
            });
        });
        it('4. Переключение между вкладками в модалке светофора идеальности', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
                query: { perfection_details: 3838 },
            }, {
                user: 'robot-abc-001',
            });
            // открылась модалка "Состояние сервиса Автотестовый сервис"
            await this.browser.waitForVisible(PO.perfectionModal.wikiText(), 5000);
            // показана информация по критерию "Актуальное состояние вложенных сервисов"
            let title = await this.browser.getText(PO.perfectionModal.content.title());
            assert(title.includes('Актуальное состояние вложенных сервисов'),
                `Вместо "Актуального состояния вложенных сервисов" открыто про "${title}"`);

            // кликнуть в меню в левой части модалки на таб "Понятность"
            await this.browser.click(PO.perfectionModal.menu.clarity());
            // в урле параметры "issuesGroup" поменял значение на "clarity"
            await this.browser.yaAssertUrlParam('issuesGroup', 'clarity');
            // показана информация по критериям про описание сервиса
            title = await this.browser.getText(PO.perfectionModal.firstIssue.title());
            assert(title.includes('Описание сервиса на русском языке'),
                `Вместо "Описание сервиса на русском языке" открыто про "${title}"`);
            title = await this.browser.getText(PO.perfectionModal.secondIssue.title());
            assert(title.includes('Описание сервиса на английском языке'),
                `Вместо "Описание сервиса на английском языке" открыто про "${title}"`);

            // кликнуть в меню в левой части модалки на таб "Ресурсы"
            await this.browser.click(PO.perfectionModal.menu.resources());
            // в урле параметры "issuesGroup" поменял значение на "resources"
            await this.browser.yaAssertUrlParam('issuesGroup', 'resources');
            // показана информация по критериям "Наличие управляющего роботами",
            // "Наличие ответственных за сертификаты", "Наличие ответственных за оборудование на балансе сервиса"
            await this.browser.assertPopupView(PO.perfectionModal(), 'resources-tab', PO.perfectionModal.wrapper(), {
                animationDisabled: true,
                redrawElements: ['.Modal_hasAnimation .Modal-Content'],
                redrawMode: 'hard',
            });

            // кликнуть в меню в левой части модалки на таб "Структура"
            await this.browser.click(PO.perfectionModal.menu.structure());
            // в урле параметры "issuesGroup" поменял значение на "structure"
            await this.browser.yaAssertUrlParam('issuesGroup', 'structure');
            // показана информация по критериям "Структура базовых сервисов", "Консистентная структура Градиента",
            // "Сервисы поискового портала под value streams"
            await this.browser.assertPopupView(PO.perfectionModal(), 'structure-tab', PO.perfectionModal.wrapper(), {
                animationDisabled: true,
                redrawElements: ['.Modal_hasAnimation .Modal-Content'],
                redrawMode: 'hard',
            });

            // кликнуть в меню в левой части модалки на таб "Команда"
            await this.browser.click(PO.perfectionModal.menu.team());
            // в урле параметры "issuesGroup" поменял значение на "team"
            await this.browser.yaAssertUrlParam('issuesGroup', 'team');
            // показана информация по критериям "Структура базовых сервисов", "Консистентная структура Градиента",
            // "Сервисы поискового портала под value streams"
            await this.browser.assertPopupView(PO.perfectionModal(), 'team-tab', PO.perfectionModal.wrapper(), {
                animationDisabled: true,
                redrawElements: ['.Modal_hasAnimation .Modal-Content'],
                redrawMode: 'hard',
            });

            // кликнуть в меню в левой части модалки на таб "Сообщения о проблемах"
            await this.browser.click(PO.perfectionModal.menu.complaints());
            // в урле параметры "issuesGroup" поменял значение на "complaints"
            await this.browser.yaAssertUrlParam('issuesGroup', 'complaints');
            // заглушка "Пользователи пока не сообщали о проблемах в сервисе"
            await this.browser.assertPopupView(PO.perfectionModal(), 'complaints-tab', PO.perfectionModal.wrapper(), {
                animationDisabled: true,
                redrawElements: ['.Modal_hasAnimation .Modal-Content'],
                redrawMode: 'hard',
            });
        });
        it('5. Возможность открыть модалку светофора идеальности через плашку с найденными проблемами', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, {
                user: 'robot-abc-001',
            });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeam.scope(), 20000);
            // в правой части блока с заголовком сервиса есть информер "Состояние сервиса"
            // и розовая плашка с найденными проблемами
            await this.browser.waitForVisible(PO.serviceHeader.perfectionInfo(), 500);
            // фоткаем розовую плашку, где написано про 5 проблем
            await this.browser.assertView('problems-description', PO.serviceHeader.perfectionInfo.problemsDescription());
            // кликнуть на кнопку "Исправить"
            await this.browser.click(PO.serviceHeader.perfectionInfo.problemsDescription.fixButton());
            // открылась модалка "Состояние сервиса Автотестовый сервис" на табе "Вложенные сервисы"
            await this.browser.waitForVisible(PO.perfectionModal.wikiText(), 5000);
            // показана информация по критерию "Актуальное состояние вложенных сервисов"
            let title = await this.browser.getText(PO.perfectionModal.content.title());
            assert(title.includes('Актуальное состояние вложенных сервисов'),
                `Вместо "Актуального состояния вложенных сервисов" открыто про "${title}"`);
            // в урле появился параметр "perfection_details=3838"
            await this.browser.yaAssertUrlParam('perfection_details', '3838');
            // cумма чисел, указанных в квадратах рядом с названиями критериев в модалке, также равна 5
            await this.browser.assertPopupView(PO.perfectionModal(), 'opened-modal-with-problems', PO.perfectionModal.wrapper(), {
                animationDisabled: true,
                redrawElements: ['.Modal_hasAnimation .Modal-Content'],
                redrawMode: 'hard',
            });
        });
        it('6. Подача апелляции через модалку светофора идеальности', async function() {
            // залогиниться пользователем robot-abc-002
            // открыть страницу сервиса "Автотестовый сервис с отрицанием проблем" сразу с модалкой
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-not-a-problem/',
                query: {
                    perfection_details: 5446,
                    issuesGroup: 'clarity',
                },
            }, {
                user: 'robot-abc-002',
            });
            // открылась модалка "Состояние сервиса Автотестовый сервис с отрицанием проблем"
            await this.browser.waitForVisible(PO.perfectionModal.wikiText(), 5000);
            // в модалке выбран таб "Понятность", показаны два критерия -
            // "Описание сервиса на русском языке" и "Описание сервиса на английском языке"
            await this.browser.yaAssertUrlParam('issuesGroup', 'clarity');
            title = await this.browser.getText(PO.perfectionModal.firstIssue.title());
            assert(title.includes('Описание сервиса на русском языке'),
                `Вместо "Описание сервиса на русском языке" открыто про "${title}"`);
            title = await this.browser.getText(PO.perfectionModal.secondIssue.title());
            assert(title.includes('Описание сервиса на английском языке'),
                `Вместо "Описание сервиса на английском языке" открыто про "${title}"`);
            // внизу модалки есть кнопка "Не считаю проблемой"
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.notProblemButton(), 500);
            // кликнуть на кнопку "Не считаю проблемой"
            await this.browser.click(PO.perfectionModal.appealBlock.notProblemButton());
            // в модалке появилось поле ввода "Не считаю проблемой" с пояснением
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.form(), 3000);
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.explanation(), 500);
            // кнопка "Отправить" задизейблена
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.disabledSubmitButton(), 500);
            // ввести текст "Тестовый текст с отрицанием проблемы" в поле ввода "Не считаю проблемой"
            await this.browser.addValue(PO.perfectionModal.appealBlock.form.textarea(), 'Тестовый текст с отрицанием проблемы');
            // кнопка "Отправить" стала доступна
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.activeSubmitButton(), 2000);
            // кликнуть на кнопку "Отправить"
            await this.browser.click(PO.perfectionModal.appealBlock.activeSubmitButton());
            // поле для ввода пропало
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.form(), 3000, true);
            // кнопка "Не считаю проблемой" осталась внизу модалки
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.notProblemButton(), 500);
        });
        it('7. Отмена подачи апелляции через модалку светофора идеальности', async function() {
            // залогиниться пользователем robot-abc-002
            // открыть страницу сервиса "Автотестовый сервис с отрицанием проблем" сразу с модалкой
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-not-a-problem/',
                query: {
                    perfection_details: 5446,
                    issuesGroup: 'clarity',
                },
            }, {
                user: 'robot-abc-002',
            });
            // открылась модалка "Состояние сервиса Автотестовый сервис с отрицанием проблем"
            await this.browser.waitForVisible(PO.perfectionModal.wikiText(), 5000);
            // в модалке выбран таб "Понятность", показаны два критерия -
            // "Описание сервиса на русском языке" и "Описание сервиса на английском языке"
            await this.browser.yaAssertUrlParam('issuesGroup', 'clarity');
            title = await this.browser.getText(PO.perfectionModal.firstIssue.title());
            assert(title.includes('Описание сервиса на русском языке'),
                `Вместо "Описание сервиса на русском языке" открыто про "${title}"`);
            title = await this.browser.getText(PO.perfectionModal.secondIssue.title());
            assert(title.includes('Описание сервиса на английском языке'),
                `Вместо "Описание сервиса на английском языке" открыто про "${title}"`);
            // внизу модалки есть кнопка "Не считаю проблемой"
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.notProblemButton(), 500);
            // кликнуть на кнопку "Не считаю проблемой"
            await this.browser.click(PO.perfectionModal.appealBlock.notProblemButton());
            // в модалке появилось поле ввода "Не считаю проблемой"
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.form(), 3000);
            // кликнуть на серую надпись "Отмена"
            await this.browser.click(PO.perfectionModal.appealBlock.cancelButton());
            // поле для ввода пропало
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.form(), 3000, true);
            // кнопка "Не считаю проблемой" осталась внизу модалки
            await this.browser.waitForVisible(PO.perfectionModal.appealBlock.notProblemButton(), 500);
        });
    });
});
