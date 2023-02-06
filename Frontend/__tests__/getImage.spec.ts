import { convertPictureToThumbnail } from '@yandex-turbo/applications/beru.ru/helpers/thumbnails';
import { ISku, IKnownThumbnails } from '@yandex-turbo/applications/beru.ru/interfaces';
import { getImage } from '../getImage';

jest.mock('@yandex-turbo/applications/beru.ru/helpers/thumbnails', () => ({
    convertPictureToThumbnail: jest.fn(),
}));

type TFn = ReturnType<typeof jest.fn>;

describe('getImage', () => {
    let sku: ISku;
    let knownThumbnails: IKnownThumbnails[];

    beforeEach(() => {
        sku = <ISku>{
            pictures: [{ entity: 'picture' }],
        };
        knownThumbnails = <IKnownThumbnails[]>[
            { namespace: 'test', thumbnails: [] },
        ];
    });

    afterEach(() => {
        (convertPictureToThumbnail as TFn).mockClear();
    });

    it('возвращает картинку для мета тэга', () => {
        (convertPictureToThumbnail as TFn).mockReturnValue({ src: 'path/to/image.png' });
        const image = getImage(sku, knownThumbnails);

        expect(image).toEqual('path/to/image.png');
        // @ts-ignore
        expect(convertPictureToThumbnail).toHaveBeenCalledWith({
            ...(sku.pictures ? sku.pictures[0] : []),
            knownThumbnails,
        }, 0);
    });

    it('возвращает пустую строку, если отстствуют картинки в sku', () => {
        sku.pictures = [];

        expect(getImage(sku, knownThumbnails)).toEqual('');
        expect(convertPictureToThumbnail).not.toHaveBeenCalled();
    });
});
