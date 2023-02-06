#pragma once

#include <market/library/libyt/YtHelpers.h>

#include <library/cpp/logger/global/global.h>

#include <mapreduce/yt/client/client.h>

#include <util/generic/guid.h>
#include <util/generic/ptr.h>
#include <util/string/cast.h>
#include <util/string/split.h>
#include <util/string/vector.h>

using namespace NYT;

class TMockYtClient: public NYT::IClient, public TRefCounted<TMockYtClient, TAtomicCounter> {
private:
    THashMap<TYPath, TNode::TListType> DATA;
    TAtomicCounter counter;

public:
    TMockYtClient() = default;
    inline void Ref(TAtomicBase d = TAtomicBase{1}) noexcept {
        counter.Add(d);
    }
    inline void UnRef(TAtomicBase d = TAtomicBase{1}) noexcept {
        counter.Sub(d);
    }

    // ICypressClient
public:
    virtual TNodeId Create(
        const TYPath& path,
        ENodeType nodeType,
        const TCreateOptions& options = TCreateOptions()) override
    {
        if (nodeType != ENodeType::NT_TABLE and nodeType != ENodeType::NT_MAP) {
            ythrow yexception() << "Method \"Create\" called with incompatible node type " << nodeType << Endl;
        }
        if (options.Attributes_) {
            DEBUG_LOG << "Storing atrributes " << NMarket::NYTHelper::ConvertNodeToJson(options.Attributes_.GetRef()) << Endl;
            DATA[path + "/@attributes"] = { options.Attributes_.GetRef() };
        }

        DATA[path] = {};

        TNodeId id;
        CreateGuid(&id);
        return id;
    }

    // Create table with schema deduced from `TRowType`.
    // If "schema" is passed in `options.Attributes`
    // it has prority over the deduced schema (the latter is ignored).
    template <typename TRowType>
    TNodeId CreateTable(
        const TYPath& ,
        const TKeyColumns& keyColumns = TKeyColumns(),
        const TCreateOptions&  = TCreateOptions());

