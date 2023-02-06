import yatest.common
import sys


BINARY_ARCINFO = yatest.common.binary_path("search/begemot/rules/thesaurus/lib/tools/arcinfo/arcinfo")
PRODUCTION_ARCHIVE_PATH = yatest.common.build_path("search/wizard/data/wizard/ThesaurusData/archive")

ARCHIVE_PATHS = [
    ("production/model/ru.bundle", "MatrixNetModelsBundle"),
    ("production/model/tr.bundle", "MatrixNetModelsBundle"),
    ("production/model/ru.clicks", "MatrixNetModel"),
    ("production/model/ru.markup", "MatrixNetModel"),
    ("production/model/ru.web_assessor", "MatrixNetModel"),
    ("production/model/tr", "MatrixNetModel"),
    ("production/dict/porno", "GazetteerDict"),
    ("production/dict/main", "GazetteerDict")
]


def get_paths_by_type():
    d = {}
    for path, resType in ARCHIVE_PATHS:
        l = d.get(resType, [])
        l.append(path)
        d[resType] = l

    return d


def do_test_production_resources(resTypes, suffix):
    outputPath = "loaded_{}.txt".format(suffix)
    outputFile = open(outputPath, "w")

    for resType in resTypes:
        paths = get_paths_by_type()[resType]

        pathsPath = yatest.common.work_path("paths.txt")
        open(pathsPath, "w").write("\n".join(paths))

        command = [
            BINARY_ARCINFO, "load",
            "-t", resType,
            "-r", PRODUCTION_ARCHIVE_PATH,
            "-"]

        sys.stderr.write("Execute: {}\n".format(command))

        yatest.common.execute(
            command,
            stdin=open(pathsPath),
            stdout=outputFile)

    return [yatest.common.canonical_file(outputPath)]


def test_production_bundles():
    return do_test_production_resources(["MatrixNetModelsBundle"], suffix="bundles")


def test_production_models():
    return do_test_production_resources(["MatrixNetModel"], suffix="models")


def test_production_dicts():
    return do_test_production_resources(["GazetteerDict"], suffix="dicts")
