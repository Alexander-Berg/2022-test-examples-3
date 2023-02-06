jest.disableAutomock();

import {
    generateCookieName,
    completeExperimentData,
    getOrSetExperimentCookie,
} from '../../experiments';

const cookies = {
    dict: {},
    set(name, value) {
        this.dict[name] = value;
    },
    get(name) {
        return this.dict[name];
    },
    clear() {
        this.dict = {};
    },
};

const experimentKey = 'magicButtons';
const cookieName = generateCookieName(experimentKey);

describe('cookies mock', () => {
    it('Кука устанавливается', () => {
        cookies.set('lolipop', true);
        expect(cookies.get('lolipop')).toBe(true);
        cookies.set('lolipop', false);
        expect(cookies.get('lolipop')).toBe(false);
    });

    it('Кука очищается', () => {
        cookies.set('abra', 'cadabra');
        expect(cookies.get('abra')).toBe('cadabra');
        cookies.clear();
        expect(cookies.get('abra')).toBeUndefined();
    });
});

describe('getOrSetExperimentCookie', () => {
    beforeEach(() => {
        cookies.clear();
    });

    it('Кука не была установлена - генерируем новое значение', () => {
        const experiment = completeExperimentData({
            type: Boolean,
            percentage: 100,
        });
        const experimentValue = getOrSetExperimentCookie(
            experimentKey,
            experiment,
            cookies,
        );
        const cookieValue = cookies.get(cookieName);

        expect(experimentValue).toBe(true);
        expect(cookieValue).toBe('1');
    });

    it('Кука была установлена - возвращаем соответствующее значение', () => {
        cookies.set(cookieName, '');
        const experiment = completeExperimentData({
            type: Boolean,
            percentage: 100,
        });
        const experimentValue = getOrSetExperimentCookie(
            experimentKey,
            experiment,
            cookies,
        );
        const cookieValue = cookies.get(cookieName);

        expect(experimentValue).toBe(false);
        expect(cookieValue).toBe('');
    });

    it('Кука была установлена, но значение куки обновляется при каждом визите - переустанавливаем куку', () => {
        cookies.set(cookieName, '');
        const experiment = completeExperimentData({
            type: Boolean,
            dynamic: true,
            percentage: 100,
        });
        const experimentValue = getOrSetExperimentCookie(
            experimentKey,
            experiment,
            cookies,
        );
        const cookieValue = cookies.get(cookieName);

        expect(experimentValue).toBe(true);
        expect(cookieValue).toBe('1');
    });
});
