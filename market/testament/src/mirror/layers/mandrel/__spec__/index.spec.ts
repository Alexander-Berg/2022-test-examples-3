import Mirror from '../../../index';
import JestLayer from '../../jest';
import MandrelLayer from '../index';
import ApiaryLayer from '../../apiary';
import {MandrelWorker} from '../worker';

declare function getBackend<TBackend>(backend: string): TBackend | null;

const mirror = new Mirror();
const jestLayer = new JestLayer(__filename, jest);
const mandrelLayer = new MandrelLayer();
const apiaryLayer = new ApiaryLayer();

const bootstrapLayers = async () => {
    await mandrelLayer.initContext();
};

beforeAll(async () => {
    await mirror.registerRuntime(jestLayer);
    await mirror.registerLayer(mandrelLayer);
    await mirror.registerLayer(apiaryLayer);
});

beforeEach(async () => {
    await bootstrapLayers();
});

afterAll(() => mirror.destroy());

describe('page', () => {
    test('resource should exists', async () => {
        const result = await jestLayer.backend.runCode(() => {
            // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
            const {getStoutPage} = require('@yandex-market/mandrel/context');
            const context = getBackend<MandrelWorker>('mandrel')?.getContext();
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            const page = getStoutPage(context!);

            return typeof page.resource === 'function';
        }, []);
        expect(result).toBe(true);
    });
});
