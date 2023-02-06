package ru.yandex.market.mbi.jaxb.jackson;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author zoom
 */
public class JacksonUsageSmokeTest {

    /**
     * Имя корневого элемента
     */
    @XmlRootElement(name = "response")
    public static class Response {

        /**
         * Без анотаций енумы нельзя сериализовать, так как в противном случае у нас во внешний мир просочатся
         * наименования энумов и в дальнейшем их имена будет невозможно поменять
         */
        @XmlEnum
        public enum Status {
            @XmlEnumValue("_OK_")
            OK,

            @XmlEnumValue("_ERROR_")
            ERROR
        }

        /**
         * Сериализация энумов
         */
        @XmlAttribute(name = "status")
        public Status status;

        /**
         * Это поле не будет сериализоваться, так как не имеет никаких анотаций. Сделано для того, чтобы во внешний мир
         * случайно не стали отдаваться непубличная информация
         */
        public Status invisibleStatus;

        /**
         * Сериализация объектов как элементов
         */
        @XmlElement(name = "message")
        public String message;

        /**
         * Массив примитивов
         */
        @XmlElement(name = "id")
        @XmlElementWrapper(name = "ids")
        public int[] ids;

        /**
         * Массив строк
         */
        @XmlElement(name = "key")
        @XmlElementWrapper(name = "keys")
        public List<String> keys;


        /**
         * Массив сложных объектов одного класс
         */
        @XmlElement(name = "item")
        @XmlElementWrapper(name = "items")
        public List<Item> items;


        /**
         * Массив объектов наследников одного класса.
         * <p>
         * Аннотация @XmlPolymorphicElement указывает, что при сериализации надо писать тип объекта, чтобы при
         * десериализации можно было корректно восстановить объект нужного класса
         */
        @XmlPolymorphicElement // Расширение Jackson'а
        @XmlElement(name = "item")
        @XmlElementWrapper(name = "polymorphicItems")
        public List<Item> polymorphicItems;

    }

    /**
     * Базовый класс
     * <p>
     * Аннотация @XmlSeeAlso указывает на наследников класса, чтобы при дессериализации можно было восстановить объект
     */
    @XmlSeeAlso({BigItem.class, SmallItem.class})
    @XmlType(name = "item")
    public static class Item {

        @XmlAttribute(name = "name")
        public String name;

        public Item(String name) {
            this.name = name;
        }
    }

    /**
     * Потомок базового класса
     * <p>
     * Аннотация @XmlType хранит имя типа объекта, используемое в десериализации для создания объекта корректного класса
     */
    @XmlType(name = "bigItem")
    public static class BigItem extends Item {

        @XmlElement(name = "bigString")
        public String bigString;

        public BigItem(String name, String bigString) {
            super(name);
            this.bigString = bigString;
        }
    }

    /**
     * Наследник {@link Item} с идентификатором типа "smallItem", который будет использоваться в атрибуте __type в
     * полиморфных коллекциях или объектах
     */
    @XmlType(name = "smallItem")
    public static class SmallItem extends Item {

        @XmlElement(name = "smallString")
        public String smallString;

        public SmallItem(String name, String smallString) {
            super(name);
            this.smallString = smallString;
        }
    }

    @Test
    public void testSmoke() throws JsonProcessingException {
        ApiObjectMapperFactory factory = new ApiObjectMapperFactory();

        // XmlNamingStrategy используется для переименования имен типа elementName в имена element-name
        ObjectMapper xmlMapper = factory.createXmlMapper(new XmlNamingStrategy());
        ObjectMapper jsonMapper = factory.createJsonMapper();

        // Инициализация объекта
        Response response = new Response();
        response.status = Response.Status.OK;
        response.message = "Hello, world!";
        response.ids = new int[]{3, 2, 1};
        response.keys = Arrays.asList("key_1", "key_2", "key_3");
        response.items = Arrays.asList(new Item("The first item"), new Item("The second item"));
        response.polymorphicItems =
                Arrays.asList(
                        new BigItem("Big item", "Very big string"),
                        new SmallItem("Small Item", "Very small string"));

        // Сериализация в XML
        assertThat(xmlMapper.writeValueAsString(response), notNullValue());

        // Сериализация в JSON
        assertThat(jsonMapper.writeValueAsString(response), notNullValue());
    }
}
