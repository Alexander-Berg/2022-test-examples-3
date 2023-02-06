#include "testing_export_data.h"

#include <search/web/configurable_rearrange/configs/data_io.h>
#include <search/web/configurable_rearrange/graph_applier.h>

using namespace NConfigurableRearrange;

bool TExportDataTester::TestBuild(IOutputStream* out) const {
    if (data.GraphsSize() == 0) {
        (*out) << "Total number of graphs 0" << Endl;
    }
    if (data.PatchesSize() == 0) {
        (*out) << "Total number of patches 0" << Endl;
    }
    return data.GraphsSize() > 0 && data.PatchesSize() > 0;
}


bool TExportDataTester::TestUniqueGraphNames(IOutputStream* out) const {
    bool result = true;
    THashSet<TString> names;

    for (const auto& graph : data.GetGraphs()) {
        if (names.emplace(graph.GetGraphName()).second == false) {
            result = false;
            (*out) << "Duplication graph name: " << graph.GetGraphName() << Endl;
        }
    }

    return result;
}


bool TExportDataTester::TestUniquePatchNames(IOutputStream* out) const {
    bool result = true;
    THashSet<std::pair<TString, TString>> names;

    for(const auto& patch : data.GetPatches()) {
        const TString& name = patch.GetPatchName();
        const TString& ns = patch.GetNamespace();

        if (names.emplace(std::make_pair(ns, name)).second == false) {
            result = false;
            (*out) << "duplication patch name \"" << name <<
                    "\" in namespace \"" << ns << "\"." << Endl;
        }
    }
    return result;
}


bool TExportDataTester::TestGraphIsCorrect(IOutputStream* out) const {
    bool result = true;
    for(const auto& graph : data.GetGraphs()) {
        try {
            TGraphApplier(graph, {});
        }
        catch(...) {
            result = false;
            (*out) << "Invalid graph :"  << graph.GetGraphName() <<
                    " (TGraphApplier(graph, {}); threw an exception:" << Endl <<
                    CurrentExceptionMessage() << Endl;
        }
    }
    return result;
}


bool TExportDataTester::TestPatchesOnUniqueApplication(IOutputStream* out) const {
    if(data.PatchesSize() == 1) {
        return true;
    }

    //PatchesSize() >= 2
    bool result = true;
    for(const auto& patch : data.GetPatches()) {
        if(patch.GetUniqueApplication() == true) {
            result = false;
            (*out) << "Patch \"" << patch.GetPatchName() <<
                      "\" has a field \"UniqueApplication\" = true," <<
                       "but the number of patches >= 2" << Endl;
        }
    }
    return result;
}


bool TExportDataTester::TestPatchesOnProductionGraphs(IOutputStream* out) const {
    THashMap<TStringBuf, const TGraph*> productionGraphs;

    for (const auto& graph : data.GetGraphs()) {
        if (IsProductionGraph(graph)) {
            productionGraphs[graph.GetGraphName()] = &graph;
        }
    }

    bool result = true;

    for(const auto& patch : data.GetPatches()) {
        const TGraph** graph = productionGraphs.FindPtr(patch.GetDefaultBaseGraph());

        if (graph == nullptr) {
            continue;
        }
        try {
            TGraphApplier(**graph, {&patch});
        }
        catch (...) {
            result = false;
            (*out) << "invalid patch : \"" << patch.GetPatchName() <<
                    "\" in namespace \"" << patch.GetNamespace() <<
                    "\" with Graph \"" << patch.GetDefaultBaseGraph() <<
                    "\". (TGraphApplier(Graph, Patch) threw an exception:" << Endl <<
                    CurrentExceptionMessage() << Endl;
        }
    }
    return result;
}


bool TExportDataTester::TestAutonomyOfNamespaces(IOutputStream* out) const {
    THashMap<TString, THashSet<TString>> nodesInNamespaces;
    for(const auto& patch : data.GetPatches()) {
        const TString& ns = patch.GetNamespace();

        for(const TString& node : patch.GetNodesToDelete()){
            nodesInNamespaces[ns].insert(node);
        }
        for(const TNode& node : patch.GetAddNode()) {
            nodesInNamespaces[ns].insert(node.GetNodeName());
        }
    }

    bool result = true;
    THashSet<TString> viewedNodes;

    for(const auto& [ns, nodes] : nodesInNamespaces) {
        for(const auto& node : nodes) {
            if (viewedNodes.emplace(node).second == false) {
            result = false;
            (*out) << "The node with name \"" << node <<
                "\" from namespace \"" << ns <<
                "\" conflicts by name with nodes from other namespaces" << Endl;
            }
        }
    }
    return result;
}


bool TExportDataTester::RunAllTests(IOutputStream* out) const {

    bool isCorrect = true;

    isCorrect &= TestBuild(out);
    isCorrect &= TestUniqueGraphNames(out);
    isCorrect &= TestUniquePatchNames(out);
    isCorrect &= TestGraphIsCorrect(out);
    isCorrect &= TestPatchesOnUniqueApplication(out);
    isCorrect &= TestPatchesOnProductionGraphs(out);
    isCorrect &= TestAutonomyOfNamespaces(out);

    return isCorrect;
}
