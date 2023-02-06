package ru.yandex.common.framework.filter.condition.factory;

import java.util.Arrays;

import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.yandex.common.framework.db.SimpleSqlAware;
import ru.yandex.common.framework.filter.ParameterizedQuery;
import ru.yandex.common.framework.filter.QueryCondition;
import ru.yandex.common.framework.filter.SimpleQueryConditionFilter;

/**
 * Date: 22.06.2009
 * Time: 19:56:18
 *
 * @author Antonina Mamaeva mamton@yandex-team.ru
 */
public class ConditionFactoryTest extends TestCase {
    public void testSimpleEqFactory() {
        SimpleEqFactoryQuery simpleEqFactory = new SimpleEqFactoryQuery();
        simpleEqFactory.setColumnName("col1");
        simpleEqFactory.setName("name1");
        QueryCondition condition = simpleEqFactory.getCondition(1);
        assertEquals("col1 = ?", condition.getCondition());
        assertEquals(Arrays.asList(1), condition.getParams());
        StringBuilder xmlBuffer = new StringBuilder();
        condition.toXml(xmlBuffer);
        assertEquals("<name1>1</name1>", xmlBuffer.toString());
    }

    public void testInCondition() {
        InQueryConditionFactory factory = new InQueryConditionFactory();
        factory.setColumnName("");
        factory.setColumnName("col1");
        factory.setName("name1");
        Integer[] values = {1, 2, 3};
        QueryCondition condition = factory.getCondition(values);
        assertEquals("col1 in (?, ?, ?)", condition.getCondition());
        assertEquals(Arrays.asList(values), condition.getParams());
        StringBuilder xmlBuffer = new StringBuilder();
        condition.toXml(xmlBuffer);
        assertEquals("<col1-in>[1, 2, 3]</col1-in>", xmlBuffer.toString());
    }

    public void testComplexCondition() {
        OrQueryConditionFactory complexFactory = new OrQueryConditionFactory();
        SimpleEqFactoryQuery simpleEqFactory = new SimpleEqFactoryQuery();
        simpleEqFactory.setColumnName("col1");
        simpleEqFactory.setName("name1");
        InQueryConditionFactory factory = new InQueryConditionFactory();
        factory.setColumnName("col2");
        factory.setName("name2");
        complexFactory.setSubFactories(new QueryConditionFactory[]{simpleEqFactory, factory});
        Object[] values = new Object[]{1, new Object[]{1, 2, 3}};
        QueryCondition condition = complexFactory.getCondition(values);
        assertEquals("(col1 = ? or col2 in (?, ?, ?))", condition.getCondition());
        assertEquals(Arrays.asList(1, 1, 2, 3), condition.getParams());
        StringBuilder xmlBuffer = new StringBuilder();
        condition.toXml(xmlBuffer);
        assertEquals("<or><name1>1</name1><col2-in>[1, 2, 3]</col2-in></or>", xmlBuffer.toString());
    }

