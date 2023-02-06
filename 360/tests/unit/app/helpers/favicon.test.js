import { getFaviconString } from '../../../../app/helpers/favicon';

jest.mock('../../../../app/render/dist/static', () => ({
    favicons: (key) => `./<relative-path>/${key}`
}), { virtual: true });

describe('app/helpers/favicon =>', () => {
    const mockedReq = {
        yandexServices: {
            yastatic: 'https://yastatic.net'
        }
    };

    it('should return favicons html', () => {
        expect(getFaviconString(mockedReq)).toMatchSnapshot();
    });
});
