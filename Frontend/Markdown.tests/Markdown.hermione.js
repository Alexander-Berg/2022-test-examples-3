describe('edu-components_Markdown_tables', () => {
  const page = 'markdown';

  it('should display a plain table', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(2)']);
  });

  it('should display a headerless table', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(4)']);
  });

  it('should display a borderless table', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(6)']);
  });

  it('should display a table with the merged first and second row', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(8)']);
  });

  it('should display a table with the merged third and forth column', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(10)']);
  });

  it('should display a table with the merged first and second row, and third and forth column', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(12)']);
  });

  it('should display a table with the defined width of 250px', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(14)']);
  });

  it('should display a table with the defined width of 250px, and first col with width 10 percents', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(16)']);
  });

  it('should display a nptable', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', page, 'tables')
      .assertView('plain', ['.Markdown-Table:nth-of-type(18)']);
  });
});
