import { getAlbumResourceIndexPath } from '../../../components/helpers/personal-albums';

describe('personal albums helper', () => {
    describe('getAlbumResourceIndexPath', () => {
        const clusters = [{
            id: 1,
            size: 2,
            items: [
                { itemId: '5de665f7e4d70e06400bc895', id: '/photounlim/2019-10-22 18-38-34.JPG' },
                { itemId: '5de665f7e4d70e06400bc896', id: '/photounlim/2019-10-22 18-31-03.JPG' }
            ]
        }, {
            id: 1,
            size: 2,
            items: [
                { itemId: '5de665f7e4d70e06400bc897', id: '/photounlim/2019-10-22 18-30-58.JPG' },
                { itemId: '5de665f7e4d70e06400bc898', id: '/photounlim/2019-10-22 18-30-52.JPG' }
            ]
        }];

        it('должен корректно находить ресурс в первом кластере', () => {
            expect(getAlbumResourceIndexPath({ clusters }, { id: '/photounlim/2019-10-22 18-38-34.JPG' }))
                .toEqual({ clusterIndex: 0, resourceIndex: 0 });
        });

        it('должен корректно находить ресурс во втором кластере', () => {
            expect(getAlbumResourceIndexPath({ clusters }, { id: '/photounlim/2019-10-22 18-30-52.JPG' }))
                .toEqual({ clusterIndex: 1, resourceIndex: 1 });
        });

        it('должен корректно находить ресурс по itemId', () => {
            expect(getAlbumResourceIndexPath({ clusters }, { itemId: '5de665f7e4d70e06400bc895' }))
                .toEqual({ clusterIndex: 0, resourceIndex: 0 });
        });

        it('Должен возвращать null если ресурс не найден', () => {
            expect(getAlbumResourceIndexPath({ clusters }, { id: '/disk/1.jpg' })).toBe(null);
        });

        it('Должен возвращать null для пустого списка кластеров', () => {
            expect(getAlbumResourceIndexPath({ clusters: [] }, { id: '/photounlim/2019-10-22 18-30-52.JPG' })).toBe(null);
        });
    });
});
