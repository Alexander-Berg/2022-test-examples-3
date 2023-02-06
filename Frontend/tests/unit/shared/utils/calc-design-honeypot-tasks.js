const calcDesignHoneypotTasks = require('../../../../src/shared/utils/calc-design-honeypot-tasks');

describe('shared/utils/get-design-honeypot-tasks', () => {
    it('honeypots: 2', () => {
        assert.equal(calcDesignHoneypotTasks([{ honeypots: [1,2] }]), 1);
    });

    it('honeypots: 0', () => {
        assert.equal(calcDesignHoneypotTasks([{ honeypots: [] }]), 0);
    });
});
