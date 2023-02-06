from search.geo.tools.personal_pois.flow_run_lib.graph import FlowGraph, InvalidGraphError, NodeRedefinitionError, MissingNameError


def test_a_plus_b():
    graph = FlowGraph()

    @graph.func_node('result')
    def plus(a, b):
        return a + b

    graph.input_nodes(a=5, b=7)

    assert graph('result') == 12


def test_missing_node():
    graph = FlowGraph()

    @graph.func_node('result')
    def plus(a, b):
        return a + b

    graph.input_nodes(a=5)

    try:
        graph('result')
        assert False
    except InvalidGraphError:
        assert True


def test_cyclical_graph():
    graph = FlowGraph()

    @graph.func_node()
    def a(b):
        pass

    @graph.func_node()
    def b(c):
        pass

    @graph.func_node()
    def c(a):
        pass

    try:
        graph('a')
        assert False
    except InvalidGraphError:
        assert True


def test_input_node_redefinition():
    graph = FlowGraph()

    @graph.func_node()
    def a(b):
        pass

    try:
        graph.input_nodes(a=5)
        assert False
    except NodeRedefinitionError:
        assert True


def test_func_node_redefinition():
    graph = FlowGraph()

    @graph.func_node()
    def a(b):
        pass

    try:
        @graph.func_node('a')
        def ttt(b):
            pass

        assert False
    except NodeRedefinitionError:
        assert True


def test_normal_decorator_params():
    graph = FlowGraph()

    @graph.func_node(b='c')
    def a(b):
        pass


def test_wrong_decorator_params():
    graph = FlowGraph()

    try:
        @graph.func_node(r='a')
        def a(b):
            pass

        assert False
    except MissingNameError:
        assert True


def test_rhombus():
    graph = FlowGraph()

    @graph.func_node('result')
    def plus(a, b):
        return a + b

    @graph.func_node()
    def a(input):
        return input

    @graph.func_node()
    def b(input):
        return input

    graph.input_nodes(input=5)

    assert graph('result') == 10
