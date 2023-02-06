const calcDesignNormalTasks = require('../../../../src/shared/utils/calc-design-normal-tasks');

describe('/shared/utils/calcDesignNormalTasks', () => {
    it('screens: 1, systems 1', () => {
        assert.equal(calcDesignNormalTasks([{ screens: [1] }]), 5);
    });

    it('screens: 2, systems 2', () => {
        assert.equal(calcDesignNormalTasks([{ screens: [1,2] }, { screens: [1,2] }]), 1);
    });

    it('screens: 2, systems 3', () => {
        assert.equal(calcDesignNormalTasks([{ screens: [1,2,3] }, { screens: [1,2,3] }]), 3);
    });

    it('screens: 2, systems 4', () => {
        assert.equal(calcDesignNormalTasks([{ screens: [1,2,3,4] }, { screens: [1,2,3,4] }]), 4);
    });

    it('screens: 2, systems 5', () => {
        assert.equal(calcDesignNormalTasks([{ screens: [1,2,3,4,5] }, { screens: [1,2,3,4,5] }]), 4);
    });

    it('screens: 1, systems 16', () => {
        assert.equal(calcDesignNormalTasks([{ screens: [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16] }]), 5);
    });
});
