package ru.yandex.market.common.test.db;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.support.DefaultTestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.market.common.test.db.DbUnitTestExecutionListener.collectAnnotations;

class DbUnitTestExecutionListenerTest {
    static class Case1 {
        @DbUnitDataSet(schema = "s")
        static class Base {
            void x() {
            }
        }
    }

    @Test
    void case1() throws NoSuchMethodException {
        TestContext testContext = makeTestContext(Case1.Base.class);
        Map<String, List<DbUnitDataSet>> groupedDatasets = collectAnnotations(testContext, DbUnitDataSet.class);
        assertThat(groupedDatasets)
                .containsOnlyKeys("dataSource")
                .hasEntrySatisfying("dataSource", annotations -> {
                    assertThat(annotations).hasSize(1);
                    assertThat(DbUnitTestExecutionListener.getSchema(annotations)).isEqualTo("s");
                    assertThat(DbUnitTestExecutionListener.shouldTruncateAllTables(annotations, Collections.emptyList())).isTrue();
                });
    }

    static class Case2 {
        @DbUnitDataSet(schema = "s", dataSource = "d")
        static class Base {
        }

        @DbUnitDataSet(truncateAllTables = false, dataSource = "d")
        static class Derived extends Base {
            void x() {
            }
        }
    }

    @Test
    void case2() throws NoSuchMethodException {
        TestContext testContext = makeTestContext(Case2.Derived.class);
        Map<String, List<DbUnitDataSet>> groupedDatasets = collectAnnotations(testContext, DbUnitDataSet.class);
        assertThat(groupedDatasets)
                .containsOnlyKeys("d")
                .hasEntrySatisfying("d", annotations -> {
                    assertThat(annotations).hasSize(2);
                    assertThat(DbUnitTestExecutionListener.getSchema(annotations)).isEqualTo("s");
                    assertThat(DbUnitTestExecutionListener.shouldTruncateAllTables(annotations, Collections.emptyList())).isFalse();
                });
    }

    static class Case3 {
        @DbUnitDataSet
        static class Base {
        }

        @DbUnitDataSet(truncateAllTables = false)
        static class Derived extends Base {
            @DbUnitDataSet(schema = "s", nonTruncatedTables = {"t1", "t2"})
            void x() {
            }
        }
    }

    @Test
    void case3() throws NoSuchMethodException {
        TestContext testContext = makeTestContext(Case3.Derived.class);
        Map<String, List<DbUnitDataSet>> groupedDatasets = collectAnnotations(testContext, DbUnitDataSet.class);
        assertThat(groupedDatasets)
                .containsOnlyKeys("dataSource")
                .hasEntrySatisfying("dataSource", annotations -> {
                    assertThat(annotations).hasSize(3);
                    assertThat(DbUnitTestExecutionListener.getSchema(annotations)).isEqualTo("s");
                    assertThat(DbUnitTestExecutionListener.shouldTruncateAllTables(annotations, Collections.emptyList())).isTrue();
                });
    }

    static class Case4 {
        @DbUnitDataSet
        @DbUnitTruncatePolicy(truncateType = TruncateType.NOT_TRUNCATE)
        static class Base {
        }

        @DbUnitDataSet(truncateAllTables = false)
        static class Derived extends Base {
            @DbUnitDataSet(schema = "s", nonTruncatedTables = {"t1", "t2"})
            void x() {
            }
        }
    }

    @Test
    void case4() throws NoSuchMethodException {
        TestContext testContext = makeTestContext(Case4.Derived.class);
        Map<String, List<DbUnitDataSet>> groupedDatasets = collectAnnotations(testContext, DbUnitDataSet.class);
        Map<String, List<DbUnitTruncatePolicy>> truncate = collectAnnotations(testContext, DbUnitTruncatePolicy.class);
        assertThat(groupedDatasets)
                .containsOnlyKeys("dataSource")
                .hasEntrySatisfying("dataSource", annotations -> {
                    List<DbUnitTruncatePolicy> truncatePolicies = truncate.get("dataSource");
                    assertThat(annotations).hasSize(3);
                    assertThat(DbUnitTestExecutionListener.getSchema(annotations)).isEqualTo("s");
                    assertThat(DbUnitTestExecutionListener.shouldTruncateAllTables(annotations, truncatePolicies)).isFalse();
                });
    }

    static class Case5 {
        @DbUnitDataSet
        @DbUnitTruncatePolicy(truncateType = TruncateType.TRUNCATE)
        static class Base {
        }

        @DbUnitDataSet(truncateAllTables = false)
        static class Derived extends Base {
            @DbUnitDataSet(schema = "s", nonTruncatedTables = {"t1", "t2"})
            void x() {
            }
        }
    }

    @Test
    void case5() throws NoSuchMethodException {
        TestContext testContext = makeTestContext(Case5.Derived.class);
        Map<String, List<DbUnitDataSet>> groupedDatasets = collectAnnotations(testContext, DbUnitDataSet.class);
        Map<String, List<DbUnitTruncatePolicy>> truncate = collectAnnotations(testContext, DbUnitTruncatePolicy.class);
        assertThat(groupedDatasets)
                .containsOnlyKeys("dataSource")
                .hasEntrySatisfying("dataSource", annotations -> {
                    List<DbUnitTruncatePolicy> truncatePolicies = truncate.get("dataSource");
                    assertThat(annotations).hasSize(3);
                    assertThat(DbUnitTestExecutionListener.getSchema(annotations)).isEqualTo("s");
                    assertThat(DbUnitTestExecutionListener.shouldTruncateAllTables(annotations, truncatePolicies)).isTrue();
                });
    }

