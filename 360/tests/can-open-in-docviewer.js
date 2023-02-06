const canOpenInDV = require('../lib/can-open-in-docviewer');

const expect = require('expect');

describe('can-open-in-docviewer', () => {
    it('.zip можно открыть в DV', () => {
        expect(canOpenInDV(undefined, 'zip')).toEqual(true);
    });
    it('application/x-rar можно открыть в DV', () => {
        expect(canOpenInDV('application/x-rar')).toEqual(true);
    });
    it('application/word можно открыть в DV', () => {
        expect(canOpenInDV('application/word', '', true)).toEqual(true);
    });
    it('application/word без превью нельзя открыть', () => {
        expect(canOpenInDV('application/word', '', false)).toEqual(false);
    });
    it('application/word с превью можно открыть', () => {
        expect(canOpenInDV('application/word', '', true)).toEqual(true);
    });
    it('image/png не открываются в DV', () => {
        expect(canOpenInDV('image/png', 'png')).toEqual(false);
    });
    it('не должны падать если от mpfs пришёл mimetype:null', () => {
        expect(canOpenInDV(null, 'rws')).toEqual(false);
    });
});
