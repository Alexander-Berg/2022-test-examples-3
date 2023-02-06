import { load } from 'lib/direct';
import detect from 'lib/direct/detector-with-cache';
import { createDirectClasses } from 'lib/direct/dynamic-classes';

jest.mock('lib/direct/detector-with-cache');
jest.mock('lib/direct/dynamic-classes');
jest.mock('lib/direct/fake');

const getState = (state) => {
    return Object.assign({
        direct: {
            orders: {
                top: [false],
                bottom: [true]
            },
            names: {
                testProp: 'testValue'
            },
            tokens: {
                main: ['ni', 'am'],
                fallback: ['kcab', 'llaf']
            }
        }
    }, state);
};

let state;
let loader;
let referenceNode;

describe('src/lib/direct/index.js', () => {
    describe('#load', () => {
        beforeEach(() => {
            loader = {};
            referenceNode = { parentNode: { insertBefore: jest.fn() } };
            jest.spyOn(document, 'createElement').mockReturnValue(loader);
            jest.spyOn(document, 'getElementsByTagName').mockReturnValue([referenceNode]);
        });

        describe('with antiadblock', () => {
            beforeEach(() => {
                state = getState();
            });

            it('should create dynamic ad classes', () => {
                return load(state).then(() => {
                    expect(createDirectClasses).toHaveBeenCalledWith({
                        testProp: 'testValue'
                    });
                });
            });

            it('should run an adblock detector', () => {
                return load(state).then(() => {
                    expect(detect).toHaveBeenCalled();
                });
            });

            it('should load an ad script via unencrypted link', () => {
                detect.mockResolvedValue({ blocked: false });
                return load(state).then(() => {
                    expect(loader).toEqual({
                        src: 'main',
                        type: 'text/javascript',
                        async: true
                    });
                    expect(referenceNode.parentNode.insertBefore).toHaveBeenCalledWith(loader, referenceNode);
                });
            });

            it('should load an ad script via encrypted link', () => {
                detect.mockResolvedValue({ blocked: true });
                return load(state).then(() => {
                    expect(loader).toEqual({
                        src: 'fallback',
                        type: 'text/javascript',
                        async: true
                    });
                    expect(referenceNode.parentNode.insertBefore).toHaveBeenCalledWith(loader, referenceNode);
                });
            });
        });
    });

    afterEach(() => jest.clearAllMocks());
});
