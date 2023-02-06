import { bind } from '../src';

class Class {
    public n: number | undefined;

    constructor(n?: number) {
        if (n) {
            this.n = n;
        }
    }

    @bind
    public publicMethod(): Class {
        return this;
    }
}

describe('BindDecorator', () => {
    describe('@bind', () => {
        it('Should bind instance context', () => {
            const instance: Class = new Class();

            expect(instance.publicMethod()).toEqual(instance);

            const publicMethod = instance.publicMethod;

            expect(publicMethod()).toEqual(instance);
            expect(publicMethod.call({})).toEqual(instance);
        });

        it('Should bind every instance to it\'s context', () => {
            const instance1: Class = new Class(1);
            const instance2: Class = new Class(2);
            const instance3: Class = new Class(3);

            expect(instance3.publicMethod()).toEqual(instance3);
            expect(instance1.publicMethod()).toEqual(instance1);
            expect(instance2.publicMethod()).toEqual(instance2);
        });

        it('Should throw TypeError when assign method to something new', () => {
            const instance: Class = new Class();

            expect(() => {
                // @ts-ignore
                instance.publicMethod = () => null;
            }).toThrow(TypeError);
        });

        it('Should throw TypeError when assign method to something new after it bound', () => {
            const instance: Class = new Class();

            instance.publicMethod();

            expect(() => {
                // @ts-ignore
                instance.publicMethod = () => null;
            }).toThrow(TypeError);
        });

        it('Should throw TypeError when trying to bind static method', () => {
            expect(() => {
                // @ts-ignore
                // eslint-disable-next-line
                class Fake {
                    @bind
                    public static staticMethod() {
                        // nope
                    }
                }
            }).toThrow(TypeError);
        });
    });
});
