require('chai').should();
var Parser = require('../lib/parser');
var AstBuilder = require('../lib/ast-builder');

describe('AstBuilder', function () {
    it('should process text node', function () {
        var ast = (new AstBuilder()).build(new Parser('Hello World'));
        ast.type.should.equal(AstBuilder.TYPE_ROOT);
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].value.should.equal('Hello World');
    });
    it('should process string expressions', function () {
        var ast = (new AstBuilder()).build(new Parser('{{"123"}}'));
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
        ast.items[0].value.type.should.equal(AstBuilder.TYPE_STRING);
        ast.items[0].value.value.should.equal('123');
    });
    it('should process numeric expressions', function () {
        var ast = (new AstBuilder()).build(new Parser('{{123}}'));
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
        ast.items[0].value.type.should.equal(AstBuilder.TYPE_NUMBER);
        ast.items[0].value.value.should.equal('123');
    });
    it('should process numeric logical expressions', function () {
        var ast = (new AstBuilder()).build(new Parser('{{123 > 321}}'));
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
        ast.items[0].value.type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
        ast.items[0].value.operator.should.equal('>');
        ast.items[0].value.left.type.should.equal(AstBuilder.TYPE_NUMBER);
        ast.items[0].value.left.value.should.equal('123');
        ast.items[0].value.right.type.should.equal(AstBuilder.TYPE_NUMBER);
        ast.items[0].value.right.value.should.equal('321');
    });
    it('should process call', function () {
        var ast = (new AstBuilder()).build(new Parser('{{exec "123"}}'));
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_CALL);
        ast.items[0].name.should.equal('exec');
        ast.items[0].arguments.list.length.should.equal(1);
        ast.items[0].arguments.list[0].type.should.equal(AstBuilder.TYPE_STRING);
        ast.items[0].arguments.list[0].value.should.equal('123');
        Object.keys(ast.items[0].arguments.hash).length.should.equal(0);
    });
    it('should process call with logical arguments', function () {
        var ast = (new AstBuilder()).build(new Parser('{{exec a > b}}'));
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_CALL);
        ast.items[0].name.should.equal('exec');
        ast.items[0].arguments.list[0].type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
        ast.items[0].arguments.list[0].operator.should.equal('>');
        ast.items[0].arguments.list[0].left.type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.list[0].left.name.should.equal('a');
        ast.items[0].arguments.list[0].right.type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.list[0].right.name.should.equal('b');
        Object.keys(ast.items[0].arguments.hash).length.should.equal(0);
    });
    it('should process call with hash params', function () {
        var ast = (new AstBuilder()).build(new Parser('{{exec x="Hello" y=World}}'));
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_CALL);
        ast.items[0].name.should.equal('exec');
        ast.items[0].arguments.list.length.should.equal(0);
        ast.items[0].arguments.hash.x.value.should.equal('Hello');
        ast.items[0].arguments.hash.x.type.should.equal(AstBuilder.TYPE_STRING);
        ast.items[0].arguments.hash.y.name.should.equal('World');
        ast.items[0].arguments.hash.y.type.should.equal(AstBuilder.TYPE_VARIABLE);
        Object.keys(ast.items[0].arguments.hash).length.should.equal(2);
    });
    it('should process call with mixed params', function () {
        var ast = (new AstBuilder()).build(new Parser('{{exec param1 x="Hello" y=World "param2"}}'));
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_CALL);
        ast.items[0].name.should.equal('exec');
        ast.items[0].arguments.list.length.should.equal(2);
        ast.items[0].arguments.list[0].type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.list[0].name.should.equal('param1');
        ast.items[0].arguments.list[1].type.should.equal(AstBuilder.TYPE_STRING);
        ast.items[0].arguments.list[1].value.should.equal('param2');
        ast.items[0].arguments.hash.x.value.should.equal('Hello');
        ast.items[0].arguments.hash.x.type.should.equal(AstBuilder.TYPE_STRING);
        ast.items[0].arguments.hash.y.name.should.equal('World');
        ast.items[0].arguments.hash.y.type.should.equal(AstBuilder.TYPE_VARIABLE);
        Object.keys(ast.items[0].arguments.hash).length.should.equal(2);
    });
    it('should process expression and text around', function () {
        var ast = (new AstBuilder()).build(new Parser('before{{"123"}}after'));
        ast.items.length.should.equal(3);
        ast.items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].value.should.equal('before');
        ast.items[1].type.should.equal(AstBuilder.TYPE_EXPRESSION);
        ast.items[1].value.type.should.equal(AstBuilder.TYPE_STRING);
        ast.items[1].value.value.should.equal('123');
        ast.items[2].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[2].value.should.equal('after');
    });
    it('should process block', function () {
        var ast = (new AstBuilder()).build(new Parser('{{#if}}body{{/if}}'));
        ast.type.should.equal(AstBuilder.TYPE_ROOT);
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_BLOCK);
        ast.items[0].name.should.equal('if');
        ast.items[0].mainSection.items.length.should.equal(1);
        ast.items[0].mainSection.items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].mainSection.items[0].value.should.equal('body');
        ast.items[0].sections.length.should.equal(0);
    });
    it('should process block with sections', function () {
        var astBuilder = new AstBuilder();
        astBuilder.registerBlockSection('if', 'else');
        var ast = astBuilder.build(new Parser('{{#if}}body{{else}}else_body{{/if}}'));
        ast.type.should.equal(AstBuilder.TYPE_ROOT);
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_BLOCK);
        ast.items[0].name.should.equal('if');
        ast.items[0].mainSection.items.length.should.equal(1);
        ast.items[0].mainSection.items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].mainSection.items[0].value.should.equal('body');
        ast.items[0].sections[0].name.should.equal('else');
        ast.items[0].sections[0].items.length.should.equal(1);
        ast.items[0].sections[0].items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].sections[0].items[0].value.should.equal('else_body');
        ast.items[0].sections.length.should.equal(1);
    });
    it('should process block with sections', function () {
        var astBuilder = new AstBuilder();
        astBuilder.registerBlockSection('switch', 'case');
        astBuilder.registerBlockSection('switch', 'default');
        var ast = astBuilder.build(new Parser('{{#switch var}}{{case "1"}}1{{case "2"}}2{{default}}0{{/switch}}'));
        ast.type.should.equal(AstBuilder.TYPE_ROOT);
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_BLOCK);
        ast.items[0].name.should.equal('switch');
        ast.items[0].mainSection.items.length.should.equal(0);

        ast.items[0].sections[0].name.should.equal('case');
        ast.items[0].sections[0].items.length.should.equal(1);
        ast.items[0].sections[0].arguments.list.length.should.equal(1);
        ast.items[0].sections[0].arguments.list[0].type.should.equal(AstBuilder.TYPE_STRING);
        ast.items[0].sections[0].arguments.list[0].value.should.equal('1');
        ast.items[0].sections[0].items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].sections[0].items[0].value.should.equal('1');

        ast.items[0].sections[1].name.should.equal('case');
        ast.items[0].sections[1].items.length.should.equal(1);
        ast.items[0].sections[1].arguments.list.length.should.equal(1);
        ast.items[0].sections[1].arguments.list[0].type.should.equal(AstBuilder.TYPE_STRING);
        ast.items[0].sections[1].arguments.list[0].value.should.equal('2');
        ast.items[0].sections[1].items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].sections[1].items[0].value.should.equal('2');

        ast.items[0].sections[2].name.should.equal('default');
        ast.items[0].sections[2].items.length.should.equal(1);
        ast.items[0].sections[2].items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].sections[2].items[0].value.should.equal('0');

        ast.items[0].sections.length.should.equal(3);
    });
    it('should process block with arguments', function () {
        var ast = (new AstBuilder()).build(new Parser('{{#if param1 key=value}}body{{/if}}'));
        ast.type.should.equal(AstBuilder.TYPE_ROOT);
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_BLOCK);
        ast.items[0].name.should.equal('if');
        ast.items[0].mainSection.items.length.should.equal(1);
        ast.items[0].mainSection.items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].mainSection.items[0].value.should.equal('body');
        ast.items[0].sections.length.should.equal(0);
        ast.items[0].arguments.list.length.should.equal(1);
        ast.items[0].arguments.list[0].type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.list[0].name.should.equal('param1');
        ast.items[0].arguments.hash.key.type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.hash.key.name.should.equal('value');
        Object.keys(ast.items[0].arguments.hash).length.should.equal(1);
    });
    it('should process block with logical arguments', function () {
        var ast = (new AstBuilder()).build(new Parser('{{#if param1 > param2 key=value1 > value2}}body{{/if}}'));
        ast.type.should.equal(AstBuilder.TYPE_ROOT);
        ast.items.length.should.equal(1);
        ast.items[0].type.should.equal(AstBuilder.TYPE_BLOCK);
        ast.items[0].name.should.equal('if');
        ast.items[0].mainSection.items.length.should.equal(1);
        ast.items[0].mainSection.items[0].type.should.equal(AstBuilder.TYPE_TEXT);
        ast.items[0].mainSection.items[0].value.should.equal('body');
        ast.items[0].sections.length.should.equal(0);
        ast.items[0].arguments.list.length.should.equal(1);
        ast.items[0].arguments.list[0].type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
        ast.items[0].arguments.list[0].operator.should.equal('>');
        ast.items[0].arguments.list[0].left.type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.list[0].left.name.should.equal('param1');
        ast.items[0].arguments.list[0].right.type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.list[0].right.name.should.equal('param2');
        ast.items[0].arguments.hash.key.type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
        ast.items[0].arguments.hash.key.operator.should.equal('>');
        ast.items[0].arguments.hash.key.left.type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.hash.key.left.name.should.equal('value1');
        ast.items[0].arguments.hash.key.right.type.should.equal(AstBuilder.TYPE_VARIABLE);
        ast.items[0].arguments.hash.key.right.name.should.equal('value2');
        Object.keys(ast.items[0].arguments.hash).length.should.equal(1);
    });
    describe('logicals', function () {
        it('should process >', function () {
            var ast = (new AstBuilder()).build(new Parser('{{a > b}}'));
            ast.items.length.should.equal(1);
            ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
            ast.items[0].value.type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
            ast.items[0].value.operator.should.equal('>');
            ast.items[0].value.left.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.left.name.should.equal('a');
            ast.items[0].value.right.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.right.name.should.equal('b');
        });
        it('should process >=', function () {
            var ast = (new AstBuilder()).build(new Parser('{{a >= b}}'));
            ast.items.length.should.equal(1);
            ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
            ast.items[0].value.type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
            ast.items[0].value.operator.should.equal('>=');
            ast.items[0].value.left.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.left.name.should.equal('a');
            ast.items[0].value.right.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.right.name.should.equal('b');
        });
        it('should process <', function () {
            var ast = (new AstBuilder()).build(new Parser('{{a < b}}'));
            ast.items.length.should.equal(1);
            ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
            ast.items[0].value.type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
            ast.items[0].value.operator.should.equal('<');
            ast.items[0].value.left.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.left.name.should.equal('a');
            ast.items[0].value.right.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.right.name.should.equal('b');
        });
        it('should process <=', function () {
            var ast = (new AstBuilder()).build(new Parser('{{a <= b}}'));
            ast.items.length.should.equal(1);
            ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
            ast.items[0].value.type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
            ast.items[0].value.operator.should.equal('<=');
            ast.items[0].value.left.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.left.name.should.equal('a');
            ast.items[0].value.right.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.right.name.should.equal('b');
        });
        it('should process ==', function () {
            var ast = (new AstBuilder()).build(new Parser('{{a == b}}'));
            ast.items.length.should.equal(1);
            ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
            ast.items[0].value.type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
            ast.items[0].value.operator.should.equal('==');
            ast.items[0].value.left.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.left.name.should.equal('a');
            ast.items[0].value.right.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.right.name.should.equal('b');
        });
        it('should process !=', function () {
            var ast = (new AstBuilder()).build(new Parser('{{a != b}}'));
            ast.items.length.should.equal(1);
            ast.items[0].type.should.equal(AstBuilder.TYPE_EXPRESSION);
            ast.items[0].value.type.should.equal(AstBuilder.TYPE_LOGICAL_EXPRESSION);
            ast.items[0].value.operator.should.equal('!=');
            ast.items[0].value.left.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.left.name.should.equal('a');
            ast.items[0].value.right.type.should.equal(AstBuilder.TYPE_VARIABLE);
            ast.items[0].value.right.name.should.equal('b');
        });
    });
});
