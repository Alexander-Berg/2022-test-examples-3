import { getDefaultMeta } from '../utils';

describe('getDefaultMeta', () => {
    it('должен дефолтные мета описания для seo', () => {
        expect(getDefaultMeta()).toEqual({
            ograph: {
                title: 'Яндекс.Маркет',
                image: '//yastatic.net/market-export/_/i/marketplace/opengraph-image-1024x512.png',
                type: 'website',
            },
            title: 'Яндекс.Маркет',
        });
    });
});
