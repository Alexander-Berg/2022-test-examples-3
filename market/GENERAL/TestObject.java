package ru.yandex.ir.stub;

import ru.yandex.ir.common.be.RankingRequest;
import ru.yandex.ir.common.knowledge.Element;

import java.util.Collections;
import java.util.List;

/**
 * @author nkondratyeva
 */
public class TestObject implements Element, RankingRequest<Object> {

    private long id;

    public TestObject() {
    }

    public TestObject(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int getLabel() {
        return 0;
    }

    @Override
    public long getQueryId() {
        return id;
    }

    @Override
    public long getDocId() {
        return id;
    }

    @Override
    public String[] getElementAuxInfo() {
        return new String[]{Long.toString(id)};
    }

    @Override
    public List<Object> getDocuments() {
        return Collections.singletonList(new Object());
    }

    @Override
    public int documentsSize() {
        return 1;
    }
}
