#include <util/folder/path.h>
#include <util/memory/tempbuf.h>
#include <util/system/file.h>

int main(int argc, char* argv[]) {
    Y_ENSURE(argc == 2);
    const TFsPath outputDir(argv[1]);
    outputDir.MkDirs();
    TFile file(outputDir / "test_file", CreateAlways | RdWr);
    const TString output = "test_data";
    file.Write(output.data(), output.size());
    TTempBuf buf;
    file.Read(buf.Data(), buf.Size());
    return 0;
}
