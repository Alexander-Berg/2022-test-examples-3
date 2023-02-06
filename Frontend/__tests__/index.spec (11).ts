import pubsub from '..';
import pubsub2 from '../index'; // иначе ts-lint ругается

test('Возвращает один и тот же инстанс на каждый запрос', () => {
    expect(pubsub === pubsub2).toBe(true);
});

test('window содержит ссылку на pubsub', () => {
    // @ts-ignore
    expect(pubsub === window.Ya.pubsub).toBe(true);
});
