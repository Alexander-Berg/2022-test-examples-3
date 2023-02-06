#include "client.h"

#include "reader.h"
#include "writer.h"

#include <extsearch/images/robot/library/logger/logging.h>

#include <yt/yt/client/chunk_client/read_limit.h>
#include <yt/yt/client/ypath/rich.h>

#include <util/folder/dirut.h>
#include <util/folder/filelist.h>
#include <util/stream/fwd.h>
#include <util/string/split.h>
#include <util/system/env.h>
#include <util/system/fs.h>
#include <util/system/yassert.h>

namespace {
    static NYT::TReadLimit ConvertLimit(const NYT::NChunkClient::TLegacyReadLimit& rawLimit) {
        NYT::TReadLimit resultLimit;

        if (rawLimit.HasRowIndex()) {
            resultLimit.RowIndex(rawLimit.GetRowIndex());
        }

        if (rawLimit.HasOffset()) {
            resultLimit.Offset(rawLimit.GetOffset());
        }

        Y_ENSURE(!rawLimit.HasLegacyKey(), "Yt mock client does not support key ranges yet!");

        return resultLimit;
    }

    static bool IsEmptyLimit(const NYT::TReadLimit& limit) {
        return !limit.Key_ && !limit.RowIndex_ && !limit.Offset_;
    }

    static NYT::TReadRange ConvertRange(const NYT::NChunkClient::TLegacyReadRange& rawRange) {
        NYT::TReadRange resultRange;

        resultRange.LowerLimit(ConvertLimit(rawRange.LowerLimit()));
        resultRange.UpperLimit(ConvertLimit(rawRange.UpperLimit()));

        return resultRange;
    }

    static bool IsEmptyRange(const NYT::TReadRange& range) {
        return IsEmptyLimit(range.LowerLimit_) && IsEmptyLimit(range.UpperLimit_) && IsEmptyLimit(range.Exact_);
    }

    void CopyDir(const TString& source, const TString& destination) {
        NFs::MakeDirectory(destination);
        TFileEntitiesList fl(TFileEntitiesList::EM_FILES_DIRS);
        fl.Fill(source, TStringBuf(), TStringBuf(), 100);
        while (const char * filename = fl.Next()) {
            if (IsDir(source + "/" + TString(filename))) {
                MakeDirIfNotExist((destination + "/" + TString(filename)).c_str());
            } else {
                NFs::Copy(source + "/" + filename, destination + "/" + filename);
            }
        }
    }

    void EnrichPathNode(const TString& path, NYT::TNode* node, bool isDir) {
        if (path.empty() || path == ".") {
            return;
        }

        TVector<TStringBuf> pathParts;
        Split(path.data(), "/", pathParts);

        NYT::TNode *curNode = node;
        for (TStringBuf& pathPart: pathParts) {
            if (pathPart == ".") {
                continue;
            }
            if (pathPart.StartsWith("@")) { // attribute
                return;
            }

            Y_ENSURE(curNode->IsMap());
            if (curNode->HasKey(pathPart)) {
                curNode = &((*curNode)[pathPart]);
            }
            else {
                (*curNode)[pathPart] = NYT::TNode::CreateMap();
                curNode = &((*curNode)[pathPart]);
            }
        }
        if (!isDir) {
            *curNode = NYT::TNode::CreateEntity();
        }
    }

    NYT::TNode TraceDir(const TString& sourcePath) {
        NYT::TNode node = NYT::TNode::CreateMap();
        TFileEntitiesList fl(TFileEntitiesList::EM_FILES_DIRS);
        fl.Fill(sourcePath, TStringBuf(), TStringBuf(), 100);
        while (const char * filename = fl.Next()) {
            EnrichPathNode(TString(filename), &node, IsDir(sourcePath + "/" + TString(filename)));
        }
        return node;
    }
} // end of anonymous namespace

constexpr char TClientBaseMock::YT_PREFIX_ENV_VAR[];

TClientBaseMock::TClientBaseMock(const TString& dir, const TString& ytPrefix, bool cleanUpOnFinish)
    : DirWithTables(dir)
    , YtPrefix(ytPrefix.empty() ? GetYtPrefixFromEnv() : ytPrefix)
    , CleanUpOnFinish(cleanUpOnFinish)
{
    NFs::EnsureExists(DirWithTables);
    Y_ENSURE(IsDir(DirWithTables), "TClientBaseMock is expected to work with a dir to be enable store tables in files");

    Y_ENSURE(!YtPrefix.empty());
    Y_ENSURE(YtPrefix.StartsWith("//"));

    Create(YtPrefix, NYT::ENodeType::NT_MAP, NYT::TCreateOptions().IgnoreExisting(true).Recursive(true));

    NLog::Message("Starting TClientBaseMock with working dir \"%s\" and yt prefix \"%s\"", DirWithTables.data(), YtPrefix.data());
}

