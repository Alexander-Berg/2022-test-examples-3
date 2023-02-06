import { getMergedAnalyticsParams } from './mergeLpcJsonHelpers';

const headerAnalyticsMock = {
    metrika: {
        ids: [12345],
        params: {
            param: 'header param',
        }
    }
};

const contentAnalyticsMock = {
    metrika: {
        ids: [12345678],
        params: {
            param_2: 'content param',
        }
    }
};

describe('getMergedLpcJSON', () => {
    describe('getMergedAnaliticsParams', () => {
        it('should return empty analytics object, when no analytics params provided', () => {
            expect(getMergedAnalyticsParams()).toEqual({
                metrika: {
                    ids: [],
                    params: {}
                }
            });
        });

        it('should return header analytics params, when only headers analytics params provided', () => {
            expect(getMergedAnalyticsParams(headerAnalyticsMock)).toEqual(headerAnalyticsMock);
        });

        it('should return analytics with header metrika ids and both metrika params of header and content', () => {
            expect(getMergedAnalyticsParams(headerAnalyticsMock, contentAnalyticsMock)).toEqual({
                metrika: {
                    ids: [12345],
                    params: {
                        param: 'header param',
                        param_2: 'content param',
                    }
                }
            });
        });
    });
});
