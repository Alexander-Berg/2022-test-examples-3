package customannotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author yuramalinov
 * @created 02.10.18
 */
@CustomControllerAnnotation(customValue = "/api")
public class CustomController {
    @RequestMapping("/call")
    public Result<Output> makeCall(@RequestParam int param) {
        return null;
    }

    @RequestMapping("/void")
    public Result<Void> makeVoid(@RequestParam int param) {
        return null;
    }

    public static class Output {
        private int intData;
        private long longData;
        private short shortData;
        private byte byteData;
        private char charData;
        private boolean booleanData;
        private Integer intDataBox;
        private Long longDataBox;
        private Short shortDataBox;
        private Byte byteDataBox;
        private Character charDataBox;
        private Boolean booleanDataBox;
        private String stringData;

        private List<Item> items = new ArrayList<>();
        private Set<Item> uniqueItems = new HashSet<>();
        private Map<String, Item> itemsByName = new HashMap<>();

        public int getIntData() {
            return intData;
        }

        public long getLongData() {
            return longData;
        }

        public short getShortData() {
            return shortData;
        }

        public byte getByteData() {
            return byteData;
        }

        public char getCharData() {
            return charData;
        }

        public boolean isBooleanData() {
            return booleanData;
        }

        public Integer getIntDataBox() {
            return intDataBox;
        }

        public Long getLongDataBox() {
            return longDataBox;
        }

        public Short getShortDataBox() {
            return shortDataBox;
        }

        public Byte getByteDataBox() {
            return byteDataBox;
        }

        public Character getCharDataBox() {
            return charDataBox;
        }

        public Boolean getBooleanDataBox() {
            return booleanDataBox;
        }

        public String getStringData() {
            return stringData;
        }

        public List<Item> getItems() {
            return items;
        }

        public Set<Item> getUniqueItems() {
            return uniqueItems;
        }

        public Map<String, Item> getItemsByName() {
            return itemsByName;
        }
    }

    public static class Item {
        @Nullable
        private final String name;

        public Item(@Nullable String name) {
            this.name = name;
        }

        @Nullable
        public String getName() {
            return name;
        }
    }

    public static class Result<T> {
        private T data;
        private boolean done;
        private String message;

        public T getData() {
            return data;
        }

        public boolean isDone() {
            return done;
        }

        public String getMessage() {
            return message;
        }
    }
}