TClientBaseMock::TClientBaseMock(TClientBaseMock&& source)
    : DirWithTables(source.DirWithTables)
    , YtPrefix(source.YtPrefix)
    , CleanUpOnFinish(source.CleanUpOnFinish)
{
    // to prevent deleting data in WD in destructor of source
    source.CleanUpOnFinish = false;
}

TClientBaseMock::~TClientBaseMock() {
    if (CleanUpOnFinish) {
        TString sourceDir = YtPrefix;
        sourceDir.erase(0, 2);
        RemoveDirWithContents(sourceDir);
    }
}

// IClientBase implementation
[[nodiscard]] NYT::ITransactionPtr TClientBaseMock::StartTransaction(const NYT::TStartTransactionOptions&) {
    Y_ENSURE(false, "TClientBaseMock::StartTransaction is unimplemented!");
}

void TClientBaseMock::AlterTable(const NYT::TYPath&, const NYT::TAlterTableOptions&) {
    Y_ENSURE(false, "TClientBaseMock::AlterTable is unimplemented!");
}

NYT::TBatchRequestPtr TClientBaseMock::CreateBatchRequest() {
    Y_ENSURE(false, "TClientBaseMock::CreateBatchRequest is unimplemented!");
}

NYT::IClientPtr TClientBaseMock::GetParentClient() {
    Y_ENSURE(false, "TClientBaseMock::GetParentClient is unimplemented!");
}

// ICypressClient implementation
NYT::TNodeId TClientBaseMock::Create(const NYT::TYPath& path, NYT::ENodeType type, const NYT::TCreateOptions& options) {
    //TODO(mseifullin): Add possibility for NT_FILE
    Y_ENSURE((type == NYT::ENodeType::NT_TABLE) || (type == NYT::ENodeType::NT_MAP),
             "TClientBaseMock supports creations for tables and map nodes only!");

    Y_ENSURE(!IsAttributePath(path), "Can not create attribute in the create function");

    if (Exists(path)) {
        if (options.Force_) {
            Remove(path, NYT::TRemoveOptions().Force(true).Recursive(true));
        } else if (options.IgnoreExisting_) {
            Y_ENSURE(GetNodetype(path) == type, "Can't ignore existing node with other type than already exist");
            Remove(path, NYT::TRemoveOptions().Force(true).Recursive(true));
        } else {
            Y_ENSURE(false, "\"" << path << "\" already exists, look forward for \"force\" of \"ignore_existing\" options!");
        }
    }

    const TString fullPath = GetFullFilePath(path);
    const TString fullFatherPath = GetDirName(fullPath);

    if (!NFs::Exists(fullFatherPath)) {
        Y_ENSURE(options.Recursive_, "Father directory \"" << fullFatherPath << "\" is missing, look forward for \"recursive\" option");
        Create("//" + fullFatherPath, NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true)); // remove this crutch
    }

    switch (type) {
    case NYT::ENodeType::NT_TABLE: {
        TFileOutput outFile(fullPath);
        break;
    }
    case NYT::ENodeType::NT_MAP: {
        NFs::MakeDirectory(fullPath);
        break;
    }
    default:
        Y_ENSURE(false, "Unsupported type of node to be created!");
    }

    // if we reached this point, then everything is ok and we can safely create file for all attributes
    // all attributes can be represented as a big TNode map
    const TString attrFilePath = GetAttributesFile(fullPath);
    {
        TFileOutput attributesFileStream(attrFilePath);

        // TODO(mseifullin): need some sanity checks for TNode
        if (!options.Attributes_.Empty()) {
            options.Attributes_->Save(&attributesFileStream);
        }
        // TODO(mseifullin): need some default system attributes
        else {
            NYT::TNode defaultNode = NYT::TNode::CreateMap();
            defaultNode.Save(&attributesFileStream);
        }
    }

    // TODO(mseifullin): think about better return value
    return NYT::TNodeId();
}

