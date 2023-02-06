import Image from '@/model/image';

describe('Image', () => {
    test('extractImages', async () => {
        const uid = Image.genUid('file:/Market/marketlife/HR/Chat-bot/2018-11-141555.png');
        expect(uid).toEqual('3066ab736fff4c9d1e23311969da29e3.jpeg');
    });
});
