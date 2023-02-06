import fnmatch
import logging
import os
import shutil
from contextlib import contextmanager
from typing import Callable

import pytest
from _pytest.logging import LogCaptureFixture

from python_terraform.terraform import IsFlagged, Terraform, TerraformCommandError, IsNotFlagged


logging.basicConfig(level=logging.DEBUG)
root_logger = logging.getLogger()

current_path = os.path.dirname(os.path.realpath(__file__))

FILE_PATH_WITH_SPACE_AND_SPACIAL_CHARS = "test 'test.out!"
STRING_CASES = [
    [
        lambda x: x.generate_cmd_string("apply", chdir="the_folder", no_color=IsFlagged),
        "terraform -chdir=the_folder apply -no-color",
    ],
    [
        lambda x: x.generate_cmd_string(
            "push", chdir="path", vcs=True, token="token", atlas_address="url"
        ),
        "terraform -chdir=path push -vcs=true -token=token -atlas-address=url",
    ],
]

CMD_CASES = [
    [
        "method",
        "expected_output",
        "expected_ret_code",
        "expected_exception",
        "expected_logs",
        "folder",
    ],
    [
        [
            lambda x: x.cmd(
                "plan",
                chdir="var_to_output",
                no_color=IsFlagged,
                var={"test_var": "test"},
                raise_on_error=False,
            ),
            # Expected output varies by terraform version
            "test_output=\"test\"",
            0,
            False,
            "",
            "var_to_output",
        ],
        # try import aws instance
        [
            lambda x: x.cmd(
                "import",
                "aws_instance.foo",
                "i-abcd1234",
                no_color=IsFlagged,
                raise_on_error=False,
            ),
            "",
            1,
            False,
            "Error: No Terraform configuration files",
            "",
        ],
        # test with space and special character in file path
        [
            lambda x: x.cmd(
                "plan",
                chdir="var_to_output",
                out=FILE_PATH_WITH_SPACE_AND_SPACIAL_CHARS,
                raise_on_error=False,
            ),
            "",
            0,
            False,
            "",
            "var_to_output",
        ],
        # test workspace command (commands with subcommand)
        [
            lambda x: x.cmd(
                "workspace", "show", no_color=IsFlagged, raise_on_error=False
            ),
            "",
            0,
            False,
            "Command: terraform workspace show -no-color",
            "",
        ],
    ],
]


@pytest.fixture(scope="function")
def fmt_test_file(request):
    target = os.path.join(current_path, "bad_fmt", "test.backup")
    orgin = os.path.join(current_path, "bad_fmt", "test.tf")
    shutil.copy(orgin, target)

    def td():
        shutil.move(target, orgin)

    request.addfinalizer(td)
    return


@pytest.fixture()
def workspace_setup_teardown():
    """Fixture used in workspace related tests.

    Create and tear down a workspace
    *Use as a contextmanager*
    """

    @contextmanager
    def wrapper(workspace_name, create=True, delete=True, *args, **kwargs):
        tf = Terraform(working_dir=current_path)
        tf.init()
        if create:
            tf.create_workspace(workspace_name, *args, **kwargs)
        yield tf
        if delete:
            tf.set_workspace("default")
            tf.delete_workspace(workspace_name)

    yield wrapper