void TClientBaseMock::Remove(const NYT::TYPath& path, const NYT::TRemoveOptions& options) {
    if (IsAttributePath(path)) {
        TString cypressNodePath;
        TString attributeName;
        SplitAttributePath(path, cypressNodePath, attributeName);

        const TString fullPath = GetFullFilePath(cypressNodePath);

        if (!NFs::Exists(fullPath)) {
            Y_ENSURE(options.Force_, "Trying to delete missing node without \"force\" option!");
            return; // everything is OK with "force" option
        }

        NYT::TNode attributes = ReadYtNode(GetAttributesFile(fullPath));
        NYT::TNode* givenAttribute = RetrieveAttribute(&attributes, attributeName);

        if (givenAttribute == nullptr) {
            Y_ENSURE(options.Force_, "Trying to delete missing node without \"force\" option!");
            return; // everything is OK with "force" option
        }

        const TString fatherAttributeName = GetDirName(attributeName);
        const TString shortAttributeName = GetFileNameComponent(attributeName);

        NYT::TNode* fatherAttribute = RetrieveAttribute(&attributes, fatherAttributeName);

        fatherAttribute->AsMap().erase(shortAttributeName);
        WriteYtNode(GetAttributesFile(fullPath), attributes);

    }
    else {
        const TString fullPath = GetFullFilePath(path);

        if (!NFs::Exists(fullPath)) {
            Y_ENSURE(options.Force_, "Trying to delete missing node without \"force\" option!");
            return; // everything is OK with "force" option
        }

        if (IsDir(fullPath)) {
            Y_ENSURE(options.Recursive_, "Trying to delete map node \"" << fullPath << "\" without \"recursive\" option!");
            RemoveDirWithContents(fullPath);
        }
        else {
            NFs::Remove(fullPath);
        }
        NFs::Remove(GetAttributesFile(fullPath));
    }
}

bool TClientBaseMock::Exists(const NYT::TYPath& path, const NYT::TExistsOptions& /* options */) {
    const TString fullPath = GetFullFilePath(path);

    if (!IsAttributePath(fullPath)) {
        return NFs::Exists(fullPath);
    }
    else {
        TString cypressNode;
        TString attributeName;
        SplitAttributePath(fullPath, cypressNode, attributeName);

        if (!NFs::Exists(cypressNode)) {
            return false;
        }

        const TString attributeFileName = GetAttributesFile(cypressNode);
        NYT::TNode attributes = ReadYtNode(attributeFileName);

        auto attrNode = RetrieveAttribute(&attributes, attributeName);
        return attrNode != nullptr;
    }
}

NYT::TNode TClientBaseMock::Get(const NYT::TYPath& path, const NYT::TGetOptions& options) {
    Y_UNUSED(options); // TODO(mseifullin): make this options usable for the mock yt client

    if (IsAttributePath(path)) {
        TString cypressNodePath;
        TString attributeName;
        SplitAttributePath(path, cypressNodePath, attributeName);

        Y_ENSURE(Exists(cypressNodePath), "Can not get non existing cypress node");

        const TString fullPath = GetFullFilePath(cypressNodePath);
        NYT::TNode attributes = ReadYtNode(GetAttributesFile(fullPath));

        NYT::TNode* attr = TClientBaseMock::RetrieveAttribute(&attributes, attributeName);
        Y_ENSURE(attr != nullptr, "No such attribute");

        return *attr;
    }
    else {
        const TString fullPath = GetFullFilePath(path);

        Y_ENSURE(Exists(path), "Can not get non existing cypress node");

        if (IsDir(fullPath)) {
            return TraceDir(fullPath);
        } else {
            return NYT::TNode::CreateEntity();
        }
    }
}

