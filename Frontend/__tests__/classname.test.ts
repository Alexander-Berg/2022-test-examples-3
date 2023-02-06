/* eslint-disable dot-notation */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { cn, withNaming } from '../classname';

const buildStyles = (strs: string[]) =>
    strs.reduce((res, className) => {
        res[className] = className + '-Hash';

        return res;
    }, {});

const classes = [
    'Block',
    'Block_theme_normal',
    'Block_modName',
    'Block_modName_0',
    'Block_modName2_modVal2',
    'Block-Elem',
    'Block-Elem_theme_normal',
    'Block-Elem_modName',
    'Block-Elem_modName2_modVal2',
];

const styles = buildStyles(classes);

const classes2 = [
    'Block-Elem2',
    'Block-Elem2_modName',
    'Block-Elem2_modName2_modVal2',
    'Block-Elem',
    'Block-Elem_modName3_modVal3',
];
const styles2 = buildStyles(classes2);

const classesOldStyle = [
    'block',
    'block_modName2_modVal2',
    'block_modName',
    'block_modName_0',
    'block__elem',
    'block__elem_modName',
    'block__elem_modName2_modVal2',
];

const stylesOldStyle = buildStyles(classesOldStyle);

const mapClassName = (className: string, styles, mix = '') => {
    const str =
        className
            .split(' ')
            .map((className: string) => styles[className])
            .join(' ') + ` ${mix}`;

    const trimmed = str.trim();

    return trimmed;
};

