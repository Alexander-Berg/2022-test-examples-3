package ru.yandex.common.report.graph;

import junit.framework.Assert;
import junit.framework.TestCase;

import ru.yandex.common.report.graph.model.SymbolicVariable;
import ru.yandex.common.report.graph.model.Variable;
import ru.yandex.common.report.graph.model.VariableResolver;

/**
 * @author Sergey Simonchik ssimonchik@yandex-team.ru
 */
public class VariableResolverTest extends TestCase {
    public void test1() {
        VariableResolver resolver = new VariableResolver();
        resolver.addVariable(new Variable("a", "${b} - ${c}"));
        resolver.addVariable(new Variable("d", "  ${b} + ${c} "));
        resolver.addVariable(new SymbolicVariable("c"));
        resolver.addVariable(new SymbolicVariable("b"));
        resolver.resolveAll();
        Assert.assertEquals(resolver.getVariableValue("a"), "${b} - ${c}");
        Assert.assertEquals(resolver.getVariableValue("d"), "  ${b} + ${c} ");
    }

    public void test2() {
        VariableResolver resolver = new VariableResolver();
        resolver.addVariable(new Variable("a", "${b} - (${c})"));
        resolver.addVariable(new Variable("d", "  ${b} + ${c} - (${a}) "));
        resolver.addVariable(new Variable("c", "2*(${b})"));
        resolver.addVariable(new Variable("b", "1+1"));
        resolver.resolveAll();
        Assert.assertEquals(resolver.getVariableValue("a"), "1+1 - (2*(1+1))");
        Assert.assertEquals(resolver.getVariableValue("d"), "  1+1 + 2*(1+1) - (1+1 - (2*(1+1))) ");
        Assert.assertEquals(resolver.getVariableValue("c"), "2*(1+1)");
        Assert.assertEquals(resolver.getVariableValue("b"), "1+1");
    }

    public void test3() {
        VariableResolver resolver = new VariableResolver();
        resolver.addVariable(new Variable("a", "${b} + ${c} + ${d}"));
        resolver.addVariable(new Variable("e", "  ${a} + ${d} "));
        resolver.addVariable(new Variable("d", "${b}"));
        resolver.addVariable(new Variable("f", "${b}"));
        resolver.addVariable(new Variable("c", "${d} + ${f}"));
        resolver.addVariable(new SymbolicVariable("b"));
        resolver.resolveAll();
        Assert.assertEquals(resolver.getVariableValue("a"), "${b} + ${b} + ${b} + ${b}");
        Assert.assertEquals(resolver.getVariableValue("e"), "  ${b} + ${b} + ${b} + ${b} + ${b} ");
        Assert.assertEquals(resolver.getVariableValue("d"), "${b}");
        Assert.assertEquals(resolver.getVariableValue("f"), "${b}");
        Assert.assertEquals(resolver.getVariableValue("c"), "${b} + ${b}");
        Assert.assertEquals(resolver.getVariableValue("b"), "${b}");
    }

    public void test4() {
        VariableResolver resolver = new VariableResolver();
        resolver.addVariable(new Variable("var1", "${var2} + ${var 3}"));
        resolver.addVariable(new Variable("var2", "  ${var 3} + ${var1} "));

        try {
            resolver.resolveAll();
            Assert.fail("resolving should fail");
        } catch (Exception e) {
        }
    }

    public void test5() {
        VariableResolver resolver = new VariableResolver();
        resolver.addVariablesFromContext("set var1 = 1 + 1; set var2 = ${var1} - (${var1}); set var3 = ${var1} + ${var2};");
        resolver.resolveAll();
        Assert.assertEquals(resolver.getVariableValue("var1"), "1 + 1");
        Assert.assertEquals(resolver.getVariableValue("var2"), "1 + 1 - (1 + 1)");
        Assert.assertEquals(resolver.getVariableValue("var3"), "1 + 1 + 1 + 1 - (1 + 1)");
    }

}
