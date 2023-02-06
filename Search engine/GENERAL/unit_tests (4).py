import json

from library.python import resource
import search.tools.devops.libs.parsers.modify_resource as mr
import search.tools.devops.libs.parsers.modify_degrade_level as mdl


CANON_ARGS = json.loads(resource.find("CanonicalArguments"))


FAIL_MESSAGE = (
    "Option `%s` not found in arguments of script `%s`. "
    "If you want to remove it, do it gradually: at first add "
    "this option to deprecated options list, so people using "
    "script with this option will have an opportunity to "
    "challenge removal or adapt to it during deprecation period. "
    "After two weeks (or in planned complete removal date) "
    "you should remove this option completely (from script "
    "arguments, from list of deprecated arguments and from "
    "canonical arguments). If you do not want to remove this "
    "option, please restore it. For details see "
    "https://st.yandex-team.ru/SEARCH-8282."
)


CANONIZE_MESSAGE = (
    "Please, canonize argument `%s` of script `%s`. For details see "
    "https://st.yandex-team.ru/SEARCH-8282."
)


def check_args(existing_args, script_name):
    script_info = CANON_ARGS["scripts"][script_name]
    canon_args = (
        script_info["script_args"]
        + CANON_ARGS["common_args"] * script_info["use_common_args"]
        + CANON_ARGS["verbose_arg"] * script_info["use_verbose_arg"]
    )
    for opt_name in canon_args:
        assert opt_name in existing_args, FAIL_MESSAGE % (opt_name,
                                                          script_name)
    for opt_name in existing_args:
        assert opt_name in canon_args, CANONIZE_MESSAGE % (opt_name,
                                                           script_name)


def test_args_existence():
    check_args(mr.get_arguments(keys_only=True), "modify_resource")
    check_args(mdl.get_arguments(keys_only=True), "modify_degrade_level")
