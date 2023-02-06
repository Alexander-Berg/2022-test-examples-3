#include <yplatform/application/config/loader.h>
#include <yplatform/ptree.h>
#include <string>
#include <vector>

using std::string;

bool throws(const string& filename)
{
    yplatform::ptree filtered;
    try
    {
        utils::config::loader::from_file(filename + ".yml", filtered, true);
    }
    catch (...)
    {
        return true;
    }
    return false;
}

int main()
{
    std::vector<string> good = { "simple", "arrays", "multiple", "noother", "strings" };
    std::vector<string> bad = { "nofile", "emptyfile" };

    for (auto& filename : good)
    {
        yplatform::ptree filtered;
        yplatform::ptree result;
        utils::config::loader::from_file(filename + ".yml", filtered, true);
        utils::config::loader::from_file(filename + "-result.yml", result, false);
        if (filtered != result)
        {
            throw std::runtime_error("failed test \"" + filename + "\"");
        }
    }

    for (auto& filename : bad)
    {
        if (!throws(filename) && filename.c_str())
        {
            throw std::runtime_error("failed test \"" + filename + "\"");
        }
    }

    return 0;
}
