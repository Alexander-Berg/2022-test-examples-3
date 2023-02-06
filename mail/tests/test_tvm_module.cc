#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wsign-conversion"
#endif

#include <yplatform/application/config/yaml_to_ptree.h>
#include <yplatform/ptree.h>

#ifdef __clang__
#pragma clang diagnostic pop
#endif
namespace tvm_guard {

boost::property_tree::ptree yamlToPtree(const std::string& yaml) {
    boost::property_tree::ptree node;
    utils::config::yaml_to_ptree::convert_str(yaml, node);
    return node;
}

}
