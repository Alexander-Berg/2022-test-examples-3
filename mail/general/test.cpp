#include <iostream>

#include "yandex/blackbox/blackbox2.h"
#include "utils.h"

using namespace bb;
using namespace std;

const string LocalIP ("127.0.0.1");

string arr[] = {"","a", "a%a0","a%qqa"," b%20c%4Fe",
    "a\nb", "a=b", "a&b=c", "фы\tва"};

void testEncDec()
{
    for(size_t i=0; i< sizeof(arr)/sizeof(string); ++i)
    {
        string enc = URLEncode(arr[i]);
        cout << "'"<<arr[i]<<"'  -->  '"<<enc;
        cout <<"'  -->  '"<<URLDecode(enc) <<"'" << endl;
    }
}

int main()
{
    testEncDec();

    // so far just test that it compiles and links successfully
    try
    {
        string request1 = InfoRequest(LoginSid("vasya"),
                                      LocalIP, optNone);
        cout<< "Request1 is: '"<< request1 << "'" <<endl;

        string request2 = InfoRequest("12345", LocalIP,
                                      Options() << optRegname << OptTestEmail("asdf"));
        cout<< "Request2 is: '"<< request2 << "'" <<endl;

        string request3 = SessionIDRequest("asdfghjkl", "yandex.ru",
                                           LocalIP, DBFields("subscriptions.login.2") << "subscriptions.suid.2");
        cout<< "Request3 is: '"<< request3 << "'" <<endl;

        LoginReqData loginreq = LoginRequest(LoginSid("vasya", "2"),"qwerty№12345","xmpp", LocalIP, optGetYandexEmails );
        cout<< "Login data is: '"<< loginreq.uri_ << "', body is '"<< loginreq.postData_ << "'" <<endl;

        string info_resp = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<doc>\n"
                           "<uid hosted=\"0\" domid=\"\" domain=\"\">57614307</uid>\n"
                           "<karma confirmed=\"0\">0</karma>\n"
                           "<dbfield id=\"subscription.login.2\">test-test</dbfield>\n"
                           "<dbfield id=\"subscription.login.4\"></dbfield>\n"
                           "<dbfield id=\"subscription.login.8\">test.test</dbfield>\n"
                           "</doc>";
        string sess_resp = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                            "<doc>"
                            "<status id=\"1\">NEED_RESET</status>"
                            "<error>OK</error>"
                            "<age>4408</age>"
                            "<uid hosted=\"0\" domid=\"\" domain=\"\">136026</uid>"
                            "<karma confirmed=\"0\">0</karma>"
                            "<new-session domain=\".yandex.ru\" expires=\"0\" http-only=\"1\">1252399938.0.0.136026.2:302235:0.60694.9956.91c2087b57ac6fac273dfbc31b3aa001</new-session>"
                            "</doc>";

        auto_ptr<Response> resp1 = InfoResponse(info_resp);
        cout << endl << resp1.get() << endl;
        //auto_ptr<Response> resp2 = InfoResponse(request2);
        auto_ptr<SessionResp> resp3 = SessionIDResponse(sess_resp);
        cout << endl << resp3.get() << endl;

    } catch (BBError& err)
    {
        cout << "Got exception with message: " << string(err.what()) <<endl;
    }

}

// vi: expandtab:sw=4:ts=4
// kate: replace-tabs on; indent-width 4; tab-width 4;
