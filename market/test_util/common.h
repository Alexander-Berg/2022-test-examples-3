#pragma once


#include <util/generic/list.h>
#include <util/generic/string.h>


void RunCmd(const TString& cmd, const TList<TString>& args);
void DoAssertTrue(bool expr, const char* expr_str, const char* file, const unsigned line);


#define AssertTrue(expr) DoAssertTrue(expr, #expr, __FILE__, __LINE__)
