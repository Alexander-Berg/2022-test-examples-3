import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

// fixtures
import navigationTree from '@self/root/src/spec/hermione/kadavr-mock/cataloger/navigationTree';

// page-objects
import PurchasedGodosBreadcrumbs
    from '@self/project/src/widgets/content/PurchasedGoodsBreadcrumbs/components/View/__pageObject';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

const WIDGET_PATH = require.resolve('@self/project/src/widgets/content/PurchasedGoodsBreadcrumbs');
const HID = 91307;

async function makeContext() {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            cookie,
            params: {
                hid: HID,
            },
        },
    });
}

beforeAll(async () => {
    // $FlowFixMe<type of jest?>
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: PurchasedGoodsBreadcrumbs', () => {
    beforeEach(async () => {
        navigationTree.navnodes[5].navnodes = [];

        await kadavrLayer.setState(
            'Cataloger.tree',
            navigationTree
        );

        await kadavrLayer.setState(
            'Cataloger.categoriesTree',
            {
                categories: [
                    {
                        __name__: 'data',
                        entity: 'category',
                        fullName: 'Продукты, напитки',
                        id: 91307,
                        isLeaf: false,
                        modelsCount: 63085,
                        name: 'Продукты',
                        parentId: 90401,
                        offersCount: 50985,
                        type: 'gurulight',
                        viewType: 'grid',
                    },
                ],
                entity: 'category',
                id: 90401,
                isLeaf: false,
                modelsCount: 2646282,
                name: 'Все товары',
                nid: 75701,
                offersCount: 637980,
            }
        );
    });

    // TODO MARKETFRONTECH-4495
    test('рендер виджета должен матчиться со снапшотом', async () => {
        await makeContext();
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
        const root = container.querySelector(PurchasedGodosBreadcrumbs.root);

        expect(root).toMatchSnapshot();
    });
});
