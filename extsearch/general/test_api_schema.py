from .api_schema import toloka_task_group_schema, toloka_task_groups_schema
from .model import TolokaTask, TolokaSolution, TolokaTaskGroup


def test_task_group():
    task_group = TolokaTaskGroup(
        pool_id="id1",
        tasks=[
            TolokaTask(
                input_values={"x": 1, "y": "Y", "z": {"1": "2"}},
                known_solutions=[
                    TolokaSolution(output_values={"q": [], "v": False})
                ]
            )
        ]
    )
    res = toloka_task_group_schema.dump(task_group)
    assert not res[1]
    g2 = toloka_task_group_schema.load(res[0])
    assert not g2[1]
    assert g2[0] == task_group


def test_task_groups_many():
    task_groups = [
        TolokaTaskGroup(
            pool_id="id1",
            tasks=[
                TolokaTask(
                    input_values={"x": 1}
                )
            ]
        ),
        TolokaTaskGroup(
            pool_id="id2",
            tasks=[
                TolokaTask(
                    input_values={"x": 2}
                )
            ]
        )
    ]
    res = toloka_task_groups_schema.dump(task_groups)
    print(res[0])
    assert not res[1]
    g2 = toloka_task_groups_schema.load(res[0])
    print(g2)
    assert not g2[1]
    assert g2[0] == task_groups
