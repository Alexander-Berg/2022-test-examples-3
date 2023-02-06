package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.domain.v2.filters.PhotoPickerEnumValue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static ru.yandex.market.api.ApiMatchers.map;

public class PhotoPickerEnumValueMatchers {
    public static Matcher<PhotoPickerEnumValue> photoPickerValue(Matcher<PhotoPickerEnumValue> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<PhotoPickerEnumValue> id(String id) {
        return map(
            PhotoPickerEnumValue::getId,
            "'id'",
            is(id),
            PhotoPickerEnumValueMatchers::toStr
        );
    }

    public static Matcher<PhotoPickerEnumValue> color(String color) {
        return map(
            PhotoPickerEnumValue::getColor,
            "'color'",
            is(color),
            PhotoPickerEnumValueMatchers::toStr
        );
    }

    public static Matcher<PhotoPickerEnumValue> photo(String photo) {
        return map(
            PhotoPickerEnumValue::getPhoto,
            "'photo'",
            is(photo),
            PhotoPickerEnumValueMatchers::toStr
        );
    }

    public static Matcher<PhotoPickerEnumValue> checked(boolean checked) {
        return map(
            PhotoPickerEnumValue::getChecked,
            "'checked'",
            is(checked),
            PhotoPickerEnumValueMatchers::toStr
        );
    }


    public static PhotoPickerEnumValue toStrWrap(PhotoPickerEnumValue value) {
        return new PhotoPickerEnumValue(value.getId(), value.getName(), value.getColor(), value.getPhoto()) {
            @Override
            public String toString() {
                return toStr(value);
            }
        };
    }

    private static String toStr(PhotoPickerEnumValue picker) {
        if (null == picker) {
            return "null";
        }
        return MoreObjects.toStringHelper(PhotoPickerEnumValue.class)
            .add("id", picker.getId())
            .add("color", picker.getColor())
            .add("photo", picker.getPhoto())
            .add("checked", picker.getChecked())
            .toString();
    }
}
