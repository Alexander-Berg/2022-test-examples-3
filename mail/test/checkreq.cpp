// Cmd-line utility to build blackbox request from given options and compare with reference
#include <iostream>
#include <fstream>
#include <string>

#include "yandex/blackbox/blackbox2.h"
#include "../src/utils.h"
#include "../src/xconfig.h"

using namespace std;
using namespace bb;

// supported methods
const string strInfo("info");
const string strLogin("login");
const string strSession("session");
const string strOAuth("oauth");
const string strMailHost("mailhost");
const string strPwdQuality("pwdquality");

// mailhost operations
const string strOpCreate("create");
const string strOpDelete("delete");
const string strOpAssign("assign");
const string strOpSetPrio("setprio");
const string strOpFind("find");

void read_opts (Options& opts, xmlConfig::Parts& xml_opts)
{
    for (int i=0 ; i < xml_opts.Size() ; ++i)
    {
        xmlConfig::Part item( xml_opts[i] );
        string key, val;
        if ( item.GetIfExists("@key", key) )
        {
            item.GetIfExists("@val", val);
            opts << Option(key, val);
        }
    }
}

DBFields read_dbfields (xmlConfig::Parts& xml_fields)
{
    DBFields out;
    for (int i=0 ; i < xml_fields.Size() ; ++i)
    {
        string field = xml_fields[i].asString();

        if ( !field.empty() ) out << field;
    }

    return out;
}

OptAliases read_aliases (xmlConfig::Parts& xml_aliases)
{
    OptAliases out;
    for (int i=0 ; i < xml_aliases.Size() ; ++i)
    {
        string alias = xml_aliases[i].asString();

        if ( !alias.empty() ) out << alias;
    }

    return out;
}

Attributes read_attributes (xmlConfig::Parts& xml_attributes)
{
    Attributes out;
    for (int i=0 ; i < xml_attributes.Size() ; ++i)
    {
        string attribute = xml_attributes[i].asString();

        if ( !attribute.empty() ) out << attribute;
    }

    return out;
}

// for now, just compare strings
bool matchUri(const string& uri, const string& ref)
{
    if ( uri != ref)
    {
        cout << "Request uri do not match!" << endl;
        cout << "Request  : '" << uri << "'" << endl;
        cout << "Reference: '" << ref << "'" << endl;

        return false;
    }

    return true;    // matches
}


