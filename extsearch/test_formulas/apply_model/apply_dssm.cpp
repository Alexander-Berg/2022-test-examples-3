#include <extsearch/images/robot/library/mropt/mropt.h>
#include <extsearch/images/robot/library/opt/opt.h>
#include <extsearch/images/robot/library/opt/tables.h>
#include <extsearch/images/robot/library/identifier/document.h>
#include <extsearch/images/robot/library/logger/logging.h>
#include <extsearch/images/robot/library/io/io_yt.h>
#include <mapreduce/yt/interface/client.h>
#include <ml/dssm/dssm/lib/dssm.h>
#include <util/string/join.h>
#include <util/folder/path.h>

#include <extsearch/images/kernel/dssm_applier/dssm_applier.h>

#include <library/cpp/dot_product/dot_product.h>

#include <kernel/dssm_applier/nn_applier/lib/layers.h>

using namespace NImages;

constexpr char DOC_ID_COLUMN[] = "document_id";
constexpr char FEATURES_COLUMN[] = "doc_features";

NYT::TTableSchema MakeDocFeaturesOutputSchema() {
    NYT::TTableSchema schema;
    schema.AddColumn(DOC_ID_COLUMN, NYT::EValueType::VT_STRING);
    schema.AddColumn(FEATURES_COLUMN, NYT::EValueType::VT_ANY);

    return schema;
};


class TApplyDssm: public NYT::IMapper<NYT::TTableReader<NYT::TNode>, NYT::TTableWriter<NYT::TNode>> {
private:
    NDssm3::IModelPtr<float> Model;
    TString ModelName;
    TString OutputFieldName;

    TVector<TString> QueryFieldNames;
    TVector<TString> DocFieldNames;

public:
    TApplyDssm() = default;

    TApplyDssm(const TString& modelName, const TString& outputFieldName)
        : ModelName(modelName)
        , OutputFieldName(outputFieldName)
    {
    }

    void Start(TWriter* /*writer*/) override {
        auto modelFactory = NDssm3::TModelFactory<float>::Load(ModelName);
        Model = modelFactory->CreateModel();

        QueryFieldNames = modelFactory->Subparams[0]->SampleProcessor.QueryFields;
        DocFieldNames = modelFactory->Subparams[0]->SampleProcessor.DocFields;
    }

    void Do(TReader* input, TWriter* output) override {
        for (; input->IsValid(); input->Next()) {
            NYT::TNode row = input->GetRow();

            NDssm3::TSample querySample;
            NDssm3::TSample docSample;

            for (const TString& fieldName : DocFieldNames) {
                docSample.Doc.push_back(row[fieldName].AsString());
            }

            for (const TString& fieldName : QueryFieldNames) {
                querySample.Query.push_back(row[fieldName].AsString());
            }

            NDssm3::TBatch batch;
            batch.push_back(querySample);

            const auto& queryEmbedding = Model->EmbedQuery(batch, 0);

            NDssm3::TBatch batch2;
            batch2.push_back(docSample);
            const auto& docEmbedding = Model->EmbedDoc(batch2, 0);

            NDssm3::TMatrix<float> similarity;
            similarity.Resize(1, 1);
            NDssm3::TLinearTransform::Fprop(docEmbedding, queryEmbedding, &similarity, true);

            output->AddRow(row(OutputFieldName, similarity[0][0]));
        }
    }

    Y_SAVELOAD_JOB(ModelName, OutputFieldName);
};

class TApplyDssmNNApplyer: public NYT::IMapper<NYT::TTableReader<NYT::TNode>, NYT::TTableWriter<NYT::TNode>> {
private:
    NNeuralNetApplier::TModel DocModel;
    TString DocModelName;
    NNeuralNetApplier::TModel QueryModel;
    TString QueryModelName;
    TString OutputFieldName;

    TVector<TString> DocFieldNames;
    TVector<TString> QueryFieldNames;

public:
    TApplyDssmNNApplyer() = default;

    TApplyDssmNNApplyer(const TString& docModelName,
                        const TString& queryModelName,
                        const TString& outputFieldName,
                        const TVector<TString>& docFields,
                        const TVector<TString>& queryFields)
            : DocModelName(docModelName)
            , QueryModelName(queryModelName)
            , OutputFieldName(outputFieldName)
            , DocFieldNames(docFields)
            , QueryFieldNames(queryFields)
    {
    }

    void Start(TWriter* /*writer*/) override {
        DocModel.Load(TBlob::FromFile(DocModelName));
        QueryModel.Load(TBlob::FromFile(QueryModelName));
    }

    void Do(TReader* input, TWriter* output) override {
        for (; input->IsValid(); input->Next()) {
            NYT::TNode row = input->GetRow();

            TVector<float> docEmbedding = ApplyModel(row, DocFieldNames, DocModel, {"doc_embedding"});
            TVector<float> queryEmbedding = ApplyModel(row, QueryFieldNames, QueryModel, {"query_embedding"});

            Y_ENSURE(queryEmbedding.size() == docEmbedding.size(), "Lengths of embeddings are not equal");
            float result = DotProduct(docEmbedding.data(), queryEmbedding.data(), docEmbedding.size());
            output->AddRow(row(OutputFieldName, result));
        }
    }

