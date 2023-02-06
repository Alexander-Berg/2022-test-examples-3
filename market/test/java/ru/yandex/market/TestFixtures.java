package ru.yandex.market;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.util.VMSupport;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 4/2/15
 * Time: 5:03 PM
 */
public class TestFixtures {

    static {
        System.out.println(VMSupport.vmDetails());
    }

    public static void footprint(Object obj, Table table) {
        ClassLayout classLayout = ClassLayout.parseClass(obj.getClass());
        System.out.println(classLayout.toPrintable(obj));
        table.fmt(obj.getClass().getSimpleName(),
                VMSupport.tryExactObjectSize(obj, classLayout).instanceSize(),
                GraphLayout.parseInstance(obj).totalSize());
    }

    public static class Table {
        private final String fmt;
        private final int length;
        private final StringBuffer buffer;

        public Table(String name, Column... columns) {
            this.buffer = new StringBuffer();
            this.fmt = fmt(columns, false);
            String header = String.format(fmt(columns, true), Stream.of(columns).map(c -> c.header).toArray());
            this.length = header.length();
            header(name, header);
        }

        private String fmt(Column[] columns, boolean header) {
            StringBuilder sb = new StringBuilder("|");
            for (Column column : columns) {
                sb.append("%").append(column.size).append(!header && column.numeric ? "d" : "s").append("|");
            }
            return sb.append("\n").toString();
        }

        private void header(String name, String header) {
            int prefixCount = (header.length() - name.length()) / 2;
            int suffixCount = header.length() - name.length() - prefixCount;
            buffer.append(StringUtils.repeat('=', prefixCount)).append(name).
                    append(StringUtils.repeat('=', suffixCount)).append("\n");
            buffer.append(header);
            breakLine();
        }

        public void breakLine() {
            buffer.append(StringUtils.repeat('-', length)).append("\n");
        }

        public String toString() {
            return buffer.toString();
        }

        private void fmt(Object... obj) {
            buffer.append(String.format(fmt, obj));
        }
    }

    public static class Column {
        public final String header;
        public final int size;
        public final boolean numeric;

        public Column(String header, int size, boolean numeric) {
            this.header = header;
            this.size = size;
            this.numeric = numeric;
        }
    }
}