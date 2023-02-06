import { likeCard, unlikeCard } from '@yandex-turbo/components/withCollectionsApi/withCollectionsApi';

jest.mock('@yandex-turbo/core/ajax', () => ({
    post: jest.fn(url => {
        if (/like/.test(url)) {
            return Promise.resolve({
                readedBody: {
                    id: 83274981,
                    url: `${url}`,
                },
            });
        }

        return Promise.reject();
    }),
    del: jest.fn(url => {
        if (/like/.test(url)) {
            return Promise.resolve({
                readedBody: {
                    id: 83274981,
                    url: `${url}`,
                },
            });
        }

        return Promise.reject();
    }),
    get: jest.fn(url => {
        if (/csrf/.test(url)) {
            return Promise.resolve({
                readedBody: {
                    'csrf-token': '1',
                },
            });
        }

        return Promise.reject();
    }),
}));

describe('Запросы на лайк/дизлайк', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('лайк', () => {
        it('должен возвращаться без ошибок', async() => {
            const data = await likeCard('test-card');
            const resp = {
                id: 83274981,
                url: '/collections/api/likes/?source_name=turbo&ui=touch',
            };
            expect(data).toEqual(resp);
        });
    });

    describe('снятие лайка', () => {
        it('должен возвращаться без ошибок', async() => {
            const data = await unlikeCard('test-card');
            const resp = {
                id: 83274981,
                url: '/collections/api/likes/?source_name=turbo&ui=touch',
            };
            expect(data).toEqual(resp);
        });
    });
});
