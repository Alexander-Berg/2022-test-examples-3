package ru.yandex.autotests.market.stat.dictionaries_yt.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 29.06.17 <br>
 *   <br>
 * Annotate @DictionaryId dictionary field or field combination which guarantees unique record identification <br>
 *  This means that there can't be 2 records in db having same values in these fields
 *   <br>
 *  Property is used for row_id creation and for sql-generation for this particular record in db
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface DictionaryIdField {
    boolean isIdPart() default true;
    boolean isForQuery() default true;
}
