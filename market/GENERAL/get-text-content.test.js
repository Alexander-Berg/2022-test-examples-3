import getTextContent from './get-text-content';

/**
 * Creates HTML element
 * @param {string} tag - tagname
 * @param {string} className
 * @param {string} textContent
 * @param {string} src
 * @param {string} alt
 * @returns {Element}
 */
function createHTMLElement(tag, className, textContent, src, alt) {
    const el = document.createElement(tag);
    if (className) {
        el.className = className;
    }
    if (textContent) {
        el.textContent = textContent;
    }
    if (src) {
        el.src = src;
    }
    if (alt) {
        el.alt = alt;
    }
    return el;
}

const testCases = [
    {
        inputElem: createHTMLElement('h1', '_718dda', 'Смартфон Apple iPhone 11 Pro Max 4/256GB, серый космос'),
        inputAlt: false,
        output: 'Смартфон Apple iPhone 11 Pro Max 4/256GB, серый космос',
    },
    {
        inputElem: createHTMLElement('span', undefined, 'Apple'),
        inputAlt: false,
        output: 'Apple',
    },
];

describe('getTextContent', () => {
    testCases.forEach((tc) => {
        const { inputElem, inputAlt, output } = tc;
        const textContent = getTextContent(inputElem, inputAlt);

        test(`"${textContent}" => "${output}"`, () => {
            expect(textContent).toBe(output);
        });
    });
});
