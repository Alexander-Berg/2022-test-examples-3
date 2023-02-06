#pragma once

#include "offer.h"
#include "trust_emulator.h"

#include <drive/backend/billing/manager.h>
#include <drive/backend/billing/accounts/bonus.h>
#include <drive/backend/billing/accounts/limited.h>
#include <drive/backend/billing/accounts/trust.h>
#include <drive/backend/billing/accounts/yandex_account.h>
#include <drive/backend/billing/client/client.h>
#include <drive/backend/billing/trust/callback.h>

#include <drive/backend/database/history/config.h>
#include <drive/backend/settings/settings.h>
#include <drive/backend/ut/library/helper.h>

#include <library/cpp/testing/unittest/registar.h>

#include <rtline/library/storage/sqlite/structured.h>

#include <util/system/env.h>


class TBillingTestEnvironment {
public:
    TBillingTestEnvironment(const TString dbType = "SQLite", TDuration failedCheckInterval = TDuration::Seconds(0), ui32 replicsCount = 2)
        : DB(TDriveAPIConfigGenerator(dbType).CreateDatabase())
        , Context(DB)
        , UsersDB(Context)
        , SettingsDB(Context, TSettingsConfig().SetPrefix("development"))
        , Config(GetBillingConfig(failedCheckInterval))
        , AccountsManager(DB, SettingsDB, THistoryConfig())
        , BillingPort(PortManager.GetPort(12345))
    {
        Singleton<NSQLite::TUniqueFields>()->RegisterRowIdColumn("drive_payments", "payment_id");
        Singleton<NSQLite::TUniqueFields>()->RegisterRowIdColumn("drive_refunds", "payment_id");
        if (!IsTrue(GetEnv("RUN"))) {
            TrustEmulator.Run(BillingPort);
            Config = GetBillingConfig(failedCheckInterval, BillingPort);
        }
        SetSettings("billing.bonus_account_type", "88");
        SetSettings("billing.card_account_type", "89");
        SetSettings("billing.freshness_accounts", "0");
        SetSettings("billing.no_funds_statuses", "not_enough_funds,authorization_reject");
        SetSettings("billing.fast_tasks_selector", "1");
        // SetSettings("billing.card_pay_min_sum", "100");
        SendGlobalMessage<NDrive::TCacheRefreshMessage>("drive_settings_history");
        Clear();
        PrepareAccounts(DB, AccountsManager, &AccountDescriptionIds);
        CHECK_WITH_LOG(replicsCount);
        for (ui32 i = 0; i < replicsCount; ++i) {
            Managers.push_back(MakeHolder<TBillingManager>(Config, Context, UsersDB, SettingsDB));
            UNIT_ASSERT(Managers.back()->Start());
        }
        DoInitGlobalLog("console", GetLogLevel(), false, false);
        const NDrive::NBilling::TAccountsManager& accountsManager =  Managers.front()->GetAccountsManager();
        {
            auto session = Managers.front()->BuildSession();
            auto userAccounts = accountsManager.GetUserAccounts(DefaultUserId, session);
            CHECK_WITH_LOG(userAccounts);
            for (auto&& account: *userAccounts) {
                if (account->GetType() == NDrive::NBilling::EAccount::Bonus && account->GetBalance() > 0) {
                    CHECK_WITH_LOG(account->Remove(account->GetBalance(), session) == EDriveOpResult::Ok);
                }
            }
            CHECK_WITH_LOG(session.Commit());
        }
        {
            auto session = Managers.front()->BuildSession();
            auto userAccounts = accountsManager.GetUserAccounts(UserWithoutCards, session);
            CHECK_WITH_LOG(userAccounts);
            for (auto&& account: *userAccounts) {
                if (account->GetType() == NDrive::NBilling::EAccount::Bonus && account->GetBalance() > 0) {
                    CHECK_WITH_LOG(account->Remove(account->GetBalance(), session) == EDriveOpResult::Ok);
                }
            }
            CHECK_WITH_LOG(session.Commit());
        }
    }

    ~TBillingTestEnvironment() {
        for (auto&& m : Managers) {
            CHECK_WITH_LOG(m->Stop());
        }
    }

