import pytest
from parallel_offline.signals import QueryTypeSignal



@pytest.mark.parametrize("query,normalized_query", [
    ("  купить шар \t", "купить шар"),
    ("\n\nкупить \t\t\nгвоздь", "купить гвоздь")
])
def test_query_normalization(query, normalized_query):
    signal = QueryTypeSignal(query=query)
    assert signal.query == normalized_query


@pytest.mark.parametrize("query", ["", "\t", "\n", "\n  \n \t \t"])
def test_empty_query_normalization(query):
    with pytest.raises(RuntimeError):
        QueryTypeSignal(query=query)


def test_id_and_batch_id_differs():
    signal = QueryTypeSignal(query="test")
    assert signal.id != signal.batch_id