describe('@bem-react/classname-modules', () => {
    describe('cn with styles', () => {
        test('block', () => {
            const b = cn('Block', styles);
            expect(b()).toEqual(styles['Block']);
        });

        test('elem', () => {
            const e = cn('Block', 'Elem', styles);
            expect(e()).toEqual(styles['Block-Elem']);
        });

        test('carry elem', () => {
            const b = cn('Block', styles);
            expect(b('Elem')).toEqual(styles['Block-Elem']);
        });

        describe('modifiers', () => {
            test('block', () => {
                const b = cn('Block', styles);
                expect(b({ modName: true })).toEqual(mapClassName('Block Block_modName', styles));
            });

            test('elem', () => {
                const e = cn('Block', 'Elem', styles);
                expect(e({ modName: true })).toEqual(mapClassName('Block-Elem Block-Elem_modName', styles));
            });

            test('carry elem', () => {
                const b = cn('Block', styles);

                expect(b('Elem', { modName: true, modName2: 'modVal2' })).toEqual(
                    mapClassName('Block-Elem Block-Elem_modName Block-Elem_modName2_modVal2', styles),
                );
            });

            test('more than one', () => {
                const mods = { modName: true, modName2: 'modVal2' };
                const b = cn('Block', styles);
                const e = cn('Block', 'Elem', styles);

                expect(b(mods)).toEqual(mapClassName('Block Block_modName Block_modName2_modVal2', styles));
                expect(e(mods)).toEqual(
                    mapClassName('Block-Elem Block-Elem_modName Block-Elem_modName2_modVal2', styles),
                );
            });

            test('empty', () => {
                const b = cn('Block', styles);
                expect(b({})).toEqual(styles['Block']);
            });

            test('falsy', () => {
                const b = cn('Block', styles);
                expect(b({ modName: false })).toEqual(styles['Block']);
            });

            test('with falsy', () => {
                const b = cn('Block', 'Elem', styles);
                expect(b({ modName: false, modName2: 'modVal2' })).toEqual(
                    mapClassName('Block-Elem Block-Elem_modName2_modVal2', styles),
                );
            });

            test('zero', () => {
                const b = cn('Block', styles);
                expect(b({ modName: '0' })).toEqual(mapClassName('Block Block_modName_0', styles));
            });

            test('undefined', () => {
                const b = cn('Block', styles);
                expect(b({ modName: undefined })).toEqual(styles['Block']);
            });
        });

        describe('mix', () => {
            test('block', () => {
                const b = cn('Block', styles);
                expect(b(null, ['Mix1', 'Mix2'])).toEqual(mapClassName('Block', styles, 'Mix1 Mix2'));
            });
            test('block with mods', () => {
                const b = cn('Block', styles);
                expect(b({ theme: 'normal' }, ['Mix'])).toEqual(
                    mapClassName('Block Block_theme_normal', styles, 'Mix'),
                );
            });
            test('elem', () => {
                const e = cn('Block', 'Elem', styles);
                expect(e(null, ['Mix1', 'Mix2'])).toEqual(mapClassName('Block-Elem', styles, 'Mix1 Mix2'));
            });
            test('elem with mods', () => {
                const e = cn('Block', 'Elem', styles);
                expect(e({ theme: 'normal' }, ['Mix'])).toEqual(
                    mapClassName('Block-Elem Block-Elem_theme_normal', styles, 'Mix'),
                );
            });
            test('carry elem', () => {
                const b = cn('Block', styles);
                expect(b('Elem', ['Mix1', 'Mix2'])).toEqual(mapClassName('Block-Elem', styles, 'Mix1 Mix2'));
            });
            test('carry elem with mods', () => {
                const b = cn('Block', styles);
                expect(b('Elem', { theme: 'normal' }, ['Mix'])).toEqual(
                    mapClassName('Block-Elem Block-Elem_theme_normal', styles, 'Mix'),
                );
            });
            test('undefined', () => {
                const b = cn('Block', styles);
                expect(b('Elem', null, [undefined])).toEqual(styles['Block-Elem']);
            });
            test('not string and not undefined', () => {
                const b = cn('Block', styles);
                expect(b('Elem', null, [false as any])).toEqual(styles['Block-Elem']);
                expect(b('Elem', null, [true as any])).toEqual(styles['Block-Elem']);
                expect(b('Elem', null, [10 as any])).toEqual(styles['Block-Elem']);
                expect(b('Elem', null, [null as any])).toEqual(styles['Block-Elem']);
            });

            test('set styles', () => {
                const b = cn('Block', styles);
                const b2 = b.setStyles(styles2);

                expect(b('Elem', { modName: true, modName2: 'modVal2' })).toEqual(
                    mapClassName('Block-Elem Block-Elem_modName Block-Elem_modName2_modVal2', styles),
                );

                expect(b2('Elem2', { modName: true, modName2: 'modVal2' })).toEqual(
                    mapClassName('Block-Elem2 Block-Elem2_modName Block-Elem2_modName2_modVal2', styles2),
                );
            });

            test('set styles with element', () => {
                const e = cn('Block', 'Elem', styles);
                const e2 = e.setStyles(styles2);

                expect(e({ modName: true, modName2: 'modVal2' })).toEqual(
                    mapClassName('Block-Elem Block-Elem_modName Block-Elem_modName2_modVal2', styles),
                );

                expect(e2({ modName3: 'modVal3' })).toEqual(
                    mapClassName('Block-Elem Block-Elem_modName3_modVal3', styles2),
                );
            });
        });

        describe('withNaming origin preset', () => {
            const cCn = withNaming({
                e: '__',
                m: '_',
            });

            test('block', () => {
                const b = cCn('block', stylesOldStyle);
                expect(b()).toEqual(stylesOldStyle['block']);
            });

            test('elem', () => {
                const e = cCn('block', 'elem', stylesOldStyle);
                expect(e()).toEqual(stylesOldStyle['block__elem']);
            });

            describe('modifiers', () => {
                test('block', () => {
                    const b = cCn('block', stylesOldStyle);
                    expect(b({ modName: true })).toEqual(mapClassName('block block_modName', stylesOldStyle));
                });

                test('elem', () => {
                    const e = cCn('block', 'elem', stylesOldStyle);
                    expect(e({ modName: true })).toEqual(
                        mapClassName('block__elem block__elem_modName', stylesOldStyle),
                    );
                });

                test('more than one', () => {
                    const mods = { modName: true, modName2: 'modVal2' };
                    const b = cCn('block', stylesOldStyle);
                    const e = cCn('block', 'elem', stylesOldStyle);

                    expect(b(mods)).toEqual(mapClassName('block block_modName block_modName2_modVal2', stylesOldStyle));
                    expect(e(mods)).toEqual(
                        mapClassName('block__elem block__elem_modName block__elem_modName2_modVal2', stylesOldStyle),
                    );
                });

                test('empty', () => {
                    const b = cCn('block', stylesOldStyle);
                    expect(b({})).toEqual(stylesOldStyle['block']);
                });

                test('falsy', () => {
                    const b = cCn('block', stylesOldStyle);
                    expect(b({ modName: false })).toEqual(stylesOldStyle['block']);
                });

                test('with falsy', () => {
                    const b = cCn('block', stylesOldStyle);
                    expect(b({ modName: false, modName2: 'modVal2' })).toEqual(
                        mapClassName('block block_modName2_modVal2', stylesOldStyle),
                    );
                });

                test('zero', () => {
                    const b = cCn('block', stylesOldStyle);
                    expect(b({ modName: '0' })).toEqual(mapClassName('block block_modName_0', stylesOldStyle));
                });
            });
        });
    });
});
