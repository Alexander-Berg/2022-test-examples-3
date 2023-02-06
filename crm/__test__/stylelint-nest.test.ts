const nestReg = /^@nest\s+([^,&]*&([^,&]*)?)(,[^,&]*&([^,&]*)?)*$|^\s*&([^,&]*)?(,\s*&([^,&]*)?)*$/;

describe('stylelint-nest', () => {
  /* match */
  test('& > .bar - should be match', () => {
    expect('& > .bar').toMatch(nestReg);
  });

  test('&.bar - should be match', () => {
    expect('&.bar').toMatch(nestReg);
  });

  test('& + .baz, &.qux - should be match', () => {
    expect('& + .baz, &.qux').toMatch(nestReg);
  });

  test('@nest & > .bar - should be match', () => {
    expect('@nest & > .bar').toMatch(nestReg);
  });

  test('@nest .parent & - should be match', () => {
    expect('@nest .parent &').toMatch(nestReg);
  });

  test('@nest & + .baz, &.qux - should be match', () => {
    expect('@nest & + .baz, &.qux').toMatch(nestReg);
  });

  /* no match */
  test('.bar - should be no match', () => {
    expect('.bar').not.toMatch(nestReg);
  });

  test('.bar & - should be no match', () => {
    expect('.bar &').not.toMatch(nestReg);
  });

  test('&.bar, .baz - should be no match', () => {
    expect('&.bar, .baz').not.toMatch(nestReg);
  });

  test('@nest .bar - should be no match', () => {
    expect('@nest .bar').not.toMatch(nestReg);
  });

  test('@nest & .bar, .baz - should be no match', () => {
    expect('@nest & .bar, .baz').not.toMatch(nestReg);
  });

  /*
  this case catch by scss/selector-no-union-class-name

  test('&_name - should be no match', () => {
    expect('&_name').not.toMatch(nestReg);
  });

  test('&name - should be no match', () => {
    expect('&name').not.toMatch(nestReg);
  });
  */
});
