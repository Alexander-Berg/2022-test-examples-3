import { PreviewSizes } from '../../typings/previews';
import { getStickerUrl } from '../stickers';

describe('helpers/stckers', () => {
    describe('#getStickerUrl', () => {
        it('Should return url', () => {
            expect(getStickerUrl('1', PreviewSizes.MIDDLE))
                .toEqual('stickersUrl?{"stickerId":"1","size":"middle"}');
        });
    });
});
