import expect from "expect.js";
import { isJavaPackageName } from "../utils/JavaPackageNameValidator";

describe("Java package name validation test", () => {
  it("base", () => {
    expect(isJavaPackageName("ru.yandex.foo")).to.eql(true);
    expect(isJavaPackageName("ru.yandex._3foo")).to.eql(true);

    expect(isJavaPackageName("")).to.eql(false);
    expect(isJavaPackageName(" ")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.3foo")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.foo.")).to.eql(false);
    expect(isJavaPackageName("ru.Yandex.foo")).to.eql(false);
    expect(isJavaPackageName("ru.ya-ndex.foo")).to.eql(false);
    expect(isJavaPackageName(".ru.yandex.foo")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.foo ")).to.eql(false);
  });

  it("Parts limit", () => {
    expect(isJavaPackageName("ru.yandex")).to.eql(true);
    expect(isJavaPackageName("ru.yandex", 3)).to.eql(false);
  });

  it("Java reserve words", () => {
    expect(isJavaPackageName("java.yandex.foo")).to.eql(false);
    expect(isJavaPackageName("javax.yandex.foo")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.java")).to.eql(true);
    expect(isJavaPackageName("ru.yandex.javax")).to.eql(true);

    expect(isJavaPackageName("ru.yandex.abstract")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.assert")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.boolean")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.break")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.byte")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.case")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.catch")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.char")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.class")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.const")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.continue")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.default")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.do")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.double")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.else")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.enum")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.extends")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.final")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.finally")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.float")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.for")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.goto")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.if")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.implements")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.import")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.instanceof")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.int")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.interface")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.false")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.long")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.native")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.new")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.null")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.package")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.private")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.protected")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.public")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.return")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.short")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.static")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.strictfp")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.super")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.switch")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.synchronized")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.this")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.throw")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.throws")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.transient")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.try")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.void")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.volatile")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.while")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.var")).to.eql(false);
    expect(isJavaPackageName("ru.yandex.true")).to.eql(false);

    expect(isJavaPackageName("ru.yandex._var")).to.eql(true);
    expect(isJavaPackageName("ru.yandex._true")).to.eql(true);
  });
});
