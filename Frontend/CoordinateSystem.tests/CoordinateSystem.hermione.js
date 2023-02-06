describe('edu-components_CoordinateSystem_fn-graphs', () => {
  const page = 'coordinatesystem';

  it('should display correct graph', function() {
    return this.browser
      .setViewportSize({ width: 1366, height: 768 })
      .openComponent('edu-components', page, 'fn-graphs')
      .assertView('linear', ['.Gemini .Linear'])
      .assertView('quadratic', ['.Gemini .Quadratic'])
      .assertView('cubic', ['.Gemini .Cubic'])
      .assertView('hyperbola', ['.Gemini .Hyperbola'])
      .assertView('squareroot', ['.Gemini .SquareRoot'])
      .assertView('tangent', ['.Gemini .Tangent'])
      .assertView('sinusoid', ['.Gemini .Sinusoid'])
      .assertView('radial', ['.Gemini .Radial']);
  });
});

describe('edu-components_CoordinateSystem_points', () => {
  const page = 'coordinatesystem';

  it('should display correct graph', function() {
    return this.browser
      .setViewportSize({ width: 1366, height: 768 })
      .openComponent('edu-components', page, 'points')
      .assertView('plain', ['.Gemini .Points']);
  });
});

describe('edu-components_CoordinateSystem_axes', () => {
  const page = 'coordinatesystem-axes';

  const components = [
    { name: 'two-axes', selector: 'TwoAxes' },
    { name: 'x-axis-with-label', selector: 'XAxisWithLabel' },
    { name: 'x-axis-without-label', selector: 'XAxisWithoutLabel' },
    { name: 'y-axis-with-label', selector: 'YAxisWithLabel' },
  ];

  components.map(({ name, selector }) => {
    return it(`should display correct graph for ${name}`, function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, name)
        .assertView('plain', [`.Gemini .${selector}`]);
    });
  });
});

describe('edu-components_CoordinateSystem_grids', () => {
  const page = 'coordinatesystem-grids';

  const components = [
    { name: 'auto-grid', selector: 'AutoGrid' },
    { name: 'predefined-grid', selector: 'PredefinedGrid' },
  ];

  components.map(({ name, selector }) => {
    return it(`should display correct graph for ${name}`, function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, name)
        .assertView('plain', [`.Gemini .${selector}`]);
    });
  });
});
