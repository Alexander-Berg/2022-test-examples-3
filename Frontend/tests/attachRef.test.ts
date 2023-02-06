import * as React from 'react';
import { attachRef } from '../src';

interface AttachRefTestProps {
    forwardedRef?: React.Ref<string>;
}

class AttachRefTest extends React.Component<AttachRefTestProps> {
    @attachRef('forwardedRef')
    private ref: React.RefObject<string>;

    public updateRef(value: string) {
        // @ts-ignore
        this.ref(value);
    }
}

describe('AttachRefDecorator', () => {
    describe('#attachRef', () => {
        it('Should update forwardedRef function', () => {
            let ref: string | undefined;

            const attachRefTest = new AttachRefTest({
                forwardedRef(value: string) {
                    ref = value;
                },
            });

            attachRefTest.updateRef('My test value');

            expect(ref).toEqual('My test value');
        });

        it('Should update forwardedRef object', () => {
            const ref = React.createRef<string>();

            const attachRefTest = new AttachRefTest({
                forwardedRef: ref,
            });

            attachRefTest.updateRef('My test value');

            expect(ref.current).toEqual('My test value');
        });

        it('Should not throw if forwardedRef is string', () => {
            const ref = 'str_ref';

            const attachRefTest = new AttachRefTest({
                // @ts-ignore
                forwardedRef: ref,
            });

            attachRefTest.updateRef('My test value');
        });

        it('Should not throw if forwardedRef is undefined', () => {
            const attachRefTest = new AttachRefTest({});

            attachRefTest.updateRef('My test value');
        });
    });
});