    TBillingClientConfig GetTrustClientConfig() {
        if (!IsTrue(GetEnv("RUN"))) {
            return TBillingClientConfig::ParseFromString(GetTrustConfigString(BillingPort));
        } else {
            return TBillingClientConfig::ParseFromString(GetTrustConfigString());
        }
    }

    NDrive::TEntitySession BuildSession(bool readonly = false) {
        return Managers.front()->BuildSession(readonly);
    }

    bool AddBonuses(const TString& userId, ui32 sum, TInstant deadline = TInstant::Max(), const TString& comment = {}) {
        return AddBonusesImpl(userId, sum, NDrive::NBilling::EAccount::Bonus, deadline, comment);
    }

    bool AddCoins(const TString& userId, ui32 sum, TInstant deadline = TInstant::Max(), const TString& comment = {}) {
        return AddBonusesImpl(userId, sum, NDrive::NBilling::EAccount::Coins, deadline, comment);
    }

    bool AddBonusesImpl(const TString& userId, ui32 sum, NDrive::NBilling::EAccount bonusType, TInstant deadline = TInstant::Max(), const TString& comment = {}) {
        auto userAccounts = GetManager().GetAccountsManager().GetUserAccounts(userId, ModelingNow());
        for (auto&& account : userAccounts) {
            if (account->GetType() == bonusType) {
                auto session = BuildSession(false);
                bool status = account->Add(sum, session, deadline, comment) == EDriveOpResult::Ok && session.Commit();
                SendGlobalMessage<NDrive::TCacheRefreshMessage>();
                return status;
            }
        }
        return false;
    }

    ui32 GetBonuses(const TString& userId) {
        return GetManager().GetAccountsManager().GetBonuses(userId, ModelingNow());
    }

    ui32 GetCoins(const TString& userId) {
        auto userAccounts = GetManager().GetAccountsManager().GetUserAccounts(userId, ModelingNow());
        ui32 sum = 0;
        for (auto&& account : userAccounts) {
            if (account->GetType() == NDrive::NBilling::EAccount::Coins) {
                sum += account->GetBalance();
            }
        }
        return sum;
    }

    NDrive::NBilling::TLimitedBalances GetLimitedBonuses(const TString& userId) {
        return GetManager().GetAccountsManager().GetLimitedBonuses(userId, ModelingNow());
    }

    bool CreateBillingTask(const TString& sessionId, const TString& userId, EBillingType bType, ui32 deposit, ui32 step, ui32 bill = 0, const TString& realSessionId = "") {
        const TBillingManager& manager = GetManager();
        {
            auto session = manager.BuildSession(false);
            if (bType == EBillingType::CarUsage) {
                auto offer = MakeAtomicShared<TFakeOffer>(deposit);
                offer->SetOfferId(sessionId).SetPaymentDiscretization(step);
                UNIT_ASSERT(manager.CreateBillingTask(userId, offer, session));
            } else {
                TBillingTask task;
                task.SetId(sessionId).SetUserId(userId).SetBill(0).SetDeposit(deposit).SetBillingType(bType).SetRealSessionId(realSessionId);
                task.SetDiscretization(step).SetQueue(manager.GetConfig().GetActiveQueue()).SetNextQueue(manager.GetConfig().GetActiveQueue());
                TSet<TString> accounts;
                accounts.emplace("card");
                task.SetChargableAccounts(accounts);
                task.SetLastUpdate(ModelingNow());

                UNIT_ASSERT(manager.CreateBillingTask(task, userId, session));
            }
            UNIT_ASSERT(session.Commit());
        }
        if (bill) {
            AddMoney(sessionId, bill);
        }
        if (bType != EBillingType::CarUsage) {
            auto session = manager.BuildSession(false);
            UNIT_ASSERT(manager.FinishingBillingTask(sessionId, session));
            UNIT_ASSERT(manager.AddClosedBillingInfo(sessionId, USER_ROOT_DEFAULT, session));
            UNIT_ASSERT(session.Commit());
        }
        return true;
    }

    TBillingManager& GetManager() {
        CycleCounter++;
        return *Managers[CycleCounter % Managers.size()];
    }

