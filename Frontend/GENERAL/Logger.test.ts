import { Logger, Counter } from './Logger';

interface SpyInstancesHash {
    [key: string]: jest.SpyInstance<Logger>;
}

// @ts-ignore
window.globalParams = {
    counters: {
        [Counter.CLOSE]: ['690.2730.486'],
        [Counter.BACK]: ['690.2730.221', '-action=back'],
        [Counter.COLLAPSE]: ['690.2730.1302', '-action=back,2819=2098'],
    },
};

describe('Logger', () => {
    const spies: SpyInstancesHash = {};

    beforeEach(() => {
        // @ts-ignore
        spies.w = jest.spyOn(Logger, 'w');
    });

    afterEach(() => {
        Logger.setPermanentVars(undefined);

        spies.w.mockReset();
        spies.w.mockRestore();
    });

    describe('send', () => {
        describe(`should calls ${Counter.CLOSE} Logger.w`, () => {
            it('with properly params', () => {
                Logger.send(Counter.CLOSE);

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.486', '');
            });

            it('with properly params with dynamic vars', () => {
                Logger.send(Counter.CLOSE, {
                    '-text': 'text',
                    '-index': 1,
                });

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.486', '-index=1,-text=text');
            });

            it('with properly params with permanent vars', () => {
                Logger.setPermanentVars({
                    '-org_id': 'decathlon.ru',
                });

                Logger.send(Counter.CLOSE);

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.486', '-org_id=decathlon.ru');
            });

            it('with properly params with dynamic vars and permanent vars', () => {
                Logger.setPermanentVars({
                    '-org_id': 'decathlon.ru',
                });

                Logger.send(Counter.CLOSE, {
                    '-text': 'text',
                    '-index': 1,
                });

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.486', '-index=1,-org_id=decathlon.ru,-text=text');
            });

            it('with properly params with dynamic vars and override permanent vars', () => {
                Logger.setPermanentVars({
                    '-org_id': 'decathlon.ru',
                });

                Logger.send(Counter.CLOSE, {
                    '-org_id': 'taxi.yandex.ru',
                    '-index': 1,
                });

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.486', '-index=1,-org_id=taxi.yandex.ru');
            });
        });

        describe(`should calls ${Counter.BACK} Logger.w`, () => {
            it('with properly params', () => {
                Logger.send(Counter.BACK);

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.221', '-action=back');
            });

            it('with properly params with dynamic vars (checks override vars)', () => {
                Logger.send(Counter.BACK, {
                    '-text': 'text',
                    '-action': 'next',
                    '-index': 2,
                });

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.221', '-action=next,-index=2,-text=text');
            });

            it('with properly params with permanent vars', () => {
                Logger.setPermanentVars({
                    '-org_id': 'decathlon.ru',
                });

                Logger.send(Counter.BACK);

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.221', '-action=back,-org_id=decathlon.ru');
            });
        });

        describe(`should calls ${Counter.COLLAPSE} Logger.w`, () => {
            it('with properly params', () => {
                Logger.send(Counter.COLLAPSE);

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.1302', '-action=back,2819=2098');
            });

            it('with properly params with dynamic vars (checks override vars)', () => {
                Logger.send(Counter.COLLAPSE, {
                    '-text': 'text',
                    '-action': 'next',
                    '-index': 3,
                });

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.1302', '-action=next,-index=3,-text=text,2819=2098');
            });

            it('with properly params with permanent vars', () => {
                Logger.setPermanentVars({
                    '-org_id': 'decathlon.ru',
                });

                Logger.send(Counter.COLLAPSE);

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w).toHaveBeenCalledWith(null, '690.2730.1302', '-action=back,-org_id=decathlon.ru,2819=2098');
            });

            it('with properly params with dynamic vars and override permanent vars and "static" vars', () => {
                Logger.setPermanentVars({
                    '-action': 'next',
                    '-org_id': 'decathlon.ru',
                });

                Logger.send(Counter.COLLAPSE, {
                    '-org_id': 'taxi.yandex.ru',
                    '-index': 1,
                });

                expect(Logger.w).toHaveBeenCalledTimes(1);
                expect(Logger.w)
                    .toHaveBeenCalledWith(null, '690.2730.1302', '-action=next,-index=1,-org_id=taxi.yandex.ru,2819=2098');
            });
        });
    });

    describe('convertVars', () => {
        it('should properly converts object into not encoded vars object', () => {
            expect(Logger.convertVars({
                a: 1,
                b: 2,
                c: 'c-var',
                d: '4',
            })).toEqual({ '-a': 1, '-b': 2, '-c': 'c-var', '-d': '4' });

            expect(Logger.convertVars({
                a: 1,
                '-b': 1,
                '-c': 1,
                d: 1,
            })).toEqual({ '-a': 1, '-b': 1, '-c': 1, '-d': 1 });

            expect(Logger.convertVars({
                '-a': 1,
                '-b': 2,
                '-c': 3,
                '-d': 4,
            })).toEqual({ '-a': 1, '-b': 2, '-c': 3, '-d': 4 });

            expect(Logger.convertVars({
                'a-b': 2,
                'c-d': '4',
            })).toEqual({ '-a-b': 2, '-c-d': '4' });
        });
    });
});
