describe('Блок из нескольких сниппетов', () => {
  hermione.only.notIn('win-ie11');
  it('с логотипами', function() {
    return this.browser
      .openComponent('snippetsgroup', 'logo', 'desktop')
      .assertView('logo', '.mg-snippets-group__storybook-wrapper');
  });

  hermione.only.notIn('win-ie11');
  it('с маленькими логотипами', function() {
    return this.browser
      .openComponent('snippetsgroup', 'logo-small', 'desktop')
      .assertView('logo-small', '.mg-snippets-group__storybook-wrapper');
  });

  hermione.only.notIn('win-ie11');
  it('с буллетами', function() {
    return this.browser
      .openComponent('snippetsgroup', 'bullet', 'desktop')
      .assertView('bullet', '.mg-snippets-group__storybook-wrapper');
  });

  hermione.only.notIn('win-ie11');
  it('без логотипов и буллетов', function() {
    return this.browser
      .openComponent('snippetsgroup', 'no-logo', 'desktop')
      .assertView('no-logo', '.mg-snippets-group__storybook-wrapper');
  });

  hermione.only.notIn('win-ie11');
  it('в карточном дизайне', function() {
    return this.browser
      .openComponent('snippetsgroup', 'card', 'desktop')
      .assertView('card', '.mg-snippets-group__storybook-wrapper');
  });
});
