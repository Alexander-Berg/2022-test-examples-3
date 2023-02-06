#include <mapreduce/yt/client/init.h>

#include <library/cpp/getopt/small/modchooser.h>

int ApplyDssm(int argc, const char* argv[]);
int ApplyDssmNNApplyer(int argc, const char* argv[]);

int main(int argc, const char* argv[]) {
    NYT::Initialize(argc, argv);

    TModChooser modChooser;
    modChooser.AddMode("ApplyDssm", ApplyDssm, "Calc score dssm3");
    modChooser.AddMode("ApplyDssmNNApplyer", ApplyDssmNNApplyer, "Calc score dssm applyer");

    return modChooser.Run(argc, argv);
}