    THolder<TBillingManager> CreateBillingManager() {
        return MakeHolder<TBillingManager>(Config, Context, UsersDB, SettingsDB);
    }

    TCachedPayments GetActualSnapshot(const TString& sessionId) {
        const TBillingManager& bManager = GetManager();
        TCachedPayments payments;
        auto session = BuildSession(true);
        CHECK_WITH_LOG(bManager.GetPaymentsManager().GetPayments(payments, sessionId, session));
        return payments;
    }

    TVector<TPaymentsData> GetPaymentsHistory(const TString& userId, bool skipZero) {
        const TBillingManager& bManager = GetManager();
        auto queryOptions = IBaseSequentialTableImpl::TQueryOptions();
        queryOptions.AddGenericCondition("user_id", userId);
        auto session = bManager.BuildSession(true);
        auto tasks = bManager.GetHistoryManager().GetEvents({}, {}, session, queryOptions);
        if (!tasks) {
            return {};
        }

        TVector<TPaymentsData> result;
        for (auto&& task : *tasks) {
            if (task.GetHistoryAction() == EObjectHistoryAction::Remove) {
                if (!skipZero || task.GetBillCorrected() > 0) {
                    TCachedPayments snapshot = GetActualSnapshot(task.GetId());
                    auto payment = TPaymentsData::BuildPaymentsData(task, std::move(snapshot));
                    UNIT_ASSERT(payment);
                    result.emplace_back(std::move(*payment));
                }
            }
        }
        return result;
    }

    TVector<TCompiledRefund> GetSessionRefunds(const TString& sessionId) {
        const TFiscalRefundsHistoryManager& manager = GetManager().GetCompiledRefunds();
        auto session = BuildSession(true);
        auto events = manager.GetSessionRefundsFromDB(sessionId, session);
        UNIT_ASSERT(events);
        return MakeVector<TCompiledRefund>(*events);
    }

    TMaybe<TCompiledBill> GetCompiledBill(const TString& sessionId) {
        const TBillingManager& bManager = GetManager();
        auto& compiledBills = bManager.GetCompiledBills();
        auto session = bManager.BuildSession(true);
        auto task = compiledBills.GetFullBillFromDB(sessionId, session);
        if (!task || !task->GetFinal()) {
            return Nothing();
        }
        return task;
    }

    void AddMoney(const TString& sessionId, double sum) {
        TMap<TString, double> money;
        money[sessionId] = sum;
        auto session = GetManager().BuildSession(false);
        UNIT_ASSERT(GetManager().SetBillingInfo(money, session));
        UNIT_ASSERT(session.Commit());
    }

    bool FinishTask(const TString& sessionId) {
        const TBillingManager& manager = GetManager();
        auto session = BuildSession(false);
        if (!(manager.FinishingBillingTask(sessionId, session)
            && session.Commit()
            && manager.AddClosedBillingInfo(sessionId, USER_ROOT_DEFAULT))) {
            return false;
        }
        session = BuildSession(false);
        auto tasks = manager.GetActiveTasksManager().GetSessionsTasks({ sessionId }, session);
        UNIT_ASSERT(tasks);
        UNIT_ASSERT(tasks->size() == 1);
        return manager.BuildCompiledBill(tasks->front(), session)
            && session.Commit();
    }

    void WaitRefunds(ui32 required = Max<ui32>()) {
        const TBillingManager& manager = GetManager();
        TInstant start = Now();
        ui32 tasksCount = manager.WaitRefundCycle();
        if (required != Max<ui32>()) {
            UNIT_ASSERT_VALUES_EQUAL(tasksCount, required);
        }

        while (tasksCount != 0) {
            tasksCount = manager.WaitRefundCycle();
            Sleep(TDuration::Seconds(3));
            if (Now() - start > TDuration::Seconds(30)) {
                break;
            }
        }
        tasksCount = manager.WaitRefundCycle();
        UNIT_ASSERT_VALUES_EQUAL(tasksCount, 0);
    }

