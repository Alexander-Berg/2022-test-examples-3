/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { createUser, createSkill, createMarketDevice, wipeDatabase } from '../../../_helpers';
import { Channel, SkillAccess } from '../../../../../db/tables/settings';
import { callApi, respondsWithError } from './_helpers';
import { getServiceTicket } from '../../_helpers';
import { completeDeploy } from '../../../../../services/skill-lifecycle';
import * as modelSearch from '../../../../../services/market/model-search';
import { SerializedMarketModel, SmartHomeDevicesCategory } from '../../../../../services/market/types';
import { sortByName } from '../../../../_helpers';

const marketModel1: SerializedMarketModel = {
    id: '1',
    categoryId: 1,
    categoryName: '1',
    name: '1',
    picture: '1',
    vendorId: 1,
    vendorName: '1',
};

const marketModel2: SerializedMarketModel = {
    id: '2',
    categoryId: 1,
    categoryName: '1',
    name: '2',
    picture: '2',
    vendorId: 2,
    vendorName: '2',
};

const marketModel3: SerializedMarketModel = {
    id: '3',
    categoryId: 2,
    categoryName: '2',
    name: '2',
    picture: '2',
    vendorId: 2,
    vendorName: '2',
};

const marketModel4: SerializedMarketModel = {
    id: '4',
    categoryId: 2,
    categoryName: '2',
    name: '2',
    picture: '2',
    vendorId: 3,
    vendorName: '3',
};
const marketModelsMap: Record<string, SerializedMarketModel> = {
    '1': marketModel1,
    '2': marketModel2,
    '3': marketModel3,
    '4': marketModel4,
};

const extractCategories = (body: any): SmartHomeDevicesCategory[] => {
    return (body.certifiedDevices.categories as SmartHomeDevicesCategory[]).map(category => ({
        ...category,
        brandIds: category.brandIds.sort((a, b) => a - b),
    }));
};

const test = anyTest as TestInterface<{ serviceTicket: string }>;

test.before(async t => {
    const serviceTicket = await getServiceTicket();

    Object.assign(t.context, { serviceTicket });
});

test.beforeEach(async() => {
    sinon.restore();
    await wipeDatabase();
});

test('Should get devices from valid smart home skill', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '1',
        },
        { isDraft: true },
    );
    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '2',
        },
        { isDraft: true },
    );

    await completeDeploy(skill);

    sinon.replace(modelSearch, 'getMarketModels', async({ modelIds }) => {
        return modelIds.map(modelId => {
            return marketModelsMap[modelId];
        });
    });

    const res = await callApi('get', `/skills/${skill.id}/certified-devices`, t.context);

    const categories = extractCategories(res.body);

    t.deepEqual(sortByName(categories), [
        {
            brandIds: [1, 2],
            devicesCount: 2,
            id: 1,
            name: '1',
        },
    ]);
});

test('Should get empty list from unpublished smart home skill', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await createMarketDevice({
        skillId: skill.id,
        marketId: '1',
    });

    sinon.replace(modelSearch, 'getMarketModels', async({ modelIds }) => {
        return modelIds.map(modelId => {
            return marketModelsMap[modelId];
        });
    });

    const res = await callApi('get', `/skills/${skill.id}/certified-devices`, t.context);

    const categories = extractCategories(res.body);

    t.deepEqual(sortByName(categories), []);
});

test('Should get empty list from private smart home skill', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: true,
        skillAccess: SkillAccess.Private,
    });

    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '1',
        },
        { isDraft: true },
    );

    await completeDeploy(skill);

    sinon.replace(modelSearch, 'getMarketModels', async({ modelIds }) => {
        return modelIds.map(modelId => {
            return marketModelsMap[modelId];
        });
    });

    const res = await callApi('get', `/skills/${skill.id}/certified-devices`, t.context);

    const categories = extractCategories(res.body);

    t.deepEqual(sortByName(categories), []);
});

test('Should get empty list from untrusted smart home skill', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: false,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '1',
        },
        { isDraft: true },
    );

    await completeDeploy(skill);

    sinon.replace(modelSearch, 'getMarketModels', async({ modelIds }) => {
        return modelIds.map(modelId => {
            return marketModelsMap[modelId];
        });
    });

    const res = await callApi('get', `/skills/${skill.id}/certified-devices`, t.context);

    const categories = extractCategories(res.body);

    t.deepEqual(sortByName(categories), []);
});

test('Should respond with error for non smart home skills', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.AliceSkill,
        isTrustedSmartHomeSkill: false,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    const res = await callApi('get', `/skills/${skill.id}/certified-devices`, t.context);

    respondsWithError(400, 'Requested devices for non smart home skills', res, t);
});

test('Should get empty list when devices are not specified', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await completeDeploy(skill);

    sinon.replace(modelSearch, 'getMarketModels', async({ modelIds }) => {
        return modelIds.map(modelId => {
            return marketModelsMap[modelId];
        });
    });

    const res = await callApi('get', `/skills/${skill.id}/certified-devices`, t.context);

    const categories = extractCategories(res.body);

    t.deepEqual(sortByName(categories), []);
});
