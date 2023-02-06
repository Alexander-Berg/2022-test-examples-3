import {unv} from '../common';

describe('unv', () => {
    it('unv(undefined, 1) => 1', ()=>{
        expect(unv(undefined, 1)).toEqual(1)
    })

    it('unv(1, undefined) => 1', ()=>{
        expect(unv(1, undefined)).toEqual(1)
    })
})