    TVector<TBillingTask> GetTasks() {
        NStorage::TObjectRecordsSet<TBillingTask> tasks;
        const TBillingManager& manager = GetManager();
        auto session = manager.BuildSession(true);
        session->Exec("SELECT * FROM billing_tasks WHERE queue='" + ToString(EBillingQueue::Tests) + "'", &tasks);
        TVector<TBillingTask> result;
        for (auto&& t : tasks) {
            result.emplace_back(t);
        }
        return result;
    }

    void WaitForFinish(TDuration timeout = TDuration::Minutes(2), bool finalize = false) {
        TInstant deadline = Now() + timeout;
        TVector<TBillingTask> controlSessions = GetTasks();

        TSet<TString> finished;
        while (true) {
            if (Now() > deadline) {
                break;
            }
            RunAllTasks();
            for (auto&& t : controlSessions) {
                const TString& sessionId = t.GetId();
                TCachedPayments payments = GetActualSnapshot(sessionId);
                auto inProc = payments.GetProcessingPayment();
                if (!!inProc) {
                    NDrive::NTrustClient::EPaymentStatus status = inProc->GetStatus();
                    INFO_LOG << "Current status for " << sessionId << " : " << status << Endl;
                } else {
                    INFO_LOG << "Finished " << sessionId << Endl;
                    finished.insert(sessionId);
                }
            }
            if (finalize && GetTasks().size() != 0) {
                Sleep(TDuration::Seconds(5));
                continue;
            }

            if (finished.size() == controlSessions.size()) {
                break;
            }
            Sleep(TDuration::Seconds(5));
        }
    }

    ui32 GetDebt(const TString& userId) {
        const TBillingManager& manager = GetManager();
        auto session = manager.BuildSession(true);
        auto optionalDebt = manager.GetDebt(userId, session);
        UNIT_ASSERT(optionalDebt);
        return *optionalDebt;
    }

    ui32 RunClearing(ui32 required = Max<ui32>()) {
        const TBillingManager& manager = GetManager();
        TMessagesCollector errors;
        ui32 tasksCount = manager.WaitClearingCycle(10, 100, TDuration::Zero(), errors);
        if (required != Max<ui32>()) {
            UNIT_ASSERT_VALUES_EQUAL(tasksCount, required);
        }
        return manager.WaitClearingCycle(10, 100, TDuration::Zero(), errors);
    }

    void RunAllTasks() {
        const TBillingManager& manager = GetManager();
        manager.WaitBillingCycle(10, 1);
    }

    void SetSettings(const TString& name, const TString& value) {
        CHECK_WITH_LOG(SettingsDB.SetValue(name, value, DefaultUserId));
    }

    ui32 GetTestTasksCount() {
        const TBillingManager& manager = GetManager();
        auto session = manager.BuildSession(true);
        TRecordsSet bTasks;
        session->Exec("SELECT * FROM billing_tasks WHERE session_id='" + DefaultSessionID + "'", &bTasks);
        return bTasks.GetRecords().size();
    }

    TMaybe<TCorrectedSessionPayments> GetSessionReport(const TString& sessionId) {
        const TBillingManager& manager = GetManager();
        NDrive::TEntitySession session = manager.BuildSession(true);
        auto payments = manager.GetPaymentsManager().GetFinishedPayments(sessionId, session);
        if (!payments) {
            return {};
        }
        return manager.GetSessionReport(*payments);
    }

    static TString GetTrustConfigString(TMaybe<ui16> billingPort = TMaybe<ui16>()) {
        TStringStream ss;
        if (billingPort.Defined()) {
            ss  << "Host: localhost\n"
                << "Port: " << billingPort << "\n"
                << "Https: 0\n";
        } else {
            ss  << "Host: trust-payments-test.paysys.yandex.net\n"
                << "Port: 8028\n"
                << "Https: 1\n";
        }
        ss << "RequestTimeout: 10s\n";
        ss << "<RequestConfig>\n"
            << "MaxAttempts: 1\n"
            << "TimeoutSendingms: 8000\n"
            << "TimeoutConnectms: 5000\n"
            << "GlobalTimeout: 5000\n"
            << "TasksCheckInterval: 5000\n"
            << "</RequestConfig>\n";
        return ss.Str();
    }

