#include "init.h"

#include "task.h"

#include <drive/backend/cars/car.h>
#include <drive/backend/database/drive_api.h>
#include <drive/backend/tags/tags_manager.h>

#include <library/cpp/json/json_reader.h>

#include <rtline/library/executor/executor.h>

TAtomicSharedPtr<NDrive::TCommonTestData> TCommonTestProcessor::GetData(const TString& id) const {
    auto raw = Server->GetGlobalTaskExecutor().RestoreDataInfo(id);
    if (!raw) {
        return nullptr;
    }

    auto data = dynamic_cast<NDrive::TCommonTestData*>(raw.Get());
    Y_ENSURE(data, "task " << id << " is incorrect: " << raw->GetType());
    Y_UNUSED(raw.Release());
    return data;
}

TAtomicSharedPtr<NRTLine::IVersionedStorage> TCommonTestProcessor::GetStorage() const {
    auto result = Server->GetVersionedStorage(Config.GetStorageName());
    Y_ENSURE(result, "storage is unavailable");
    return result;
}

NJson::TJsonValue TCommonTestProcessor::GetNullInfo(const TString& type) const {
    NJson::TJsonValue result;
    result["action"] = NJson::JSON_NULL;
    result["description"] = NJson::JSON_NULL;
    result["id"] = NJson::JSON_NULL;
    result["message"] = NJson::JSON_NULL;
    result["status"] = NJson::JSON_NULL;
    result["type"] = type;
    return result;
}

TString TCommonTestProcessor::GetIMEI(const TString& carId) const {
    const TDriveAPI* api = Server->GetDriveAPI();
    Y_ENSURE(api, "Drive API is unavailable");
    auto cars = api->GetCarsData();
    Y_ENSURE(cars, "CarsDB is unavailable");
    const auto fetched = cars->GetCachedOrFetch(carId);
    const TDriveCarInfo* info = fetched.GetResultPtr(carId);
    Y_ENSURE(info, "car_id " << carId << " is not found");
    Y_ENSURE(info->GetIMEI(), "IMEI is empty for car_id " << carId);
    return info->GetIMEI();
}

TString TCommonTestProcessor::GetIMEI(const NJson::TJsonValue& post) const {
    if (post.Has("imei")) {
        return post["imei"].GetStringRobust();
    }
    if (post.Has("car_id")) {
        return GetIMEI(post["car_id"].GetStringRobust());
    }
    ythrow yexception() << "either 'imei' or 'car_id' should be present";
}

TString TCommonTestProcessor::GetIMEI(const TCgiParameters& cgi) const {
    const TString& imei = cgi.Get("imei");
    if (imei) {
        return imei;
    }

    const TString& carId = cgi.Get("car_id");
    if (carId) {
        return GetIMEI(carId);
    }
    ythrow yexception() << "either 'imei' or 'car_id' should be present";
}

TString TCommonTestProcessor::GetModel(const TCgiParameters& cgi) const {
    const TString& carId = cgi.Get("car_id");
    if (carId) {
        return GetModel(carId);
    } else {
        return {};
    }
}

TString TCommonTestProcessor::GetModel(const TString& carId) const {
    const TDriveAPI* api = Server->GetDriveAPI();
    Y_ENSURE(api, "Drive API is unavailable");
    auto cars = api->GetCarsData();
    Y_ENSURE(cars, "CarsDB is unavailable");
    const auto fetched = cars->GetCachedOrFetch(carId);
    const TDriveCarInfo* info = fetched.GetResultPtr(carId);
    Y_ENSURE(info, "car_id " << carId << " is not found");
    return info->GetModel();
}

TString TCommonTestProcessor::GetStatePath(const TString& imei) const {
    return "/test/status/" + imei;
}

