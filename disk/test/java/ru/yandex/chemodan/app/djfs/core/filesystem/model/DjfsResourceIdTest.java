package ru.yandex.chemodan.app.djfs.core.filesystem.model;

import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.ActionContext;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.exception.InvalidClientInputDjfsResourceIdException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.exception.InvalidDjfsResourceIdException;
import ru.yandex.misc.test.Assert;

public class DjfsResourceIdTest {
    @Test
    public void consCorrectDjfsResourceId() {
        String raw = "1234:0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9aed0366b986235b3c";
        DjfsResourceId resourceId = DjfsResourceId.cons(raw);
        Assert.equals(resourceId.getUid().toString(), "1234");
        Assert.equals(resourceId.getFileId().getValue(),
                "0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9aed0366b986235b3c");
        Assert.equals(resourceId.toString(), raw);
    }

    @Test(expected = InvalidDjfsResourceIdException.class)
    public void exceptionOnWrongUid() {
        DjfsResourceId.cons("abc:0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9aed0366b986235b3");
    }

    @Test(expected = InvalidDjfsResourceIdException.class)
    public void exceptionOnWrongFileIdTooShort() {
        DjfsResourceId.cons("1234:0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9aed0366b986235b3");
    }

    @Test(expected = InvalidDjfsResourceIdException.class)
    public void exceptionOnWrongFileIdTooLong() {
        DjfsResourceId.cons("1234:0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9aed0366b986235b3cc");
    }

    @Test(expected = InvalidDjfsResourceIdException.class)
    public void exceptionOnMissingSeparator() {
        DjfsResourceId.cons("1234-0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9aed0366b986235b3c");
    }

    @Test(expected = InvalidDjfsResourceIdException.class)
    public void exceptionOnExtraColonCharactersInFileId() {
        DjfsResourceId.cons("1234:0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9a:ed0366b986235b3");
    }

    @Test(expected = InvalidDjfsResourceIdException.class)
    public void exceptionOnWrongCharactersInFileId() {
        DjfsResourceId.cons("1234:0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9a?ed0366b986235b3");
    }

    @Test(expected = InvalidClientInputDjfsResourceIdException.class)
    public void clientInputExceptionForInvalidInput() {
        DjfsResourceId.cons("1234:0551efc2f4fc0576aab6662cf16922fdf8c2f71ca26d2e9a?ed0366b986235b3",
                ActionContext.CLIENT_INPUT);
    }
}
