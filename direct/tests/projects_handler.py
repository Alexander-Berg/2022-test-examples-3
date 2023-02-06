import os
import mock
import pytest

from sandbox import common


class TestProjectsHandler(object):
    @pytest.mark.usefixtures("sandbox_tasks_dir")
    def test__task_type_relative_path(self):
        common.projects_handler.load_project_types(raise_exceptions=True)
        task_code_path = common.projects_handler.task_type_relative_path("TEST_TASK")
        assert task_code_path == "projects/sandbox/test_task/__init__.py"

    def test__task_type_owners(self, sandbox_tasks_dir, monkeypatch):
        import yasandbox.controller.user

        users = ["any", "username", "ever"]
        monkeypatch.setattr(yasandbox.controller.user.Group, "rb_group_content", mock.Mock(return_value=users))

        common.projects_handler.load_project_types(raise_exceptions=True)
        task_path = os.path.join(
            sandbox_tasks_dir, os.path.dirname(common.projects_handler.task_type_relative_path("TEST_TASK"))
        )
        assert os.path.exists(os.path.join(task_path, "ya.make"))
        assert sorted(
            common.projects_handler.task_type_owners("TEST_TASK")
        ) == sorted(users)
        yasandbox.controller.user.Group.rb_group_content.assert_called_with("sandbox-dev")
