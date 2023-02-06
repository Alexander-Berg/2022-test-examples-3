with_ava = '''
{
   "users" : [
      {
         "login" : "ald00",
         "regname" : "ald00",
         "have_password" : true,
         "have_hint" : true,
         "karma" : {
            "value" : 0
         },
         "display_name" : {
            "name" : "ald00",
            "avatar" : {
               "default" : "24700/51364272-3439956",
               "empty" : false
            }
         },
         "karma_status" : {
            "value" : 0
         },
         "uid" : {
            "value" : "51364272",
            "hosted" : false,
            "lite" : false
         },
         "id" : "51364272"
      }
   ]
}
'''

with_ava_and_bad_karma = '''
{
   "users" : [
      {
         "login" : "ald00",
         "regname" : "ald00",
         "have_password" : true,
         "have_hint" : true,
         "karma" : {
            "value" : 100
         },
         "display_name" : {
            "name" : "ald00",
            "avatar" : {
               "default" : "24700/51364272-3439956",
               "empty" : false
            }
         },
         "karma_status" : {
            "value" : 0
         },
         "uid" : {
            "value" : "51364272",
            "hosted" : false,
            "lite" : false
         },
         "id" : "51364272"
      }
   ]
}
'''

without_ava = '''
{
   "users" : [
      {
         "karma" : {
            "value" : 0
         },
         "regname" : "jkennedy",
         "have_hint" : true,
         "display_name" : {
            "name" : "jkennedy",
            "avatar" : {
               "default" : "0/0-0",
               "empty" : true
            }
         },
         "karma_status" : {
            "value" : 0
         },
         "uid" : {
            "lite" : false,
            "hosted" : false,
            "value" : "14053877"
         },
         "have_password" : true,
         "id" : "14053877",
         "login" : "jkennedy"
      }
   ]
}
'''

without_ava_and_bad_karma = '''
{
   "users" : [
      {
         "karma" : {
            "value" : 100
         },
         "regname" : "jkennedy",
         "have_hint" : true,
         "display_name" : {
            "name" : "jkennedy",
            "avatar" : {
               "default" : "0/0-0",
               "empty" : true
            }
         },
         "karma_status" : {
            "value" : 0
         },
         "uid" : {
            "lite" : false,
            "hosted" : false,
            "value" : "14053877"
         },
         "have_password" : true,
         "id" : "14053877",
         "login" : "jkennedy"
      }
   ]
}
'''

no_user = '''
{
   "users" : [
      {
         "karma" : {
            "value" : 0
         },
         "karma_status" : {
            "value" : 0
         },
         "id" : "",
         "uid" : {}
      }
   ]
}
'''

error = '''
{
   "error" : "BlackBox error: Missing userip argument",
   "exception" : {
      "id" : 2,
      "value" : "INVALID_PARAMS"
   }
}
'''

not_json = '''
<?xml version="1.0" encoding="UTF-8"?>
<doc>
<exception id="2">INVALID_PARAMS</exception>
<error>BlackBox error: Missing userip argument</error>
</doc>
'''
