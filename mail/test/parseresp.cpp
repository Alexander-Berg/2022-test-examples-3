// Cmd-line utility to parse the XML blackbox response and print out its contents
#include <iostream>
#include <fstream>
#include <string>

#include "yandex/blackbox/blackbox2.h"
#include "../src/utils.h"

using namespace std;
using namespace bb;

enum RespType { tResp, tBulk, tLogin, tSession, tMultiSession, tHost, tPwdQuality };

const string strResp("resp");
const string strBulk("bulk");
const string strLogin("login");
const string strSession("session");
const string strMultiSession("multisession");
const string strHost("host");
const string strPwdQuality("pwdquality");

int main(int argc, char* argv[])
{
    string response;

    if ( argc < 2 )
    {
        cout << " Response parser usage: ";
        cout << " \"parseresp <type> [<filename>]\"" << endl;
        cout << " <type> = resp | bulk | login | session | multisession | host | pwdquality" << endl;
        cout << " if <filename> empty, stdin is used." << endl;

        return 1;
    }

    RespType type;
    string strType( argv[1] );

    if ( strType == strLogin ) type = tLogin;
    else if ( strType == strSession ) type = tSession;
    else if ( strType == strMultiSession ) type = tMultiSession;
    else if ( strType == strResp ) type = tResp;
    else if ( strType == strBulk ) type = tBulk;
    else if ( strType == strHost ) type = tHost;
    else if ( strType == strPwdQuality ) type = tPwdQuality;
    else
    {
        cout << "Error: unknown response type specified: " << strType << endl;
        return 2;
    }

    if ( argc > 2 )
    {
        string filename( argv[2] );
        ifstream file (filename.c_str());

        if ( !file.is_open() ) {
            cout << "Error: unable to open file: " << filename << endl;
            return 3;
        }

        string str;

        while ( !file.eof() )
        {
            getline(file, str);
            if (file.eof() && str.empty()) break;

            response.append(str+"\n");
        }
    }
    else
    {
        string str;

        while ( !cin.eof() )
        {
            getline(cin, str);
            if (cin.eof() && str.empty()) break;
            response.append(str+"\n");
        }
    }

    try
    {
        switch (type) {
            case tLogin:
            {
                auto_ptr<LoginResp> pR = LoginResponse(response);
                cout << pR.get();
                break;
            }
            case tSession:
            {
                auto_ptr<SessionResp> pR = SessionIDResponse(response);
                cout << pR.get();
                break;
            }
            case tMultiSession:
            {
                auto_ptr<MultiSessionResp> pR = SessionIDResponseMulti(response);
                cout << pR.get();
                break;
            }
            case tHost:
            {
                auto_ptr<HostResp> pR = MailHostResponse(response);
                cout << pR.get();
                break;
            }
            case tBulk:
            {
                auto_ptr<BulkResponse> pR = InfoResponseBulk(response);
                cout << pR.get();
                break;
            }
            case tPwdQuality:
            {
                auto_ptr<PwdQualityResp> pR = PwdQualityResponse(response);
                cout << pR.get();
                break;
            }
            case tResp:
            default:
            {
                auto_ptr<Response> pR = InfoResponse(response);
                cout << pR.get();
                break;
            }
        };
    } catch ( FatalError& err )
    {
        cout << "Got a fatal blackbox exception:";
        cout << " code=" << err.code() << ", message='" << err.what() << "'" << endl;
    } catch ( TempError& err )
    {
        cout << "Got a temporary blackbox exception:";
        cout << " code=" << err.code() << ", message='" << err.what() << "'" << endl;
    } catch ( ... )
    {
        cout << "Error: Got unknown exception!" << endl;
        return 4;
    }

    return 0;
}

// vi: expandtab:sw=4:ts=4
// kate: replace-tabs on; indent-width 4; tab-width 4;
