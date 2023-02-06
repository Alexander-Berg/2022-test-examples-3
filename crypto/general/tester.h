#pragma once
#include <crypta/graph/mrcc_opt/lib/mrcc.h>
#include <crypta/graph/mrcc_opt/lib/data.h>
#include <mapreduce/yt/interface/client.h>
#include <util/random/random.h>

using namespace NYT;
using namespace NConnectedComponents;

namespace NTest {



    static void CreateTable(IClientPtr client, const NYT::TRichYPath& path, const TVector<TNode>& records) {
        auto writer = client->CreateTableWriter<TNode>(path);
        for (const auto& record: records) {
            writer->AddRow(record);
        }
        writer->Finish();
    }

    static void CreateTable(IClientPtr client, const TString& path, const TVector<TNode>& records, bool append = false) {
        return CreateTable(client, TRichYPath(path).Append(append), records);
    }

    template <typename TDataView>
    TDataPaths<TDataView> PrepareWorkdir(IClientPtr client, TDataView dataView, bool withPreviousLabels = false, const TString& workdirPath = "//tmp/test/workdir") {
        TString workdir = JoinYPaths(workdirPath, CreateGuidAsString());
        client->Create(workdir, NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true).IgnoreExisting(true));
        TString source = JoinYPaths(workdir, "edges");
        TString destination = JoinYPaths(workdir, "destination");
        TString previousLabels = JoinYPaths(workdir, "previousLabels");
        if (!withPreviousLabels) {
            previousLabels = "";
        }
        TDataPaths dataPaths(dataView, source, destination, workdir, previousLabels);
        return dataPaths;
    }

    template <typename TIdType>
    class ComponentsTester {
    public:
        ComponentsTester(IClientPtr client, const TDataPaths<TGeneralDataView>& dataPaths) {
            Init(client, dataPaths.DataView, dataPaths.SourceData, dataPaths.PreviousLabels);
        }

        ComponentsTester (const TVector<std::pair<TIdType, TIdType>>& edges) {
            Init(edges);
        }

        void Init(IClientPtr client, const TGeneralDataView& dataView, const TString& edgesPath, const TString& previousLabels = "") {
            TVector<std::pair<TIdType, TIdType>> edges;
            auto reader = client->CreateTableReader<TNode>(edgesPath);
            for (; reader->IsValid(); reader->Next()) {
                const auto& row = reader->GetRow();
                const auto source = GetID<TIdType>(row, dataView.Edge.FirstIDFields);
                const auto target = GetID<TIdType>(row, dataView.Edge.SecondIDFields);
                edges.push_back(std::make_pair(source, target));
            }
            if (previousLabels) {
                auto reader = client->CreateTableReader<TNode>(previousLabels);
                THashMap<TIdType, TIdType> componentToVertex;
                for (; reader->IsValid(); reader->Next()) {
                    const auto& row = reader->GetRow();
                    auto source = GetID<TIdType>(row, dataView.Vertex.IDFields);
                    auto target = row[dataView.Vertex.ComponentField].ConvertTo<TIdType>();
                    if (!componentToVertex.count(target)) {
                        componentToVertex[target] = source;
                    }
                    target = componentToVertex[target];
                    edges.push_back(std::make_pair(source, target));
                }
            }
            Init(edges);
        }

        void Init(const TVector<std::pair<TIdType, TIdType>>& edges) {
            int index = 0;
            for (const auto& item : edges) {
                const auto& source = item.first;
                const auto& target = item.second;
                if (!IndexedVertices.count(source)) {
                    IndexedVertices[source] = index++;
                }
                if (!IndexedVertices.count(target)) {
                    IndexedVertices[target] = index++;
                }
            }

            TVector<TVector<ui64>> graph(index);
            for (const auto& item: edges) {
                auto source  = IndexedVertices[item.first];
                auto target = IndexedVertices[item.second];
                graph[source].push_back(target);
                graph[target].push_back(source);
            }
            Graph = graph;

            ComputeComponents();
        }

        bool CheckComponents(IClientPtr client, const TDataPaths<TGeneralDataView>& dataPaths) {
            return CheckComponents(client, dataPaths.DataView, dataPaths.DestinationComponents);
        }

        bool CheckComponents(IClientPtr client, const TGeneralDataView& dataView, const TString& componentsPath) {
            auto reader = client->CreateTableReader<TNode>(componentsPath);
            THashMap<TIdType, ui64> labels;
            for (; reader->IsValid(); reader->Next()) {
                const auto& row = reader->GetRow();
                labels[GetID<TIdType>(row, dataView.Vertex.IDFields)] = row[dataView.Vertex.ComponentField].AsUint64();
            }
            return CheckComponents(labels);
        }

        bool CheckComponents(const THashMap<TIdType, ui64>& labels) {
            TVector<ui64> indexedLabels(NumComponents);
            for (const auto& item: labels) {
                indexedLabels[IndexedVertices[item.first]] = item.second;
            }
            return CheckComponents(indexedLabels);
        }

    private:
        THashMap<TIdType, ui64> IndexedVertices{};
        TVector<TVector<ui64>> Graph{};
        TVector<ui64> Labels{};
        int NumComponents = 0;

        bool CheckComponents(const TVector<ui64>& labels) {
            THashMap<ui64, ui64> tr;
            THashMap<ui64, ui64> reversedTr;
            for (ui32 i = 0; i < labels.size(); ++i) {
                auto label = labels[i];
                auto realLabel = Labels[i];
                if (!tr.count(label)) {
                    if (reversedTr.count(realLabel)) {
                        return false;
                    }
                    tr[label] = realLabel;
                    reversedTr[realLabel] = label;
                }
                if (tr[label] != realLabel) {
                    return false;
                }
            }
            for (ui32 i = 0; i < labels.size(); ++i) {
                auto label = labels[i];
                auto realLabel = Labels[i];
                if (!(label == reversedTr[realLabel]) || !(tr[label] == realLabel)) {
                    return false;
                }
            }
            return true;
        }

        void MarkComponent(ui64 label, ui64 vertex, TVector<int>& used) {
            if (used[vertex]) {
                return;
            }
            used[vertex] = 1;
            Labels[vertex] = label;
            for (const auto& neighbour: Graph[vertex]) {
                MarkComponent(label, neighbour, used);
            }
        }

        void ComputeComponents() {
            TVector<int> used(Graph.size(), 0);
            Labels = TVector<ui64>(Graph.size(), 0);
            ui64 label = 0;
            for (ui32 i = 0; i < Graph.size(); ++i) {
                MarkComponent(label++, i, used);
            }
            NumComponents = label;
        }
    };
}
