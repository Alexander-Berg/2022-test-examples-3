const path = require('path');

const testExecute = require('@yandex-levitan/codemods/utils/testExecute');

const transform = require('../b2b.transform');
const transformNext = require('../b2b.next.transform');
const transformParagraph = require('../b2b-paragraph.transform');
const transformHeading = require('../b2b-heading.transform');

describe('Text', () => {
    testExecute(test, expect, path.join(__dirname, 'cases/text'), transform);
});

describe('Text.next', () => {
    testExecute(
        test,
        expect,
        path.join(__dirname, 'cases/text.next'),
        transformNext,
    );
});

describe('Paragraph', () => {
    testExecute(
        test,
        expect,
        path.join(__dirname, 'cases/paragraph'),
        transformParagraph,
    );
});

describe('Heading', () => {
    testExecute(
        test,
        expect,
        path.join(__dirname, 'cases/heading'),
        transformHeading,
    );
});
