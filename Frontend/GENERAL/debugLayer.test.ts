import { DebugLayer } from '@dataLayers/debug/debugLayer';
import { YENV } from '@typings/app';
import { runInAction } from 'mobx';
import { AppLayerMock } from 'tests/mocksClasses/AppLayer';
import { mockRum } from 'tests/unitHelpers/RumMock';

describe('debugLayer', () => {
    describe('static methods', () => {
        [true, 'true', ' true '].forEach(val => {
            [false, true].forEach(arrayLike => {
                const wrapVal = arrayLike ? [val] : val;
                const stringVal = arrayLike ? `["${val}"]` : `"${val}"`;
                it(`should correct find value like true if val === "${stringVal}"`, () => {
                    expect(DebugLayer.isUrlTrue(wrapVal)).toEqual(true);
                });
            });
        });

        [false, undefined, ' tru ', ' ', '', null, {}, 'somestring'].forEach(val => {
            [false, true].forEach(arrayLike => {
                const wrapVal = arrayLike ? [val] : val;
                const stringVal = arrayLike ? `["${val}"]` : `"${val}"`;
                it(`should correct find value like false if val === "${stringVal}"`, () => {
                    expect(DebugLayer.isUrlTrue(wrapVal)).toEqual(false);
                });
            });
        });
    });

    describe('instance', () => {
        describe('availableDebug should', () => {
            for (const [_, env] of Object.entries(YENV)) {
                [false, true].forEach(isYandexNet => {
                    it(`${isYandexNet} if env=${env} and has user and isYandexNet:${isYandexNet}`, () => {
                        const appLayerMock = new AppLayerMock();
                        appLayerMock.isYandexNet = isYandexNet;
                        appLayerMock.user.emails = ['email.com'];
                        appLayerMock.user.id = 'someUserId';
                        appLayerMock.yenv = env;
                        const inst = new DebugLayer(appLayerMock, mockRum().mock);
                        expect(inst.availableDebug).toEqual(isYandexNet);
                    });

                    it(`false if env=${env} and doesnt has user and isYandexNet:${isYandexNet}`, () => {
                        const appLayerMock = new AppLayerMock();
                        appLayerMock.isYandexNet = isYandexNet;
                        appLayerMock.user.emails = [];
                        appLayerMock.user.id = '';
                        appLayerMock.yenv = env;
                        const inst = new DebugLayer(appLayerMock, mockRum().mock);
                        expect(inst.availableDebug).toEqual(false);
                    });
                });
            }
        });

        describe('should log if switch', () => {
            it('enabledVisualDebug to true', () => {
                const appLayerMock = new AppLayerMock();
                const rum = mockRum();
                const inst = new DebugLayer(appLayerMock, rum.mock);
                inst.setDebug({
                    enabledVisualDebug: true,
                });
                expect(rum.mockInst.logError).toBeCalledTimes(1);
            });

            it('enabledDebugPaths to true', () => {
                const appLayerMock = new AppLayerMock();
                const rum = mockRum();
                const inst = new DebugLayer(appLayerMock, rum.mock);
                inst.setDebug({
                    enabledDebugPaths: true,
                });
                expect(rum.mockInst.logError).toBeCalledTimes(1);
            });

            it('enabledDebugPaths and enabledVisualDebug to true', () => {
                const appLayerMock = new AppLayerMock();
                const rum = mockRum();
                const inst = new DebugLayer(appLayerMock, rum.mock);
                inst.setDebug({
                    enabledDebugPaths: true,
                    enabledVisualDebug: true,
                });
                expect(rum.mockInst.logError).toBeCalledTimes(1);
            });
        });

        describe('should sync params', () => {
            [false, true].forEach(VD => {
                [false, true].forEach(DP => {
                    it(`if VD=${VD} and DP=${DP}`, () => {
                        const appLayerMock = new AppLayerMock();
                        const rum = mockRum();
                        appLayerMock.params = {
                            DP: DP.toString(),
                            VD: VD.toString(),
                        };
                        const inst = new DebugLayer(appLayerMock, rum.mock);

                        expect(inst.enabledVisualDebug).toEqual(VD);
                        expect(inst.enabledDebugPaths).toEqual(DP);
                    });
                });
            });
        });

        describe('shouldn\'t apply change if not availableDebug', () => {
            const appLayerMock = new AppLayerMock();
            appLayerMock.user.emails = [];
            appLayerMock.user.id = '';
            const inst = new DebugLayer(appLayerMock, mockRum().mock);
            [
                {
                    changer: () => inst.setDebug({
                        enabledDebugPaths: true,
                        enabledVisualDebug: true,
                    }),
                    desc: 'manual enable',
                },
                {
                    changer: () => runInAction(
                        () => appLayerMock.params = {
                            DP: 'true',
                            VD: 'true',
                        },
                    ),
                    desc: 'by params enable',
                },
            ].forEach(({ changer, desc }) => {
                it(desc, () => {
                    changer();
                    expect(inst.enabledVisualDebug).toEqual(false);
                    expect(inst.enabledDebugPaths).toEqual(false);
                });
            });
        });
    });
});
