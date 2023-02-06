package ru.yandex.market.mbo.db.vendor;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.vendor.Filter;

/**
 * @author ayratgdl
 * @since 18.11.18
 */
public class VendorQueryBuilderTest {
    @Test
    public void getQueryByFilterWhenFilterIsEmpty() {
        String actualSql = QueryBuilder.getQueryByFilter(new Filter());
        String expectedSql = "select * from (\n" +
            "select gv.*, rownum rn from global_vendor gv \n" +
            ") where rn >= :rn_from and rn < :rn_to";
        Assert.assertEquals(expectedSql, actualSql);
    }

    @Test
    public void getQueryByFilterWhenFilterWithOrder() {
        Filter filter = new Filter();
        filter.setOrderField(Filter.Field.NAME);
        filter.setOrderAscending(true);

        String actualSql = QueryBuilder.getQueryByFilter(filter);
        String expectedSql = "select * from (\n" +
            "select gv.*, rownum rn from ( \n" +
            "select gv.* from global_vendor gv \n" +
            "left outer join global_vendor_name gvn on gvn.vendor_id = gv.id AND gvn.lang_id = 225 \n" +
            "order by lower(gvn.name) ASC) gv\n" +
            ") where rn >= :rn_from and rn < :rn_to";
        Assert.assertEquals(expectedSql, actualSql);
    }

    @Test
    public void getQueryByFilterWhenFilterWithHasSeoFieldsAndOrder() {
        Filter filter = new Filter();
        filter.setHasSeoFields(true);
        filter.setOrderField(Filter.Field.NAME);
        filter.setOrderAscending(true);

        String actualSql = QueryBuilder.getQueryByFilter(filter);
        String expectedSql = "select * from (\n" +
            "select gv.*, rownum rn from ( \n" +
            "select gv.* from global_vendor gv \n" +
            "left outer join global_vendor_name gvn on gvn.vendor_id = gv.id AND gvn.lang_id = 225 \n" +
            "where (seo_title is not null or seo_description is not null)\n" +
            "order by lower(gvn.name) ASC) gv\n" +
            ") where rn >= :rn_from and rn < :rn_to";
        Assert.assertEquals(expectedSql, actualSql);
    }

    @Test
    public void getQueryByFilterWhenFilterWithIdAndHasSeoFieldsAndOrder() {
        Filter filter = new Filter();
        filter.setId(1L);
        filter.setHasSeoFields(true);
        filter.setOrderField(Filter.Field.NAME);
        filter.setOrderAscending(true);

        String actualSql = QueryBuilder.getQueryByFilter(filter);
        String expectedSql = "select * from (\n" +
            "select gv.*, rownum rn from ( \n" +
            "select gv.* from global_vendor gv \n" +
            "left outer join global_vendor_name gvn on gvn.vendor_id = gv.id AND gvn.lang_id = 225 \n" +
            "where gv.id = :id and (seo_title is not null or seo_description is not null)\n" +
            "order by lower(gvn.name) ASC) gv\n" +
            ") where rn >= :rn_from and rn < :rn_to";
        Assert.assertEquals(expectedSql, actualSql);
    }

    @Test
    public void getQueryByFilterWhenFilterWithLineName() {
        Filter filter = new Filter();
        filter.setLineName("line name");

        String actualSql = QueryBuilder.getQueryByFilter(filter);
        String expectedSql = "select * from (\n" +
            "select gv.*, rownum rn from global_vendor gv \n" +
            "where gv.id in ( \n" +
            "select distinct gv.id from global_vendor gv \n" +
            "join (select distinct(eo.parent_vendor_id) vendor_id from parameter line \n" +
            "  join enum_option eo on eo.param_id = line.id\n" +
            "  join enum_option_name en on en.id = eo.id\n" +
            "  left join enum_option_alias ea on ea.option_id = eo.id and ea.lang_id = 225\n" +
            "  where line.xsl_name = 'vendor_line' and line.category_hid = 0\n" +
            "   and en.lang_id = 225 " +
            "and (       lower(en.name) like :line_name or lower(ea.alias) like :line_name)   ) lns " +
            "on lns.vendor_id = gv.id\n" +
            ")\n" +
            ") where rn >= :rn_from and rn < :rn_to";
        Assert.assertEquals(expectedSql, actualSql);
    }

    @Test
    public void getQueryByFilterWhenFilterWithManufacturerName() {
        Filter filter = new Filter();
        filter.setManufacturer("manufacturer name");

        String actualSql = QueryBuilder.getQueryByFilter(filter);
        String expectedSql = "select * from (\n" +
            "select gv.*, rownum rn from global_vendor gv \n" +
            "where gv.id in ( \n" +
            "select distinct gv.id from global_vendor gv \n" +
            "join (select distinct(v.source_option_id) vendor_id from V_MANUFACTURES_WITH_NAME v\n" +
            "      where lower(name) like :manufacturer_name or\n" +
            "      lower(alias) like :manufacturer_name\n" +
            "   ) lv on lv.vendor_id = gv.id\n" +
            ")\n" +
            ") where rn >= :rn_from and rn < :rn_to";
        Assert.assertEquals(expectedSql, actualSql);
    }

    @Test
    public void getCountQueryByFilterWhenFilterIsEmpty() {
        String actualSql = QueryBuilder.getCountQueryByFilter(new Filter());
        String expectedSql = "select count(1) from global_vendor";
        Assert.assertEquals(expectedSql, actualSql);
    }

    @Test
    public void getCountQueryByFilterWhenFilterWithHasSeoFields() {
        Filter filter = new Filter();
        filter.setHasSeoFields(true);
        filter.setOrderField(Filter.Field.NAME);
        filter.setOrderAscending(true);

        String actualSql = QueryBuilder.getCountQueryByFilter(filter);
        String expectedSql = "select count(1) from global_vendor gv " +
            "where (seo_title is not null or seo_description is not null)";
        Assert.assertEquals(expectedSql, actualSql);
    }

    @Test
    public void getCountQueryByFilterWhenFilterWithIdAndHasSeoFields() {
        Filter filter = new Filter();
        filter.setId(1L);
        filter.setHasSeoFields(true);

        String actualSql = QueryBuilder.getCountQueryByFilter(filter);
        String expectedSql = "select count(1) from global_vendor gv " +
            "where gv.id = :id and (seo_title is not null or seo_description is not null)";
        Assert.assertEquals(expectedSql, actualSql);
    }
}