    static class Case6 {
        @DbUnitDataSet
        @DbUnitTruncatePolicy(truncateType = TruncateType.INHERITANCE)
        static class Base {
        }

        @DbUnitDataSet(truncateAllTables = false)
        @DbUnitTruncatePolicy(truncateType = TruncateType.TRUNCATE)
        static class Derived extends Base {
            @DbUnitDataSet(schema = "s", nonTruncatedTables = {"t1", "t2"})
            void x() {
            }
        }
    }

    @Test
    void case6() throws NoSuchMethodException {
        TestContext testContext = makeTestContext(Case6.Derived.class);
        Map<String, List<DbUnitDataSet>> groupedDatasets = collectAnnotations(testContext, DbUnitDataSet.class);
        Map<String, List<DbUnitTruncatePolicy>> truncate = collectAnnotations(testContext, DbUnitTruncatePolicy.class);
        assertThat(groupedDatasets)
                .containsOnlyKeys("dataSource")
                .hasEntrySatisfying("dataSource", annotations -> {
                    List<DbUnitTruncatePolicy> truncatePolicies = truncate.get("dataSource");
                    assertThat(annotations).hasSize(3);
                    assertThat(DbUnitTestExecutionListener.getSchema(annotations)).isEqualTo("s");
                    assertThat(DbUnitTestExecutionListener.shouldTruncateAllTables(annotations, truncatePolicies)).isTrue();
                });
    }

    static class Case7 {
        @DbUnitDataSet
        @DbUnitTruncatePolicy(truncateType = TruncateType.INHERITANCE)
        static class Base {
        }

        @DbUnitDataSet(truncateAllTables = false)
        @DbUnitTruncatePolicy(truncateType = TruncateType.NOT_TRUNCATE)
        static class Derived extends Base {
            @DbUnitDataSet(schema = "s", nonTruncatedTables = {"t1", "t2"})
            void x() {
            }
        }
    }

    @Test
    void case7() throws NoSuchMethodException {
        TestContext testContext = makeTestContext(Case7.Derived.class);
        Map<String, List<DbUnitDataSet>> groupedDatasets = collectAnnotations(testContext, DbUnitDataSet.class);
        Map<String, List<DbUnitTruncatePolicy>> truncate = collectAnnotations(testContext, DbUnitTruncatePolicy.class);
        assertThat(groupedDatasets)
                .containsOnlyKeys("dataSource")
                .hasEntrySatisfying("dataSource", annotations -> {
                    List<DbUnitTruncatePolicy> truncatePolicies = truncate.get("dataSource");
                    assertThat(annotations).hasSize(3);
                    assertThat(DbUnitTestExecutionListener.getSchema(annotations)).isEqualTo("s");
                    assertThat(DbUnitTestExecutionListener.shouldTruncateAllTables(annotations, truncatePolicies)).isFalse();
                });
    }

    static class Case8 {
        @DbUnitDataSet
        @DbUnitTruncatePolicy(truncateType = TruncateType.TRUNCATE)
        static class Base {
        }

        @DbUnitDataSet(truncateAllTables = false)
        @DbUnitTruncatePolicy(truncateType = TruncateType.NOT_TRUNCATE)
        static class Derived extends Base {
            @DbUnitDataSet(schema = "s", nonTruncatedTables = {"t1", "t2"})
            void x() {
            }
        }
    }

    @Test
    void case8() throws NoSuchMethodException {
        TestContext testContext = makeTestContext(Case8.Derived.class);
        Map<String, List<DbUnitDataSet>> groupedDatasets = collectAnnotations(testContext, DbUnitDataSet.class);
        Map<String, List<DbUnitTruncatePolicy>> truncate = collectAnnotations(testContext, DbUnitTruncatePolicy.class);
        assertThat(groupedDatasets)
                .containsOnlyKeys("dataSource")
                .hasEntrySatisfying("dataSource", annotations -> {
                    List<DbUnitTruncatePolicy> truncatePolicies = truncate.get("dataSource");
                    assertThat(annotations).hasSize(3);
                    assertThat(DbUnitTestExecutionListener.getSchema(annotations)).isEqualTo("s");
                    assertThatCode(() -> DbUnitTestExecutionListener.shouldTruncateAllTables(annotations, truncatePolicies))
                            .hasMessageContaining("Test methods contains both TRUNCATE and NOT_TRUNCATE, choose only one");
                });
    }

    static TestContext makeTestContext(Class<?> testClass) throws NoSuchMethodException {
        TestContext tc = new DefaultTestContext(
                testClass,
                new MergedContextConfiguration(testClass, null, null, null, null),
                new DefaultCacheAwareContextLoaderDelegate()
        );
        tc.updateState(null, testClass.getDeclaredMethod("x"), null);
        return tc;
    }
}
