import { isSuccessful } from '../../../../src/store/selectors/document-state';

describe('store/selectors/document-state', () => {
    describe('isSuccessful', () => {
        it('READY state is successful', () => {
            expect(isSuccessful({
                doc: {
                    state: 'READY'
                }
            })).toEqual(true);
        });
        it('ARCHIVE state is successful', () => {
            expect(isSuccessful({
                doc: {
                    state: 'ARCHIVE'
                }
            })).toEqual(true);
        });
        it('WAIT state is not successful', () => {
            expect(isSuccessful({
                doc: {
                    state: 'WAIT'
                }
            })).toEqual(false);
        });
        it('FAIL state is not successful', () => {
            expect(isSuccessful({
                doc: {
                    state: 'FAIL'
                }
            })).toEqual(false);
        });
    });
});
