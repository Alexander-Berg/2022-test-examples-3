package ru.yandex.autotests.testpers.misc.filters;

import ru.yandex.qatools.pagediffer.document.XmlDocument;
import ru.yandex.qatools.pagediffer.document.filter.DocumentFilter;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

import static ru.yandex.qatools.pagediffer.utils.PageDifferUtils.applyXslTransformation;

/**
 * User: lanwen
 * Date: 08.05.15
 * Time: 19:05
 */
public class SortFilter<T extends XmlDocument> implements DocumentFilter<T> {

    @Override
    public void apply(T document) {
        String transformedDocumentBody = applyXslTransformation(
                new StreamSource(getClass().getClassLoader().getResourceAsStream("xsl/sort-name.xsl")),
                new StreamSource(new StringReader(document.getDocumentBody())));

        String transformedDocumentBody2 = applyXslTransformation(
                new StreamSource(getClass().getClassLoader().getResourceAsStream("xsl/sort-fid.xsl")),
                new StreamSource(new StringReader(transformedDocumentBody)));

//        String transformedDocumentBody3 = applyXslTransformation(
//                new StreamSource(getClass().getClassLoader().getResourceAsStream("xsl/sort-lid.xsl")),
//                new StreamSource(new StringReader(transformedDocumentBody2)));

        document.setDocumentBody(transformedDocumentBody2);
    }

}
