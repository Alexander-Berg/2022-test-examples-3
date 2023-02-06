import {getEntryPointContent} from '../../bundling';

describe('getEntryPointContent', () => {
    it('Для пустого объекта со словарями будет выброшено исключение', () => {
        const wrapper = () => getEntryPointContent([], 'ru');

        expect(wrapper).toThrowError('no locales to export');
    });

    it('Для непустого списка языков - вернёт контент с соответствующими экспортами', () => {
        expect(getEntryPointContent(['ru', 'uk'], 'ru')).toBe(
            "import Keyset from '../../interfaces/Keyset';\n\n" +
                'let exportModule: Keyset;\n\n' +
                'if (process.env.BUNDLE_LANGUAGE === "uk") {\n' +
                '    exportModule = require("./uk.js").default;\n' +
                '} else {\n' +
                '    exportModule = require("./ru.js").default;\n' +
                '}\n\n' +
                'export default exportModule;\n',
        );
    });
});
