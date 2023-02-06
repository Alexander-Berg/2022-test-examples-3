import yatest.common
import md5
import os
import sys

from gen import make_pool_file

def get_file_path(prefix, formatName):
    return "./pool.{0}.{1}".format(prefix, formatName)

conv_binary = yatest.common.binary_path("search/tools/idx_ops/converter/pool_converter")

in_formats = ["mr-proto", "final", "sortable", "tsv", "mr-tsv"]
out_formats = ["mr-proto", "final", "sortable", "tsv", "mr-tsv", "mr-zeus"] ## should be superset of in_formats

class Converter:
    def __init__(self, inFormat, outFormat):
        self.Path = conv_binary
        self.InFormat = inFormat
        self.OutFormat = outFormat

    def get_cmd_line(self, inPath, outPath, args):
        return "{0} -i {1} -o {2} {3} {4} {5}".format(self.Path,
            inPath, outPath, " ".join(args), self.InFormat, self.OutFormat)

    def run(self, inPath, outPath, args):
        cmdLine = self.get_cmd_line(inPath, outPath, args)

        sys.stderr.write("Executing: {0}\n".format(cmdLine))
        status = os.system(cmdLine)
        if status:
            sys.stderr.write("Aborted with status {0}\n".format(status))
            sys.exit(status)

class Checker:
    def __init__(self, leftPath, rightPath):
        self.LeftPath = leftPath
        self.RightPath = rightPath

    def check_md5(self, msg):
        leftMd5 = md5.new()
        leftMd5.update(open(self.LeftPath).read())
        rightMd5 = md5.new()
        rightMd5.update(open(self.RightPath).read())
        if leftMd5.digest() != rightMd5.digest():
            sys.stderr.write("Failed MD5 check. {0}\n".format(msg))
            sys.exit(1)


def test_with_tsv_base():
    in_tsv = get_file_path("in", "tsv")
    out_tsv= get_file_path("out", "tsv")

    make_pool_file(1000, 100, 1984, open(in_tsv, "w"))
    make_pool_file(1000, 100, 1984, open(out_tsv, "w"))

    for f in out_formats:
        if f == "tsv":
            continue

        in_f = get_file_path("in", f)
        out_f = get_file_path("out", f)

        if f == "mr-zeus":
            args = ["-l", "10"]
        else:
            args = []

        Converter("tsv", f).run(in_tsv, in_f, args)
        Converter("tsv", f).run(out_tsv, out_f, args)

    for f1 in in_formats:
        in_f = get_file_path("in", f1)
        if not os.path.exists(in_f):
            sys.stderr.write("Failed to find temporary file \"{0}\"".format(in_f))
            sys.exit(1)

        for f2 in out_formats:
            out_f = get_file_path("out", f2)
            if not os.path.exists(out_f):
                sys.stderr.write("Failed to find temporary file \"{0}\"".format(out_f))
                sys.exit(1)

            out_check_f = get_file_path("out.check", f2)
            if f2 == "mr-zeus":
                args = ["-l", "10"]
            else:
                args = []

            Converter(f1, f2).run(in_f, out_check_f, args)
            Checker(out_f, out_check_f).check_md5("Diff when converting {0} -> {1}".format(f1, f2))

    return [yatest.common.canonical_file(get_file_path("out", f)) for f in out_formats]

