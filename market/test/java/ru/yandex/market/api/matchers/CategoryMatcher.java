package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.CategoryV2;

public class CategoryMatcher {
     public static Matcher<CategoryV2> categories(Matcher<CategoryV2> ... matchers) {
         return Matchers.allOf(matchers);
     }

     public static Matcher<CategoryV2> id(int id) {
         return ApiMatchers.map(
             CategoryV2::getId,
             "'id'",
             Matchers.is(id),
             CategoryMatcher::toStr
         );
     }

     public static String toStr(CategoryV2 category) {
         if (null == category) {
             return "null";
         }

         return MoreObjects.toStringHelper(category)
             .add("id", category.getId())
             .toString();
     }
}
