describe('edu-components_Accordion', () => {
  it('should-have-flat-item-structure', function() {
    return this.browser
      .openComponent('edu-components', 'accordion', 'flat')
      .setViewportSize({ width: 800, height: 600 })
      .assertView('plain', ['.Gemini'])
      .moveToObject('.Accordion-Item:nth-child(2)')
      .assertView('hovered', ['.Gemini'])
      .click('.Accordion-Item:nth-child(2)')
      .assertView('open', ['.Gemini']);
  });

  it('should-have-grouped-item-structure', function() {
    return this.browser
      .openComponent('edu-components', 'accordion', 'grouped')
      .setViewportSize({ width: 800, height: 600 })
      .assertView('plain', ['.Gemini']);
  });

  it('should-allow-opening-a-single-item-by-default', function() {
    return this.browser
      .openComponent('edu-components', 'accordion', 'flat')
      .setViewportSize({ width: 800, height: 600 })
      .click('.Accordion-Item:nth-child(2)')
      .click('.Accordion-Item:nth-child(4)')
      .assertView('single', ['.Gemini']);
  });

  it('should-allow-opening-multiple-items', function() {
    return this.browser
      .openComponent('edu-components', 'accordion', 'multi')
      .setViewportSize({ width: 800, height: 600 })
      .click('.Accordion-Item:nth-child(2)')
      .click('.Accordion-Item:nth-child(4)')
      .assertView('multi', ['.Gemini']);
  });
});
