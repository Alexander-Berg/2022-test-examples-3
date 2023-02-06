#include <yplatform/loader.h>
#include <iostream>

int main(int argc, char* argv[])
{
    if (argc != 2)
    {
        std::cout << "Usage: " << argv[0] << " <config>\n";
        return 1;
    }

    return ::yplatform_start(argv[1]);
}
