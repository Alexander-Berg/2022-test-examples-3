from sendr_interactions.retry_budget import RetryBudget


def test_base_retry_budget():
    key = "some_key"
    max_tokens = 10
    token_ration = 0.1
    budget = RetryBudget(max_tokens=max_tokens, token_ratio=token_ration)

    for _ in range(max_tokens // 2 + 1):
        budget.fail(key)

    assert not budget.can_retry(key)

    for _ in range(int(1 / token_ration) + 1):
        budget.success(key)

    assert budget.can_retry(key)


def test_base_retry_budget_so_many_fails():
    key = "some_key"
    max_tokens = 10
    token_ration = 0.1
    budget = RetryBudget(max_tokens=max_tokens, token_ratio=token_ration)

    for _ in range(max_tokens + 1):
        budget.fail(key)

    assert not budget.can_retry(key)

    for _ in range(int(max_tokens / token_ration * 2) + 1):
        budget.success(key)

    assert budget.can_retry(key)


def test_base_retry_budget_so_many_success():
    key = "some_key"
    max_tokens = 10
    token_ration = 0.1
    budget = RetryBudget(max_tokens=max_tokens, token_ratio=token_ration)

    for _ in range(max_tokens):
        budget.success(key)

    assert budget.can_retry(key)

    for _ in range(max_tokens // 2 + 1):
        budget.fail(key)

    assert not budget.can_retry(key)
