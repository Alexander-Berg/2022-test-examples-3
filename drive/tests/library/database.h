#pragma once

#include <rtline/library/storage/sql/transaction.h>

namespace NDrive {
    NSQL::IDatabase::TPtr CreateDatabase(const TString& configFile);

    void ApplyMigrations(const NSQL::IDatabase& database, bool sqlite = false);

    void CreateStructure(const NSQL::IDatabase& database);
    void CreateTable(const NSQL::IDatabase& database, const TString& table);
    void CreateTable(const NSQL::ITransaction::TPtr tx, const TString& table);
    void DropTable(const NSQL::IDatabase& database, const TString& table);
    void TransferData(const NSQL::IDatabase& from, const NSQL::IDatabase& to, TInstant historySince = TInstant(), bool matchFields = false);
    void TransferData(const NSQL::IDatabase& from, const NSQL::IDatabase& to, const TString& table, TInstant historySince = TInstant(), const TString& orderBy = "", ui64 limit = 0);
}