void TClientBaseMock::Set(const NYT::TYPath& path, const NYT::TNode& value, const NYT::TSetOptions& options) {
    Y_UNUSED(options);

    if (IsAttributePath(path)) {
        TString cypressNodePath;
        TString attributeName;
        SplitAttributePath(path, cypressNodePath, attributeName);

        Y_ENSURE(Exists(cypressNodePath), "Can not set attribute on non existing cypress node");

        const TString fullPath = GetFullFilePath(cypressNodePath);
        NYT::TNode attributes = ReadYtNode(GetAttributesFile(fullPath));

        NYT::TNode* attr = TClientBaseMock::RetrieveAttribute(&attributes, attributeName);
        if (attr != nullptr) {
            Y_ENSURE(attr->GetType() == value.GetType(), "Can not update node with node with another type");
            *attr = value;
        }
        else {
            const TString fatherAttributeName = GetDirName(attributeName);
            const TString shortAttributeName = GetFileNameComponent(attributeName);

            NYT::TNode* fatherAttr = TClientBaseMock::RetrieveAttribute(&attributes, fatherAttributeName);
            Y_ENSURE(fatherAttr != nullptr, "Father attribute \"" << fatherAttributeName << "\" is missing"
                                            ", can not set attribute \"" << attributeName << "\"");
            (*fatherAttr)[shortAttributeName] = value;
        }

        WriteYtNode(GetAttributesFile(fullPath), attributes);
    }
    else {
        Y_ENSURE(false, "TClientBaseMock::Set is unimplemented for non attributes!");
    }
}

void TClientBaseMock::MultisetAttributes(const NYT::TYPath&, const NYT::TNode::TMapType&, const NYT::TMultisetAttributesOptions&) {
    Y_ENSURE(false, "TClientBaseMock::MultisetAttributes is unimplemented!");
}

NYT::TNode::TListType TClientBaseMock::List(const NYT::TYPath&, const NYT::TListOptions&) {
    Y_ENSURE(false, "TClientBaseMock::List is unimplemented!");
}

NYT::TNodeId TClientBaseMock::Copy(const NYT::TYPath& sourcePath, const NYT::TYPath& destinationPath, const NYT::TCopyOptions& options) {
    Y_ENSURE(!IsAttributePath(sourcePath) && !IsAttributePath(destinationPath), "Yt client (including TClientBaseMock) does not apply attributes in copy fun");

    Y_ENSURE(Exists(sourcePath), "Source path \"" << sourcePath << "\"  does not exist, can't execute copy command");

    if (Exists(destinationPath)) {
        Y_ENSURE(options.Force_, "Destination path \"" << destinationPath << "\" does exist, user \"force\" option to suppress this error");
        Remove(destinationPath, NYT::TRemoveOptions().Force(true).Recursive(true));
    }

    const TString fullSourcePath = GetFullFilePath(sourcePath);
    const TString fullDestinationPath = GetFullFilePath(destinationPath);
    const TString fullDestinationFatherPath = GetDirName(fullDestinationPath);

    if (!NFs::Exists(fullDestinationFatherPath)) {
        Y_ENSURE(options.Recursive_, "Father directory \"" << fullDestinationFatherPath << "\" is missing, look forward for \"recursive\" option");
        NFs::MakeDirectoryRecursive(fullDestinationFatherPath);
    }

    if (IsDir(fullSourcePath)) {
        CopyDir(fullSourcePath, fullDestinationPath);
    } else {
        NFs::Copy(fullSourcePath, fullDestinationPath);
    }
    NFs::Copy(GetAttributesFile(fullSourcePath), GetAttributesFile(fullDestinationPath));

    // TODO(mseifullin): think about better return value
    return NYT::TNodeId();
}

NYT::TNodeId TClientBaseMock::Move(const NYT::TYPath& sourcePath, const NYT::TYPath& destinationPath, const NYT::TMoveOptions& options) {
    Y_ENSURE(!IsAttributePath(sourcePath) && !IsAttributePath(destinationPath), "Yt client (including TClientBaseMock) does not apply attributes in move fun");

    Y_ENSURE(Exists(sourcePath), "Source path \"" << sourcePath << "\"  does not exist, can't execute move command");

    if (Exists(destinationPath)) {
        Y_ENSURE(options.Force_, "Destination path \"" << destinationPath << "\" does exist, user \"force\" option to suppress this error");
        Remove(destinationPath, NYT::TRemoveOptions().Force(true).Recursive(true));
    }

    const TString fullSourcePath = GetFullFilePath(sourcePath);
    const TString fullDestinationPath = GetFullFilePath(destinationPath);
    const TString fullDestinationFatherPath = GetDirName(fullDestinationPath);

    if (!NFs::Exists(fullDestinationFatherPath)) {
        Y_ENSURE(options.Recursive_, "Father directory \"" << fullDestinationFatherPath << "\" is missing, look forward for \"recursive\" option");
        NFs::MakeDirectoryRecursive(fullDestinationFatherPath);
    }

    NFs::Rename(fullSourcePath, fullDestinationPath);
    NFs::Rename(GetAttributesFile(fullSourcePath), GetAttributesFile(fullDestinationPath));

    // TODO(mseifullin): think about better return value
    return NYT::TNodeId();
}

