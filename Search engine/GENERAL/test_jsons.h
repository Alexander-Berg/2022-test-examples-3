#pragma once

#include "util/generic/string.h"

static const TString TEST_OBJECT_KEY = "enw171988";

TString GetTestObjectJSON();

static const TString MADONNA_ALBUMS_KEY = "rulst309";

static const TString MADONNA_ALBUMS_OBJECT = R"raw_json_string(
{
   "Title@on" : [
      {
         "value" : "альбомы мадонны"
      }
   ],
   "Key@on" : [
      {
         "value" : "альбомы мадоны [[#ruw131024]]"
      }
   ],
   "isa" : {
      "Wtype@on" : [
         "List"
      ]
   }
}
)raw_json_string";

static const TString BUEST_KEY = "enw10087573";

static const TString BUEST_OBJECT = R"raw_json_string(
{
   "Key@on" : [
      {
         "value" : "[[#enw10087573|Baddi University]],BUEST"
      }
   ],
   "Title@on" : [
      {
         "value" : "Baddi University of Emerging Sciences and Technologies"
      }
   ],
   "isa" : {
      "Wtype@on" : "Org"
   }
}
)raw_json_string";