    virtual void Remove(
        const TYPath& ,
        const TRemoveOptions&  = TRemoveOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual bool Exists(
        const TYPath& path)  override
    {
        return DATA.contains(path) or DATA.contains(path + "/@path");
    }

    virtual TNode Get(
        const TYPath& path,
        const TGetOptions&  = TGetOptions())  override
    {
        DEBUG_LOG << "Requested GET path: " << path << Endl;
        DEBUG_LOG << "Existing paths: " << Endl;
        for (const auto& [key, value]: DATA) {
            DEBUG_LOG << "key: " << key << " value: " << value.size() << Endl;
        }

        TVector<TString> suffixes = {"/@path", "/@tablet_state", "/@attributes"};
        for (const auto& suffix: suffixes) {
            if (path.EndsWith(suffix)) {
                DEBUG_LOG << "Return an attribute(" << suffix << ") value: "
                          << NMarket::NYTHelper::ConvertNodeToJson(DATA[path][0]) << Endl;
                return DATA[path][0];
            }
        }

        if (not DATA.contains(path)) {
            return TNode{""};
        }
        TNode result = TNode::CreateList();
        for (const auto& row: DATA[path]) {
            result.Add(row);
        }
        return result;
    }

    NSc::TValue GetJson(
        const TYPath& path,
        const TGetOptions&  = TGetOptions())
    {
        if (DATA.contains(path + "/@path")) {
            TYPath realPath = Get(path + "/@path").AsString();
            DEBUG_LOG << path << " is link. Resolved to " << realPath << Endl;
            return GetJson(realPath);
        }

        DEBUG_LOG << "Requested GET JSON path: " << path << Endl;
        DEBUG_LOG << "Existing paths: " << Endl;
        for (const auto& [key, value]: DATA) {
            DEBUG_LOG << "key: " << key << " value: " << value.size() << Endl;
        }

        NSc::TValue result;
        result.GetArrayMutable();  // just create empty vector
        for (const auto& row: DATA[path]) {
            result.GetArrayMutable().emplace_back(
                NMarket::NYTHelper::ConvertNodeToJson(row)
            );
        }
        return result;
    }

    virtual void Set(
        const TYPath& ,
        const TNode& ,
        const TSetOptions&  = TSetOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TNode::TListType List(
        const TYPath& ,
        const TListOptions&  = TListOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TNodeId Copy(
        const TYPath& ,
        const TYPath& ,
        const TCopyOptions&  = TCopyOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TNodeId Move(
        const TYPath& ,
        const TYPath& ,
        const TMoveOptions&  = TMoveOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TVector<TTabletInfo> GetTabletInfos(
        const TYPath& ,
        const TVector<int>& ,
        const TGetTabletInfosOptions&  = TGetTabletInfosOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TNodeId Link(
        const TYPath& src,
        const TYPath& dst,
        const TLinkOptions&  = TLinkOptions())  override
    {
        DATA[dst + "/@path"] = { TNode{src} };
        DEBUG_LOG << "src: " << src << " mounted to dst: " << dst << Endl;

        TNodeId id;
        CreateGuid(&id);
        return id;
    }

    virtual void Concatenate(
        const TVector<TRichYPath>&,
        const TRichYPath&,
        const TConcatenateOptions& = TConcatenateOptions()) override { ythrow yexception() << "Not Implemented"; }

    virtual void Concatenate(
        const TVector<TYPath>& ,
        const TYPath& ,
        const TConcatenateOptions&  = TConcatenateOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TRichYPath CanonizeYPath(const TRichYPath& )  override { ythrow yexception() << "Not Implemented"; }

    virtual TVector<TTableColumnarStatistics> GetTableColumnarStatistics(
        const TVector<TRichYPath>&,
        const TGetTableColumnarStatisticsOptions&)  override { ythrow yexception() << "Not Implemented"; }



    // IIOClient

    virtual IFileReaderPtr CreateFileReader(
        const TRichYPath& ,
        const TFileReaderOptions&  = TFileReaderOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual IFileWriterPtr CreateFileWriter(
        const TRichYPath& ,
        const TFileWriterOptions&  = TFileWriterOptions())  override { ythrow yexception() << "Not Implemented"; }

    template <class T>
    TTableReaderPtr<T> CreateTableReader(
        const TRichYPath& ,
        const TTableReaderOptions&  = TTableReaderOptions());

    template <class T>
    TTableWriterPtr<T> CreateTableWriter(
        const TRichYPath& ,
        const TTableWriterOptions&  = TTableWriterOptions());

    virtual TTableWriterPtr<::google::protobuf::Message> CreateTableWriter(
        const TRichYPath& ,
        const ::google::protobuf::Descriptor& ,
        const TTableWriterOptions&  = TTableWriterOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TRawTableReaderPtr CreateRawReader(
        const TRichYPath& ,
        const TFormat& ,
        const TTableReaderOptions&  = TTableReaderOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TRawTableWriterPtr CreateRawWriter(
        const TRichYPath& ,
        const TFormat& ,
        const TTableWriterOptions&  = TTableWriterOptions())  override { ythrow yexception() << "Not Implemented"; }

    //
    // Read blob table.
    // https://wiki.yandex-team.ru/yt/userdoc/blob_tables/
    //
    // Blob table is a table that stores a number of blobs. Blobs are sliced into parts of the same size (maybe except of last part).
    // Those parts are stored in the separate rows.
    //
    // Blob table have constaints on its schema.
    //  - There must be columns that identify blob (blob id columns). That columns might be of any type.
    //  - There must be a column of int64 type that identify part inside the blob (this column is called `part index`).
    //  - There must be a column of string type that stores actual data (this column is called `data column`).
    virtual IFileReaderPtr CreateBlobTableReader(
        const TYPath& ,
        const TKey& ,
        const TBlobTableReaderOptions&  = TBlobTableReaderOptions())  override { ythrow yexception() << "Not Implemented"; }

private:
    virtual ::TIntrusivePtr<INodeReaderImpl> CreateNodeReader(
        const TRichYPath& , const TTableReaderOptions& )  override
    {
            ythrow yexception() << "Not Implemented";
    }

    virtual ::TIntrusivePtr<IYaMRReaderImpl> CreateYaMRReader(
        const TRichYPath& , const TTableReaderOptions& )  override { ythrow yexception() << "Not Implemented"; }

    virtual ::TIntrusivePtr<IProtoReaderImpl> CreateProtoReader(
        const TRichYPath& ,
        const TTableReaderOptions& ,
        const ::google::protobuf::Message* )  override { ythrow yexception() << "Not Implemented"; }

    virtual ::TIntrusivePtr<INodeWriterImpl> CreateNodeWriter(
        const TRichYPath& , const TTableWriterOptions& )  override { ythrow yexception() << "Not Implemented"; }

    virtual ::TIntrusivePtr<IYaMRWriterImpl> CreateYaMRWriter(
        const TRichYPath& , const TTableWriterOptions& )  override { ythrow yexception() << "Not Implemented"; }

    virtual ::TIntrusivePtr<IProtoWriterImpl> CreateProtoWriter(
        const TRichYPath& ,
        const TTableWriterOptions& ,
        const ::google::protobuf::Message* )  override { ythrow yexception() << "Not Implemented"; }


    // IOperationClient
public:

    virtual IOperationPtr RawMap(
        const TRawMapOperationSpec& ,
        ::TIntrusivePtr<IRawJob> ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr RawReduce(
        const TRawReduceOperationSpec& ,
        ::TIntrusivePtr<IRawJob> ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr RawJoinReduce(
        const TRawJoinReduceOperationSpec& ,
        ::TIntrusivePtr<IRawJob> ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    //
    // mapper might be nullptr in that case it's assumed to be identity mapper

    // mapper and/or reduceCombiner may be nullptr
    virtual IOperationPtr RawMapReduce(
        const TRawMapReduceOperationSpec& ,
        ::TIntrusivePtr<IRawJob> ,
        ::TIntrusivePtr<IRawJob> ,
        ::TIntrusivePtr<IRawJob> ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr Sort(
        const TSortOperationSpec& ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr Merge(
        const TMergeOperationSpec& ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr Erase(
        const TEraseOperationSpec& ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr RemoteCopy(
        const TRemoteCopyOperationSpec& ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr RunVanilla(
        const TVanillaOperationSpec& ,
        const TOperationOptions&  = TOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual void AbortOperation(
        const TOperationId& )  override { ythrow yexception() << "Not Implemented"; }

    virtual void CompleteOperation(
        const TOperationId& )  override { ythrow yexception() << "Not Implemented"; }

    virtual void SuspendOperation(
        const TOperationId&,
        const TSuspendOperationOptions& = TSuspendOperationOptions()) override { ythrow yexception() << "Not Implemented"; }

    virtual void ResumeOperation(
        const TOperationId&,
        const TResumeOperationOptions& = TResumeOperationOptions()) override { ythrow yexception() << "Not Implemented"; }

    virtual void WaitForOperation(
        const TOperationId& )  override { ythrow yexception() << "Not Implemented"; }

    //
    // Checks and returns operation status.
    // NOTE: this function will never return EOperationBriefState::Failed or EOperationBriefState::Aborted status,
    // it will throw TOperationFailedError instead.
    virtual EOperationBriefState CheckOperation(
        const TOperationId& )  override { ythrow yexception() << "Not Implemented"; }

    //
    // Creates operation object given operation id.
    // Will throw TErrorResponse exception if operation doesn't exist.
    virtual IOperationPtr AttachOperation(const TOperationId& )  override { ythrow yexception() << "Not Implemented"; }

private:
    virtual IOperationPtr DoMap(
        const TMapOperationSpec& ,
        ::TIntrusivePtr<IStructuredJob> ,
        const TOperationOptions& )  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr DoReduce(
        const TReduceOperationSpec& ,
        ::TIntrusivePtr<IStructuredJob> ,
        const TOperationOptions& )  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr DoJoinReduce(
        const TJoinReduceOperationSpec& ,
        ::TIntrusivePtr<IStructuredJob> ,
        const TOperationOptions& )  override { ythrow yexception() << "Not Implemented"; }

    virtual IOperationPtr DoMapReduce(
        const TMapReduceOperationSpec& ,
        ::TIntrusivePtr<IStructuredJob> ,
        ::TIntrusivePtr<IStructuredJob> ,
        ::TIntrusivePtr<IStructuredJob> ,
        const TOperationOptions& )  override { ythrow yexception() << "Not Implemented"; }


    // NYT::IClientBase
public:

    [[nodiscard]] virtual ITransactionPtr StartTransaction(
        const TStartTransactionOptions&  = TStartTransactionOptions())  override { ythrow yexception() << "Not Implemented"; }

    //
    // Change properties of table:
    //   - switch table between dynamic/static mode
    //   - or change table schema
    virtual void AlterTable(
        const TYPath& ,
        const TAlterTableOptions&  = TAlterTableOptions())  override { ythrow yexception() << "Not Implemented"; }

    //
    // Create batch request object that allows to execute several light requests in parallel.
    // https://wiki.yandex-team.ru/yt/userdoc/api/#executebatch18.4
    virtual TBatchRequestPtr CreateBatchRequest()  override { ythrow yexception() << "Not Implemented"; }

    //
    // Return 'this' for IClient and the underlying client for ITransaction.
    virtual IClientPtr GetParentClient()  override { ythrow yexception() << "Not Implemented"; }



    // NYT::IClient
    //
    // Attach to existing transaction.
    //
    // Returned object WILL NOT ping transaction automatically.
    // Otherwise returened object is similar to the object returned by StartTransaction
    // and it can see all the changes made inside the transaction.
    [[nodiscard]] virtual ITransactionPtr AttachTransaction(
        const TTransactionId&,
        const TAttachTransactionOptions& = TAttachTransactionOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual void MountTable(
        const TYPath& path,
        const TMountTableOptions&  = TMountTableOptions())  override
    {
        DATA[path + "/@tablet_state"] = { TNode{TString("mounted")} };
        DEBUG_LOG << "Mounted " << path << Endl;
    }

    virtual void UnmountTable(
        const TYPath& path,
        const TUnmountTableOptions&  = TUnmountTableOptions())  override
    {
        DATA[path + "/@tablet_state"] = { TNode{TString("unmounted")} };
        DEBUG_LOG << "Unmounted " << path << Endl;
    }

    virtual void RemountTable(
        const TYPath& ,
        const TRemountTableOptions&  = TRemountTableOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Switch dynamic table from `mounted' into `frozen' state.
    // When table is in frozen state all its data is flushed to disk and writes are disabled.
    //
    // NOTE: this function launches the process of switching, but doesn't wait until switching is accomplished.
    // Waiting has to be performed by user.
    virtual void FreezeTable(
        const TYPath& ,
        const TFreezeTableOptions&  = TFreezeTableOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Switch dynamic table from `frozen' into `mounted' state.
    //
    // NOTE: this function launches the process of switching, but doesn't wait until switching is accomplished.
    // Waiting has to be performed by user.
    virtual void UnfreezeTable(
        const TYPath& ,
        const TUnfreezeTableOptions&  = TUnfreezeTableOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual void ReshardTable(
        const TYPath& ,
        const TVector<TKey>& ,
        const TReshardTableOptions&  = TReshardTableOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual void ReshardTable(
        const TYPath& ,
        i64 ,
        const TReshardTableOptions&  = TReshardTableOptions())  override { ythrow yexception() << "Not Implemented"; }

    TVector<TString> GetColumnNames(const TYPath& path, bool onlyPrimary = true) {
        TVector<TString> columnNames;
        if (DATA.contains(path + "/@attributes")) {
            const auto attributes = Get(path + "/@attributes");
            if (attributes.AsMap().contains("schema")) {
                const auto& schemaList = attributes["schema"].AsList();
                for (const auto& item: schemaList) {
                    if (onlyPrimary and not item.AsMap().contains("sort_order")) {
                        continue;
                    }
                    columnNames.emplace_back(item["name"].AsString());
                }
            }
        }
        return columnNames;
    }

    bool IsDynamic(const TYPath& path) {
        bool isDynamicTable = false;
        if (DATA.contains(path + "/@attributes")) {
            const auto attributes = Get(path + "/@attributes");
            if (attributes.AsMap().contains("dynamic")) {
                const auto& attrDynamic = attributes["dynamic"];
                if (attrDynamic.IsBool()) {
                    isDynamicTable = attrDynamic.AsBool();
                }
                if (attrDynamic.IsString()) {
                    isDynamicTable = FromString<bool>(attrDynamic.AsString());
                }
            }
        }
        return isDynamicTable;
    }


    bool IsMatched(const TNode& a, const TNode& b, const TVector<TString>& params) {
        for (const auto& param: params) {
            if (a.AsMap().contains(param) and b.AsMap().contains(param)) {
                if (a[param].ConvertTo<TString>() != b[param].ConvertTo<TString>()) {
                    return false;
                }
            }
        }
        return true;
    }


    virtual void InsertRows(
        const TYPath& path,
        const TNode::TListType& data,
        const TInsertRowsOptions&  = TInsertRowsOptions())  override {
            TYPath realPath = path.EndsWith("/@path") ? Get(path).AsString() : path;
            if (not Exists(realPath)) {
                ythrow yexception() << "Create table " << realPath << " before inserting data" << Endl;
            }
            DEBUG_LOG << "Inserting path: " << path << "(" << realPath << ")" << Endl;
            DEBUG_LOG << "Previos data: " << GetJson(realPath).ToJson() << Endl;
            DEBUG_LOG << "Inserting data: " << NMarket::NYTHelper::ConvertNodeToJson(data) << Endl;
            bool isDynamicTable = IsDynamic(realPath);
            if (not isDynamicTable) {
                DEBUG_LOG << "Just append data for non-dynamic table" << Endl;
                DATA[realPath].insert(DATA[realPath].end(), data.cbegin(), data.cend());
            } else {
                DEBUG_LOG << "Merge data for dynamic table" << Endl;
                const auto columns = GetColumnNames(realPath);
                TNode::TListType result;
                const auto rows = Get(realPath).AsList();
                // insert not modified data
                for (const auto& row: rows) {
                    bool noMatch = true;
                    for (const auto& item: data) {
                        if (IsMatched(row, item, columns)) {
                            noMatch = false;
                            break;
                        }
                    }
                    if (noMatch) {
                        result.emplace_back(row);
                    }
                }
                // insert all new data
                for (const auto& item: data) {
                    result.emplace_back(item);
                }
                DATA[realPath] = result;
            }
            DEBUG_LOG << "Completed value: " << GetJson(realPath).ToJson() << Endl;
        }

    virtual void DeleteRows(
        const TYPath& path,
        const TNode::TListType& data,
        const TDeleteRowsOptions&  = TDeleteRowsOptions())  override {
            TYPath realPath = path.EndsWith("/@path") ? Get(path).AsString() : path;
            if (not Exists(realPath)) {
                WARNING_LOG << "Table " << realPath << " not exists. Nothing to delete" << Endl;
                return;
            }
            DEBUG_LOG << "Deleting path: " << path << "(" << realPath << ")" << Endl;
            DEBUG_LOG << "Previos data: " << GetJson(realPath).ToJson() << Endl;
            DEBUG_LOG << "Removimg data: " << NMarket::NYTHelper::ConvertNodeToJson(data) << Endl;

            const auto columns = GetColumnNames(realPath);
            TNode::TListType result;
            const auto rows = Get(realPath).AsList();
            for (const auto& row: rows) {
                bool noMatch = true;
                for (const auto& item: data) {
                    if (IsMatched(row, item, columns)) {
                        noMatch = false;
                        break;
                    }
                }
                if (noMatch) {
                    result.emplace_back(row);
                }
            }
            DATA[realPath] = result;
            DEBUG_LOG << "Completed value: " << GetJson(realPath).ToJson() << Endl;
        }

    virtual void TrimRows(
        const TYPath& ,
        i64 ,
        i64 ,
        const TTrimRowsOptions&  = TTrimRowsOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TNode::TListType LookupRows(
        const TYPath& ,
        const TNode::TListType& ,
        const TLookupRowsOptions&  = TLookupRowsOptions())  override { ythrow yexception() << "Not Implemented"; }

    virtual TNode::TListType SelectRows(
        const TString& query,
        const TSelectRowsOptions&  = TSelectRowsOptions())  override
    {
        TVector<TString> fields;
        TString FROM("FROM"), table, WHERE("WHERE"), f,s;
        TVector<std::pair<TString, TString>> pairs;
        enum class ESTATE{
            fields,
            from,
            table,
            where,
            equals,
        };
        ESTATE current = ESTATE::fields;

        for (const auto& it : StringSplitter(query).SplitBySet(", []=").SkipEmpty()) {
            DEBUG_LOG << it.Token() << Endl;
            switch(current) {
                case ESTATE::fields:
                    if (it.Token() == FROM) {
                        current = ESTATE::from;
                    } else {
                        fields.emplace_back(it.Token());
                    }
                    break;
                case ESTATE::from:
                    current = ESTATE::table;
                    table = it.Token();
                    break;
                case ESTATE::table:
                    if (it.Token() == WHERE) {
                        current = ESTATE::where;
                    }
                    break;
                case ESTATE::where:
                    f = it.Token();
                    current = ESTATE::equals;
                    break;
                case ESTATE::equals:
                    s = it.Token();
                    pairs.emplace_back(std::make_pair(f,s));
                    current = ESTATE::where;
                    break;
            }
        }
        TNode::TListType result;

        TVector<TString> columns;
        TNode matcher;
        for (const auto& [fkey, fvalue]: pairs) {
            columns.emplace_back(fkey);
            matcher(fkey, fvalue);
        }

        const auto rows = Get(table).AsList();
        for (const auto& row: rows) {
            if (IsMatched(row, matcher, columns)) {
                result.emplace_back(row);
            }
        }
        return result;
    }

    // Change properties of table replica.
    // Allows to enable/disable replica and/or change its mode.
    virtual void AlterTableReplica(
        const TReplicaId& ,
        const TAlterTableReplicaOptions& )  override { ythrow yexception() << "Not Implemented"; }

    virtual ui64 GenerateTimestamp()  override { ythrow yexception() << "Not Implemented"; }

    // Return YT username of current client.
    virtual TAuthorizationInfo WhoAmI()  override { ythrow yexception() << "Not Implemented"; }

    // Get operation attributes.
    virtual TOperationAttributes GetOperation(
        const TOperationId& ,
        const TGetOperationOptions&  = TGetOperationOptions())  override { ythrow yexception() << "Not Implemented"; }

    // List operations satisfying given filters.
    virtual TListOperationsResult ListOperations(
        const TListOperationsOptions&  = TListOperationsOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Update operation runtime parameters.
    virtual void UpdateOperationParameters(
        const TOperationId& ,
        const TUpdateOperationParametersOptions& )  override { ythrow yexception() << "Not Implemented"; }

    // Get job attributes.
    virtual TJobAttributes GetJob(
        const TOperationId& ,
        const TJobId& ,
        const TGetJobOptions&  = TGetJobOptions())  override { ythrow yexception() << "Not Implemented"; }

    // List jobs satisfying given filters.
    virtual TListJobsResult ListJobs(
        const TOperationId& ,
        const TListJobsOptions&  = TListJobsOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Get the input of a running or failed job.
    // (TErrorResponse exception is thrown if it is missing).
    virtual IFileReaderPtr GetJobInput(
        const TJobId& ,
        const TGetJobInputOptions&  = TGetJobInputOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Get fail context of a failed job.
    // (TErrorResponse exception is thrown if it is missing).
    virtual IFileReaderPtr GetJobFailContext(
        const TOperationId& ,
        const TJobId& ,
        const TGetJobFailContextOptions&  = TGetJobFailContextOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Get stderr of a running or failed job
    // (TErrorResponse exception is thrown if it is missing).
    virtual IFileReaderPtr GetJobStderr(
        const TOperationId& ,
        const TJobId& ,
        const TGetJobStderrOptions&  = TGetJobStderrOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Get a file with given md5 from Cypress file cache located at 'cachePath'.
    virtual TMaybe<TYPath> GetFileFromCache(
        const TString& ,
        const TYPath& ,
        const TGetFileFromCacheOptions&  = TGetFileFromCacheOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Put a file 'filePath' to Cypress file cache located at 'cachePath'.
    // The file must have "md5" attribute and 'md5Signature' must match its value.
    virtual TYPath PutFileToCache(
        const TYPath& ,
        const TString& ,
        const TYPath& ,
        const TPutFileToCacheOptions&  = TPutFileToCacheOptions())  override { ythrow yexception() << "Not Implemented"; }

    // Create rbtorrent for given table written in special format
    // https://wiki.yandex-team.ru/yt/userdoc/blob_tables/#shag3.sozdajomrazdachu
    virtual NYT::TNode::TListType SkyShareTable(const std::vector<TYPath>& , const NYT::TSkyShareTableOptions& )  override { ythrow yexception() << "Not Implemented"; }

    virtual TCheckPermissionResponse CheckPermission(
        const TString&,
        EPermission,
        const TYPath&,
        const TCheckPermissionOptions& = TCheckPermissionOptions()) override { ythrow yexception() << "Not Implemented"; }
};
