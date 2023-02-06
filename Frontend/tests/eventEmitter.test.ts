import { EventEmitter } from '../src';

const EVENT_NEW_MESSAGE = 'new_message';
const EVENT_NEW_MEMBER = 'new_member';
const EVENT_ONLINE = 'member_is_online';

const greetNewMember = jest.fn();
const kickNewMember = jest.fn();
const memberIsOnline = jest.fn();
const notifyNewMessage = jest.fn();
const printNewMessage = jest.fn();

describe('EventEmitter', () => {
    let emitter;

    beforeEach(() => {
        emitter = new EventEmitter();
    });

    afterEach(() => {
        emitter.removeAllListeners();
        emitter = undefined;
    });

    describe('#removeAllListeners', () => {
        beforeEach(() => {
            emitter.addListener(EVENT_NEW_MEMBER, greetNewMember);
            emitter.addListener(EVENT_NEW_MEMBER, kickNewMember);
            emitter.addListener(EVENT_ONLINE, memberIsOnline);
        });

        afterEach(() => {
            emitter.removeAllListeners();
        });

        it('Не должен упасть на удалении подписок несуществующего события', () => {
            const EVENT_UNKNOWN = '123';
            expect(() => {
                emitter.removeAllListeners(EVENT_UNKNOWN);
            }).not.toThrow();
        });
    });

    describe('#removeListener', () => {
        beforeEach(() => {
            emitter.addListener(EVENT_NEW_MESSAGE, printNewMessage);
            emitter.addListener(EVENT_NEW_MESSAGE, notifyNewMessage);
            emitter.addListener(EVENT_NEW_MESSAGE, printNewMessage);
            emitter.addListener(EVENT_NEW_MEMBER, greetNewMember);
            emitter.addListener(EVENT_NEW_MEMBER, kickNewMember);
            emitter.addListener(EVENT_NEW_MEMBER, memberIsOnline);
            emitter.addListener(EVENT_ONLINE, memberIsOnline);
        });

        afterEach(() => {
            emitter.removeAllListeners();
        });

        it('Не должен упасть при удалении несуществующей подписки из события', () => {
            const unknownEventHandler = () => 0;
            expect(() => {
                emitter.removeListener(EVENT_NEW_MEMBER, unknownEventHandler);
            }).not.toThrow();
        });

        it('Не должен упасть при удалении подписки из несуществующего события', () => {
            const EVENT_UNKNOWN = '123';
            expect(() => {
                emitter.removeListener(EVENT_UNKNOWN, printNewMessage);
            }).not.toThrow();
        });
    });

    describe('#emit', () => {
        beforeEach(() => {
            emitter.addListener(EVENT_NEW_MESSAGE, printNewMessage);
            emitter.addListener(EVENT_NEW_MESSAGE, printNewMessage);
            emitter.addListener(EVENT_NEW_MEMBER, greetNewMember);
            emitter.addListener(EVENT_NEW_MEMBER, kickNewMember);
            emitter.addListener(EVENT_ONLINE, memberIsOnline);
        });

        afterEach(() => {
            emitter.removeAllListeners();
        });

        it('Не должен упасть при эмитте несуществующего события', () => {
            const UNKNOWN_EVENT = '123';
            expect(() => {
                emitter.emit(UNKNOWN_EVENT);
            }).not.toThrow();
        });

        it('Должен выполнить добавленный к событию EVENT_ONLINE листенер', () => {
            emitter.emit(EVENT_ONLINE, 'user1');
            expect(memberIsOnline).toBeCalled();
        });

        it('Должен выполнить все добавленные к событию EVENT_NEW_MEMBER листенеры', () => {
            emitter.emit(EVENT_NEW_MEMBER, 'user1');
            expect(greetNewMember).toBeCalled();
            expect(kickNewMember).toBeCalled();
        });

        it('Должен дважды подписанный к событию EVENT_NEW_MEMBER листенер выполнить дважды', () => {
            emitter.emit(EVENT_NEW_MESSAGE, 'user1');
            expect(printNewMessage).toBeCalledTimes(2);
        });

        it('Должен выполнять подписанный на событие листенер без указанных аргументов', () => {
            emitter.emit(EVENT_ONLINE);
            expect(memberIsOnline).toHaveBeenCalledWith();
        });

        it('Должен выполнять подписанный на событие листенер с указанным аргументом', () => {
            emitter.emit(EVENT_ONLINE, 'user1');
            expect(memberIsOnline).toHaveBeenCalledWith('user1');
        });

        it('Должен выполнять подписанный на событие листенер с указанными аргументами', () => {
            emitter.emit(EVENT_NEW_MESSAGE, 'user1', 'Hello!');
            expect(printNewMessage).toHaveBeenCalledWith('user1', 'Hello!');
        });
    });
});