int main(int argc, char* argv[])
{
  // Cmd-line arg - input file name, stdin if no args
  string strConfig;

  if ( argc > 1 )
  {
    string filename( argv[1] );
    ifstream file (filename.c_str());

    if ( !file.is_open() ) {
        cout << "Error: unable to open file: " << filename << endl;
        return 3;
    }

    string str;

    while ( !file.eof() )
    {
        getline(file, str);
        strConfig.append(str);
    }
  }
  else
  {
    string str;

    while ( !cin.eof() )
    {
        getline(cin, str);
        strConfig.append(str);
    }
  }

  xmlConfig::XConfig conf;
  conf.Parse(strConfig);

  // common options
  string type, uid, suid, sid, login, userip;
  conf.GetIfExists("/doc/type", type);
  conf.GetIfExists("/doc/uid", uid);
  conf.GetIfExists("/doc/suid", suid);
  conf.GetIfExists("/doc/sid", sid);
  conf.GetIfExists("/doc/login", login);
  conf.GetIfExists("/doc/userip", userip);

  if ( type.empty() )
  {
      cout << "Unknown test type." << endl;
      return 2;
  }

  // options
  Options opts;
  string val;

  if ( conf.GetIfExists("/doc/regname", val) )
      opts << optRegname;

  if ( conf.GetIfExists("/doc/email/getall", val) )
      opts << optGetAllEmails;

  if ( conf.GetIfExists("/doc/email/getyandex", val) )
      opts << optGetYandexEmails;

  if ( conf.GetIfExists("/doc/email/testone", val) )
      opts << OptTestEmail(val);

  if ( conf.GetIfExists("/doc/email/getdefault", val) )
      opts << optGetDefaultEmail;

  if ( conf.GetIfExists("/doc/aliases/getsocial", val) )
      opts << optGetSocialAliases;

  if ( conf.GetIfExists("/doc/ver2", val) )
      opts << optVersion2;

  if ( conf.GetIfExists("/doc/authid", val) )
      opts << optAuthId;

  if ( conf.GetIfExists("/doc/multisession", val) )
      opts << optMultisession;

  if ( conf.GetIfExists("/doc/fullinfo", val) )
      opts << optFullInfo;

  xmlConfig::Parts xml_opts = conf.GetParts("/doc/option");
  xmlConfig::Parts xml_fields = conf.GetParts("/doc/dbfield");
  xmlConfig::Parts xml_aliases = conf.GetParts("/doc/aliases/alias");
  xmlConfig::Parts xml_attributes = conf.GetParts("/doc/attribute");

  read_opts(opts, xml_opts);

  DBFields fields = read_dbfields(xml_fields);
  opts << fields;

  OptAliases aliases = read_aliases(xml_aliases);
  opts << aliases;

  if ( conf.GetIfExists("/doc/aliases/all", val) )
      opts << optGetAllAliases;

  Attributes attributes = read_attributes(xml_attributes);
  opts << attributes;

  // login && session specific params
  string password, authtype, sessionid, hostname, token;
  conf.GetIfExists("/doc/password", password);
  conf.GetIfExists("/doc/authtype", authtype);
  conf.GetIfExists("/doc/sessionid", sessionid);
  conf.GetIfExists("/doc/session_host", hostname);
  conf.GetIfExists("/doc/token", token);

  // mailhost specific params
  string operation, scope, dbid, priority, mx, olddbid;
  conf.GetIfExists("/doc/operation", operation);
  conf.GetIfExists("/doc/scope", scope);
  conf.GetIfExists("/doc/dbid", dbid);
  conf.GetIfExists("/doc/priority", priority);
  conf.GetIfExists("/doc/mx", mx);
  conf.GetIfExists("/doc/olddbid", olddbid);

  // reference request
  string ref_request, ref_body;
  conf.GetIfExists("/doc/request", ref_request);
  conf.GetIfExists("/doc/reqbody", ref_body);

  if ( type == strLogin )
  {
    if ( (login.empty() && uid.empty()) || password.empty() )
    {
        cout << "Bad login request parameters." << endl;
        return 2;
    }

    LoginReqData login_data = uid.empty() ?
        LoginRequest(LoginSid(login, sid), password, authtype, userip, opts) :
        LoginRequestUid(uid, password, authtype, userip, opts) ;

    if ( !matchUri(login_data.uri_, ref_request) )
        return 1;

    if ( login_data.postData_ != ref_body )
    {
        cout << "Login request bodies do not match!" << endl;
        cout << "Request  :'" << login_data.postData_ << "'" << endl;
        cout << "Reference:'" << ref_body << "'" << endl;
        return 1;
    }

    return 0;
  }
  else
  {
      string req;

      if ( type == strInfo )
      {
          if ( !uid.empty() )                       // info by uid
              req = InfoRequest(uid, userip, opts);
          else if ( !login.empty() )                // info by login/sid
              req = InfoRequest( LoginSid(login, sid), userip, opts);
          else if ( !suid.empty() && !sid.empty() ) // info by suid/sid
              req = InfoRequest( SuidSid(suid, sid), userip, opts );
          else
          {
              xmlConfig::Parts uidspart(conf.GetParts("/doc/uids/uid"));
              if ( uidspart.Size()==0 )
              {
                cout << "Bad info request data." << endl;
                return 2;
              }
              vector<string> uids;

              for(int i=0; i<uidspart.Size(); ++i)
                  uids.push_back(uidspart[i].asString());

              req = InfoRequest( uids, userip, opts);
          }
      }
      else if ( type == strSession )
      {
          req = SessionIDRequest(sessionid, hostname, userip, opts);
      }
      else if ( type == strOAuth )
      {
          req = OAuthRequest(token, userip, opts);
      }
      else if ( type == strMailHost )
      {
          if ( operation.empty() || scope.empty() )
          {
              cout << "Bad mailhost request parameters." << endl;
              return 2;
          }

          // select operation type
          if ( operation == strOpCreate )
          {
              req = MailHostCreateRequest(scope, dbid, priority, mx);
          }
          else if ( operation == strOpDelete )
          {
              req = MailHostDeleteRequest(scope, dbid);
          }
          else if ( operation == strOpAssign )
          {
              req = MailHostAssignRequest(scope, suid, dbid, olddbid);
          }
          else if ( operation == strOpSetPrio )
          {
              req = MailHostSetPriorityRequest(scope, dbid, priority);
          }
          else if ( operation == strOpFind )
          {
              req = MailHostFindRequest(scope, priority);
          }
          else
          {
              cout << "Unknown mailhost operation." << endl;
              return 2;
          }
      }
      else if ( type == strPwdQuality )
      {
          req = PwdQualityRequest(sessionid, hostname);
      }

      if ( !matchUri(req, ref_request) )
          return 1;

      return 0;
  }

  return 0;
}


// vi: expandtab:sw=4:ts=4
// kate: replace-tabs on; indent-width 4; tab-width 4;