    static TBillingConfig GetBillingConfig(TDuration failedCheckInterval, TMaybe<ui16> billingPort = TMaybe<ui16>()) {
        TString token = GetEnv("TTOKEN");
        TStringStream ss;

        ss << "PaymentsCacheInterval: 60h" << Endl
            << "EventLog: eventlog" << Endl
            << "HistoryDeep: 10m" << Endl
            << "RobotUserId:" << RobotUserId << "" << Endl
            << "FailedTaskCheckInterval: " << failedCheckInterval << "" << Endl
            << "ServiceToken: " << token << "" << Endl
            << "Threads: 8" << Endl
            << "BatchUpdates: false" << Endl
            << "ActiveQueue: tests" << Endl
            << "ClearindSleep: 1ms" << Endl
            << "RefreshInterval: 10s" << Endl
            << "<Logics>" << Endl
            << "Modules: wallet,bonus,coins,yandex_account,card" << Endl
            << "<wallet>" << Endl
            << "</wallet>" << Endl
            << "<bonus>" << Endl
            << "</bonus>" << Endl
            << "<coins>" << Endl
            << "</coins>" << Endl
            << "<card>" << Endl
            << GetTrustConfigString(billingPort) << Endl
            << "</card>" << Endl
            << "<yandex_account>" << Endl
            << GetTrustConfigString(billingPort) << Endl
            << "</yandex_account>" << Endl
            << "</Logics>" << Endl
            << "<TrustStorage>" << Endl
            << "Type: local" << Endl
            << "PaymentUpdaterName: trust" << Endl
            << "</TrustStorage>" << Endl
            << "UseDBJsonStatements: false" << Endl;
        return TBillingConfig::ParseFromString(ss.Str());
    }

    ELogPriority GetLogLevel() const {
        return TLOG_INFO;
    }

    void Clear() {
        auto transaction = DB->CreateTransaction();
        transaction->Exec("DELETE FROM billing_tasks where user_id='" + DefaultUserId + "'");
        transaction->Exec("DELETE FROM billing_tasks where queue='" + ToString(EBillingQueue::Tests) + "'");
        transaction->Exec("DELETE FROM clearing_tasks");
        transaction->Exec("DELETE FROM refund_issues  where session_id like '" + DefaultSessionID + "%'");
        transaction->Exec("DELETE FROM drive_payments where session_id like '" + DefaultSessionID + "%'");
        transaction->Exec("DELETE FROM drive_refunds where session_id like '" + DefaultSessionID + "%'");
        transaction->Exec("DELETE FROM refund_issues_history where session_id like '" + DefaultSessionID + "%'");
        transaction->Exec("DELETE FROM billing_tasks_history where session_id like '" + DefaultSessionID + "%'");
        transaction->Exec("DELETE FROM compiled_refunds where session_id like '" + DefaultSessionID + "%'");
        transaction->Exec("DELETE FROM compiled_bills where session_id like '" + DefaultSessionID + "%'");
        transaction->Commit();
    }

