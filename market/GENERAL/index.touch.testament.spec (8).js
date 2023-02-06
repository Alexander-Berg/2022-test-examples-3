// @flow
// page objects
import PurchasedGoodsLinks from '@self/project/src/widgets/content/PurchasedGoodsLinks/components/View/__pageObject';

// helpers
import {makeMirror} from '@self/platform/helpers/testament';

// fixtures
import {appliancesNavigationPath} from '@self/root/src/spec/hermione/kadavr-mock/cataloger/navigationPath';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

async function makeContext(hid) {
    return mandrelLayer.initContext({
        request: {
            cookie: {
                kadavr_session_id: await kadavrLayer.getSessionId(),
            },
            params: {hid},
        },
    });
}

const navigationTree = {
    category:
        {
            entity: 'category',
            id: 90401,
            isLeaf: false,
            modelsCount: 2646282,
            name: 'Все товары',
            nid: 75701,
            offersCount: 637980,
        },
    childrenType: 'mixed',
    entity: 'navnode',
    fullName: 'Все товары',
    hasPromo: true,
    id: 75701,
    isLeaf: false,
    navnodes: [appliancesNavigationPath],
};

// skip
// https://proxy.sandbox.yandex-team.ru/3286461939/index.html#suites/99c52379ddb106d55d583a7a2405ca26/a310103c731efa16/
// SKIPPED MARKETFRONT-96354
// eslint-disable-next-line jest/no-disabled-tests
describe.skip('Widget: PurchasedGoodsLinks', () => {
    const widgetPath = require.resolve('@self/project/src/widgets/content/PurchasedGoodsLinks');
    const DEP_HID = appliancesNavigationPath.category.id;
    const NESTED_DEP_HID = appliancesNavigationPath.navnodes[0].category.id;

    beforeAll(async () => {
        mirror = await makeMirror(__filename, jest);
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        jestLayer.runCode(() => {
            const {mockRouterFabric} = require('@self/root/src/helpers/testament/mock');
            mockRouterFabric()({
                'touch:catalog': ({slug, nid}) => `/catalog--${slug}/${nid}`,
                'touch:list': ({slug, nid}) => `/catalog--${slug}/${nid}/list`,
                'touch:purchased': '/my/purchased',
            });
        }, []);
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Кейс с департаментом', () => {
        test('должен рендерить 2 ссылки: на все ранее купленные и департамент', async () => {
            await makeContext(DEP_HID);
            await kadavrLayer.setState('Cataloger.tree', {
                ...navigationTree,
                navnodes: [
                    {
                        ...appliancesNavigationPath,
                        navnodes: [],
                    },
                ],
            });

            const {container} = await apiaryLayer.mountWidget(widgetPath);
            const root = container.querySelector(PurchasedGoodsLinks.root);

            expect(root).toMatchSnapshot();
        });
    });

    describe('Кейс с департаментом -1', () => {
        test('должен рендерить 3 ссылки: на все ранее купленные, департамент -1 и департамент', async () => {
            await makeContext(NESTED_DEP_HID);
            await kadavrLayer.setState('Cataloger.tree', navigationTree);

            const {container} = await apiaryLayer.mountWidget(widgetPath);
            const root = container.querySelector(PurchasedGoodsLinks.root);

            expect(root).toMatchSnapshot();
        });
    });
});
