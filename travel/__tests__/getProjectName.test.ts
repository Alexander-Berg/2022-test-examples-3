import getProjectName from '../getProjectName';

describe('getProjectName', () => {
    test.each([
        ['/', 'index'],
        ['/avia', 'avia'],
        ['/avia/search/result/', 'avia'],
        ['/trains', 'trains'],
        ['/trains/moscow--saint-petersburg/?when=2020-05-07', 'trains'],
        ['/hotels', 'hotels'],
        ['/hotels/search/?geoId=213', 'hotels'],
        ['/hotel', 'hotel'],
        ['/hotels/hotel', 'hotels'],
        ['/my', 'account'],
        ['/my/passengers', 'account'],
        ['/404', 'notFound'],
        ['/wrongPath', 'index'],
    ])(
        'for url: %s should return project name: %s',
        (url, expectedProjectName) => {
            expect(getProjectName(url)).toBe(expectedProjectName);
        },
    );
});
