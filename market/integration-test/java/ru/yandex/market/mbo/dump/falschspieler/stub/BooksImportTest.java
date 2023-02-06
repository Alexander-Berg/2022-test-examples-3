package ru.yandex.market.mbo.dump.falschspieler.stub;

import org.junit.Test;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class BooksImportTest  extends AbstractDependencyInjectionSpringContextTests { 

    private BooksImport booksImport;

    protected String[] getConfigLocations() {
        return new String[]{
                "classpath:ru/yandex/local-books-import-test-config.xml",
        };
    }

    @Test
    public void test() {
        try {
            booksImport.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setBooksImport(final BooksImport booksImport) {
        this.booksImport = booksImport;
    }

}