NYT::TNodeId TClientBaseMock::Link(const NYT::TYPath&, const NYT::TYPath&, const NYT::TLinkOptions&) {
    Y_ENSURE(false, "TClientBaseMock::Link is unimplemented!");
}

void TClientBaseMock::Concatenate(const TVector<NYT::TRichYPath>&, const NYT::TRichYPath&, const NYT::TConcatenateOptions&) {
    Y_ENSURE(false, "TClientBaseMockMock::Concatenate is unimplemented!");
}

NYT::TRichYPath TClientBaseMock::CanonizeYPath(const NYT::TRichYPath& path) {
    NYT::NYPath::TRichYPath RawParsedPath = NYT::NYPath::TRichYPath::Parse(ApplyYtPrefix(path.Path_));
    NYT::TRichYPath result;

    result.Path_ = RawParsedPath.GetPath();
    for (const NYT::NChunkClient::TLegacyReadRange& rawReadRange: RawParsedPath.GetRanges()) {
        NYT::TReadRange readRange = ConvertRange(rawReadRange);
        if (!IsEmptyRange(readRange)) {
            result.Ranges_.push_back(readRange);
        }
    }
    //TODO(mseifullin): think about others parameters
    // to pass NYT::NYPath::TRichYPath ---> NYT::TRichYPath
    return result;
}

TVector<NYT::TTableColumnarStatistics> TClientBaseMock::GetTableColumnarStatistics(const TVector<NYT::TRichYPath>&, const NYT::TGetTableColumnarStatisticsOptions&) {
    Y_ENSURE(false, "TClientBaseMock::GetTableColumnarStatistics is unimplemented!");
}

TMaybe<NYT::TYPath> TClientBaseMock::GetFileFromCache(const TString&, const NYT::TYPath&, const NYT::TGetFileFromCacheOptions&) {
    Y_ENSURE(false, "TClientBaseMock::GetFileFromCache is unimplemented!");
}

NYT::TYPath TClientBaseMock::PutFileToCache(const NYT::TYPath&, const TString&, const NYT::TYPath&, const NYT::TPutFileToCacheOptions&) {
    Y_ENSURE(false, "TClientBaseMock::PutFileToCache is unimplemented!");
}

// IIOClient implementation
NYT::IFileReaderPtr TClientBaseMock::CreateFileReader(const NYT::TRichYPath&, const NYT::TFileReaderOptions&) {
    Y_ENSURE(false, "TClientBaseMock::CreateFileReader is unimplemented!");
}

NYT::IFileWriterPtr TClientBaseMock::CreateFileWriter(const NYT::TRichYPath&, const NYT::TFileWriterOptions&) {
    Y_ENSURE(false, "TClientBaseMock::CreateFileWriter is unimplemented!");
}

NYT::TTableWriterPtr<::google::protobuf::Message> TClientBaseMock::CreateTableWriter(
    const NYT::TRichYPath&,
    const ::google::protobuf::Descriptor&,
    const NYT::TTableWriterOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::CreateTableWriter is unimplemented!");
}

NYT::TRawTableReaderPtr TClientBaseMock::CreateRawReader(const NYT::TRichYPath&, const NYT::TFormat&, const NYT::TTableReaderOptions&) {
    Y_ENSURE(false, "TClientBaseMock::CreateRawReader is unimplemented!");
}

NYT::TRawTableWriterPtr TClientBaseMock::CreateRawWriter(const NYT::TRichYPath&, const NYT::TFormat&, const NYT::TTableWriterOptions&) {
    Y_ENSURE(false, "TClientBaseMock::CreateRawWriter is unimplemented!");
}

NYT::IFileReaderPtr TClientBaseMock::CreateBlobTableReader(const NYT::TYPath&, const NYT::TKey&, const NYT::TBlobTableReaderOptions&) {
    Y_ENSURE(false, "TClientBaseMock::CreateBlobTableReader is unimplemented!");
}

// IIOClient implementation (private functions)
::TIntrusivePtr<NYT::INodeReaderImpl> TClientBaseMock::CreateNodeReader(const NYT::TRichYPath& path, const NYT::TTableReaderOptions& options) {
    Y_UNUSED(options);

    const TString fullPath = GetFullFilePath(path.Path_);

    TIntrusivePtr<NYT::INodeReaderImpl> slaveReader = MakeIntrusive<TFixtureTableReader>(fullPath);
    return slaveReader;
}

