const {sampleTest, sampleH2test} = require('../helpers/tests.data');
const {getTestName} = require('../../plugin/helpers/tests');

describe('tests', () => {
    describe('getTestName', () => {
        it('returns valid test name', () => {
            expect(getTestName(sampleTest)).toBe('Морда карточки модели. ' +
                'Карточка модели с параметром sku. Табы. Отзывы. ' +
                'Проверка url на наличие sku в query параметрах. ' +
                'По умолчанию параметр sku находится в URL страницы'
            );
        });

        it('returns valid test name (h2)', () => {
            expect(getTestName(sampleH2test)).toBe(
                'Страница поисковой выдачи. Взаимодействие с фильтрами. Ссылка ' +
                '"Искать везде" и плитки категорий. По поисковому запросу "Мобильные телефоны" На категорийной ' +
                'выдаче ссылка "Искать везде" отображается'
            );
        });

        it('returns empty string on empty object', () => {
            expect(getTestName({})).toBe('');
        });

        it('returns empty string on undefined', () => {
            expect(getTestName(undefined)).toBe('');
        });
    });
});
