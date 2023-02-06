package ru.yandex.market.checkerxservice.emias.erecipeservice.v1.types.jaxbcustom;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.checkerxservice.chekservice.emias.erecipeservice.v1.types.jaxbcustom.XMLGregorianCalendarAdapter;

public class XMLGregorianCalendarAdapterTest {
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private final XMLGregorianCalendarAdapter XML_ADAPTER = new XMLGregorianCalendarAdapter();

    @Test
    public void unmarshal() {
        final String DATE_STR = "2022-01-24";

        XMLGregorianCalendar expected = xmlDateFromStr(DATE_STR);

        XMLGregorianCalendar result = null;
        try {
            result = XML_ADAPTER.unmarshal(DATE_STR);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertEquals(expected, result);
    }

    @Test
    public void unmarshalNull() {
        final String DATE_STR = null;

        XMLGregorianCalendar result = null;
        try {
            result = XML_ADAPTER.unmarshal(DATE_STR);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertNull(result);
    }

    @Test
    public void marshal() {
        final String EXPECTED = "2022-01-24";

        XMLGregorianCalendar xmlDate = xmlDateFromStr(EXPECTED);
        String result = null;
        try {
            result = XML_ADAPTER.marshal(xmlDate);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertEquals(EXPECTED, result);
    }

    @Test
    public void marshalNull() {
        String result = null;
        try {
            result = XML_ADAPTER.marshal(null);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertNull(result);
    }

    @Test
    public void unmarshalNegative() {
        final String DATE_STR = "2022-01-32";

        Assertions.assertThatThrownBy(() -> XML_ADAPTER.unmarshal(DATE_STR))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unable convert string to Date: 2022-01-32");
    }

    private XMLGregorianCalendar xmlDateFromStr(String dateStr) {
        GregorianCalendar cal = new GregorianCalendar();
        Date date;
        try {
            date = format.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable convert string to Date: " + dateStr);
        }
        cal.setTime(date);

        XMLGregorianCalendar xmlGregCal;
        try {
            xmlGregCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to create calendar Date from string: " + dateStr);
        }
        return xmlGregCal;
    }
}