    public void testSpringConfiguration() {
        ApplicationContext context = new ClassPathXmlApplicationContext("config/conditionConfig.xml");
        QueryConditionFactory factory = (QueryConditionFactory) context.getBean("eqConditionFactory");
        QueryCondition condition = factory.getCondition(1);
        assertEquals("col1 = ?", condition.getCondition());
        assertEquals(Arrays.asList(1), condition.getParams());

        factory = (QueryConditionFactory) context.getBean("inConditionFactory");
        condition = factory.getCondition(new Object[]{1, 2, 3});
        assertEquals("col2 in (?, ?, ?)", condition.getCondition());
        assertEquals(Arrays.asList(1, 2, 3), condition.getParams());

        /*
        <bean id="notInFactory" class="ru.yandex.common.framework.filter.condition.factory.NotQueryConditionFactory">
            <property name="subFactoriy" ref="inConditionFactory"/>
        </bean>
         */
        factory = (QueryConditionFactory) context.getBean("notInFactory");
        condition = factory.getCondition(new Object[]{1, 2, 3});
        assertEquals(" not (col2 in (?, ?, ?))", condition.getCondition());
        assertEquals(Arrays.asList(1, 2, 3), condition.getParams());
        /*
        <bean id="orFactory" class="ru.yandex.common.framework.filter.condition.factory.OrQueryConditionFactory">
            <property name="subFactories">
                <list>
                    <ref bean="eqConditionFactory"/>
                    <ref bean="inConditionFactory"/>
                </list>
            </property>
        </bean>
        */
        factory = (QueryConditionFactory) context.getBean("orFactory");
        condition = factory.getCondition(new Object[]{1, new Object[]{1, 2, 3}});
        assertEquals("(col1 = ? or col2 in (?, ?, ?))", condition.getCondition());
        assertEquals(Arrays.asList(1, 1, 2, 3), condition.getParams());
        /*
         <bean id="andFactory" class="ru.yandex.common.framework.filter.condition.factory.OrQueryConditionFactory">
             <property name="subFactories">
                 <list>
                     <ref bean="notInFactory"/>
                     <ref bean="orFactory"/>
                 </list>
             </property>
         </bean>
        */
        factory = (QueryConditionFactory) context.getBean("andFactory");
        condition = factory.getCondition(new Object[]{
                new Object[]{3, 4},
                new Object[]{
                        1,
                        new Object[]{1, 2, 3}}});
        assertEquals("( not (col2 in (?, ?)) and (col1 = ? or col2 in (?, ?, ?)))", condition.getCondition());
        assertEquals(Arrays.asList(3, 4, 1, 1, 2, 3), condition.getParams());
    }

    public void testComplexFactoryCreation() {
        ApplicationContext context = new ClassPathXmlApplicationContext("config/conditionConfig.xml");
        QueryConditionFactory factory1 = (QueryConditionFactory) context.getBean("eqConditionFactory");
        QueryConditionFactory factory2 = (QueryConditionFactory) context.getBean("notInFactory");
        ComplexQueryConditionFactory factory = new AndQueryConditionFactory(factory1, factory2);
        QueryCondition condition = factory.getCondition(new Object[]{1, new Object[]{1, 2, 3}});
        assertEquals("(col1 = ? and  not (col2 in (?, ?, ?)))", condition.getCondition());
        assertEquals(Arrays.asList(1, 1, 2, 3), condition.getParams());
        StringBuilder xmlBuffer = new StringBuilder();
        condition.toXml(xmlBuffer);
        assertEquals("<and><name1>1</name1><not><col2-in>[1, 2, 3]</col2-in></not></and>", xmlBuffer.toString());
        try {
            condition = factory.getCondition(new Object[]{1});
            fail("shouldn't create condition when too few parameters");
        } catch (Exception e) {
        }
    }

    public void testParametizedQueryOnCondition() {
        InQueryConditionFactory factory = new InQueryConditionFactory("col1");
        Integer[] values = {1, 2, 3};
        QueryCondition condition = factory.getCondition(values);

        ParameterizedQuery query = new ParameterizedQuery(
                new SimpleQueryConditionFilter(condition), null, new SimpleSqlAware("t1", "c1", "c2"));
        assertEquals("select c1, c2 from t1 where col1 in (?, ?, ?)", query.getQuery());
        assertTrue(Arrays.equals(values, query.getParams()));
    }

    public void testInSubQueryCondition() {
        SimpleEqFactoryQuery simpleEqFactory = new SimpleEqFactoryQuery("c1");
        InSubQueryConditionFactory fac = new InSubQueryConditionFactory(simpleEqFactory,
                new SimpleSqlAware("t1", "c3"), "c2");
        QueryCondition condition = fac.getCondition(15);
        assertEquals("c2 in (select c3 from t1 where c1 = ?)", condition.getCondition());
        StringBuilder xmlBuffer = new StringBuilder();
        condition.toXml(xmlBuffer);
        assertEquals("<c2-in>select c3 from t1 where c1 = ? [[15]]</c2-in>", xmlBuffer.toString());
        try {
            SimpleSqlAware aware = new SimpleSqlAware("t1", "c3", "c4");
            fac = new InSubQueryConditionFactory(simpleEqFactory, aware, "c2");
            fail("can't create factory with sqlAware " + aware);
        } catch (Exception e) {
        }
    }

}
