#include <string>
#include <stdint.h>
#include <openssl/sha.h>
#include <yplatform/range.h>
#include <yplatform/encoding/base64.h>

const std::string TEST = "C8JkLNAxsSJyK1s+jUuZbg==258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
const std::string RESULT = "YBuRdGZL45uguDtzeskRA18fBl4=";

int main()
{
    SHA_CTX ctx;
    unsigned char result[SHA_DIGEST_LENGTH];
    // unsigned char test_byte=0xCE;
    ::SHA1_Init(&ctx);
    ::SHA1_Update(&ctx, TEST.c_str(), TEST.size());
    ::SHA1_Final(result, &ctx);
    std::string res;
    res += yplatform::base64_encode(result + 0, result + SHA_DIGEST_LENGTH);
    // std::cout << res << " " << RESULT << "\n";
    assert(res == RESULT);
    return 0;
}
