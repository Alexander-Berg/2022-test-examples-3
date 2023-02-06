import checkFullscreenBanner from '../../../../app/helpers/check-fullscreen-banner';

jest.mock('asker-as-promised');
const mockedAsk = require('asker-as-promised');

const bannerAskerResult = {
    data: JSON.stringify({
        rtb: {
            posterSrc: 'banner-image',
            clickUrl: 'banner-link'
        },
        settings: {
            viewNotices: ['first-view-notice', 'second-view-notice'],
            winNotice: 'win-notice',
            mrcImpressions: ['first-impression', 'second-impression'],
            linkTail: 'link-tail'
        }
    })
};
const expectedBannerResult = {
    src: 'banner-image',
    link: 'banner-link',
    viewNotices: ['first-view-notice', 'second-view-notice'],
    winNotice: 'win-notice',
    mrcImpressionsAndLinkTail: ['first-impression', 'second-impression', 'link-tail']
};

const runCheckFullscreenBanner = (req, resourceType) => {
    req = Object.assign({
        ua: {},
        header: () => null,
        cookies: {}
    }, req);
    return checkFullscreenBanner(req, resourceType);
};

describe('app/helpers/check-fullscreen-banner', () => {
    it('Домен `tr` - не ходим в аскер, возвращаем undefined', (done) => {
        mockedAsk.setMockedResult(bannerAskerResult);
        runCheckFullscreenBanner({
            tld: 'tr'
        }).then((result) => {
            expect(popFnCalls(mockedAsk).length).toEqual(0);
            expect(result).toBeUndefined();
            done();
        });
    });

    it('Домен `ru`, Вернулся баннер', (done) => {
        mockedAsk.setMockedResult(bannerAskerResult);
        runCheckFullscreenBanner({
            tld: 'ru'
        }).then((result) => {
            const askerCalls = popFnCalls(mockedAsk);
            expect(askerCalls.length).toEqual(1);
            expect(result).toEqual(expectedBannerResult);
            done();
        });
    });

    it('Домен `ru`, Вернулось что-то неожиданное', (done) => {
        mockedAsk.setMockedResult({ data: '{}' });
        runCheckFullscreenBanner({
            tld: 'ru'
        }).then((result) => {
            expect(popFnCalls(mockedAsk).length).toEqual(1);
            expect(result).toBeUndefined();
            done();
        });
    });

    it('Домен `ru`, Вернулась ошибка', (done) => {
        mockedAsk.setMockedResult({ error: true });
        runCheckFullscreenBanner({
            tld: 'ru'
        }).then((result) => {
            expect(popFnCalls(mockedAsk).length).toEqual(1);
            expect(result).toBeUndefined();
            done();
        });
    });

    it('Домен `ru`, запрос упал', (done) => {
        mockedAsk.setMockedResult({}, true);
        runCheckFullscreenBanner({
            tld: 'ru'
        }).then((result) => {
            expect(popFnCalls(mockedAsk).length).toEqual(1);
            expect(result).toBeNull();
            done();
        });
    });
});