::TIntrusivePtr<NYT::IYaMRReaderImpl> TClientBaseMock::CreateYaMRReader(const NYT::TRichYPath&, const NYT::TTableReaderOptions&) {
    Y_ENSURE(false, "TClientBaseMock::CreateYaMRReader is unimplemented!");
}

::TIntrusivePtr<NYT::IProtoReaderImpl> TClientBaseMock::CreateProtoReader(
    const NYT::TRichYPath&,
    const NYT::TTableReaderOptions&,
    const ::google::protobuf::Message*)
{
    Y_ENSURE(false, "TClientBaseMock::CreateProtoReader is unimplemented!");
}

::TIntrusivePtr<NYT::ISkiffRowReaderImpl> TClientBaseMock::CreateSkiffRowReader(
    const NYT::TRichYPath&,
    const NYT::TTableReaderOptions&,
    const NYT::ISkiffRowSkipperPtr&,
    const NSkiff::TSkiffSchemaPtr&)
{
    Y_ENSURE(false, "TClientBaseMock::CreateSkiffRowReader is unimplemented!");
}

::TIntrusivePtr<NYT::INodeWriterImpl> TClientBaseMock::CreateNodeWriter(const NYT::TRichYPath& path, const NYT::TTableWriterOptions& options) {
    Y_UNUSED(options);

    const TString fullPath = GetFullFilePath(path.Path_);

    TIntrusivePtr<NYT::INodeWriterImpl> slaveWriter = MakeIntrusive<TFixtureTableWriter>(fullPath);
    return slaveWriter;
}

::TIntrusivePtr<NYT::IYaMRWriterImpl> TClientBaseMock::CreateYaMRWriter(const NYT::TRichYPath&, const NYT::TTableWriterOptions&) {
    Y_ENSURE(false, "TClientBaseMock::CreateYaMRWriter is unimplemented!");
}

::TIntrusivePtr<NYT::IProtoWriterImpl> TClientBaseMock::CreateProtoWriter(
    const NYT::TRichYPath&,
    const NYT::TTableWriterOptions&,
    const ::google::protobuf::Message*)
{
    Y_ENSURE(false, "TClientBaseMock::CreateProtoWriter is unimplemented!");
}

