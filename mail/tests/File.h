#ifndef FILE_H
#define FILE_H


#include <string>
#include <fstream>
#include <iostream>

#ifdef ARCADIA_BUILD
#include <library/cpp/resource/resource.h>
#endif

std::string readFile(const std::string& path) {
#ifdef ARCADIA_BUILD
    return NResource::Find(path);
#else
    std::string data;
    const size_t BUFF_SIZE = 1024;
    std::ifstream file(path.c_str(), std::ios_base::in | std::ios_base::ate);
    if (file.good()) {
        data.reserve(file.tellg());
        file.seekg(std::ios_base::beg);
    }
    char buff[BUFF_SIZE];
    while (file.read(buff, BUFF_SIZE)) {
        data.append(buff, BUFF_SIZE);
    }
    if (file.fail() && file.eof()) {
        data.append(buff, file.gcount());
    }
    file.close();
    return data;
#endif

}

class File {
public:
    File(const std::string& name) {
        data = readFile(name);
    }
    std::string& contents() {
        return data;
    }
    const std::string& contents() const {
        return data;
    }
private:
    std::string data;
};

#endif
