import * as faker from 'faker';
import { entityCreatorFactory } from './utils';

export const generateGuid = () => faker.random.uuid();

export const generateGuids = entityCreatorFactory(generateGuid);

export const generateTsMcs = () => faker.date.past(0, new Date(2022, 0, 26)).getTime() * 1000;
