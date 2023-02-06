#pragma once

#include <mapreduce/yt/interface/client.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>

#include <utility> // std::pair

class TClientBaseMock final : public NYT::IClientBase {
private:
    static constexpr char YT_PREFIX_ENV_VAR[] = "YT_PREFIX";

private:
    const TString DirWithTables;
    const TString YtPrefix;
    bool CleanUpOnFinish;

// lifecycle
public:
    TClientBaseMock(const TString& dir, const TString& ytPrefix = "", bool cleanUpOnFinish = true);
    TClientBaseMock() = delete;
    TClientBaseMock(const TClientBaseMock&) = delete;
    TClientBaseMock(TClientBaseMock&& source);

    virtual ~TClientBaseMock() final;

// IClientBase implementation
public:
    [[nodiscard]] virtual NYT::ITransactionPtr StartTransaction(const NYT::TStartTransactionOptions& options = NYT::TStartTransactionOptions()) final;
    virtual void AlterTable(const NYT::TYPath& path, const NYT::TAlterTableOptions& options = NYT::TAlterTableOptions()) final;
    virtual NYT::TBatchRequestPtr CreateBatchRequest() final;
    virtual NYT::IClientPtr GetParentClient() final;

// ICypressClient implementation
public:
    virtual NYT::TNodeId Create(const NYT::TYPath& path, NYT::ENodeType type, const NYT::TCreateOptions& options = NYT::TCreateOptions()) final;
    virtual void Remove(const NYT::TYPath& path, const NYT::TRemoveOptions& options = NYT::TRemoveOptions()) final;
    virtual bool Exists(const NYT::TYPath& path, const NYT::TExistsOptions& options = NYT::TExistsOptions()) final;
    virtual NYT::TNode Get(const NYT::TYPath& path, const NYT::TGetOptions& options = NYT::TGetOptions()) final;
    virtual void Set(const NYT::TYPath& path, const NYT::TNode& value, const NYT::TSetOptions& options = NYT::TSetOptions()) final;
    virtual void MultisetAttributes(const NYT::TYPath& path, const NYT::TNode::TMapType& value, const NYT::TMultisetAttributesOptions& options = NYT::TMultisetAttributesOptions()) final;
    virtual NYT::TNode::TListType List(const NYT::TYPath& path, const NYT::TListOptions& options = NYT::TListOptions()) final;
    virtual NYT::TNodeId Copy(const NYT::TYPath& sourcePath, const NYT::TYPath& destinationPath, const NYT::TCopyOptions& options = NYT::TCopyOptions()) final;
    virtual NYT::TNodeId Move(const NYT::TYPath& sourcePath, const NYT::TYPath& destinationPath, const NYT::TMoveOptions& options = NYT::TMoveOptions()) final;
    virtual NYT::TNodeId Link(const NYT::TYPath& targetPath, const NYT::TYPath& linkPath, const NYT::TLinkOptions& options = NYT::TLinkOptions()) final;
    virtual void Concatenate(const TVector<NYT::TRichYPath>& sourcePaths, const NYT::TRichYPath& destinationPath, const NYT::TConcatenateOptions& options = NYT::TConcatenateOptions()) final;
    virtual NYT::TRichYPath CanonizeYPath(const NYT::TRichYPath& path) final;
    virtual TVector<NYT::TTableColumnarStatistics> GetTableColumnarStatistics(const TVector<NYT::TRichYPath>& paths, const NYT::TGetTableColumnarStatisticsOptions&) final;
    virtual TMaybe<NYT::TYPath> GetFileFromCache(const TString& md5Signature, const NYT::TYPath& cachePath, const NYT::TGetFileFromCacheOptions& options = NYT::TGetFileFromCacheOptions()) final;
    virtual NYT::TYPath PutFileToCache(const NYT::TYPath& filePath, const TString& md5Signature, const NYT::TYPath& cachePath, const NYT::TPutFileToCacheOptions& options = NYT::TPutFileToCacheOptions()) final;

// IIOClient implementation
public:
    virtual NYT::IFileReaderPtr CreateFileReader(const NYT::TRichYPath& path, const NYT::TFileReaderOptions& options = NYT::TFileReaderOptions()) final;
    virtual NYT::IFileWriterPtr CreateFileWriter(const NYT::TRichYPath& path, const NYT::TFileWriterOptions& options = NYT::TFileWriterOptions()) final;

    virtual NYT::TTableWriterPtr<::google::protobuf::Message> CreateTableWriter(
        const NYT::TRichYPath& path,
        const ::google::protobuf::Descriptor& descriptor,
        const NYT::TTableWriterOptions& options = NYT::TTableWriterOptions()) final;

    virtual NYT::TRawTableReaderPtr CreateRawReader(
        const NYT::TRichYPath& path,
        const NYT::TFormat& format,
        const NYT::TTableReaderOptions& options = NYT::TTableReaderOptions()) final;

    virtual NYT::TRawTableWriterPtr CreateRawWriter(
        const NYT::TRichYPath& path,
        const NYT::TFormat& format,
        const NYT::TTableWriterOptions& options = NYT::TTableWriterOptions()) final;

