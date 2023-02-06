import { getUpdatedSteps } from './utils';
import { IFormData, IStepEntity } from '../Wizard.lib';

const baseStepEntity: IStepEntity<IFormData, {}> = {
    id: 'base',
    title: 'Base',
    description: 'Base description',
    validate: () => Promise.resolve({ passed: true }),
    checkStepCondition: () => false,
    component: jest.fn(),
};

const steps = [{
    ...baseStepEntity, id: 'id1', order: 1,
}, {
    ...baseStepEntity, id: 'id2', order: 3,
}, {
    ...baseStepEntity, id: 'id3', order: 7,
}, {
    ...baseStepEntity, id: 'id4', order: 8,
}, {
    ...baseStepEntity, id: 'id5', order: 10,
}, {
    ...baseStepEntity, id: 'id6', order: 11,
}];

describe('utils', () => {
    describe('getUpdatedSteps', () => {
        it('Removing step', () => {
            const result = getUpdatedSteps(steps, steps[2], false, 3);

            expect(result.length).toBe(5);
            expect(result.find(step => step.order === 7)).toBeUndefined();
        });

        it('Inserting existing step', () => {
            const result = getUpdatedSteps(steps, steps[2], true, 3);

            expect(result.length).toBe(6);
            expect(result.filter(step => step.order === 7).length).toBe(1);
        });

        it('Inserting new step', () => {
            const newStep = { ...baseStepEntity, id: 'newId1', order: 5 };
            const result = getUpdatedSteps(steps, newStep, true, 3);

            expect(result.length).toBe(7);
            expect(result.indexOf(newStep)).toBe(2);
        });
    });
});
