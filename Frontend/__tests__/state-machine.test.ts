import { BaseState, StateContext } from '../state-machine';

class TestContext implements StateContext {}

class TestState extends BaseState {
    public name = 'TestState';
}

describe('BaseState', () => {
    let context;
    let state;

    beforeEach(() => {
        context = new TestContext();
        state = new TestState(context);
    });

    describe('#constuctor', () => {
        test('Initialy not active', () => {
            expect(state.active).toBeFalsy();
        });
    });

    describe('#enter', () => {
        test('Change active from false to true', () => {
            state.enter();

            expect(state.active).toBeTruthy();
        });
    });

    describe('#exit', () => {
        test('Change active from true to false', () => {
            state.active = true;

            state.exit();

            expect(state.active).toBeFalsy();
        });
    });
});
