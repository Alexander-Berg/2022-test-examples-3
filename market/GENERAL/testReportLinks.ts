/* global it,expect,describe */

const testValidation = (getLink: $Star, getValidParams: $Star) => {
    // eslint-disable-next-line flowtype/require-return-type
    it('отрабатывает позитивный сценарий', () => {
        const params = getValidParams();

        return Promise.resolve(getLink(params)).then(link => expect(link).toMatchSnapshot());
    });
};

const testRequiredParams = (getLink: $Star, getValidParams: $Star) => {
    describe('не работает без обязательного поля', () => {
        // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
        const runTest = fieldName => {
            it(fieldName, () => {
                const params = getValidParams();
                delete params[fieldName];

                expect(() => getLink(params)).toThrow();
            });
        };

        Object.keys(getValidParams())
            .filter(key => ['campaignId', 'userId', 'dateFrom', 'dateTo'].includes(key))
            .forEach(runTest);
    });
};

const testIncorrectDates = (getLink: $Star, getValidParams: $Star) => {
    it('не даёт себя обмануть некорректными датами', () => {
        const params = getValidParams();
        if (!Object.keys(params).includes('dateFrom')) {
            return;
        }

        params.dateFrom = true;

        expect(() => getLink(params)).toThrow();
    });
};

export default (getLink: $Star, getValidParams: $Star) => {
    testValidation(getLink, getValidParams);
    testRequiredParams(getLink, getValidParams);
    testIncorrectDates(getLink, getValidParams);
};
