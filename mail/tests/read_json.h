#include <yplatform/json.h>
#include <fstream>
#include <streambuf>
#include <string>

using yplatform::json_value;

inline json_value read_json(const char* file_name)
{
    std::ifstream data_input(file_name);
    std::string input_str(
        std::istreambuf_iterator<char>(data_input), std::istreambuf_iterator<char>{});
    data_input.close();
    json_value ret;
    ret.parse(input_str);
    return ret;
}