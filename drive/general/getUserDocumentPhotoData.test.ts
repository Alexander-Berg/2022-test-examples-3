import { getUserDocumentPhotoData } from 'entities/User/helpers/getUserDocumentPhotoData/getUserDocumentPhotoData';

describe('getUserDocumentPhotoData', function () {
    it('works with empty params', function () {
        expect(getUserDocumentPhotoData({})).toMatchInlineSnapshot(`Array []`);
        expect(getUserDocumentPhotoData({ pb: {}, ps: {} })).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "pb",
              },
              Object {
                "type": "ps",
              },
            ]
        `);
    });

    it('works with full params', function () {
        expect(
            getUserDocumentPhotoData({
                pb: {
                    userId: '123',
                    photoId: '321',
                    date: new Date('2022-01-01'),
                },

                ps: {
                    userId: '456',
                    photoId: '678',
                    date: new Date('2022-01-01'),
                },
            }),
        ).toMatchInlineSnapshot(`
            Array [
              Object {
                "date": 2022-01-01T00:00:00.000Z,
                "photoId": "321",
                "type": "pb",
                "userId": "123",
              },
              Object {
                "date": 2022-01-01T00:00:00.000Z,
                "photoId": "678",
                "type": "ps",
                "userId": "456",
              },
            ]
        `);
    });
});
