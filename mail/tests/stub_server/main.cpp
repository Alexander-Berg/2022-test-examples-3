#include <iostream>
#include <string>

#include <yplatform/loader.h>

#include <chrono>
#include <thread>
#include <memory>

int main(int argc, char* argv[]) {

    if (argc != 2) {
        std::cout << "usage: " << argv[0] << " <config>\n";
        exit(2);
    }

    return yplatform_start(argv[1]);
}
