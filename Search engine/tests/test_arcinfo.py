import yatest.common
import sys


BINARY_ARCINFO = yatest.common.binary_path("search/begemot/rules/thesaurus/lib/tools/arcinfo/arcinfo")

ARCHIVE_PATHS = [
    "production/model/ru1",
    "production/model/ru2",
    "production/model/ru3",
    "production/model/tr1",
    "production/model/xyz",
    "production/model/n1",
    "production/model/n2",
    "production/ledom/ru1",
    "foo",
    "production/parse_error/file.dat",
    "production/parse_error/file2.dat",
    "experiment/flag/enable_exp",
    "experiment/flag/override_transitive",
    "experiment/flag/override_broken",
    "experiment/flag/override_by_broken",
    "experiment/flag/override_flag",
    "experiment/flag/override_type_mismatch",
    "experiment/flag/override_bad_name",
    "experiment/flag/override_by_bad_name",
    ("production/model/ru1", ("enable_exp",)),
    ("production/model/ru1", ("override_transitive",)),
    ("experiment/model/ru_exp", ("override_transitive",)),
    ("production/model/ru2", ("override_broken",)),
    ("production/model/ru1", ("override_by_broken",))
]


def filter_exception_paths(path):
    command = [
        "sed",
        "-i",
        "-e",
        r"s/\([a-zA-Z0-9_.-]*\/\)*[a-zA-Z0-9_.-]*\(h\|cpp\):[0-9]\+:/<code path removed>:/g",
        path
    ]
    sys.stderr.write("Execute: {}\n".format(command))
    yatest.common.execute(command)


def test_arcinfo_parse():
    pathsFile = yatest.common.work_path("paths.txt")
    open(pathsFile, "w").write("\n".join([x for x in ARCHIVE_PATHS if isinstance(x, str)]))

    command = [BINARY_ARCINFO, "parse", "-r", "./archive", "--dont-stop-on-error", "-"]
    sys.stderr.write("Execute: {}\n".format(command))
    yatest.common.execute(
        command,
        stdin=open(pathsFile),
        stdout=open("parsed.txt", "w"))

    filter_exception_paths("parsed.txt")
    return [yatest.common.canonical_file("parsed.txt")]


def do_test_arcinfo_load(is_multi_threaded=False, is_verify_get=False):
    paths_by_flags = {}
    all_flags = set()
    for entry in ARCHIVE_PATHS:
        if isinstance(entry, tuple):
            flags = entry[1]
            path = entry[0]
        else:
            flags = tuple()
            path = entry

        all_flags.add(flags)
        paths_by_flags[flags] = paths_by_flags.get(flags, []) + [path]

    all_flags = list(all_flags)
    all_flags.sort()

    prefix = "loaded"
    if is_multi_threaded:
        prefix += "_multi_threaded"

    outputPath = prefix + ".txt"
    outputFile = open(outputPath, "w")
    errFile = open(prefix + ".err", "w")

    for flags in all_flags:
        pathsFile = yatest.common.work_path("paths.txt")
        open(pathsFile, "w").write("\n".join(paths_by_flags.get(flags)))

        command = [
            BINARY_ARCINFO,
            "load",
            "-r",
            "./archive",
            "--dont-stop-on-error"] \
            + sum([["-f", x] for x in flags], []) \
            + ["-"] \
            + (["--test-multiple-threads"] if is_multi_threaded else []) \
            + (["--test-verify-get"] if is_verify_get else [])

        sys.stderr.write("Execute: {}\n".format(command))

        if flags:
            outputFile.write("---> FLAGS: {}\n".format(", ".join(flags)))
            outputFile.write("\n")
            outputFile.flush()

        yatest.common.execute(
            command,
            stdin=open(pathsFile),
            stdout=outputFile,
            stderr=errFile)

        outputFile.write("\n")
        outputFile.flush()

    outputFile.close()
    errFile.close()

    filter_exception_paths(outputPath)
    return [yatest.common.canonical_file(outputPath)]


def test_arcinfo_load():
    return do_test_arcinfo_load(is_verify_get=True)


def test_arcinfo_load_multi_threaded():
    return do_test_arcinfo_load(is_multi_threaded=True)
