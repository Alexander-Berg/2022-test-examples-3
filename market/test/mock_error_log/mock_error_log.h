#pragma once

#include <market/report/library/logger/logger.h>
#include <market/library/log/error_log.h>
#include <market/library/shiny/log/factory/format.h>

#include <util/generic/list.h>
#include <util/generic/ptr.h>
#include <regex>

class MockErrorLog {
public:
    MockErrorLog()
        : ErrorsList(new TList<TString>())
        , LogBackend(CreateBackend(LogData))
        , FileName("mock-error-log")
    {
        ::ErrorLog().SwapBackend(LogBackend, FileName);
    }

    ~MockErrorLog() {
        ::ErrorLog().SwapBackend(LogBackend, FileName);
    }

    bool ExpectErrorLike(const TString &str) {
        std::regex re(str.data());
        while (!LogData.Empty()) {
            const auto line = LogData.ReadLine();
            std::cmatch m;
            if (std::regex_search(line.data(), m, re)) {
                return true;
            }
        }
        return false;
    }

    bool IsEmpty() const {
        return ErrorsList->empty();
    }

    void Print() const {
        for (const auto &s : *ErrorsList) {
            Cerr << s << Endl;
        }
    }

private:
    static THolder<NLog::IErrorLogWriter> CreateBackend(IOutputStream& outputStream) {
        NMarket::NShiny::IFormatLogFactory<NLog::TErrorLogRecord>::TOptions opt = {{Nothing(), &outputStream}, NMarket::NShiny::ELogFormat::TSKV};
        return NMarket::NShiny::IFormatLogFactory<NLog::TErrorLogRecord>::Create(opt);
    }

private:
    TAtomicSharedPtr<TList<TString>> ErrorsList;
    TStringStream LogData;
    THolder<NLog::IErrorLogWriter> LogBackend;
    TString FileName;
};
