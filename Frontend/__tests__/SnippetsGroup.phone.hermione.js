describe('Блок из нескольких сниппетов', () => {
  it('с логотипами', function() {
    return this.browser
      .openComponent('snippetsgroup', 'logo', 'phone')
      .assertView('logo', '.mg-snippets-group__storybook-wrapper');
  });

  it('с маленькими логотипами', function() {
    return this.browser
      .openComponent('snippetsgroup', 'logo-small', 'phone')
      .assertView('logo-small', '.mg-snippets-group__storybook-wrapper');
  });

  it('с буллетами', function() {
    return this.browser
      .openComponent('snippetsgroup', 'bullet', 'phone')
      .assertView('bullet', '.mg-snippets-group__storybook-wrapper');
  });

  it('без логотипов и буллетов', function() {
    return this.browser
      .openComponent('snippetsgroup', 'no-logo', 'phone')
      .assertView('no-logo', '.mg-snippets-group__storybook-wrapper');
  });

  it('в карточном дизайне', function() {
    return this.browser
      .openComponent('snippetsgroup', 'card', 'phone')
      .assertView('card', '.mg-snippets-group__storybook-wrapper');
  });
});
