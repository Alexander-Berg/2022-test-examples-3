import {IImage} from 'types/common/IImage';

import {getOptimalImage} from 'utilities/images/getOptimalImage';

const x100y200: IImage = {width: 100, height: 200, url: ''};
const x200y400: IImage = {width: 200, height: 400, url: ''};
const x1000y100: IImage = {width: 1000, height: 100, url: ''};
const x200y190: IImage = {width: 200, height: 190, url: ''};

describe('getOptimalImage', () => {
    it('', () => {
        expect(getOptimalImage([x100y200, x200y400], 100, 200)).toEqual(
            x100y200,
        );
        expect(getOptimalImage([x100y200, x200y400], 90, 150)).toEqual(
            x100y200,
        );
        expect(getOptimalImage([x100y200, x200y400], 150, 300)).toEqual(
            x200y400,
        );
        expect(getOptimalImage([x100y200, x200y400], 100500, 100500)).toEqual(
            x200y400,
        );
    });

    it('Оба изображения не соответствуют параметрам', () => {
        expect(getOptimalImage([x1000y100, x200y190], 200, 200)).toEqual(
            x200y190,
        );
        expect(getOptimalImage([x1000y100, x200y190], 300, 300)).toEqual(
            x200y190,
        );
    });
});
