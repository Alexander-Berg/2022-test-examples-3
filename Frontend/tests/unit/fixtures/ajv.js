const compileStub = sinon.stub();
const compileDefaultResultStub = sinon.stub();
const errorsTextStub = sinon.stub();

class Ajv {
    constructor() {
        Ajv.resetBehavior();
    }

    compile() {
        return compileStub();
    }

    errorsText() {
        return errorsTextStub();
    }

    static get stubs() {
        return {
            get compile() {
                return compileStub;
            },

            get compileDefaultResult() {
                return compileDefaultResultStub;
            },

            get errorsText() {
                return errorsTextStub;
            },
        };
    }

    static resetBehavior() {
        compileStub.resetBehavior();
        compileStub.returns(compileDefaultResultStub);
        compileDefaultResultStub.resetBehavior();
        errorsTextStub.resetBehavior();
        errorsTextStub.returns('');
    }
}

Ajv.resetBehavior();

module.exports = Ajv;