    TVector<float> ApplyModel(const NYT::TNode& row,
                                const TVector<TString>& fieldNames,
                                NNeuralNetApplier::TModel& model,
                                TVector<TString> outputName) const
    {
        TVector<TString> docValues;
        for (const TString& name : fieldNames) {
            docValues.push_back(row[name].AsString());
        }

        TVector<float> docEmbedding;
        TAtomicSharedPtr<NNeuralNetApplier::ISample> sample = new NNeuralNetApplier::TSample(fieldNames, docValues);
        model.Apply(sample, outputName, docEmbedding);

        return docEmbedding;
    }

    Y_SAVELOAD_JOB(DocModelName, QueryModelName, OutputFieldName, QueryFieldNames, DocFieldNames);
};

REGISTER_MAPPER(TApplyDssm);
REGISTER_MAPPER(TApplyDssmNNApplyer);

int ApplyDssm(int argc, const char* argv[]) {
    TMrOpts mrOpts;
    TTablesNameOpts tablesNameOpts;
    TString scoreFieldName;
    TString modelFileName;
    NImages::TIndexOpts indexOpts;

    TCmdParams cmd;
    TMrOptsParser(cmd, mrOpts)
            .AddAllMrParams();

    TTablesNameOptsParser(cmd, tablesNameOpts)
            .AddInputTable()
            .AddOutputTable();

    cmd.AddRequired("score-field", "Score field name", "<string>", &scoreFieldName);
    cmd.AddRequired("model-file", "File name with model", "<string>", &modelFileName);

    cmd.Parse(argc, argv);

    auto client = NYT::CreateClient(mrOpts.ServerName);

    NLog::TScope scope("Apply dssm model");
    {
        NYT::TMapOperationSpec spec;

        spec.AddInput<NYT::TNode>(tablesNameOpts.InputTable);
        spec.AddOutput<NYT::TNode>(tablesNameOpts.OutputTable);

        NYT::TUserJobSpec userJobSpec;
        userJobSpec.AddLocalFile(modelFileName);
        spec.MapperSpec(userJobSpec);

        client->Map(spec, new TApplyDssm(TFsPath(modelFileName).GetName(), scoreFieldName), NImages::MakeMapOpts(mrOpts));
    }

    NLog::Message("sort scored table");
    {
        NYT::TSortOperationSpec spec;
        spec.SortBy({"batch_id"});

        spec.AddInput(NYT::TRichYPath(tablesNameOpts.OutputTable));
        spec.Output(NYT::TRichYPath(tablesNameOpts.OutputTable));

        client->Sort(spec);
    }

    return 0;
}

int ApplyDssmNNApplyer(int argc, const char* argv[]) {
    TMrOpts mrOpts;
    TTablesNameOpts tablesNameOpts;
    TString scoreFieldName;
    TString docModelFileName;
    TString queryModelFileName;
    NImages::TIndexOpts indexOpts;
    TString queryFieldsStr;
    TString docFieldsStr;

    TCmdParams cmd;
    TMrOptsParser(cmd, mrOpts)
            .AddAllMrParams();

    TTablesNameOptsParser(cmd, tablesNameOpts)
            .AddInputTable()
            .AddOutputTable();

    cmd.AddRequired("score-field", "Score field name", "<string>", &scoreFieldName);
    cmd.AddRequired("doc-model-file", "File name with model", "<string>", &docModelFileName);
    cmd.AddRequired("query-model-file", "File name with model", "<string>", &queryModelFileName);
    cmd.AddRequired("query-fields", "Query fields comm separetad", "<string>", &queryFieldsStr);
    cmd.AddRequired("doc-fields", "Doc fields comm separetad", "<string>", &docFieldsStr);

    cmd.Parse(argc, argv);

    TVector<TString> queryFields;
    Split(queryFieldsStr, ",", queryFields);

    TVector<TString> docFields;
    Split(docFieldsStr, ",", docFields);

    auto client = NYT::CreateClient(mrOpts.ServerName);

    NLog::TScope scope("Apply dssm model");
    {
        NYT::TMapOperationSpec spec;

        spec.AddInput<NYT::TNode>(tablesNameOpts.InputTable);
        spec.AddOutput<NYT::TNode>(tablesNameOpts.OutputTable);

        NYT::TUserJobSpec userJobSpec;
        userJobSpec.AddLocalFile(docModelFileName);
        userJobSpec.AddLocalFile(queryModelFileName);
        spec.MapperSpec(userJobSpec);

        client->Map(spec, new TApplyDssmNNApplyer(TFsPath(docModelFileName).GetName(),
                                                  TFsPath(queryModelFileName).GetName(),
                                                  scoreFieldName,
                                                  docFields,
                                                  queryFields),
                    NImages::MakeMapOpts(mrOpts));
    }

    NLog::Message("sort scored table");
    {
        NYT::TSortOperationSpec spec;
        spec.SortBy({"batch_id"});

        spec.AddInput(NYT::TRichYPath(tablesNameOpts.OutputTable));
        spec.Output(NYT::TRichYPath(tablesNameOpts.OutputTable));

        client->Sort(spec);
    }

    return 0;
}
