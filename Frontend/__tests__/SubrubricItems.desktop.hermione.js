const height = 700;

describe('Подрубрики (корректное отображение)', () => {
  const widths = [1920, 1000, 600, 300];

  async function yaComponentTest(id) {
    for (const width of widths) {
      await this
        .setViewportSize({ width, height })
        .openComponent('subrubricitems', id, 'desktop')
        .yaAssertOuterView(`subrubrics-${id}-${width}`, '.news-subrubric-items', { paddings: 5 });
    }
  }

  beforeEach(function() {
    this.browser.yaComponentTest || this.browser.addCommand('yaComponentTest', yaComponentTest);
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должны отображаться корректно (default)', function() {
    return this.browser.yaComponentTest('default');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должны отображаться корректно (with icon)', function() {
    return this.browser.yaComponentTest('with-icon');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должны отображаться корректно (with large list)', function() {
    return this.browser.yaComponentTest('with-large-list');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должны отображаться корректно (with prevent default)', function() {
    return this.browser.yaComponentTest('with-prevent-default');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должны отображаться корректно (with event handler)', function() {
    return this.browser.yaComponentTest('with-event-handler');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должны отображаться корректно (with only one item)', function() {
    return this.browser.yaComponentTest('with-only-one-item');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('при клике на кнопку, активная кнопка меняется', function() {
    return this.browser
      .setViewportSize({ width: 700, height })
      .openComponent('subrubricitems', 'with-prevent-default', 'desktop')
      .yaAssertOuterView('subrubrics-click-switch-before', '.news-subrubric-items', { paddings: 5 })
      .click('.mg-collapsing-items__item:nth-of-type(4) .news-subrubric-items__button')
      .yaAssertOuterView('subrubrics-click-switch-after', '.news-subrubric-items', { paddings: 5 });
  });
});

describe('Подрубрики (открытие попапа)', () => {
  async function yaComponentTestPopup(id, widths = []) {
    for (const width of widths) {
      await this
        .setViewportSize({ width, height })
        .openComponent('subrubricitems', id, 'desktop')
        .yaWaitForVisible('.mg-collapsing-items__item-more')
        .moveToObject('.mg-collapsing-items__item-more button', 10, 10)
        .yaWaitForVisible('.news-subrubric-items__popup', 5000)
        .assertView(`subrubrics-popup-${id}-${width}`, '.news-subrubric-items__popup', {
          allowViewportOverflow: true,
          screenshotDelay: 1000,
        });
    }
  }

  beforeEach(function() {
    this.browser.yaComponentTestPopup || this.browser.addCommand('yaComponentTestPopup', yaComponentTestPopup);
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('попап должен открыться корректно (default)', function() {
    return this.browser.yaComponentTestPopup('default', [600]);
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('попап должен открыться корректно (with icon)', function() {
    return this.browser.yaComponentTestPopup('with-icon', [400]);
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('попап должен открыться корректно (with large list)', function() {
    return this.browser.yaComponentTestPopup('with-large-list', [600, 1000, 1920]);
  });
});