bool TCommonTestProcessor::RemoveTag(const TString& carId, TUserPermissions::TPtr permissions) const {
    const IDriveTagsManager& tagsManager = DriveApi->GetTagsManager();
    const TDeviceTagsManager& deviceTagsManager = tagsManager.GetDeviceTags();
    const TString tagName = Server->GetSettings().GetValue<TString>("telematics.acceptance.tag_name").GetOrElse("need_telematic_acceptance");

    auto objects = TSet<TString>{ carId };
    auto session = deviceTagsManager.BuildTx<NSQL::Writable>();
    auto tags = deviceTagsManager.RestoreTags(objects, { tagName }, session);
    if (tags) {
        for (auto&& dbTag : *tags) {
            if (!dbTag) {
                continue;
            }
            if (!dbTag->GetPerformer().empty()) {
                if (!deviceTagsManager.DropPerformer({ dbTag.GetTagId() }, {}, *permissions, Server, session)) {
                    ERROR_LOG << "cannot DropPerformer: " << session.GetStringReport() << Endl;
                    return false;
                }
            } else {
                if (!deviceTagsManager.RemoveTag(dbTag, permissions->GetUserId(), Server, session)) {
                    ERROR_LOG << "cannot RemoveTag: " << session.GetStringReport() << Endl;
                    return false;
                }
            }
        }
    }
    return session.Commit();
}

IRequestProcessor::TPtr TTestProcessorConfig::DoConstructAuthProcessor(IReplyContext::TPtr context, IAuthModule::TPtr authModule, const IServerBase* server) const {
    const TServerRequestData& rd = context->GetRequestData();
    const TStringBuf handle = rd.ScriptName();
    if (handle.EndsWith("create")) {
        return new TCreateTestProcessor(*this, context, authModule, server->GetAsPtrSafe<NDrive::IServer>());
    }
    if (handle.EndsWith("input")) {
        return new TInputTestProcessor(*this, context, authModule, server->GetAsPtrSafe<NDrive::IServer>());
    }
    if (handle.EndsWith("list")) {
        return new TListTestProcessor(*this, context, authModule, server->GetAsPtrSafe<NDrive::IServer>());
    }
    if (handle.EndsWith("view")) {
        return new TViewTestProcessor(*this, context, authModule, server->GetAsPtrSafe<NDrive::IServer>());
    }
    return nullptr;
}

void TTestProcessorConfig::CheckServerForProcessor(const IServerBase* server) const {
    if (server) {
        server->GetGlobalTaskExecutor();
    }
}

void TTestProcessorConfig::DoInit(const TYandexConfig::Section* section) {
    TBase::DoInit(section);
    const auto& directives = section->GetDirectives();
    StorageName = directives.Value("StorageName", StorageName);

    TSet<TString> registered;
    NDrive::TCommonTestTask::TTestFactory::GetRegisteredKeys(registered);

    const auto sections = section->GetAllChildren();
    const auto scenarios = sections.equal_range("Scenario");
    for (auto i = scenarios.first; i != scenarios.second; ++i) {
        const auto& d = i->second->GetDirectives();

        TScenario scenario;
        scenario.Name = d.Value("Name", scenario.Name);
        scenario.Description = d.Value("Description", scenario.Description);
        TString allowedModels = d.Value("AllowedModels", TString());
        StringSplitter(allowedModels).Split(',').SkipEmpty().Collect(&scenario.AllowedModels);
        TString bannedModels = d.Value("BannedModels", TString());
        StringSplitter(bannedModels).Split(',').SkipEmpty().Collect(&scenario.BannedModels);

        Y_ENSURE(registered.contains(scenario.Name), "Scenario " << scenario.Name << " is not registered");
        Scenarios.push_back(std::move(scenario));
    }
}

void TTestProcessorConfig::ToString(IOutputStream& os) const {
    TBase::ToString(os);
    os << "StorageName: " << StorageName << Endl;
    for (auto&& scenario : Scenarios) {
        os << "<Scenario>" << Endl;
        os << "Name: " << scenario.Name << Endl;
        os << "Description: " << scenario.Description << Endl;
        os << "AllowedModels: " << JoinStrings(scenario.AllowedModels.begin(), scenario.AllowedModels.end(), ",") << Endl;
        os << "BannedModels: " << JoinStrings(scenario.BannedModels.begin(), scenario.BannedModels.end(), ",") << Endl;
        os << "</Scenario>" << Endl;
    }
}