// IOperationClient implementation
NYT::IOperationPtr TClientBaseMock::RawMap(
    const NYT::TRawMapOperationSpec&,
    ::TIntrusivePtr<NYT::IRawJob>,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::RawMap is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::RawReduce(
    const NYT::TRawReduceOperationSpec&,
    ::TIntrusivePtr<NYT::IRawJob>,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::RawReduce is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::RawJoinReduce(
    const NYT::TRawJoinReduceOperationSpec&,
    ::TIntrusivePtr<NYT::IRawJob>,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::RawJoinReduce is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::RawMapReduce(
    const NYT::TRawMapReduceOperationSpec&,
    ::TIntrusivePtr<NYT::IRawJob>,
    ::TIntrusivePtr<NYT::IRawJob>,
    ::TIntrusivePtr<NYT::IRawJob>,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::RawMapReduce is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::Sort(
    const NYT::TSortOperationSpec&,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::Sort is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::Merge(
    const NYT::TMergeOperationSpec&,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::Merge is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::Erase(
    const NYT::TEraseOperationSpec&,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::Erase is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::RemoteCopy(
    const NYT::TRemoteCopyOperationSpec&,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::RemoteCopy is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::RunVanilla(
    const NYT::TVanillaOperationSpec&,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::RunVanilla is unimplemented!");
}

void TClientBaseMock::AbortOperation(
    const NYT::TOperationId&)
{
    Y_ENSURE(false, "TClientBaseMock::AbortOperation is unimplemented!");
}

void TClientBaseMock::CompleteOperation(
    const NYT::TOperationId&)
{
    Y_ENSURE(false, "TClientBaseMock::CompleteOperation is unimplemented!");
}

void TClientBaseMock::WaitForOperation(
    const NYT::TOperationId&)
{
    Y_ENSURE(false, "TClientBaseMock::WaitForOperation is unimplemented!");
}

NYT::EOperationBriefState TClientBaseMock::CheckOperation(
    const NYT::TOperationId&)
{
    Y_ENSURE(false, "TClientBaseMock::CheckOperation is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::AttachOperation(const NYT::TOperationId&)
{
    Y_ENSURE(false, "TClientBaseMock::AttachOperation is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::DoMap(
    const NYT::TMapOperationSpec&,
    ::TIntrusivePtr<NYT::IStructuredJob>,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::DoMap is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::DoReduce(
    const NYT::TReduceOperationSpec&,
    ::TIntrusivePtr<NYT::IStructuredJob>,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::DoReduce is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::DoJoinReduce(
    const NYT::TJoinReduceOperationSpec&,
    ::TIntrusivePtr<NYT::IStructuredJob>,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::DoJoinReduce is unimplemented!");
}

NYT::IOperationPtr TClientBaseMock::DoMapReduce(
    const NYT::TMapReduceOperationSpec&,
    ::TIntrusivePtr<NYT::IStructuredJob>,
    ::TIntrusivePtr<NYT::IStructuredJob>,
    ::TIntrusivePtr<NYT::IStructuredJob>,
    const NYT::TOperationOptions&)
{
    Y_ENSURE(false, "TClientBaseMock::DoMapReduce is unimplemented!");
}

// auxiliary functions
const TString TClientBaseMock::ApplyYtPrefix(const TString& path) const {
    if (path.StartsWith("//")) {
        return path;
    }
    else {
        return YtPrefix + path;
    }
}

const TString TClientBaseMock::GetFullFilePath(const TString& path) const {
    TString result = ApplyYtPrefix(path);
    result.erase(0, 2); // removing leading '//'
    return DirWithTables + GetDirectorySeparator() + result;
}

// TODO(mseifullin): implement logic base on ${PATH}/@type value
NYT::ENodeType TClientBaseMock::GetNodetype(const TString& path) const {
    TString fullPath = GetFullFilePath(path);
    if (IsDir(fullPath)) {
        return NYT::ENodeType::NT_MAP;
    } else {
        return NYT::ENodeType::NT_TABLE;
    }
}

bool TClientBaseMock::IsAttributePath(const TString& path) {
    TVector<TStringBuf> pathParts;
    Split(path.data(), "/", pathParts);
    for (TStringBuf& pathPart: pathParts) {
        if (pathPart.StartsWith("@")) {
            return true;
        }
    }
    return false;
}

void TClientBaseMock::SplitAttributePath(const TString& path, TString& cypressNode, TString& attributeName) {
    TStringBuf cypressNodeBuf;
    TStringBuf attributeNameBuf;
    Y_ENSURE(TStringBuf(path.data(), path.length()).TrySplit("/@", cypressNodeBuf, attributeNameBuf));
    cypressNode = TString(cypressNodeBuf);
    attributeName = TString(attributeNameBuf);
}

NYT::TNode* TClientBaseMock::RetrieveAttribute(NYT::TNode* attrNode, const TString& attrPath) {
    if (attrPath.empty() || attrPath == ".") {
        return attrNode;
    }

    TVector<TStringBuf> pathParts;
    Split(attrPath.data(), "/", pathParts);

    NYT::TNode* curNode = attrNode;

    for (TStringBuf pathPart: pathParts) {
        if (!curNode->IsMap() || !curNode->HasKey(pathPart)) {
            return nullptr;
        }
        curNode = &(curNode->AsMap().find(pathPart)->second);
    }
    return curNode;
}

const TString TClientBaseMock::GetYtPrefixFromEnv() {
    TString result = GetEnv(YT_PREFIX_ENV_VAR, "//home/");
    if (result.empty()) {
        result = "//home/";
    }
    return result;
}

const TString TClientBaseMock::GetAttributesFile(const TString& fullFilePath) {
    const TString result = GetDirName(fullFilePath) + GetDirectorySeparator() + "@" + GetFileNameComponent(fullFilePath);
    return result;
}

const TString& TClientBaseMock::GetDirWithTables() const {
    return DirWithTables;
}

const TString& TClientBaseMock::GetYtPrefix() const {
    return YtPrefix;
}

bool TClientBaseMock::GetCleanUpOnFinish() const {
    return CleanUpOnFinish;
}

NYT::TNode TClientBaseMock::ReadYtNode(const TString& path) {
    NYT::TNode result;
    TFileInput inputStream(path);
    result.Load(&inputStream);
    return result;
}

void TClientBaseMock::WriteYtNode(const TString& path, const NYT::TNode& node) {
    TFileOutput outputStream(path);
    node.Save(&outputStream);
}

