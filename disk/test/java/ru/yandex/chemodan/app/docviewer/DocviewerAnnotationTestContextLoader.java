package ru.yandex.chemodan.app.docviewer;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author akirakozov
 */
public class DocviewerAnnotationTestContextLoader extends AnnotationConfigContextLoader {

    @Override
    protected void prepareContext(GenericApplicationContext context) {
        super.prepareContext(context);
        Configuration.loadTestsProperties();
    }

}
