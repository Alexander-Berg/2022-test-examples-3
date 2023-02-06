#include "util.h"


void create_test_environment(const TFsPath& directory)
{
    directory.ForceDelete();
    directory.MkDirs();
}
