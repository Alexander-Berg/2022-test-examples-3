const general = require('../../../config/general');

describe('консистентность step и stage', () => {
    const { expStages, expSteps } = general;

    it('для каждого stage из expStages должен сущестовать step из expSteps', () => {
        const steps = Object.keys(expSteps);
        Object.keys(expStages).forEach((stage) => {
            if (expStages[stage].step) {
                assert.ok(steps.includes(expStages[stage].step));
            }
        });
    });

    it('для каждого step из expSteps должен сущестовать хотя бы один stage из expStages', () => {
        const stageSteps = Object.keys(expStages).map((stage) => expStages[stage].step);

        const steps = Object.keys(expSteps);
        steps.forEach((step) =>
            assert.ok(stageSteps.includes(step)));
    });

    it('шаги autostartExpSteps должны содержаться в expSteps', () => {
        const steps = Object.keys(expSteps);
        const autostartExpSteps = general.autostartExpSteps;

        autostartExpSteps.forEach((step) => assert.ok(steps.includes(step)));
    });

    it('шаги withoutAutostartExpSteps должны содержаться в expSteps', () => {
        const steps = Object.keys(expSteps);
        const withoutAutostartExpSteps = general.withoutAutostartExpSteps;

        withoutAutostartExpSteps.forEach((step) => assert.ok(steps.includes(step)));
    });
});