    virtual NYT::IFileReaderPtr CreateBlobTableReader(
        const NYT::TYPath& path,
        const NYT::TKey& blobId,
        const NYT::TBlobTableReaderOptions& options = NYT::TBlobTableReaderOptions()) final;

// IIOClient implementation (private functions)
private:
    virtual ::TIntrusivePtr<NYT::INodeReaderImpl> CreateNodeReader(const NYT::TRichYPath& path, const NYT::TTableReaderOptions& options) final;
    virtual ::TIntrusivePtr<NYT::IYaMRReaderImpl> CreateYaMRReader(const NYT::TRichYPath& path, const NYT::TTableReaderOptions& options) final;
    virtual ::TIntrusivePtr<NYT::IProtoReaderImpl> CreateProtoReader(
        const NYT::TRichYPath& path,
        const NYT::TTableReaderOptions& options,
        const ::google::protobuf::Message* prototype) final;
    virtual ::TIntrusivePtr<NYT::ISkiffRowReaderImpl> CreateSkiffRowReader(
        const NYT::TRichYPath& path,
        const NYT::TTableReaderOptions& options,
        const NYT::ISkiffRowSkipperPtr& skipper,
        const NSkiff::TSkiffSchemaPtr& schema) final;
    virtual ::TIntrusivePtr<NYT::INodeWriterImpl> CreateNodeWriter(const NYT::TRichYPath& path, const NYT::TTableWriterOptions& options) final;
    virtual ::TIntrusivePtr<NYT::IYaMRWriterImpl> CreateYaMRWriter(const NYT::TRichYPath& path, const NYT::TTableWriterOptions& options) final;
    virtual ::TIntrusivePtr<NYT::IProtoWriterImpl> CreateProtoWriter(
        const NYT::TRichYPath& path,
        const NYT::TTableWriterOptions& options,
        const ::google::protobuf::Message* prototype) final;

// IOperationClient implementation
public:
    virtual NYT::IOperationPtr RawMap(
        const NYT::TRawMapOperationSpec& spec,
        ::TIntrusivePtr<NYT::IRawJob> rawJob,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual NYT::IOperationPtr RawReduce(
        const NYT::TRawReduceOperationSpec& spec,
        ::TIntrusivePtr<NYT::IRawJob> rawJob,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual NYT::IOperationPtr RawJoinReduce(
        const NYT::TRawJoinReduceOperationSpec& spec,
        ::TIntrusivePtr<NYT::IRawJob> rawJob,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual NYT::IOperationPtr RawMapReduce(
        const NYT::TRawMapReduceOperationSpec& spec,
        ::TIntrusivePtr<NYT::IRawJob> mapper,
        ::TIntrusivePtr<NYT::IRawJob> reduceCombiner,
        ::TIntrusivePtr<NYT::IRawJob> reducer,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual NYT::IOperationPtr Sort(
        const NYT::TSortOperationSpec& spec,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual NYT::IOperationPtr Merge(
        const NYT::TMergeOperationSpec& spec,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual NYT::IOperationPtr Erase(
        const NYT::TEraseOperationSpec& spec,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual NYT::IOperationPtr RemoteCopy(
        const NYT::TRemoteCopyOperationSpec& spec,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual NYT::IOperationPtr RunVanilla(
        const NYT::TVanillaOperationSpec& spec,
        const NYT::TOperationOptions& options = NYT::TOperationOptions()) final;

    virtual void AbortOperation(
        const NYT::TOperationId& operationId) final;

    virtual void CompleteOperation(
        const NYT::TOperationId& operationId) final;

    virtual void WaitForOperation(
        const NYT::TOperationId& operationId) final;

    virtual NYT::EOperationBriefState CheckOperation(
        const NYT::TOperationId& operationId) final;

    virtual NYT::IOperationPtr AttachOperation(const NYT::TOperationId& operationId) final;

// IOperationClient implementation (private functions)
private:
    virtual NYT::IOperationPtr DoMap(
        const NYT::TMapOperationSpec& spec,
        ::TIntrusivePtr<NYT::IStructuredJob>,
        const NYT::TOperationOptions& options) final;

    virtual NYT::IOperationPtr DoReduce(
        const NYT::TReduceOperationSpec& spec,
        ::TIntrusivePtr<NYT::IStructuredJob>,
        const NYT::TOperationOptions& options) final;

    virtual NYT::IOperationPtr DoJoinReduce(
        const NYT::TJoinReduceOperationSpec& spec,
        ::TIntrusivePtr<NYT::IStructuredJob>,
        const NYT::TOperationOptions& options) final;

    virtual NYT::IOperationPtr DoMapReduce(
        const NYT::TMapReduceOperationSpec& spec,
        ::TIntrusivePtr<NYT::IStructuredJob>,
        ::TIntrusivePtr<NYT::IStructuredJob>,
        ::TIntrusivePtr<NYT::IStructuredJob>,
        const NYT::TOperationOptions& options) final;

// public handy auxiliary functions
public:
    const TString& GetDirWithTables() const;
    const TString& GetYtPrefix() const;
    bool GetCleanUpOnFinish() const;

// auxiliary functions
private:
    const TString ApplyYtPrefix(const TString& path) const;
    const TString GetFullFilePath(const TString& path) const;
    NYT::ENodeType GetNodetype(const TString& path) const;

// auxiliary functions for attributes
private:
    static bool IsAttributePath(const TString& path);
    static void SplitAttributePath(const TString& path, TString& cypressNode, TString& attributeName); // "//home/images/index/@some/attr/path" -> ("//home/images/index" + "some/attr/path")
    static const TString GetAttributesFile(const TString& fullFilePath);
    static NYT::TNode* RetrieveAttribute(NYT::TNode* attrNode, const TString& attrPath);

// static auxiliary functions
private:
    static const TString GetYtPrefixFromEnv();

// statix auxililary functions for IO
private:
    static NYT::TNode ReadYtNode(const TString& path);
    static void WriteYtNode(const TString& path, const NYT::TNode& node);
};
