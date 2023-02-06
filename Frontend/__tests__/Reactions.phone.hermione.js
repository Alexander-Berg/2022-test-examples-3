describe('Реакции (news, phone)', () => {
  function yaPhoneReactions(id) {
    return this
      .openComponent('reactions-news', id)
      .yaAssertOuterView('reactions-' + id, '.mg-reactions', {
        paddings: 5,
      });
  }

  beforeEach(function() {
    this.browser.yaPhoneReactions || this.browser.addCommand('yaPhoneReactions', yaPhoneReactions);
  });

  it('должны отображаться корректно (default)', function() {
    return this.browser.yaPhoneReactions('default');
  });

  it('должны отображаться корректно (with counts)', function() {
    return this.browser.yaPhoneReactions('with-counts');
  });

  it('должны отображаться корректно (with reaction)', function() {
    return this.browser.yaPhoneReactions('with-reaction');
  });

  it('должны отображаться корректно (with reaction and counts)', function() {
    return this.browser.yaPhoneReactions('with-reaction-and-counts');
  });

  it('при клике открывается попап', function() {
    return this.browser
      .openComponent('reactions-news', 'default')
      .click('button.mg-reactions__preview')
      .yaAssertOuterView('reactions-default-popup', '.mg-reactions', {
        paddings: [80, 300, 5, 20],
        allowViewportOverflow: true,
      });
  });

  it('при клике на оверлейную область, попап закрывается', function() {
    return this.browser
      .openComponent('reactions-news', 'default')
      .click('button.mg-reactions__preview')
      .yaAssertOuterView('reactions-popup-with-paranja', '.mg-reactions', {
        paddings: [80, 300, 5, 20],
      })
      .yaShouldExist('.mg-reactions__paranja')
      .click('.mg-reactions__paranja')
      .yaAssertOuterView('reactions-popup-after-close', '.mg-reactions', {
        paddings: [80, 300, 5, 20],
      });
  });

  it('при клике на смайлик, он выбирается, а попап закрывается (with-counts)', function() {
    return this.browser
      .openComponent('reactions-news', 'with-counts')
      .yaMockFetch({
        delay: 300,
        urlDataMap: {
          '\/reactions.*': {
            counts: {},
            reaction: 'Haha',
          },
        },
      })
      .click('button.mg-reactions__preview')
      .yaShouldExist('button.mg-reactions__button-icon .mg-reactions__icon_reaction_Haha')
      .click('button.mg-reactions__button-icon .mg-reactions__icon_reaction_Haha')
      .yaWaitForHidden('.mg-reactions_progress', 5000)
      .yaShouldExist('.mg-reactions__icon_user.mg-reactions__icon_reaction_Haha')
      .execute(() => document.querySelectorAll('.mg-reactions__reactions .mg-reactions__icon'))
      .then(({ value: elements }) => {
        assert.equal(elements.length, 4, 'Неверное количество смайликов');
      })
      .yaAssertOuterView('reactions-with-selected-reaction', '.mg-reactions', {
        paddings: 5,
      });
  });

  it('при клике на смайлик, он выбирается (default)', function() {
    return this.browser
      .openComponent('reactions-news', 'default')
      .yaMockFetch({
        delay: 300,
        urlDataMap: {
          '\/reactions.*': {
            counts: {},
            reaction: 'Haha',
          },
        },
      })
      .yaShouldExist('.mg-reactions__icon_empty')
      .yaShouldNotExist('.mg-reactions__icon_user')
      .click('button.mg-reactions__preview')
      .yaShouldExist('button.mg-reactions__button-icon .mg-reactions__icon_reaction_Haha')
      .click('button.mg-reactions__button-icon .mg-reactions__icon_reaction_Haha')
      .yaWaitForHidden('.mg-reactions_progress', 5000)
      .yaShouldExist('.mg-reactions__icon_user')
      .yaShouldNotExist('.mg-reactions__icon_empty')
      .execute(() => document.querySelectorAll('.mg-reactions__reactions .mg-reactions__icon'))
      .then(({ value: elements }) => {
        assert.equal(elements.length, 1, 'Неверное количество смайликов');
      })
      .yaAssertOuterView('reactions-with-single-selected-reaction', '.mg-reactions', {
        paddings: 5,
      });
  });

  it('при клике снимается выбранный смайлик', function() {
    return this.browser
      .openComponent('reactions-news', 'with-reaction')
      .yaMockFetch({
        delay: 300,
        urlDataMap: {
          '\/reactions.*': {
            counts: {},
          },
        },
      })
      .yaShouldExist('div.mg-reactions__icon_reaction_Like')
      .click('button.mg-reactions__preview')
      .yaWaitForHidden('.mg-reactions_progress', 5000)
      .yaShouldNotExist('div.mg-reactions__icon_reaction_Like')
      .yaShouldBeVisible('.mg-reactions__icon_empty')
      .yaAssertOuterView('reactions-with-empty', '.mg-reactions', {
        paddings: 5,
      });
  });

  it('при клике предлагает авторизоваться неавторизованному пользователю', function() {
    return this.browser
      .openComponent('reactions-news', 'unauth')
      .click('button.mg-reactions__preview')
      .yaAssertOuterView('reactions-login-popup', '.mg-login-popup', {
        allowViewportOverflow: true,
      });
  });
});