    static void PrepareAccounts(TDatabasePtr database, const NDrive::NBilling::TAccountsManager& accountsManager, TMap<TString, ui64>* accountDescriptionIds = nullptr) {
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            NJson::TJsonValue descJson;
            descJson["type"] = ::ToString(NDrive::NBilling::EAccount::Bonus);
            descJson["hard_limit"] = NDrive::NBilling::AccountLimit;
            descJson["soft_limit"] = NDrive::NBilling::AccountLimit;
            descJson["name"] = "bonus";
            descJson["meta"]["hr_name"] = "bonus";
            NDrive::NBilling::TAccountDescriptionRecord description;
            UNIT_ASSERT(description.FromJson(descJson, nullptr));
            UNIT_ASSERT(accountsManager.UpsertAccountDescription(description, DefaultUserId, session));
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            NJson::TJsonValue descJson;
            descJson["type"] = ::ToString(NDrive::NBilling::EAccount::Bonus);
            descJson["hard_limit"] = NDrive::NBilling::AccountLimit;
            descJson["soft_limit"] = NDrive::NBilling::AccountLimit;
            descJson["name"] = "limited_bonuses";
            descJson["meta"]["hr_name"] = "bonus";
            descJson["meta"]["limited_policy"] = true;
            NDrive::NBilling::TAccountDescriptionRecord description;
            UNIT_ASSERT(description.FromJson(descJson, nullptr));
            UNIT_ASSERT(accountsManager.UpsertAccountDescription(description, DefaultUserId, session));
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            NJson::TJsonValue descJson;
            descJson["type"] = ::ToString(NDrive::NBilling::EAccount::Trust);
            descJson["hard_limit"] = NDrive::NBilling::AccountLimit;
            descJson["soft_limit"] = NDrive::NBilling::AccountLimit;
            descJson["name"] = "card";
            descJson["meta"]["hr_name"] = "card";
            descJson["meta"]["selectable"] = true;
            NDrive::NBilling::TAccountDescriptionRecord description;
            UNIT_ASSERT(description.FromJson(descJson, nullptr));
            UNIT_ASSERT(accountsManager.UpsertAccountDescription(description, DefaultUserId, session));
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            NJson::TJsonValue descJson;
            descJson["type"] = ::ToString(NDrive::NBilling::EAccount::Wallet);
            descJson["hard_limit"] = WalletHardLimit;
            descJson["soft_limit"] = WalletSoftLimit;
            descJson["name"] = "simple_limited";
            descJson["meta"]["hr_name"] = "simple_limited";
            descJson["meta"]["selectable"] = true;
            NDrive::NBilling::TAccountDescriptionRecord description;
            UNIT_ASSERT(description.FromJson(descJson, nullptr));
            UNIT_ASSERT(accountsManager.UpsertAccountDescription(description, DefaultUserId, session));
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            NJson::TJsonValue descJson;
            descJson["type"] = ::ToString(NDrive::NBilling::EAccount::Wallet);
            descJson["hard_limit"] = WalletSoftLimit;
            descJson["soft_limit"] = WalletSoftLimit;
            descJson["name"] = "parent_limited";
            descJson["meta"]["hr_name"] = "parent_limited";
            descJson["meta"]["selectable"] = true;
            NDrive::NBilling::TAccountDescriptionRecord description;
            UNIT_ASSERT(description.FromJson(descJson, nullptr));
            UNIT_ASSERT(accountsManager.UpsertAccountDescription(description, DefaultUserId, session));
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            NJson::TJsonValue descJson;
            descJson["type"] = ::ToString(NDrive::NBilling::EAccount::Wallet);
            descJson["hard_limit"] = WalletHardLimit;
            descJson["soft_limit"] = WalletSoftLimit;
            descJson["name"] = "priority_limited";
            descJson["meta"]["hr_name"] = "drive";
            descJson["meta"]["selectable"] = true;
            descJson["meta"]["priority"] = 1;
            NDrive::NBilling::TAccountDescriptionRecord description;
            UNIT_ASSERT(description.FromJson(descJson, nullptr));
            UNIT_ASSERT(accountsManager.UpsertAccountDescription(description, DefaultUserId, session));
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            NJson::TJsonValue descJson;
            descJson["type"] = ::ToString(NDrive::NBilling::EAccount::Coins);
            descJson["hard_limit"] = WalletSoftLimit;
            descJson["soft_limit"] = WalletSoftLimit;
            descJson["name"] = "coins";
            descJson["meta"]["hr_name"] = "coins";
            descJson["meta"]["selectable"] = true;
            descJson["meta"]["priority"] = 1;
            descJson["meta"]["data_type"] = ::ToString(NDrive::NBilling::EWalletDataType::Coins);
            NDrive::NBilling::TAccountDescriptionRecord description;
            UNIT_ASSERT(description.FromJson(descJson, nullptr));
            UNIT_ASSERT(accountsManager.UpsertAccountDescription(description, DefaultUserId, session));
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            NJson::TJsonValue descJson;
            descJson["type"] = ::ToString(NDrive::NBilling::EAccount::YAccount);
            descJson["hard_limit"] = NDrive::NBilling::AccountLimit;
            descJson["soft_limit"] = NDrive::NBilling::AccountLimit;
            descJson["name"] = "yandex_account";
            descJson["meta"]["hr_name"] = "yandex_account";
            descJson["meta"]["selectable"] = true;
            descJson["meta"]["data_type"] = ::ToString(NDrive::NBilling::EWalletDataType::YAccount);
            NDrive::NBilling::TAccountDescriptionRecord description;
            UNIT_ASSERT(description.FromJson(descJson, nullptr));
            UNIT_ASSERT(accountsManager.UpsertAccountDescription(description, DefaultUserId, session));
            UNIT_ASSERT(session.Commit());
        }

