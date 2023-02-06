package ru.yandex.utils.jdbc2;

import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.*;
import static ru.yandex.utils.jdbc2.RowMapperBuilder.*;


public class RowMapperBuilderTest {
    @Test
    public void testConstructor() throws SQLException {
        ResultSet mockRs = createMock(ResultSet.class);
        expect(mockRs.getInt(1)).andReturn(11).anyTimes();
        expect(mockRs.getInt(2)).andReturn(12).anyTimes();
        expect(mockRs.getString(3)).andReturn("col3").anyTimes();
        expect(mockRs.getString(4)).andReturn("col4").anyTimes();
        replay(mockRs);

        RowMapper<?> mapper = constructor(A.class, intAt(1), intAt(2));
        assertEquals("A(Object, Object)", mapper.mapRow(mockRs, 1).toString());

        mapper = constructor(A.class, intAt(1));
        assertEquals("A(Integer)", mapper.mapRow(mockRs, 1).toString());

        mapper = constructor(A.class, stringAt(3), stringAt(4));
        assertEquals("A(Object, Object)", mapper.mapRow(mockRs, 1).toString());

        mapper = constructor(B.class, intAt(1), intAt(2));
        assertEquals("B(x=11, y=12)", mapper.mapRow(mockRs, 1).toString());
    }

    @Test
    public void testPriceStats() throws SQLException {
//        ResultSet mockRs = createMock(ResultSet.class);
//        expect(mockRs.getInt("model_id")).andReturn(1).anyTimes();
//        expect(mockRs.getDouble("max_price")).andReturn(1.1).anyTimes();
//        expect(mockRs.getDouble("min_price")).andReturn(1.2).anyTimes();
//        expect(mockRs.getDouble("avg_price")).andReturn(1.3).anyTimes();
//        expect(mockRs.getInt("count")).andReturn(5).anyTimes();
//        replay(mockRs);
//
//        RowMapper mapper = constructor(PriceStats.class,
//            intAt("model_id"), doubleAt("max_price"), doubleAt("min_price"), doubleAt("avg_price"), intAt("count"));
//
//        assertEquals("PriceStats[modelId=1 min=1.200 max=1.100 avg=1.300 count=5]", mapper.mapRow(mockRs, 1).toString());

    }

    public static class A {
        private final String m;

        public A() {
            m = "A()";
        }

        public A(int i) {
            m = "A(int)";
        }

        public A(Integer i) {
            m = "A(Integer)";
        }

        public A(Object o) {
            m = "A(Object)";
        }

        public A(Object o1, Object o2) {
            m = "A(Object, Object)";
        }

        @Override
        public String toString() {
            return m;
        }
    }

    public static class B {
        private final String m;

        public B(int x, int y) {
            m = "B(x=" + x + ", y=" + y + ")";
        }

        @Override
        public String toString() {
            return m;
        }
    }

    public static class PriceStats {
        public final int modelId;
        public final double maxPrice;
        public final double minPrice;
        public final double avgPrice;
        public final int count;

        public PriceStats(int modelId, double maxPrice, double minPrice, double avgPrice, int count) {
            this.modelId = modelId;
            this.maxPrice = maxPrice;
            this.minPrice = minPrice;
            this.avgPrice = avgPrice;
            this.count = count;
        }

        @Override
        public String toString() {
            return String.format(
                "PriceStats[modelId=%d min=%.3f max=%.3f avg=%.3f count=%d]",
                modelId, minPrice, maxPrice, avgPrice, count);
        }
    }
}