void TCreateTestProcessor::ProcessServiceRequest(TJsonReport::TGuard& g, TUserPermissions::TPtr permissions, const NJson::TJsonValue& content) {
    Y_ENSURE(permissions, "UserPermissions is missing");
    const TString& userId = permissions->GetUserId();
    const TString& imei = GetIMEI(content);

    Y_ENSURE(content.Has("type"), "parameter 'type' is missing");
    const TString& type = content["type"].GetStringRobust();
    Y_ENSURE(type, "parameter 'type' is empty");

    auto path = GetStatePath(imei);
    auto storage = GetStorage();
    auto lock = storage->WriteLockNode(path);
    Y_ENSURE(lock->IsLocked(), "cannot acquire write lock on " << path);

    if (!storage->ExistsNode(path)) {
        Y_ENSURE(storage->SetValue(path, "{}"), "cannot create " << path);
    }
    NJson::TJsonValue state;
    {
        TString stateString;
        Y_ENSURE(storage->GetValue(path, stateString), "cannot read " << path);
        Y_ENSURE(NJson::ReadJsonFastTree(stateString, &state), "cannot parse state json: " << stateString);
    }

    auto data = NDrive::TCommonTestData::Create(type, imei, userId);
    Y_ENSURE(data, "cannot create data for " << type);
    auto task = NDrive::TCommonTestTask::Create(*data);

    auto& executor = Server->GetGlobalTaskExecutor();
    Y_ENSURE(executor.StoreData2(data.Get()), "Cannot store data");
    Y_ENSURE(executor.StoreTask2(task.Get()), "Cannot store task");
    executor.EnqueueTask(task->GetIdentifier(), data->GetIdentifier());

    state[type].AppendValue(data->GetIdentifier());
    Y_ENSURE(storage->SetValue(path, state.GetStringRobust()), "cannot write " << path);

    g.MutableReport().SetExternalReport(data->GetInfo());
    g.SetCode(HTTP_OK);
}

void TInputTestProcessor::ProcessServiceRequest(TJsonReport::TGuard& g, TUserPermissions::TPtr permissions, const NJson::TJsonValue& content) {
    Y_ENSURE(permissions, "UserPermissions are missing");
    const TString& userId = permissions->GetUserId();

    Y_ENSURE(content.Has("task-id"), "parameter 'task-id' is missing");
    Y_ENSURE(content.Has("input-id"), "parameter 'input-id' is missing");
    Y_ENSURE(content.Has("value"), "parameter 'input' is missing");
    const TString& taskId = content["task-id"].GetString();
    const TString& inputId = content["input-id"].GetString();
    const i64 value = content["value"].GetUIntegerRobust();
    Y_ENSURE(taskId, "parameter 'task-id' is empty");
    Y_ENSURE(inputId, "parameter 'input-id' is empty");

    NDrive::TTestInputBase base(taskId, userId, inputId, value);
    NDrive::TTestInputTask task(base);
    auto dataId = taskId;

    auto& executor = Server->GetGlobalTaskExecutor();
    Y_ENSURE(executor.StoreTask2(&task), "cannot store task");
    executor.EnqueueTask(task.GetIdentifier(), dataId);

    NJson::TJsonValue result;
    result["id"] = task.GetIdentifier();
    g.MutableReport().SetExternalReport(std::move(result));
    g.SetCode(HTTP_OK);
}

