const assert = require('chai').assert;

const PO = require('../../../../page-objects');

describe('Создание новой цели', function() {
    beforeEach(function() {
        return this.browser
            .loginToGoals()
            .preparePage('main', '/')
            .waitForVisible(PO.content())

            .yaOpenCreateMenuAndClick(PO.header, 'newGoalDefaultButton')

            .waitForVisible(PO.goalEditForm())
            .yaShouldExist(PO.goalEditForm.responsibleFormField.chosenBox(), null, false);
    });

    it('Форма открывается', function() {
        return this.browser
            .assertView('plain', PO.goalEditForm());
    });

    it('Форма требует заполнить поля название и ответственный', function() {
        return this.browser
            .click(PO.goalEditForm.submitButton())

            .yaShouldBeVisible(PO.goalEditForm.titleFormField.error(), 'Ошибка в тайтле не отобразилась')
            .yaShouldBeVisible(PO.goalEditForm.responsibleFormField.error(), 'Ошибка в ответственном не отобразилась')

            .execute(function(selector) {
                document.querySelector(selector).closest('.cards__ii').scroll(0, -1000);
            }, PO.goalEditForm())
            .assertView('validation-full-error', PO.goalEditForm());
    });

    it('Форма требует заполнить поле название', function() {
        return this.browser
            .yaSuggestChooseItem(
                PO.goalEditForm.responsibleFormField(),
                PO.goalEditForm.responsibleFormField.suggest(),
                'dusty',
                'Александр Шлейко (@dusty)',
            )

            .click(PO.goalEditForm.submitButton())

            .yaShouldBeVisible(PO.goalEditForm.titleFormField.error(), 'Ошибка в тайтле не отобразилась')
            .yaShouldBeVisible(PO.goalEditForm.responsibleFormField.error(), 'Ошибка в ответственном отобразилась', false)

            .execute(function(selector) {
                document.querySelector(selector).closest('.cards__ii').scroll(0, -1000);
            }, PO.goalEditForm())
            .assertView('validation-title-error', PO.goalEditForm());
    });

    it('Форма требует заполнить поле ответственный', function() {
        return this.browser
            .click(PO.goalEditForm.titleFormField.control())
            .yaKeyPress('Goal dusty')

            .click(PO.goalEditForm.submitButton())

            .yaShouldBeVisible(PO.goalEditForm.titleFormField.error(), 'Ошибка в тайтле отобразилась', false)
            .yaShouldBeVisible(PO.goalEditForm.responsibleFormField.error(), 'Ошибка в ответственном не отобразилась')

            .execute(function(selector) {
                document.querySelector(selector).closest('.cards__ii').scroll(0, -1000);
            }, PO.goalEditForm())
            .assertView('validation-responsible-error', PO.goalEditForm());
    });

    it('Форма создает цель по названию и ответственному', function() {
        return this.browser
            .click(PO.goalEditForm.titleFormField.control())
            .yaKeyPress('Goal dusty')

            .yaSuggestChooseItem(
                PO.goalEditForm.responsibleFormField(),
                PO.goalEditForm.responsibleFormField.suggest(),
                'dusty',
                'Александр Шлейко (@dusty)',
            )

            .click(PO.goalEditForm.submitButton())

            .waitForVisible(PO.goal())
            .getText(PO.goal.info.title())
            .then(title => {
                assert.equal(title, 'Goal dusty', 'Название цели не совпадает с данными формы');
            })
            .getText(PO.goal.info.details.responsive())
            .then(title => {
                assert.equal(title, 'Александр Шлейко', 'Автор цели не совпадает с данными формы');
            });
    });

    it('Форма создает цель по всем данным формы', function() {
        return this.browser
            // выбираем "Без срока"
            .yaSelectChooseItem(PO.goalEditForm.deadline(), 'Без срока')

            .click(PO.goalEditForm.titleFormField.control())
            .yaKeyPress('Goal dusty')

            .yaSuggestChooseItem(
                PO.goalEditForm.responsibleFormField(),
                PO.goalEditForm.responsibleFormField.suggest(),
                'dusty',
                'Александр Шлейко (@dusty)',
            )

            // выбираем "личная"
            .yaSelectChooseItem(PO.goalEditForm.importanceInput(), 'Личная')

            .yaSuggestChooseItem(
                PO.goalEditForm.customersFormField(),
                PO.goalEditForm.customersFormField.suggest(),
                'a-lexx',
                'Александр Фурсенко',
            )

            .yaSuggestChooseItem(
                PO.goalEditForm.implementersFormField(),
                PO.goalEditForm.implementersFormField.suggest(),
                'poalrom',
                'Алексей Попков',
            )

            .click(PO.goalEditForm.body.AddParentButton())
            .yaSuggestChooseItem(
                PO.goalEditForm.parentFormField.control(),
                PO.goalEditForm.parentFormField.suggest(),
                'omg',
                'omg (57210)',
            )

            .click(PO.goalEditForm.descriptionInput())
            .yaKeyPress('Description goal dusty')

            .yaSuggestChooseItem(
                PO.goalEditForm.tagsFormField(),
                PO.goalEditForm.tagsFormField.suggest(),
                'test-goal',
                'test-goal',
            )

            .execute(function(selector) {
                document.querySelector(selector).closest('.cards__ii').scroll(0, -1000);
            }, PO.goalEditForm())
            .assertView('filled', PO.goalEditForm())

            .click(PO.goalEditForm.submitButton())

            .waitForVisible(PO.goal())
            .assertView('with-success-toast', PO.layout())
            .getText(PO.goal.info.title())
            .then(title => {
                assert.equal(title, 'Goal dusty', 'Название цели не совпадает с данными формы');
            })
            .getText(PO.goal.info.details.responsive())
            .then(title => {
                assert.equal(title, 'Александр Шлейко', 'Автор цели не совпадает с данными формы');
            })
            .getText(PO.goal.info.breadcrumbs())
            .then(breadcrumbs => {
                assert.equal(
                    breadcrumbs,
                    '[I4I] [Dispenser] Выдача ресурсов по заявкам на оборудование / omg / ',
                    'Родительская цель не соотвествует выбранной',
                );
            })
            .getText(PO.goal.info.tags())
            .then(tags => {
                assert.equal(
                    tags,
                    'test-goal',
                    'Теги не соответсвуют данным формы',
                );
            })
            .yaShouldBeVisible(PO.goal.info.importancePrivate())
            .getText(PO.goal.info.statusNew())
            .then(status => {
                assert.equal(
                    status,
                    'Новая',
                    'Статус должен быть "Новая"',
                );
            })
            .getText(PO.goal.info.deadline())
            .then(status => {
                assert.equal(
                    status,
                    'Без срока',
                    'Срок не соответсвует данным формы',
                );
            })
            .getAttribute(PO.goal.info.details.customer(), 'title')
            .then(title => {
                assert.equal(title, 'Александр Фурсенко', 'Заказчик цели не совпадает с данными формы');
            })
            .getAttribute(PO.goal.info.details.implementers(), 'title')
            .then(title => {
                assert.equal(title, 'Алексей Попков', 'В главных ролях цели не совпадает с данными формы');
            })
            .getText(PO.goal.info.details.description())
            .then(description => {
                assert.equal(description, 'Description goal dusty', 'Описание цели не совпадает с данными формы');
            });
    });
});
