package ru.yandex.market.pers.author.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.pers.author.takeout.TakeoutHelper;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.11.2021
 */
public class TakeoutHelperBuffer implements TakeoutHelper {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(out);
    }

    @Override
    public ContentProvider getContentProvider() {
        return new StreamContentProvider(new ByteArrayInputStream(out.toByteArray()));
    }

    @Override
    public void close() {
        //do nothing
    }
}
