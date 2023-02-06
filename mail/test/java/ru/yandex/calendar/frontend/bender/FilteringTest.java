package ru.yandex.calendar.frontend.bender;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.bender.Bender;
import ru.yandex.misc.bender.annotation.BenderBindAllFields;
import ru.yandex.misc.bender.annotation.BenderFlatten;
import ru.yandex.misc.bender.config.BenderConfiguration;
import ru.yandex.misc.bender.serialize.BenderJsonSerializer;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class FilteringTest extends CalendarTestBase {

    @BenderBindAllFields
    public static class Root {
        public boolean includedField;
        public boolean omittedField;

        public ListF<FilterableElement> filteredList;
        public ListF<Element> includedList;
        public ListF<Element> omittedList;

        public Option<FilterableElement> filteredOption;
        public Option<Element> includedOption;
        public Option<Element> omittedOption;

        @BenderFlatten
        public FlattedObject flatten;
    }

    @BenderBindAllFields
    public static class FilterableElement {
        public String included;
        public String omitted;

        public FilterableElement(String included, String omitted) {
            this.included = included;
            this.omitted = omitted;
        }
    }

    @BenderBindAllFields
    public static class Element {
        public int a;
        public int b;

        public Element(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    @BenderBindAllFields
    public static class FlattedObject {
        public int flattenIncluded;
        public int flattenOmitted;
    }

    @Test
    public void filter() {
        FilterableElement f1 = new FilterableElement("included", "omitted");
        FilterableElement f2 = new FilterableElement("xxx", "yyy");

        Element e1 = new Element(7, 7);
        Element e2 = new Element(9, 10);

        Root root = new Root();
        root.includedField = true;
        root.omittedField = false;

        root.filteredList = Cf.list(f1, f2);
        root.includedList = Cf.list(e1, e2);
        root.omittedList = Cf.list(e1, e2);

        root.filteredOption = Option.of(f1);
        root.includedOption = Option.of(e1);
        root.omittedOption = Option.of(e2);

        root.flatten = new FlattedObject();
        root.flatten.flattenIncluded = 1;
        root.flatten.flattenOmitted = 0;

        ListF<String> fields = Cf.list("includedField",
                "filteredList/included", "includedList",
                "filteredOption/included", "includedOption", "flattenIncluded");

        BenderJsonSerializer<FilterablePojo> serializer = Bender.jsonSerializer(
                FilterablePojo.class, FilterablePojoBenderConfiguration.extend(BenderConfiguration.defaultConfiguration()));

        Assert.equals("{" +
                "\"includedField\":true," +
                "\"filteredList\":[{\"included\":\"included\"},{\"included\":\"xxx\"}]," +
                "\"includedList\":[{\"a\":7,\"b\":7},{\"a\":9,\"b\":10}]," +
                "\"filteredOption\":{\"included\":\"included\"}," +
                "\"includedOption\":{\"a\":7,\"b\":7}," +
                "\"flattenIncluded\":1" +
                "}", new String(serializer.serializeJson(FilterablePojo.wrap(root, fields))));
    }
}