        SendGlobalMessage<NDrive::TCacheRefreshMessage>();
        auto descriptions = accountsManager.GetRegisteredAccounts();

        if (accountDescriptionIds) {
            for (auto&& d : descriptions) {
                (*accountDescriptionIds)[d.GetName()] = d.GetId();
            }
        }

        TVector<ui32> accountsToRemove;
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            auto accounts = accountsManager.GetUserAccounts(DefaultUserId, session);
            UNIT_ASSERT(accounts);
            for (auto&& acc : *accounts) {
                if (acc->GetId() == 0) {
                    continue;
                }
                accountsToRemove.push_back(acc->GetId());
                accountsManager.UnLinkAccount(acc->GetUserId(), acc->GetId(), DefaultUserId, session);
            }
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            auto accounts = accountsManager.GetUserAccounts(UserWithoutCards, session);
            UNIT_ASSERT(accounts);
            for (auto&& acc : *accounts) {
                if (acc->GetId() == 0) {
                    continue;
                }
                accountsToRemove.push_back(acc->GetId());
                accountsManager.UnLinkAccount(acc->GetUserId(), acc->GetId(), DefaultUserId, session);
            }
            UNIT_ASSERT(session.Commit());
        }
        {
            NDrive::TEntitySession session(database->CreateTransaction());
            for (auto&& account : accountsToRemove) {
                CHECK_WITH_LOG(accountsManager.RemoveAccount(account, DefaultUserId, session));
            }
            UNIT_ASSERT(session.Commit());
        }

        SendGlobalMessage<NDrive::TCacheRefreshMessage>();
    }

    NDrive::NBilling::TAccountsManager& GetAccountsManager() {
        return AccountsManager;
    }

    NDrive::NBilling::IBillingAccount::TPtr GetAccount(const TString& userId, const TString& accName) {
        auto userAccounts = AccountsManager.GetUserAccounts(userId, Now());
        for (auto&& account : userAccounts) {
            if (account->GetUniqueName() == accName) {
                return account;
            }
        }
        CHECK_WITH_LOG(false) << accName;
        return nullptr;
    }

    ui32 LinkWallet(const TString& userId, const TString& accName) {
        auto session = BuildSession();
        ui32 accountId = 0;
        NDrive::NBilling::TAccountRecord::TPtr accountRecord(new NDrive::NBilling::TLimitedAccountRecord());
        accountRecord->SetTypeId(AccountsManager.GetDescriptionByName(accName, TInstant::Now())->GetId());
        accountRecord->SetActive(true);
        UNIT_ASSERT(AccountsManager.RegisterAccount(accountRecord, userId, session, &accountId));
        UNIT_ASSERT(AccountsManager.LinkAccount(userId, accountId, userId, session));
        UNIT_ASSERT(session.Commit());
        SendGlobalMessage<NDrive::TCacheRefreshMessage>();
        return accountId;
    }

    ui32 LinkBonusAccount(const TString& userId, const TString& accountName = "bonus") {
        ui32 accountId = 0;
        auto session = BuildSession();
        NDrive::NBilling::TAccountRecord::TPtr accountRecord(new NDrive::NBilling::TBonusAccountRecord());
        accountRecord->SetTypeId(GetAccountDescriptionId(accountName));
        accountRecord->SetActive(true);
        UNIT_ASSERT(AccountsManager.RegisterAccount(accountRecord, userId, session, &accountId));
        UNIT_ASSERT(AccountsManager.LinkAccount(userId, accountId, DefaultUserId, session));
        UNIT_ASSERT(session.Commit());
        SendGlobalMessage<NDrive::TCacheRefreshMessage>();
        return accountId;
    }

    ui32 LinkCoinsAccount(const TString& userId, const TString& accountName = "coins") {
        ui32 accountId = 0;
        auto session = BuildSession();
        NDrive::NBilling::TAccountRecord::TPtr accountRecord(new NDrive::NBilling::TCoinsAccountRecord());
        accountRecord->SetTypeId(GetAccountDescriptionId(accountName));
        accountRecord->SetActive(true);
        UNIT_ASSERT(AccountsManager.RegisterAccount(accountRecord, userId, session, &accountId));
        UNIT_ASSERT(AccountsManager.LinkAccount(userId, accountId, DefaultUserId, session));
        UNIT_ASSERT(session.Commit());
        SendGlobalMessage<NDrive::TCacheRefreshMessage>();
        return accountId;
    }

    void LinkTrustAccount(const TString& userId) {
        ui32 trustAccountId = 0;
        auto session = BuildSession();
        NDrive::NBilling::TAccountRecord::TPtr accountRecord(new NDrive::NBilling::TTrustAccountRecord());
        accountRecord->SetTypeId(GetAccountDescriptionId("card"));
        accountRecord->SetActive(true);
        UNIT_ASSERT(AccountsManager.RegisterAccount(accountRecord, userId, session, &trustAccountId));
        UNIT_ASSERT(AccountsManager.LinkAccount(userId, trustAccountId, DefaultUserId, session));
        UNIT_ASSERT(session.Commit());
        SendGlobalMessage<NDrive::TCacheRefreshMessage>();
    }

    void LinkYandexAccount(const TString& userId) {
        ui32 accountId = 0;
        auto session = BuildSession();
        NDrive::NBilling::TYandexAccountRecord* yandexRecord = new NDrive::NBilling::TYandexAccountRecord();
        yandexRecord->SetPaymethodId("fakeId");
        NDrive::NBilling::TAccountRecord::TPtr accountRecord(yandexRecord);
        accountRecord->SetTypeId(GetAccountDescriptionId("yandex_account"));
        accountRecord->SetActive(true);
        UNIT_ASSERT(AccountsManager.RegisterAccount(accountRecord, userId, session, &accountId));
        UNIT_ASSERT(AccountsManager.LinkAccount(userId, accountId, DefaultUserId, session));
        UNIT_ASSERT(session.Commit());
        SendGlobalMessage<NDrive::TCacheRefreshMessage>();
    }

    ui32 GetAccountDescriptionId(const TString& name) const {
        auto it = AccountDescriptionIds.find(name);
        CHECK_WITH_LOG(it != AccountDescriptionIds.end());
        return it->second;
    }

    TDatabasePtr GetDatabase() {
        return DB;
    }

    TVector<TBillingTask> GetBillingTasksByUser(const TString& userId) {
        const TBillingManager& manager = GetManager();
        auto session = manager.BuildSession(true);
        auto tasks = manager.GetActiveTasksManager().GetUsersTasks(NContainer::Scalar(userId), session);
        UNIT_ASSERT(tasks);
        return std::move(*tasks);
    }

    TVector<TBillingTask> GetBillingTasksBySession(const TString& sessionId) {
        const TBillingManager& manager = GetManager();
        auto session = manager.BuildSession(true);
        auto tasks = manager.GetActiveTasksManager().GetSessionsTasks({ sessionId }, session);
        UNIT_ASSERT(tasks);
        return std::move(*tasks);
    }

    TBillingTask GetBillingTaskByDefaultSession() {
        TVector<TBillingTask> actualTasks = GetBillingTasksBySession(DefaultSessionID);
        UNIT_ASSERT_VALUES_EQUAL(actualTasks.size(), 1);
        return actualTasks.front();
    }

public:
    TDatabasePtr DB;
    TFakeHistoryContext Context;
    TUsersDB UsersDB;
    TSettingsDB SettingsDB;
    TBillingConfig Config;
    TVector<THolder<TBillingManager>> Managers;
    ui32 CycleCounter = 0;
    NDrive::NBilling::TAccountsManager AccountsManager;
    TMap<TString, ui64> AccountDescriptionIds;
    TPortManager PortManager;
    TTrustEmulator TrustEmulator;
    ui16 BillingPort;
};

