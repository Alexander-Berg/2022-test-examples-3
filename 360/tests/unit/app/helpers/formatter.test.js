import formatter from '../../../../app/helpers/formatter';
import getFixture from '../../../fixtures';

describe('app/helpers/formatter', () => {
    const originalDate = Date.now;
    beforeEach(() => {
        Date.now = () => 1537190300000;
    });
    afterEach(() => {
        Date.now = originalDate;
    });
    it('formatResource для папки', () => {
        const publicInfoFixture = getFixture({
            type: 'public_info',
            params: { hash: '/+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=' }
        });
        expect(
            formatter.formatResource(publicInfoFixture.resource)
        ).toMatchSnapshot();
    });

    it('formatResource для ресурса', () => {
        const publicInfoFixture = getFixture({
            type: 'public_info',
            params: { hash: 'Pc+MIothz8JeA0GSIWCiaTEkpSklOGg2AA4YZsoYCo=' }
        });

        expect(
            formatter.formatResource(
                publicInfoFixture.resource,
                {
                    user: publicInfoFixture.user,
                    hash: 'Pc+MIothz8JeA0GSIWCiaTEkpSklOGg2AA4YZsoYCo='
                }
            )
        ).toMatchSnapshot();
    });

    it('formatResource для ресурса, c размером картинки', () => {
        const publicInfoFixture = getFixture({
            type: 'public_info',
            params: { hash: 'Pc+MIothz8JeA0GSIWCiaTEkpSklOGg2AA4YZsoYCo=' }
        });

        expect(
            formatter.formatResource(
                publicInfoFixture.resource,
                {
                    user: publicInfoFixture.user,
                    hash: 'Pc+MIothz8JeA0GSIWCiaTEkpSklOGg2AA4YZsoYCo=',
                    imageSize: 'xxl'
                }
            )
        ).toMatchSnapshot();
    });

    it('formatResource для АнтиФО ресурса', () => {
        const publicInfoFixture = getFixture({
            type: 'public_info',
            params: { hash: 'antiFS-AYfvfgSPPj4ZKKlFc8w8sefDFfG+TFEwcbmfzqfoLTo=' }
        });

        const formatted = formatter.formatResource(
            publicInfoFixture.resource,
            {
                user: publicInfoFixture.user,
                hash: 'AYfvfgSPPj4ZKKlFc8w8sefDFfG+TFEwcbmfzqfoLTo=='
            }
        );
        expect(formatted.meta.antiFileSharing).toEqual(true);
        expect(formatted.meta.blockings).toBeUndefined();
        expect(formatted).toMatchSnapshot();
    });

    it('formatResource для вируса', () => {
        const publicInfoFixture = getFixture({
            type: 'public_info',
            params: { hash: 'virus-AYfvfgSPPj4ZKKlFc8w8sefDFfG+TFEwcbmfzqfoLTo=' }
        });

        const formatted = formatter.formatResource(
            publicInfoFixture.resource,
            {
                user: publicInfoFixture.user,
                hash: 'AYfvfgSPPj4ZKKlFc8w8sefDFfG+TFEwcbmfzqfoLTo=='
            }
        );
        expect(formatted.virus).toEqual(true);
        expect(formatted.meta.drweb).toEqual(2);
        expect(formatted).toMatchSnapshot();
    });

    it('formatUser', () => {
        expect(formatter.formatUser(
            getFixture({
                type: 'public_info',
                params: { hash: 'Pc+MIothz8JeA0GSIWCiaTEkpSklOGg2AA4YZsoYCo=' }
            }).user
        )).toEqual({
            displayName: 'kri0-gen',
            uid: '4004250101',
            paid: 0
        });

        expect(formatter.formatUser({})).toEqual({});
    });

    it('formatPublicInfo', () => {
        const publicInfoFixture = getFixture({
            type: 'public_info',
            params: { hash: '/+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=' }
        });
        expect(
            formatter.formatPublicInfo(publicInfoFixture)
        ).toMatchSnapshot();
    });

    it('formatResourcesList', () => {
        const publicListFixture = getFixture({
            type: 'public_list',
            params: { hash: '+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=', offset: 80 }
        });
        expect(
            formatter.formatResourcesList(publicListFixture)
        ).toMatchSnapshot();
    });
});
