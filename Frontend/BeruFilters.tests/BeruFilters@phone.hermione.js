specs({
    feature: 'beruFilters',
}, () => {
    const layout = PO.blocks.beruFiltersLayout;
    const filters = PO.blocks.beruFilters;

    function testCounter(that, expectedNumber) {
        return that.browser
            .getText(layout.filterCounter())
            .then(text => assert.equal(text, `Выбрано: ${expectedNumber}`, `Текст счетчика должен быть "Выбрано: ${expectedNumber}"`));
    }

    function shouldVisibleResetButton(that) {
        return that.browser
            .isVisible(layout.resetButton())
            .then(exists => assert.isTrue(exists, 'Кнопка "Сбросить" должна отображаться"'));
    }

    function shouldVisibleBackButton(that) {
        return that.browser.isVisible(layout.backButton())
            .then(visible => assert.isTrue(visible, 'Кнопка "Назад"(←) должна отображаться'));
    }

    function shouldVisibleApplyButton(that) {
        return that.browser
            .isVisible(layout.applyButton())
            .then(visible => assert.isTrue(visible, 'Кнопка "Готово" должна быть видима'));
    }

    function shouldVisibleCloseButton(that) {
        return that.browser
            .isVisible(layout.closeButton())
            .then(visible => assert.isTrue(visible, 'Кнопка "Закрыть"(✕) должна отображаться'));
    }

    function shouldNotVisibleResetButton(that) {
        return that.browser
            .isExisting(layout.resetButton())
            .then(exists => assert.isNotTrue(exists, 'Кнопка "Cбросить" не должна отображаться'));
    }

    function shouldNotVisibleCloseButton(that) {
        return that.browser
            .isExisting(layout.closeButton())
            .then(visible => assert.isNotTrue(visible, 'Кнопка "Закрыть"(✕) не должна отображаться'));
    }

    function shouldNotVisibleApplyButton(that) {
        return that.browser
            .isExisting(layout.applyButton())
            .then(visible => assert.isNotTrue(visible, 'Кнопка "Готово" не должна отображаться'));
    }

    function shouldNotVisibleCounter(that) {
        return that.browser
            .isExisting(layout.filterCounter())
            .then(visible => assert.isNotTrue(visible, 'Счетчик не должен отображаться'));
    }

    function shouldNotVisibleBackButton(that) {
        return that.browser
            .isExisting(layout.backButton())
            .then(visible => assert.isNotTrue(visible, 'Кнопка "Назад"(←) не должна отображаться'));
    }

    function editModeShouldBeClosed(that, filterSelector) {
        return that.browser
            .isExisting(filterSelector)
            .then(exists => assert.isNotTrue(exists, 'Должен отображаться список фильтров'));
    }

    function checkMainControlsInEditState(that) {
        return Promise.resolve(shouldVisibleBackButton(that))
            .then(() => testCounter(that, 0))
            .then(() => shouldNotVisibleResetButton(that))
            .then(() => shouldNotVisibleCloseButton(that))
            .then(() => shouldNotVisibleApplyButton(that))
            .then(() => shouldVisibleBackButton(that));
    }

    describe('Range фильтр', function() {
        const rangeFilter = PO.blocks.beruRangeFilter;

        beforeEach(function() {
            return this.browser.url('/turbo?stub=berufilters/default.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .click(filters.rangeFilter())
                .then(() => checkMainControlsInEditState(this));
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра по умолчанию', function() {
            return this.browser.assertView('range-filter', PO.page());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра с заполненными значениями', function() {
            return this.browser
                .setValue(rangeFilter.firstInput(), '4')
                .setValue(rangeFilter.secondInput(), '5')
                .assertView('range-filter-filled', PO.page());
        });

        hermione.only.notIn('safari13');
        it('При изменении значений фильтра, счетчик изменений должен показывать кол-во измененных значений', function() {
            return this.browser
                .setValue(rangeFilter.firstInput(), '4')
                .then(() => testCounter(this, 1))
                .setValue(rangeFilter.secondInput(), '5')
                .then(() => testCounter(this, 2))
                .click(rangeFilter.firstInput())
                .keys('Backspace')
                .then(() => testCounter(this, 1))
                .click(rangeFilter.secondInput())
                .keys('Backspace')
                .then(() => testCounter(this, 0));
        });

        hermione.only.notIn('safari13');
        it('Кнопка "Cбросить" должна отображаться если значение фильтра отличается от дефолтного, в противном случае скрываться', function() {
            return this.browser
                .setValue(rangeFilter.firstInput(), '4')
                .then(() => shouldVisibleResetButton(this))
                .keys('Backspace')
                .then(() => shouldNotVisibleResetButton(this));
        });

        hermione.only.notIn('safari13');
        it('Нажатие по кнопке "Cбросить" должно сбрасывать значение фильтра до дефолтного и скрывать саму кнопку', function() {
            return this.browser
                .setValue(rangeFilter.firstInput(), '4')
                .setValue(rangeFilter.firstInput(), '5')
                .click(layout.resetButton())
                .then(() => Promise.all([
                    this.browser.getValue(rangeFilter.firstInput()),
                    this.browser.getValue(rangeFilter.secondInput()),
                ]))
                .then(([value1, value2]) => {
                    assert.equal(value1, '', 'Значение первого поля должно быть пустым');
                    assert.equal(value2, '', 'Значение второго поля должно быть пустым');
                })
                .then(() => shouldNotVisibleResetButton(this));
        });

        hermione.only.notIn('safari13');
        it('Кнопка "Готово" должна всегда отображаться, если значение фильтра изменилось', function() {
            return this.browser
                .setValue(rangeFilter.firstInput(), '4')
                .then(() => shouldVisibleApplyButton(this))
                .click(layout.resetButton())
                .then(() => shouldVisibleApplyButton(this));
        });

        hermione.only.notIn('safari13');
        it('Нажатие по кнокпе "Готово", должно применять значения фильтра и возвращать к списку фильтров', function() {
            return this.browser
                .setValue(rangeFilter.firstInput(), '4')
                .click(layout.applyButton())
                .then(() => editModeShouldBeClosed(this, rangeFilter()))
                .getText(filters.rangeFilter.acceptedValue())
                .then(value => assert.isOk(value, 'Значение фильтра должно быть задано'));
        });

        hermione.only.notIn('safari13');
        it('Нажатие по кнопке "Назад"(←), должно возвращать к списку фильтров и сбрасывать не примененные изменения', function() {
            return this.browser
                .setValue(rangeFilter.firstInput(), '4')
                .click(layout.backButton())
                .then(() => editModeShouldBeClosed(this, rangeFilter()))
                .getText(filters.rangeFilter.acceptedValue())
                .then(value => assert.notOk(value, 'Значение фильтра должно быть не задано'))
                .click(filters.rangeFilter())
                .getValue(rangeFilter.firstInput())
                .then(value => assert.equal(value, '', 'Значение первого поля должно быть пустым'));
        });
    });

    describe('Enum фильтр', function() {
        const enumFilter = PO.blocks.beruEnumFilter;

        beforeEach(function() {
            return this.browser.url('/turbo?stub=berufilters/default.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .click(filters.enumFilter())
                .then(() => checkMainControlsInEditState(this));
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра по умолчанию', function() {
            return this.browser.assertView('enum-filter', PO.page());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра с выбранными значениями', function() {
            return this.browser
                .click(enumFilter.firstCheckbox())
                .click(enumFilter.secondCheckbox())
                .assertView('enum-filter-filled', PO.page());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра в раскрытом состоянии', function() {
            return this.browser
                .click(enumFilter.expandButton())
                .assertView('enum-filter-expanded', PO.page())
                .isExisting(enumFilter.expandButton())
                .then(exists => assert.isNotTrue(exists, 'Кнопка "Еще" не должна отображаться'));
        });

        hermione.only.notIn('safari13');
        it('При изменении значений фильтра, счетчик изменений должен показывать кол-во измененных значений', function() {
            return this.browser
                .click(enumFilter.firstCheckbox())
                .then(() => testCounter(this, 1))
                .click(enumFilter.secondCheckbox())
                .then(() => testCounter(this, 2))
                .click(enumFilter.firstCheckbox())
                .then(() => testCounter(this, 1))
                .click(enumFilter.secondCheckbox())
                .then(() => testCounter(this, 0));
        });

        hermione.only.notIn('safari13');
        it('Кнопка "Cбросить" должна отображаться, если есть выбранные значение, в противном случае скрываться', function() {
            return this.browser
                .click(enumFilter.firstCheckbox())
                .then(() => shouldVisibleResetButton(this))
                .click(enumFilter.firstCheckbox())
                .then(() => shouldNotVisibleResetButton(this));
        });

        hermione.only.notIn('safari13');
        it('Нажатие по кнопке "Cбросить" должно сбрасывать все выбранные значения и скрывать саму кнопку', function() {
            return this.browser
                .click(enumFilter.firstCheckbox())
                .click(enumFilter.secondCheckbox())
                .click(layout.resetButton())
                .then(() => Promise.all([
                    this.browser.getAttribute(enumFilter.firstCheckbox(), 'class'),
                    this.browser.getAttribute(enumFilter.secondCheckbox(), 'class'),
                ]))
                .then(([firstAttr, secondAttr]) => {
                    firstAttr.split(' ').forEach(name => assert.notEqual(name, 'beru-checkbox_checked', 'Первый элемент должен быть не выбран'));
                    secondAttr.split(' ').forEach(name => assert.notEqual(name, 'beru-checkbox_checked', 'Второй элемент должен быть не выбран'));
                })
                .then(() => shouldNotVisibleResetButton(this));
        });

        hermione.only.notIn('safari13');
        it('Кнопка "Готово" должна всегда отображаться, если значение фильтра изменилось', function() {
            return this.browser
                .click(enumFilter.firstCheckbox())
                .then(() => shouldVisibleApplyButton(this))
                .click(enumFilter.firstCheckbox())
                .then(() => shouldVisibleApplyButton(this));
        });

        hermione.only.notIn('safari13');
        it('Нажатие по кнокпе "Готово", должно применять значения фильтра и возвращать к списку фильтров', function() {
            return this.browser
                .click(enumFilter.firstCheckbox())
                .click(layout.applyButton())
                .then(() => editModeShouldBeClosed(this, enumFilter()))
                .getText(filters.enumFilter.acceptedValue())
                .then(value => assert.isOk(value, 'Значение фильтра должно быть задано'));
        });

        hermione.only.notIn('safari13');
        it('Нажатие по кнопке "Назад"(←), должно возвращать к списку фильтров и сбрасывать не примененные изменения', function() {
            return this.browser
                .click(enumFilter.firstCheckbox())
                .click(layout.backButton())
                .then(() => editModeShouldBeClosed(this, enumFilter()))
                .getText(filters.enumFilter.acceptedValue())
                .then(value => assert.notOk(value, 'Значение фильтра должно быть не задано'));
        });
    });

    describe('Фильтр выбора цвета(enum подобный)', () => {
        const enumFilter = PO.blocks.beruEnumFilter;

        beforeEach(function() {
            return this.browser.url('/turbo?stub=berufilters/default.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .click(filters.colorFilter())
                .then(() => checkMainControlsInEditState(this));
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра по умолчанию', function() {
            return this.browser.assertView('color-filter', PO.page());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра с выбранными значениями', function() {
            return this.browser
                .click(enumFilter.firstCheckbox())
                .click(enumFilter.secondCheckbox())
                .assertView('color-filter-filled', PO.page());
        });
    });

    describe('Radio фильтр', function() {
        const radioFilter = PO.blocks.beruRadioFilter;

        beforeEach(function() {
            return this.browser.url('/turbo?stub=berufilters/default.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .click(filters.radioFilter())
                .then(() => shouldNotVisibleResetButton(this))
                .then(() => shouldNotVisibleCloseButton(this))
                .then(() => shouldNotVisibleApplyButton(this))
                .then(() => shouldNotVisibleCounter(this))
                .then(() => shouldVisibleBackButton(this));
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра по умолчанию', function() {
            return this.browser.assertView('radio-filter', PO.page());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра с выбранными значением отличным от дефолтного', function() {
            return this.browser
                .click(radioFilter.firstRadio())
                .assertView('radio-filter-filled', PO.page());
        });

        hermione.only.notIn('safari13');
        it('При изменении значения фильтра, счетчик и кнопка "Cбросить" не должны отображаться', function() {
            return this.browser
                .click(radioFilter.firstRadio())
                .then(() => shouldNotVisibleResetButton(this))
                .then(() => shouldNotVisibleCounter(this));
        });

        hermione.only.notIn('safari13');
        it('Кнопка "Готово" должна отображаться, если значение фильтра изменилось', function() {
            return this.browser
                .click(radioFilter.firstRadio())
                .then(() => shouldVisibleApplyButton(this));
        });

        hermione.only.notIn('safari13');
        it('Нажатие по кнокпе "Готово", должно применять значение фильтра и возвращать к списку фильтров', function() {
            return this.browser
                .click(radioFilter.firstRadio())
                .click(layout.applyButton())
                .then(() => editModeShouldBeClosed(this, radioFilter()))
                .getText(filters.radioFilter.acceptedValue())
                .then(currentValue => assert.equal(currentValue, 'Нет', 'Выбранное значение должно быть "Нет"'));
        });

        hermione.only.notIn('safari13');
        it('Нажатие по кнопке "Назад"(←), должно возвращать к списку фильтров и сбрасывать не примененное изменение', function() {
            return this.browser
                .click(radioFilter.firstRadio())
                .click(layout.backButton())
                .then(() => editModeShouldBeClosed(this, radioFilter()))
                .getText(filters.radioFilter.acceptedValue())
                .then(value => assert.equal(value, 'Не важно', 'Значение фильтра должно быть  "Не важно"'));
        });
    });

    describe('Boolean фильтр', function() {
        beforeEach(function() {
            return this.browser.url('/turbo?stub=berufilters/default.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        });

        hermione.only.notIn('safari13');
        it('При клике по превью фильтра не происходит переход к редактированию', function() {
            return this.browser
                .click(filters.booleanFilter())
                .getText(layout.title())
                .then(title => assert.equal(title, 'Фильтры', 'Заголовок должен быть "Фильтры"'))
                .then(() => shouldNotVisibleCounter(this));
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра по умолчанию', function() {
            return this.browser.assertView('boolean-filter', filters.booleanFilter());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид фильтра в активном состоянии', function() {
            return this.browser
                .click(filters.booleanFilter.tumbler())
                .assertView('boolean-filter-selected', filters.booleanFilter());
        });
    });

    describe('Превью фильтров', function() {
        beforeEach(function() {
            return this.browser.url('/turbo?stub=berufilters/default.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .then(() => shouldVisibleCloseButton(this))
                .then(() => shouldNotVisibleCounter(this))
                .then(() => shouldNotVisibleResetButton(this))
                .then(() => shouldNotVisibleApplyButton(this))
                .then(() => shouldNotVisibleBackButton(this));
        });

        hermione.only.notIn('safari13');
        it('Внешний вид по умолчанию', function() {
            return this.browser
                .assertView('preview-default', PO.page());
        });

        hermione.only.notIn('safari13');
        it('Кнопка "Закрыть"(✕) должна быть кликабельна и вести на страницу выдачи со всеми пришедшими параметрами', function() {
            return this.browser
                .yaCheckLink({
                    selector: layout.closeButton(),
                    message: 'Неправильная ссылка',
                    target: '_self',
                    url: {
                        href: `/turbo?text=${encodeURIComponent('https://m.beru.ru/catalog/123/list?hid=444&anyParam=1&anyParam=2')}`,
                        ignore: ['protocol', 'hostname'],
                    },
                });
        });
    });

    describe('Превью фильтров(c предвыбранными значениями)', function() {
        beforeEach(function() {
            return this.browser.url('/turbo?stub=berufilters/all-selected.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .then(() => shouldVisibleResetButton(this))
                .then(() => shouldVisibleCloseButton(this))
                .then(() => shouldNotVisibleBackButton(this))
                .then(() => shouldNotVisibleApplyButton(this))
                .then(() => testCounter(this, 6));
        });

        hermione.only.notIn('safari13');
        it('Внешний вид cо всеми выбранными значениями', function() {
            return this.browser
                .assertView('preview-all-selected', PO.page());
        });

        hermione.only.notIn('safari13');
        it('При клике по кнопке "Сбросить", должны сбрасываться все значения фильтров', function() {
            return this.browser
                .click(layout.resetButton())
                .then(() => shouldNotVisibleCounter(this))
                .then(() => shouldNotVisibleResetButton(this))
                .then(() => shouldNotVisibleBackButton(this))
                .then(() => shouldVisibleApplyButton(this))
                .then(() => shouldVisibleCloseButton(this))
                .getText(filters.radioFilter.acceptedValue())
                .then(currentValue => assert.equal(currentValue, 'Не важно', 'Выбранное значение должно быть "Не важно"'))
                .assertView('dropped-values', PO.page());
        });
    });
});
