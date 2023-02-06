let { join } = require('path');
let { expect } = require('chai');

let { TankerBuilder } = require('./TankerBuilder');
let { TankerCollector } = require('./TankerCollector');

let collector = new TankerCollector({
    files: join(__dirname, './files/*.i18n.json'),
});

let tanker = new TankerBuilder({
    env: {},
    language: 'ru',
    languages: ['ru', 'en'],
    collector,
});

tanker.prepareTranslates()
    //.then(() => {
    //    expect(tanker.exportNamespace('TankerTest')).to.eql({
    //        simpleKey: 'Simple key ru',
    //        pluralKey: ['Plural key ru 1', 'Plural key ru 2', 'Plural key ru 3']
    //    });
    //
    //    expect(tanker.generateNamespaceWrapper('TankerTest')).to.eql('let {translateWrapper} = require(\'libs/Translate/translateWrapper.ts\');\n\nmodule.exports = translateWrapper({"simpleKey":"Simple key ru","pluralKey":["Plural key ru 1","Plural key ru 2","Plural key ru 3"]});');
    //
    //    expect(tanker.translate('TankerTest.simpleKey', {canBeInline: true}).value).to.eql('Simple key ru');
    //    expect(tanker.translate('TankerTest.pluralKey', {canBeInline: true}).funcKey).to.eql('pluralKey');
    //})
    //.then(() => {
    //    expect(tanker.exportNamespace('TankerTestLinks')).to.eql({
    //        simpleLink: 'Simple key ru',
    //        pluralLink: ['Plural key ru 1', 'Plural key ru 2', 'Plural key ru 3']
    //    });
    //
    //    expect(tanker.translate('TankerTestLinks.simpleLink', {canBeInline: true}).value).to.eql('Simple key ru');
    //    expect(tanker.translate('TankerTestLinks.pluralLink', {canBeInline: true}).funcKey).to.eql('pluralLink');
    //})
    .then(() => {
        expect(tanker.generateNamespaceWrapper('TankerTestHtml')).to.eql('var translateWrapper = require("libs/Translate/translateWrapper.ts").translateWrapper;\n' +
            'var React = require("react");\n' +
            'var e = require("hvalanka/lib/Runtime/DomRuntime").e;\n' +
            'var er = require("hvalanka/lib/Runtime/DomRuntime").er;\n' +
            'var f = require("hvalanka/lib/Runtime/DomRuntime").f;\n' +
            'var fr = require("hvalanka/lib/Runtime/DomRuntime").fr;\n' +
            'module.exports = translateWrapper({"simpleKey":function(context){return context["var"]+" ru"},"pluralKey":[{v:"<i>Plural</i> key ru 1",r: function(context){ return React.createElement(React.Fragment, null, React.createElement("i", {},"Plural"), " key ru 1") },h: function(context){ var view = this, result = view.isRestored ? fr : f, element = view.isRestored ? er : e; return result([element("i", {}, ["Plural"], {view: view})," key ru 1"]) }},{v:"<i id=\\"test __var__\\">Plural key ru 2</i>",r: function(context){ return React.createElement("i", {id: "test " + context["var"]},"Plural key ru 2") },h: function(context){ var view = this, result = view.isRestored ? fr : f, element = view.isRestored ? er : e; return result(element("i", {id: "test " + context["var"]}, ["Plural key ru 2"], {view: view})) }},"Plural key ru 3"]});');
    })
    .catch((err) => {
        console.error(err);
    });
