import { getCombinedStaticUrl, IGetCombinedStaticUrlParams, StaticServerUrlType } from './index';

describe('test', () => {
    const params: IGetCombinedStaticUrlParams = {
        project: 'project',
        type: StaticServerUrlType.SCRIPT,
        normalizeChunkIds: false,
        manifestUrl: 'manifestUrl',
        chunks: [
            { url: 'url1' },
            { url: 'url2' },
            { url: 'url3' },
        ],
        staticManifest: {
            project: 'project',
            version: 'version',
            publicPath: 'publicPath',
            assets: {
                '1': 'url1',
                '2': 'url2',
                '3': 'url3',
            },
            assetsInverted: {
                url1: '1',
                url2: '2',
                url3: '3',
            },
        },
    };

    it('should return correct static url', () => {
        expect(getCombinedStaticUrl(params)).toBe('//yastatic.net/static-combine-server/project-testing/combine-js/149e8b91a259b0cffb6889a5114b5bca/?manifest=manifestUrl&cid=1&cid=2&cid=3');
    });
});
