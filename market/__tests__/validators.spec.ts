import {Validators} from '../validators';

describe('integer validator', () => {
    it('', () => {
        expect(
            Validators.integer()
                .validate('')
                .isSuccess()
        ).toEqual(true);
    });
    it('null', () => {
        expect(
            Validators.integer()
                .validate(null)
                .isSuccess()
        ).toEqual(true);
    });
    it('3', () => {
        expect(
            Validators.integer()
                .validate('3')
                .isSuccess()
        ).toEqual(true);
    });
    it('+3', () => {
        expect(
            Validators.integer()
                .validate('+3')
                .isSuccess()
        ).toEqual(true);
    });
    it('-3', () => {
        expect(
            Validators.integer()
                .validate('-3')
                .isSuccess()
        ).toEqual(true);
    });
});

describe('post code', () => {
    it('', () => {
        expect(
            Validators.intGreaterOrEquals(3)
                .validate('')
                .isSuccess()
        ).toEqual(true);
    });
    it('null', () => {
        expect(
            Validators.intGreaterOrEquals(3)
                .validate(null)
                .isSuccess()
        ).toEqual(true);
    });
    it('4', () => {
        expect(
            Validators.intGreaterOrEquals(3)
                .validate('4')
                .isSuccess()
        ).toEqual(true);
    });
    it('2', () => {
        expect(
            Validators.intGreaterOrEquals(3)
                .validate('2')
                .isSuccess()
        ).toEqual(false);
    });
    it('test', () => {
        expect(
            Validators.intGreaterOrEquals(3)
                .validate('test')
                .isSuccess()
        ).toEqual(false);
    });
});
