from yamarec1.data import DataQuery


def test_mangling_works_correctly_without_preamble():
    query = DataQuery("SELECT *")
    assert query.mangle() == "SELECT *"


def test_mangling_works_correctly_with_preamble():
    query = DataQuery("SELECT *", preamble=("$x = 1", "$y = 2"))
    assert query.mangle() == "SELECT *<YQL-PREAMBLE>$x = 1;\n$y = 2</YQL-PREAMBLE>"


def test_mangling_works_correctly_with_attachments():
    query = DataQuery("SELECT *", attachments=["A", "B"])
    assert query.mangle() == "SELECT *<YQL-ATTACHMENT>A</YQL-ATTACHMENT><YQL-ATTACHMENT>B</YQL-ATTACHMENT>"


def test_demangling_works_correctly_without_preamble():
    result = DataQuery.demangle("SELECT *")
    assert result.body == "SELECT *"
    assert result.preamble == ()


def test_demangling_works_correctly_with_preamble():
    mangling = "<YQL-PREAMBLE>$x = 1;\n$y = 2</YQL-PREAMBLE>SELECT * FROM (<YQL-PREAMBLE>$z = 3</YQL-PREAMBLE>SELECT *)"
    result = DataQuery.demangle(mangling)
    assert result.body == "SELECT * FROM (SELECT *)"
    assert result.preamble == ("$x = 1;\n$y = 2", "$z = 3")


def test_demangling_works_correctly_with_attachments():
    mangling = "<YQL-ATTACHMENT>A</YQL-ATTACHMENT>SELECT * FROM (<YQL-ATTACHMENT>B</YQL-ATTACHMENT>SELECT *)"
    result = DataQuery.demangle(mangling)
    assert result.body == "SELECT * FROM (SELECT *)"
    assert result.attachments == ["A", "B"]