void TListTestProcessor::ProcessServiceRequest(TJsonReport::TGuard& g, TUserPermissions::TPtr permissions, const NJson::TJsonValue& requestData) {
    Y_ENSURE(permissions, "UserPermissions are missing");
    Y_UNUSED(requestData);
    const TCgiParameters& cgi = Context->GetCgiParameters();
    const TString& carId = cgi.Get("car_id");
    const TString& imei = GetIMEI(cgi);
    const TString& model = GetModel(cgi);
    const bool debug = IsTrue(cgi.Get("debug"));

    auto path = GetStatePath(imei);
    auto storage = GetStorage();
    auto lock = storage->ReadLockNode(path);
    Y_ENSURE(lock->IsLocked(), "cannot acquire read lock on " << path);

    NJson::TJsonValue state;
    if (storage->ExistsNode(path)) {
        TString stateString;
        Y_ENSURE(storage->GetValue(path, stateString), "cannot read " << path);
        Y_ENSURE(NJson::ReadJsonFastTree(stateString, &state), "cannot parse state json: " << stateString);
    }

    TMap<TString, TAtomicSharedPtr<NDrive::TCommonTestData>> infos;
    for (auto&& i : state.GetMap()) {
        const TString& type = i.first;
        const auto& ids = i.second.GetArray();
        const auto data = !ids.empty() ? GetData(ids.back().GetStringRobust()) : nullptr;
        if (data) {
            infos[type] = data;
        }
    }

    TInstant lastFailure;
    for (auto&& i : infos) {
        auto data = i.second;
        if (data) {
            if (data->GetStatus() == NDrive::NProto::TTestTaskData::FAILURE) {
                lastFailure = std::max(lastFailure, data->GetCreatedTime());
            }
        }
    }

    ui32 passed = 0;
    ui32 total = 0;
    NJson::TJsonValue types;
    for (auto&& scenario : Config.GetScenarios()) {
        if (!scenario.AllowedModels.empty() && !scenario.AllowedModels.contains(model)) {
            continue;
        }
        if (scenario.BannedModels.contains(model)) {
            continue;
        }
        const TString& type = scenario.Name;
        NJson::TJsonValue element;
        if (state.Has(type)) {
            const auto& ids = state[type].GetArray();
            const auto data = !ids.empty() ? GetData(ids.back().GetStringRobust()) : nullptr;
            if (data && (data->GetCreatedTime() >= lastFailure || debug)) {
                if (data->GetStatus() == NDrive::NProto::TTestTaskData::SUCCESS) {
                    passed += 1;
                }
                element = data->GetInfo();
            } else {
                element = GetNullInfo(type);
            }
        } else {
            element = GetNullInfo(type);
        }
        total += 1;
        types.AppendValue(element);
    }
    if (DriveApi && total && total == passed) {
        RemoveTag(carId, permissions);
    }
    g.MutableReport().SetExternalReportString(types.GetStringRobust());
    g.SetCode(HTTP_OK);
}

void TViewTestProcessor::ProcessServiceRequest(TJsonReport::TGuard& g, TUserPermissions::TPtr permissions, const NJson::TJsonValue& requestData) {
    Y_UNUSED(permissions);
    Y_UNUSED(requestData);
    const TCgiParameters& cgi = Context->GetCgiParameters();
    const TString& id = cgi.Get("id");
    const bool debug = IsTrue(cgi.Get("debug"));
    Y_ENSURE(id, "parameter 'id' is missing or empty");

    auto data = GetData(id);
    Y_ENSURE(data, "cannot find info for " << id);

    NJson::TJsonValue result = data->GetInfo();
    if (debug) {
        result["debug"] = data->GetDebugInfo();
    }
    g.MutableReport().SetExternalReport(std::move(result));
    g.SetCode(HTTP_OK);
}

TTestProcessorConfig::TFactory::TRegistrator<TTestProcessorConfig> CreateTestProcessor("test/create");
TTestProcessorConfig::TFactory::TRegistrator<TTestProcessorConfig> InputTestProcessor("test/input");
TTestProcessorConfig::TFactory::TRegistrator<TTestProcessorConfig> ListTestProcessor("test/list");
TTestProcessorConfig::TFactory::TRegistrator<TTestProcessorConfig> ViewTestProcessor("test/view");
