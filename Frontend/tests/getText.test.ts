import { NBSP } from '@src/constants/char';
import { getText } from '../utils/getText';

describe('getText', () => {
    it('должен склонять количество предложений', () => {
        expect(getText({ count: 1 })).toEqual(`1${NBSP}предложение`);
        expect(getText({ count: 2 })).toEqual(`2${NBSP}предложения`);
        expect(getText({ count: 5 })).toEqual(`5${NBSP}предложений`);
    });

    it('должен добавлять «ещё» и сокращать количество предложений на единицу', () => {
        expect(getText({ count: 2, moreOffersStyle: true })).toEqual(`ещё${NBSP}1${NBSP}предложение`);
    });

    it('не должен добавлять «ещё» при наличии единственного предложения', () => {
        expect(getText({ count: 1, moreOffersStyle: true })).toEqual(`1${NBSP}предложение`);
    });
});
