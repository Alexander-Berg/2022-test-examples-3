/* eslint-disable global-require */

/**
 * @expFlag all_checkout_chef
 * @ticket MARKETPROJECT-9717
 * start
 */

import {screen, within} from '@testing-library/dom';

import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {ON_DEMAND_COLLECTION_ID} from '@self/root/src/entities/onDemandState/constants';


/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await jestLayer.backend.runCode(id => {
        jest.spyOn(
            require('@self/root/src/resolvers/checkout/resolveOnDemandStateFromCookieSync'),
            'default'
        ).mockReturnValue({
            result: [id],
            collections: {
                onDemandState: {[id]: {id, tooltipWasShown: false}},
            },
        });
    }, [ON_DEMAND_COLLECTION_ID]);

    mandrelLayer.initContext({});
});

afterAll(() => {
    mirror.destroy();
});

const widgetPath = '@self/root/src/widgets/content/chef/common/Parcels';

describe('ChefCheckoutParcels', () => {
    describe('a11y', () => {
        it('содержит правильный заголовок', async () => {
            await apiaryLayer.mountWidget(widgetPath, {
                parcelsIds: ['parcelId'],
            });

            const a11yH2 = within(screen.getByRole('heading')).getByText('Посылки');

            expect(a11yH2).toBeTruthy();
        });
    });
});

/**
 * @expFlag all_checkout_chef
 * @ticket MARKETPROJECT-9717
 * end
 */
