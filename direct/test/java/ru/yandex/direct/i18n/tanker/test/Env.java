package ru.yandex.direct.i18n.tanker.test;


import ru.yandex.direct.i18n.tanker.Branch;
import ru.yandex.direct.i18n.tanker.Tanker;
import ru.yandex.direct.i18n.tanker.TankerWithBranch;

/**
 * Связка Tanker и branch.
 * <p>
 * Нужно для replay через mockserver. В обычном режиме и в режиме записи
 * имя бранча генерируется случайно, а в режиме воспроизведения подставляется
 * из replay-лога.
 */
public interface Env extends AutoCloseable {
    Branch getBranch();

    Tanker getTanker();

    TankerWithBranch getTankerWithBranch();
}
