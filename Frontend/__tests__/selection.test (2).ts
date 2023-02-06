import {
    update,
    toggle,
    clear,
    selectionReducer,
    initialState,
} from '../selection';
import { SelectionMode } from '../../constants/Chat';
import { messagesMockFactory } from './mock/messages';

describe('Selection reducer', () => {
    const messagesMock = messagesMockFactory();

    describe('#updateMessageSelection', () => {
        it('Should return updated state', () => {
            const [key] = messagesMock.getMessageKeys(...messagesMock.createTextMessage()(1));
            const data = {
                mode: SelectionMode.REGULAR,
                selected: [key],
            };
            const result = selectionReducer(initialState, update(data));

            expect(result).toStrictEqual({
                mode: SelectionMode.REGULAR,
                selected: [key],
            });
        });

        it('Should return initialState if data is empty', () => {
            const result = selectionReducer(initialState, update({}));

            expect(result).toStrictEqual(initialState);
        });
    });

    describe('#toggleMessageSelection', () => {
        it('Should return state with selected message if it wasn\'t selected', () => {
            const keys = messagesMock.getMessageKeys(...messagesMock.createTextMessage()(3));

            const stateWithSelected = {
                mode: SelectionMode.REGULAR,
                selected: keys,
            };

            const [messageToToggle] = messagesMock.getMessageKeys(...messagesMock.createTextMessage()(1));

            const expected = {
                mode: SelectionMode.REGULAR,
                selected: [...keys, messageToToggle],
            };
            const result = selectionReducer(stateWithSelected, toggle(messageToToggle));
            expect(result).toStrictEqual(expected);
        });

        it('Should return state without message that was selected before', () => {
            const [key1, key2, key3] = messagesMock.getMessageKeys(...messagesMock.createTextMessage()(3));

            const stateWithSelected = {
                mode: SelectionMode.REGULAR,
                selected: [key1, key2, key3],
            };
            const messageToToggle = key2;

            const expected = {
                mode: SelectionMode.REGULAR,
                selected: [key1, key3],
            };
            const result = selectionReducer(stateWithSelected, toggle(messageToToggle));
            expect(result).toStrictEqual(expected);
        });
    });

    describe('#clearMessageSelection', () => {
        it('Returns state with cleared message selection', () => {
            const [key1, key2, key3] = messagesMock.getMessageKeys(...messagesMock.createTextMessage()(3));

            const stateWithSelected = {
                mode: SelectionMode.REGULAR,
                selected: [key1, key2, key3],
            };

            const result = selectionReducer(stateWithSelected, clear());
            expect(result).toStrictEqual(initialState);
        });
    });
});
