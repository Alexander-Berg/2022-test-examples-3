def mypy_check_root() -> str:
    # Specify path relative to Arcadia root,
    # pointing to subdirectory of your project.
    # Everything else will be autogenerated.

    return 'cloud/mdb/ui/internal'


def mypy_config_resource() -> tuple[str, str]:
    return "__tests__", "cloud/mdb/ui/internal/mypy.ini"
