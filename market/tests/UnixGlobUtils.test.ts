import expect from "expect.js";
import { toRegexPattern } from "../utils/UnixGlobUtils";

const matchGlob = (path: string, globPatter: string): boolean => {
  return Boolean(path.match(toRegexPattern(globPatter)));
};

const isWrongPattern = (globPatter: string) => {
  try {
    return !new RegExp(toRegexPattern(globPatter));
  } catch (ignore) {
    return true;
  }
};

describe("Glob utils test", () => {
  it("base", () => {
    expect(matchGlob("foo.html", "foo.html")).to.eql(true);
    expect(matchGlob("foo.html", "foo.htm")).to.eql(false);
    expect(matchGlob("foo.html", "bar.html")).to.eql(false);
  });

  it("match zero or more characters", () => {
    expect(matchGlob("foo.html", "f*")).to.eql(true);
    expect(matchGlob("foo.html", "*.html")).to.eql(true);
    expect(matchGlob("foo.html", "foo.html*")).to.eql(true);
    expect(matchGlob("foo.html", "*foo.html")).to.eql(true);
    expect(matchGlob("foo.html", "*foo.html*")).to.eql(true);
    expect(matchGlob("foo.html", "*.htm")).to.eql(false);
    expect(matchGlob("foo.html", "f.*")).to.eql(false);
  });

  it("match one character", () => {
    expect(matchGlob("foo.html", "?oo.html")).to.eql(true);
    expect(matchGlob("foo.html", "??o.html")).to.eql(true);
    expect(matchGlob("foo.html", "???.html")).to.eql(true);
    expect(matchGlob("foo.html", "???.htm?")).to.eql(true);
    expect(matchGlob("foo.html", "foo.???")).to.eql(false);
  });

  it("group of subpatterns", () => {
    expect(matchGlob("foo.html", "foo{.html,.class}")).to.eql(true);
    expect(matchGlob("foo.html", "foo.{class,html}")).to.eql(true);
    expect(matchGlob("foo.html", "foo{.htm,.class}")).to.eql(false);
  });

  it("bracket expressions", () => {
    expect(matchGlob("foo.html", "[f]oo.html")).to.eql(true);
    expect(matchGlob("foo.html", "[e-g]oo.html")).to.eql(true);
    expect(matchGlob("foo.html", "[abcde-g]oo.html")).to.eql(true);
    expect(matchGlob("foo.html", "[abcdefx-z]oo.html")).to.eql(true);
    expect(matchGlob("foo.html", "[!a]oo.html")).to.eql(true);
    expect(matchGlob("foo.html", "[!a-e]oo.html")).to.eql(true);
    expect(matchGlob("foo-bar", "foo[-a-z]bar")).to.eql(true);
    expect(matchGlob("foo.html", "foo[!-]html")).to.eql(true);
  });

  it("groups of subpattern with bracket expressions", () => {
    expect(matchGlob("foo.html", "[f]oo.{[h]tml,class}")).to.eql(true);
    expect(matchGlob("foo.html", "foo.{[a-z]tml,class}")).to.eql(true);
    expect(matchGlob("foo.html", "foo.{[!a-e]tml,.class}")).to.eql(true);
  });

  it("assume special characters are allowed in file names", () => {
    expect(matchGlob("{foo}.html", "\\{foo*")).to.eql(true);
    expect(matchGlob("{foo}.html", "*\\}.html")).to.eql(true);
    expect(matchGlob("[foo].html", "\\[foo*")).to.eql(true);
    expect(matchGlob("[foo].html", "*\\].html")).to.eql(true);
  });

  it("errors", () => {
    expect(isWrongPattern("*[a--z]")).to.eql(true);
    expect(isWrongPattern("*[a--]")).to.eql(true);
    expect(isWrongPattern("*[a-z")).to.eql(true);
    expect(isWrongPattern("*{class,java")).to.eql(true);
    expect(isWrongPattern("*.{class,{.java}}")).to.eql(true);
    expect(isWrongPattern("*.html\\")).to.eql(true);
  });

  it("unix specific", () => {
    expect(matchGlob("/tmp/foo", "/tmp/*")).to.eql(true);
    expect(matchGlob("/tmp/foo/bar", "/tmp/**")).to.eql(true);
    expect(matchGlob("myfile?", "myfile\\?")).to.eql(true);
    expect(matchGlob("one\\two", "one\\\\two")).to.eql(true);
    expect(matchGlob("one*two", "one\\*two")).to.eql(true);
  });
});
