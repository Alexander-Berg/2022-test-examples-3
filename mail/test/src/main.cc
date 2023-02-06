#include <iostream>
#include <yplatform/loader.h>

int main(int argc, char* argv[]) {
    if (argc != 2) {
        std::cout << "usage " << argv[0] << " <config>\n";
        return 1;
    }

    return yplatform_start(argv[1]);
}
