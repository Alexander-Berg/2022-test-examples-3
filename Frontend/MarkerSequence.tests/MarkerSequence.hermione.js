describe('edu-components_MarkerSequence_Layout', () => {
  const page = 'markersequence-layout';

  it('default', function() {
    return this.browser
      .setViewportSize({ width: 1400, height: 600 })
      .openComponent('edu-components', page, 'default')
      .assertView('LessMin', ['.LessMin .MarkerSequence'])
      .assertView('Normal', ['.Normal .MarkerSequence'])
      .assertView('Max', ['.Max .MarkerSequence'])
      .assertView('Disabled', ['.Disabled .MarkerSequence'])
      .assertView('ReadOnly', ['.Readonly .MarkerSequence']);
  });

  it('horizontal', function() {
    return this.browser
      .setViewportSize({ width: 1400, height: 600 })
      .openComponent('edu-components', page, 'horizontal')
      .assertView('LessMin', ['.LessMin .MarkerSequence'])
      .assertView('Normal', ['.Normal .MarkerSequence'])
      .assertView('Max', ['.Max .MarkerSequence'])
      .assertView('Disabled', ['.Disabled .MarkerSequence'])
      .assertView('ReadOnly', ['.Readonly .MarkerSequence']);
  });

  it('should NOT visually mark disabled element on hover', function() {
    const itemElement = '.Disabled .MarkerSequence .MarkerSequence-Item';

    return this.browser
      .setViewportSize({ width: 1400, height: 600 })
      .openComponent('edu-components', page, 'default')
      .moveToObject(itemElement)
      .assertView('hovered, not selected', ['.Disabled .MarkerSequence'])
      .click(itemElement)
      .assertView('hovered, selected', ['.Disabled .MarkerSequence']);
  });

  it('should NOT visually mark readOnly element on hover', function() {
    const itemElement = '.Readonly .MarkerSequence .MarkerSequence-Item';

    return this.browser
      .setViewportSize({ width: 1400, height: 600 })
      .openComponent('edu-components', page, 'default')
      .moveToObject(itemElement)
      .assertView('hovered, not selected', ['.Readonly .MarkerSequence'])
      .click(itemElement)
      .assertView('hovered, selected', ['.Readonly .MarkerSequence']);
  });
});
