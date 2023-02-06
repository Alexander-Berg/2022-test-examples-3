package ru.yandex.market.aa.easy.kmax;

/**
 * @author antipov93.
 */
public class KMaxPriorityQueueTest extends KMaxTest {

    @Override
    protected KMax solution() {
        return new KMaxPriorityQueue();
    }
}
