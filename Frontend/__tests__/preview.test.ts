import { update, previewReducer } from '../preview';

describe('Preview reducer', () => {
    const NOW = 123;
    global.Date.now = jest.fn(() => NOW);

    describe('#updatePreviewData', () => {
        it('Should return new state with specified url and actual time', () => {
            const url = 'https://yandex.ru';
            const data = { url, preview: { title: 'test' } };
            const expected = {
                [url]: {
                    validUntil: NOW,
                    data,
                },
            };

            const result = previewReducer({}, update(url, NOW, data));
            expect(result).toStrictEqual(expected);
        });
    });
});
