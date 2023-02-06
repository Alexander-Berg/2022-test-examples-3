package ru.yandex.chemodan.test;

import java.io.IOException;

import ru.yandex.bolts.function.Function1V;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.IoUtils;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.random.Random2;

/**
 * @author akirakozov
 */
public class TestManager {

    public static void withEmptyTemporaryFile(String prefix, Function1V<File2> handler) {
        File2 file = File2.tmpDir().child(prefix + "_" + Random2.R.nextAlnum(8));
        try {
            handler.apply(file);
        } finally {
            file.safeDelete();
        }
    }

    public static void copyISStoFile2(InputStreamSource source, File2 a) {
        try {
            IoUtils.copy(source.getInput(), a.asOutputStreamTool().getOutput());
        } catch (IOException e) {
            throw IoUtils.translate(e);
        }
    }

}
