#include "common.h"

#include <util/generic/yexception.h>
#include <util/stream/output.h>
#include <util/string/vector.h>
#include <util/system/backtrace.h>
#include <util/system/shellcommand.h>


void RunCmd(const TString& cmd, const TList<TString>& args)
{
    Cout << "Запускаю: " << cmd  << " " << JoinStrings(args.begin(), args.end(), " ") << Endl;
    if (TShellCommand(cmd, args,
                      TShellCommandOptions().SetOutputStream(&Cout).SetErrorStream(&Cout))
            .Run().GetStatus() != TShellCommand::SHELL_FINISHED)
        ythrow yexception() << cmd << " навернулась";
}


void DoAssertTrue(bool expr, const char* expr_str, const char* file, const unsigned line)
{
    if (not expr)
    {
        Cerr << "\nSTACK TRACE\n\n";
        PrintBackTrace();

        ythrow yexception()
            << "Проверка не прошла, ищите багу, господа программисты: "
            << file << ':' << line << ' '
            << "\"" << expr_str << "\"";
    }
}