class TestTerraform:
    def teardown_method(self, _) -> None:
        """Teardown any state that was previously setup with a setup_method call."""
        exclude = ["test_tfstate_file", "test_tfstate_file2", "test_tfstate_file3"]

        def purge(dir: str, pattern: str) -> None:
            for root, dirnames, filenames in os.walk(dir):
                dirnames[:] = [d for d in dirnames if d not in exclude]
                for filename in fnmatch.filter(filenames, pattern):
                    f = os.path.join(root, filename)
                    os.remove(f)
                for dirname in fnmatch.filter(dirnames, pattern):
                    d = os.path.join(root, dirname)
                    shutil.rmtree(d)

        purge(current_path, "*.tfstate")
        purge(current_path, "*.tfstate.backup")
        purge(current_path, "*.terraform")
        purge(current_path, ".terraform.lock.hcl")
        purge(current_path, FILE_PATH_WITH_SPACE_AND_SPACIAL_CHARS)
        purge(current_path, "terraform.tfstate.d")
        purge(current_path, "plan.tfplan")

    @pytest.mark.parametrize(["method", "expected"], STRING_CASES)
    def test_generate_cmd_string(self, method: Callable[..., str], expected: str):
        tf = Terraform(working_dir=current_path)
        result = method(tf)

        strs = expected.split()
        for s in strs:
            assert s in result

    @pytest.mark.parametrize(*CMD_CASES)
    def test_cmd(
        self,
        method: Callable[..., str],
        expected_output: str,
        expected_ret_code: int,
        expected_exception: bool,
        expected_logs: str,
        caplog: LogCaptureFixture,
        folder: str,
    ):
        with caplog.at_level(logging.INFO):
            tf = Terraform(working_dir=current_path)
            tf.init(chdir=folder)
            try:
                ret, out, _ = method(tf)
                assert not expected_exception
            except TerraformCommandError as e:
                assert expected_exception
                ret = e.returncode
                out = e.out

        assert expected_output in out.replace("\n", "").replace(" ", "")
        assert expected_ret_code == ret
        assert expected_logs in caplog.text

    @pytest.mark.parametrize(
        ("folder", "variables", "var_files", "expected_output", "options"),
        [
            ("var_to_output", {"test_var": "test"}, None, "test_output=\"test\"", {}),
            (
                "var_to_output",
                {"test_list_var": ["c", "d"]},
                None,
                'test_list_output=[+"c",+"d",]',
                {},
            ),
            (
                "var_to_output",
                {"test_map_var": {"c": "c", "d": "d"}},
                None,
                'test_map_output={+"c"="c"+"d"="d"}',
                {},
            ),
            (
                "var_to_output",
                {"test_map_var": {"c": "c", "d": "d"}},
                "test_map_var.json",
                # Values are overriden
                'test_map_output={+"e"="e"+"f"="f"}',
                {},
            ),
            (
                "var_to_output",
                {},
                None,
                "\x1b[0m\x1b[1m\x1b[32mApplycomplete!",
                {"no_color": IsNotFlagged},
            ),
        ],
    )
    def test_apply(self, folder, variables, var_files, expected_output, options):
        tf = Terraform(
            working_dir=current_path, variables=variables, var_file=var_files
        )
        chdir = {"chdir": folder}
        tf.init(**chdir)
        options.update(chdir)
        ret, out, err = tf.apply(**options)
        assert ret == 0
        assert expected_output in out.replace("\n", "").replace(" ", "")
        assert err == ""

    def test_apply_with_var_file(self, caplog: LogCaptureFixture):
        with caplog.at_level(logging.INFO):
            tf = Terraform(working_dir=current_path)

            folder = "var_to_output"
            tf.init(chdir=folder)
            tf.apply(
                chdir=folder,
                var_file=os.path.join(current_path, "tfvar_files", "test.tfvars"),
            )
        for log in caplog.messages:
            if log.startswith("Command: terraform apply"):
                assert log.count("-var-file=") == 1

    def test_apply_with_plan_file(self, caplog: LogCaptureFixture):
        with caplog.at_level(logging.INFO):
            tf = Terraform(working_dir=current_path)

            folder = "var_to_output"
            tf.init(chdir=folder)
            tf.plan(chdir=folder, out="../plan.tfplan", detailed_exitcode=None)
            tf.apply("../plan.tfplan", chdir=folder)

        for log in caplog.messages:
            if log.startswith("Command: terraform -chdir=var_to_output apply"):
                assert log.count("plan.tfplan") == 1

    @pytest.mark.parametrize(
        ["cmd", "args", "options"],
        [
            # bool value
            ("fmt", ["bad_fmt"], {"list": False, "diff": False})
        ],
    )
    def test_options(self, cmd, args, options, fmt_test_file):
        tf = Terraform(working_dir=current_path)
        ret, out, err = getattr(tf, cmd)(*args, **options)
        assert ret == 0
        assert out == ""

    def test_state_data(self):
        cwd = os.path.join(current_path, "test_tfstate_file")
        tf = Terraform(working_dir=cwd, state="tfstate.test")
        tf.read_state_file()
        assert tf.tfstate.modules[0]["path"] == ["root"]

    def test_state_default(self):
        cwd = os.path.join(current_path, "test_tfstate_file2")
        tf = Terraform(working_dir=cwd)
        tf.read_state_file()
        assert tf.tfstate.modules[0]["path"] == ["default"]

    def test_state_default_backend(self):
        cwd = os.path.join(current_path, "test_tfstate_file3")
        tf = Terraform(working_dir=cwd)
        tf.read_state_file()
        assert tf.tfstate.modules[0]["path"] == ["default_backend"]

    def test_pre_load_state_data(self):
        cwd = os.path.join(current_path, "test_tfstate_file")
        tf = Terraform(working_dir=cwd, state="tfstate.test")
        assert tf.tfstate.modules[0]["path"] == ["root"]

    @pytest.mark.parametrize(
        ("folder", "variables"), [("var_to_output", {"test_var": "test"})]
    )
    def test_override_default(self, folder, variables):
        tf = Terraform(working_dir=current_path, variables=variables)
        tf.init(chdir=folder)
        ret, out, err = tf.apply(
            chdir=folder, var={"test_var": "test2"}, no_color=IsNotFlagged,
        )
        out = out.replace("\n", "")
        assert "\x1b[0m\x1b[1m\x1b[32mApply" in out
        out = tf.output("test_output", chdir=folder)
        assert "test2" in out

    @pytest.mark.parametrize("output_all", [True, False])
    def test_output(self, caplog: LogCaptureFixture, output_all: bool):
        expected_value = "test"
        required_output = "test_output"
        folder = "var_to_output"
        with caplog.at_level(logging.INFO):
            tf = Terraform(
                working_dir=current_path, variables={"test_var": expected_value}
            )
            tf.init(chdir=folder)
            tf.apply(chdir=folder)
            params = tuple() if output_all else (required_output,)
            result = tf.output(*params,  chdir=folder)
        if output_all:
            assert result[required_output]["value"] == expected_value
        else:
            assert result == expected_value
        assert expected_value in caplog.messages[-1]

    def test_destroy(self):
        tf = Terraform(working_dir=current_path, variables={"test_var": "test"})
        tf.init()
        ret, out, err = tf.destroy()
        assert ret == 0
        assert "Destroy complete! Resources: 0 destroyed." in out

    @pytest.mark.parametrize(
        ("folder", "variables", "expected_ret"), [("vars_require_input", {}, 1)]
    )
    def test_plan(self, folder, variables, expected_ret):
        tf = Terraform(working_dir=current_path, variables=variables)
        tf.init(chdir=folder)
        with pytest.raises(TerraformCommandError) as e:
            tf.plan(chdir=folder)
        assert (
            e.value.err
            == """\nError: Missing required argument\n\nThe argument "region" is required, but was not set.\n"""
        )

    def test_fmt(self, fmt_test_file):
        tf = Terraform(working_dir=current_path, variables={"test_var": "test"})
        ret, out, err = tf.fmt(diff=True)
        assert ret == 0

    def test_create_workspace(self, workspace_setup_teardown):
        workspace_name = "test"
        with workspace_setup_teardown(workspace_name, create=False) as tf:
            ret, out, err = tf.create_workspace("test")
        assert ret == 0
        assert err == ""

    def test_create_workspace_with_args(self, workspace_setup_teardown, caplog):
        workspace_name = "test"
        state_file_path = os.path.join(
            current_path, "test_tfstate_file2", "terraform.tfstate"
        )
        with workspace_setup_teardown(
            workspace_name, create=False
        ) as tf, caplog.at_level(logging.INFO):
            ret, out, err = tf.create_workspace(
                "test", chdir=current_path, no_color=IsFlagged
            )

        assert ret == 0
        assert err == ""
        assert (
            f"Command: terraform -chdir={current_path} workspace new -no-color test"
            in caplog.messages
        )

    def test_set_workspace(self, workspace_setup_teardown):
        workspace_name = "test"
        with workspace_setup_teardown(workspace_name) as tf:
            ret, out, err = tf.set_workspace(workspace_name)
        assert ret == 0
        assert err == ""

    def test_set_workspace_with_args(self, workspace_setup_teardown, caplog):
        workspace_name = "test"
        with workspace_setup_teardown(workspace_name) as tf, caplog.at_level(
            logging.INFO
        ):
            ret, out, err = tf.set_workspace(
                workspace_name, chdir=current_path, no_color=IsFlagged
            )

        assert ret == 0
        assert err == ""
        assert (
            f"Command: terraform -chdir={current_path} workspace select -no-color test"
            in caplog.messages
        )

    def test_show_workspace(self, workspace_setup_teardown):
        workspace_name = "test"
        with workspace_setup_teardown(workspace_name) as tf:
            ret, out, err = tf.show_workspace()
        assert ret == 0
        assert err == ""

    def test_show_workspace_with_no_color(self, workspace_setup_teardown, caplog):
        workspace_name = "test"
        with workspace_setup_teardown(workspace_name) as tf, caplog.at_level(
            logging.INFO
        ):
            ret, out, err = tf.show_workspace(no_color=IsFlagged)

        assert ret == 0
        assert err == ""
        assert "Command: terraform workspace show -no-color" in caplog.messages

    def test_delete_workspace(self, workspace_setup_teardown):
        workspace_name = "test"
        with workspace_setup_teardown(workspace_name, delete=False) as tf:
            tf.set_workspace("default")
            ret, out, err = tf.delete_workspace(workspace_name)
        assert ret == 0
        assert err == ""

    def test_delete_workspace_with_args(self, workspace_setup_teardown, caplog):
        workspace_name = "test"
        with workspace_setup_teardown(
            workspace_name, delete=False
        ) as tf, caplog.at_level(logging.INFO):
            tf.set_workspace("default")
            ret, out, err = tf.delete_workspace(
                workspace_name, chdir=current_path, force=IsFlagged,
            )

        assert ret == 0
        assert err == ""
        assert (
            f"Command: terraform -chdir={current_path} workspace delete -force test"
            in caplog.messages
        )