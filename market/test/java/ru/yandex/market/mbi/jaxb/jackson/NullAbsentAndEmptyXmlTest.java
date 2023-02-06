package ru.yandex.market.mbi.jaxb.jackson;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class NullAbsentAndEmptyXmlTest {
    private static final ObjectMapper MAPPER = new ApiObjectMapperFactory().createXmlMapper(new XmlNamingStrategy());

    @Test
    public void testNulls() throws JsonProcessingException {
        TestValue value = new TestValue.Builder().setInts(null)
                .setLongs(null)
                .setDoubles(null)
                .setStrings(null)
                .setIntList(null)
                .setLongList(null)
                .setDoubleList(null)
                .setStringList(null)
                .setOptionalInt(null)
                .setOptionalLong(null)
                .setOptionalDouble(null)
                .setOptionalString(null)
                .setIntValue(null)
                .setLongValue(null)
                .setDoubleValue(null)
                .setStringValue(null)
                .build();
        String expected = "<test/>";
        Assert.assertEquals(expected, MAPPER.writeValueAsString(value));
    }

    @Test
    public void testEmpty() throws JsonProcessingException {
        TestValue value = new TestValue.Builder()
                .setInts(new int[] {})
                .setLongs(new long[] {})
                .setDoubles(new double[] {})
                .setStrings(new String[] {})
                .setIntList(Collections.emptyList())
                .setLongList(Collections.emptyList())
                .setDoubleList(Collections.emptyList())
                .setStringList(Collections.emptyList())
                .setOptionalInt(OptionalInt.empty())
                .setOptionalLong(OptionalLong.empty())
                .setOptionalDouble(OptionalDouble.empty())
                .setOptionalString(Optional.empty())
                .setIntValue(null)
                .setLongValue(null)
                .setDoubleValue(null)
                .setStringValue(null)
                .build();
        String expected = "<test><ints/><longs/><doubles/><strings/><int-list-elements/><long-list-elements/><double-list-elements/><string-list-elements/></test>";
        Assert.assertEquals(expected, MAPPER.writeValueAsString(value));
    }

    @Test
    public void testSingleValue() throws JsonProcessingException {
        TestValue value = new TestValue.Builder()
                .setInts(new int[] {1})
                .setLongs(new long[] {2})
                .setDoubles(new double[] {3})
                .setStrings(new String[] {"4"})
                .setIntList(Collections.singletonList(5))
                .setLongList(Collections.singletonList(6L))
                .setDoubleList(Collections.singletonList(7.0))
                .setStringList(Collections.singletonList("8"))
                .setOptionalInt(OptionalInt.of(9))
                .setOptionalLong(OptionalLong.of(10))
                .setOptionalDouble(OptionalDouble.of(11))
                .setOptionalString(Optional.of("12"))
                .setIntValue(13)
                .setLongValue(14L)
                .setDoubleValue(15.0)
                .setStringValue("16")
                .build();
        String expected = "<test><ints><int>1</int></ints><longs><long>2</long></longs><doubles><double>3.0</double></doubles><strings><string>4</string></strings><int-list-elements><int-list-element>5</int-list-element></int-list-elements><long-list-elements><long-list-element>6</long-list-element></long-list-elements><double-list-elements><double-list-element>7.0</double-list-element></double-list-elements><string-list-elements><string-list-element>8</string-list-element></string-list-elements><optional-int>9</optional-int><optional-long>10</optional-long><optional-double>11.0</optional-double><optional-string>12</optional-string><int-value>13</int-value><long-value>14</long-value><double-value>15.0</double-value><string-value>16</string-value></test>";
        Assert.assertEquals(expected, MAPPER.writeValueAsString(value));
    }

    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "test")
    public static class TestValue {
        @XmlElementWrapper(name = "ints")
        @XmlElement(name = "int")
        private final int[] ints;

        @XmlElementWrapper(name = "longs")
        @XmlElement(name = "long")
        private final long[] longs;

        @XmlElementWrapper(name = "doubles")
        @XmlElement(name = "double")
        private final double[] doubles;

        @XmlElementWrapper(name = "strings")
        @XmlElement(name = "string")
        private final String[] strings;

        @XmlElementWrapper(name = "intListElements")
        @XmlElement(name = "intListElement")
        private final List<Integer> intList;

        @XmlElementWrapper(name = "longListElements")
        @XmlElement(name = "longListElement")
        private final List<Long> longList;

        @XmlElementWrapper(name = "doubleListElements")
        @XmlElement(name = "doubleListElement")
        private final List<Double> doubleList;

        @XmlElementWrapper(name = "stringListElements")
        @XmlElement(name = "stringListElement")
        private final List<String> stringList;

        @XmlElement(name = "optionalInt")
        private final OptionalInt optionalInt;

        @XmlElement(name = "optionalLong")
        private final OptionalLong optionalLong;

        @XmlElement(name = "optionalDouble")
        private final OptionalDouble optionalDouble;

        @XmlElement(name = "optionalString")
        private final Optional<String> optionalString;

        @XmlElement(name = "intValue")
        private final Integer intValue;

        @XmlElement(name = "longValue")
        private final Long longValue;

        @XmlElement(name = "doubleValue")
        private final Double doubleValue;

        @XmlElement(name = "stringValue")
        private final String stringValue;

        public TestValue(Builder builder) {
            this.ints = builder.ints;
            this.longs = builder.longs;
            this.doubles = builder.doubles;
            this.strings = builder.strings;
            this.intList = builder.intList;
            this.longList = builder.longList;
            this.doubleList = builder.doubleList;
            this.stringList = builder.stringList;
            this.optionalInt = builder.optionalInt;
            this.optionalLong = builder.optionalLong;
            this.optionalDouble = builder.optionalDouble;
            this.optionalString = builder.optionalString;
            this.intValue = builder.intValue;
            this.longValue = builder.longValue;
            this.doubleValue = builder.doubleValue;
            this.stringValue = builder.stringValue;
        }

        public static class Builder {

            private int[] ints;
            private long[] longs;
            private double[] doubles;
            private String[] strings;
            private List<Integer> intList;
            private List<Long> longList;
            private List<Double> doubleList;
            private List<String> stringList;
            private OptionalInt optionalInt;
            private OptionalLong optionalLong;
            private OptionalDouble optionalDouble;
            private Optional<String> optionalString;
            private Integer intValue;
            private Long longValue;
            private Double doubleValue;
            private String stringValue;

            public Builder setInts(int[] ints) {
                this.ints = ints;
                return this;
            }

            public Builder setLongs(long[] longs) {
                this.longs = longs;
                return this;
            }

            public Builder setDoubles(double[] doubles) {
                this.doubles = doubles;
                return this;
            }

            public Builder setStrings(String[] strings) {
                this.strings = strings;
                return this;
            }

            public Builder setIntList(List<Integer> intList) {
                this.intList = intList;
                return this;
            }

            public Builder setLongList(List<Long> longList) {
                this.longList = longList;
                return this;
            }

            public Builder setDoubleList(List<Double> doubleList) {
                this.doubleList = doubleList;
                return this;
            }

            public Builder setStringList(List<String> stringList) {
                this.stringList = stringList;
                return this;
            }

            public Builder setOptionalInt(OptionalInt optionalInt) {
                this.optionalInt = optionalInt;
                return this;
            }

            public Builder setOptionalLong(OptionalLong optionalLong) {
                this.optionalLong = optionalLong;
                return this;
            }

            public Builder setOptionalDouble(OptionalDouble optionalDouble) {
                this.optionalDouble = optionalDouble;
                return this;
            }

            public Builder setOptionalString(Optional<String> optionalString) {
                this.optionalString = optionalString;
                return this;
            }

            public Builder setIntValue(Integer intValue) {
                this.intValue = intValue;
                return this;
            }

            public Builder setLongValue(Long longValue) {
                this.longValue = longValue;
                return this;
            }

            public Builder setDoubleValue(Double doubleValue) {
                this.doubleValue = doubleValue;
                return this;
            }

            public Builder setStringValue(String stringValue) {
                this.stringValue = stringValue;
                return this;
            }

            public TestValue build() {
                return new TestValue(this);
            }
        }
    }
}
