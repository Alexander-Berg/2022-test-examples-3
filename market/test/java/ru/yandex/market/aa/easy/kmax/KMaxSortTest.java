package ru.yandex.market.aa.easy.kmax;

/**
 * @author antipov93.
 */
class KMaxSortTest extends KMaxTest {

    @Override
    protected KMax solution() {
        return new KMaxSort();
    }
}