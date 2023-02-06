#include <search/geo/tools/mrlogstat/common/population_tree.h>

void Print(const TRegionsDB& db, const TPopulationTree& tree, TGeoRegion region) {
    Cout << "Paths for " << region << " (" << db.GetName(region) << ") :" << Endl;

    const TVector<TString>& ps = tree.GetPaths(region);
    for (const TString& p : ps) {
        Cout << p << Endl;
    }
    Cout << Endl;
}

int main() {
    TRegionsDB db("/var/cache/geobase/geodata3.bin");
    TPopulationTree tree(db);

    Print(db, tree, 225);
    Print(db, tree, 0);
    Print(db, tree, 213);
    Print(db, tree, 1);
    Print(db, tree, 2);
    Print(db, tree, 149);
    Print(db, tree, 157);
    Print(db, tree, 153);
    Print(db, tree, 8);
    Print(db, tree, 21015);
    Print(db, tree, 101766);
    Print(db, tree, 102175);
    Print(db, tree, 12345678);
    Print(db, tree, 216);
    Print(db, tree, 20468);
    Print(db, tree, 177);
}
