package ru.yandex.chemodan.app.djfs.core.filesystem.model;

import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.ActionContext;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.exception.InvalidClientInputDjfsFileIdException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.exception.InvalidDjfsFileIdException;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class DjfsFileIdTest {
    @Test
    public void cons() {
        DjfsFileId fileId = DjfsFileId.cons("0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9aed0366b986235b3c");
        Assert.equals(fileId.getValue(), "0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9aed0366b986235b3c");
    }

    @Test
    public void random() {
        DjfsFileId.random();
    }

    @Test(expected = InvalidDjfsFileIdException.class)
    public void exceptionOnShortFileId() {
        DjfsFileId.cons(StringUtils.repeat("a", 32));
    }

    @Test(expected = InvalidDjfsFileIdException.class)
    public void exceptionOnLongFileId() {
        DjfsFileId.cons(StringUtils.repeat("a", 96));
    }

    @Test(expected = InvalidDjfsFileIdException.class)
    public void exceptionOnWrongCharactersInFileId() {
        DjfsFileId.cons("0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9a?ed0366b986235b3");
    }

    @Test(expected = InvalidClientInputDjfsFileIdException.class)
    public void clientExceptionOnShortFileId() {
        DjfsFileId.cons(StringUtils.repeat("a", 32), ActionContext.CLIENT_INPUT);
    }
}
