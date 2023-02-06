/* eslint-disable */
import test from 'ava';
import { addImports, formatGrammarSourceText } from '../../../../services/granet';

test('addImports should not change source without impoerts', t => {
    const imports: string[] = [];
    const source = `root:
    включи свет
`;

    const expected = `root:
    включи свет
`;

    t.is(addImports(source, imports), expected);
});

test('addImports should add import', t => {
    const imports: string[] = ['test.grnt'];
    const source = `root:
    включи свет
`;

    const expected = `import: test.grnt
root:
    включи свет
`;

    t.is(addImports(source, imports), expected);
});

test('addImports should add imports', t => {
    const imports: string[] = ['test1.grnt', 'test2.grnt'];
    const source = `root:
    включи свет
`;

    const expected = `import: test1.grnt
import: test2.grnt
root:
    включи свет
`;

    t.is(addImports(source, imports), expected);
});

test('formatGrammarSourceText formats grammar', t => {
    const formName = 'paskills.test_form';
    const source = `root:
    включи свет
`;
    const expected = {
        grammar: `form cb4b7e98-7fa1-40a8-948b-f76a68f0d4cc.paskills.test_form:
    root:
        включи свет
`,
        prefixLines: 1,
        indentColumns: 4,
    };

    t.deepEqual(formatGrammarSourceText('cb4b7e98-7fa1-40a8-948b-f76a68f0d4cc', formName, source), expected);
});

test('formatGrammarSourceText formats grammar without imports', t => {
    const imports: string[] = [];
    const formName = 'paskills.test_form';
    const source = `root:
    включи свет
`;
    const expected = {
        grammar: `form cb4b7e98-7fa1-40a8-948b-f76a68f0d4cc.paskills.test_form:
    root:
        включи свет
`,
        prefixLines: 1 + imports.length,
        indentColumns: 4,
    };

    t.deepEqual(formatGrammarSourceText('cb4b7e98-7fa1-40a8-948b-f76a68f0d4cc', formName, source, imports), expected);
});

test('formatGrammarSourceText formats grammar with one import', t => {
    const imports = ['test.grnt'];
    const formName = 'paskills.test_form';
    const source = `root:
    включи свет
`;
    const expected = {
        grammar: `import: test.grnt
form cb4b7e98-7fa1-40a8-948b-f76a68f0d4cc.paskills.test_form:
    root:
        включи свет
`,
        prefixLines: 1 + imports.length,
        indentColumns: 4,
    };

    t.deepEqual(formatGrammarSourceText('cb4b7e98-7fa1-40a8-948b-f76a68f0d4cc', formName, source, imports), expected);
});

test('formatGrammarSourceText formats grammar with several imports', t => {
    const imports = ['test1.grnt', 'test2.grnt'];
    const formName = 'paskills.test_form';
    const source = `root:
    включи свет
`;
    const expected = {
        grammar: `import: test1.grnt
import: test2.grnt
form cb4b7e98-7fa1-40a8-948b-f76a68f0d4cc.paskills.test_form:
    root:
        включи свет
`,
        prefixLines: 1 + imports.length,
        indentColumns: 4,
    };

    t.deepEqual(formatGrammarSourceText('cb4b7e98-7fa1-40a8-948b-f76a68f0d4cc', formName, source, imports), expected);
});
