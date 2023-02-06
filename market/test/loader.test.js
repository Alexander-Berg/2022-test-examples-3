const compiler = require('./compiler');

test('empty case', async () => {
    const output = await compiler('empty');

    expect(output).toContain('someHandler');
    expect(output).toContain('"empty.js"');
    expect(output).toContain('[]');
    expect(output).toMatchSnapshot();
});

test('simple case', async () => {
    const output = await compiler('simple.js');

    expect(output).toContain('someHandler');
    expect(output).toContain('"simple.js"');
    expect(output).toContain('"b"');
    expect(output).not.toContain('"a"');
    expect(output).not.toContain('"c"');
    expect(output).toMatchSnapshot();
});

test('bulk queue', async () => {
    const output = await compiler('bulk-queue.js');

    expect(output).toContain('someHandler');
    expect(output).toContain('"bulk-queue.js"');
    expect(output).toContain('"e"');
    expect(output).toMatchSnapshot();
});

test('runtime options case', async () => {
    const output = await compiler('simple.js', {runtimeOptions: {headers: {foo: 'bar'}}});

    expect(output).toContain('someHandler');
    expect(output).toContain('"simple.js"');
    expect(output).toContain('"b"');
    expect(output).toContain('{"foo":"bar"}');
    expect(output).not.toContain('"a"');
    expect(output).not.toContain('"c"');
    expect(output).toMatchSnapshot();
});

test('typescript case', async () => {
    const output = await compiler('typescript.ts');

    expect(output).toContain('someHandler');
    expect(output).toContain('"typescript.js"');
    expect(output).toContain('"b"');
    expect(output).not.toContain('"a"');
    expect(output).not.toContain('"c"');
    expect(output).toMatchSnapshot();
});
