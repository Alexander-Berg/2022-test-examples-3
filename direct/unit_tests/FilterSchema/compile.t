#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use JSON::XS;

BEGIN {
    use_ok 'FilterSchema';
}

sub get_checker {
    return FilterSchema->new(schema => JSON::XS->new->relaxed(1)->utf8(1)->decode( shift ));
}


my $schema = <<END
{
    "checkedStruct": {
        "type" : "#/types/arrayOfMyObjects",
        "constraints" : [
           {"name": "itemsCountMax", "parameters": 10, "error": "Too many items" },
        ]
    },

    "myObjects":{
        "type": "object",
        "fields": {
            "name": {"type": "string"},
            "weight": {"type": "integer"},
            "color" : {"type": "string"}
        },
        "allowOptionalFields":1
    },

    "myVariants":{
        "orange": {
            "type" : "#/myOrange",
            "errorOnWrongType": "broken orange"
        },
        "apple": {
            "type": "#/myApple",
        },
        "brick": {
            "type":"object",
            "errorOnWrongType" : "Invalid brick",
            "fields": {
                "name": {"type": "string"},
                "weight": {
                    "type": "integer",
                    "constraints":[
                        {"name":"minValue", "parameters": 100, "error":"too lightweight brick"}
                    ]
                },
                "color":{
                    "typeByCondition": [
                        {
                            "condition": {"field": "weight", "in": [100,200,300]},
                            "type":{"enum":["Red"]},
                            "errorOnWrongType": "color must be red"
                        },
                        {
                            "condition": {"field": "weight", "in": [500,600,700]},
                            "type":{"enum":["White"]},
                            "errorOnWrongType": "color must be white"
                        },
                    ],
                    "default": {"type" : {"enum":[]}, "errorOnWrongType":"Invalid weight of brick"}
                },

                "smoothness":{"type":"integer", "errorOnWrongType": "invalid smoothness"}
            }
        }
    },
    "myApple": {
        "type":"object",
        "fields": {
            "name": {"type": "string"},
            "weight": {
                "type": "integer",
                "constraints":[
                    {"name":"minValue", "parameters": 10, "error":"too lightweight apple"}
                ]
            },
            "color":{"type":{"enum":["Red", "Green"]}, "errorOnWrongType": "invalid apple color"}
        }
    },
    "myOrange": {
        "type":"object",
        "fields": {
            "name": {"type": "string"},
            "weight": {
                "type": "integer",
                "constraints":[
                    {"name":"minValue", "parameters": 15, "error":"too lightweight orange"}
                ]
            },
            "color":{"type":{"enum":["Orange", "Green", "Yellow"]}, "errorOnWrongType": "invalid orange color"}
        }
    },

  "types": {
    "arrayOfMyObjects" : {
      "type" : "array",
      "items" : { "type":"#/myObjects"},
      "errorOnWrongType" : "Array of myObjects required",
      "constraints" : [
        {"name": "itemAnyOf", "parameters":{ "variants": "#/myVariants", "mapBy" : "name"}, "error": "Wrong items"}
      ]
    }
  },

  "explain": ["#/types/*", "#/myVariants/*", "#/checkedStruct"]

}
END
;



my $result = <<END
{
   "checkedStruct" : {
      "errorOnWrongType" : "Array of myObjects required",
      "items" : {
         "fields" : {
            "weight" : {
               "typeName" : "integer",
               "type" : "integer"
            },
            "name" : {
               "typeName" : "string",
               "type" : "string"
            },
            "color" : {
               "type" : "string",
               "typeName" : "string"
            }
         },
         "typeName" : "myObjects",
         "allowOptionalFields" : 1,
         "type" : "object"
      },
      "typeName" : "arrayOfMyObjects",
      "constraints" : [
         {
            "parameters" : {
               "mapBy" : "name",
               "variants" : "#/myVariants"
            },
            "error" : "Wrong items",
            "name" : "itemAnyOf"
         },
         {
            "parameters" : 10,
            "error" : "Too many items",
            "name" : "itemsCountMax"
         }
      ],
      "type" : "array"
   },
   "myApple" : {
      "type" : "object",
      "fields" : {
         "name" : {
            "type" : "string"
         },
         "weight" : {
            "type" : "integer",
            "constraints" : [
               {
                  "parameters" : 10,
                  "name" : "minValue",
                  "error" : "too lightweight apple"
               }
            ]
         },
         "color" : {
            "errorOnWrongType" : "invalid apple color",
            "type" : {
               "enum" : [
                  "Red",
                  "Green"
               ]
            }
         }
      }
   },
   "types" : {
      "arrayOfMyObjects" : {
         "type" : "array",
         "constraints" : [
            {
               "name" : "itemAnyOf",
               "error" : "Wrong items",
               "parameters" : {
                  "mapBy" : "name",
                  "variants" : "#/myVariants"
               }
            }
         ],
         "typeName" : "array",
         "items" : {
            "type" : "object",
            "allowOptionalFields" : 1,
            "typeName" : "myObjects",
            "fields" : {
               "color" : {
                  "type" : "string",
                  "typeName" : "string"
               },
               "weight" : {
                  "type" : "integer",
                  "typeName" : "integer"
               },
               "name" : {
                  "type" : "string",
                  "typeName" : "string"
               }
            }
         },
         "errorOnWrongType" : "Array of myObjects required"
      }
   },
   "myOrange" : {
      "fields" : {
         "color" : {
            "errorOnWrongType" : "invalid orange color",
            "type" : {
               "enum" : [
                  "Orange",
                  "Green",
                  "Yellow"
               ]
            }
         },
         "weight" : {
            "constraints" : [
               {
                  "error" : "too lightweight orange",
                  "name" : "minValue",
                  "parameters" : 15
               }
            ],
            "type" : "integer"
         },
         "name" : {
            "type" : "string"
         }
      },
      "type" : "object"
   },
   "myVariants" : {
      "orange" : {
         "typeName" : "myOrange",
         "fields" : {
            "color" : {
               "type" : {
                  "enum" : [
                     "Orange",
                     "Green",
                     "Yellow"
                  ]
               },
               "errorOnWrongType" : "invalid orange color",
               "typeName" : "enum"
            },
            "name" : {
               "type" : "string",
               "typeName" : "string"
            },
            "weight" : {
               "typeName" : "integer",
               "type" : "integer",
               "constraints" : [
                  {
                     "name" : "minValue",
                     "error" : "too lightweight orange",
                     "parameters" : 15
                  }
               ]
            }
         },
         "errorOnWrongType" : "broken orange",
         "type" : "object"
      },
      "apple" : {
         "fields" : {
            "color" : {
               "errorOnWrongType" : "invalid apple color",
               "typeName" : "enum",
               "type" : {
                  "enum" : [
                     "Red",
                     "Green"
                  ]
               }
            },
            "name" : {
               "type" : "string",
               "typeName" : "string"
            },
            "weight" : {
               "typeName" : "integer",
               "type" : "integer",
               "constraints" : [
                  {
                     "error" : "too lightweight apple",
                     "name" : "minValue",
                     "parameters" : 10
                  }
               ]
            }
         },
         "typeName" : "myApple",
         "type" : "object"
      },
      "brick" : {
         "type" : "object",
         "typeName" : "object",
         "fields" : {
            "color" : {
               "default" : {
                  "errorOnWrongType" : "Invalid weight of brick",
                  "typeName" : "enum",
                  "type" : {
                     "enum" : []
                  }
               },
               "typeByCondition" : [
                  {
                     "type" : {
                        "enum" : [
                           "Red"
                        ]
                     },
                     "typeName" : "enum",
                     "condition" : {
                        "field" : "weight",
                        "in" : [
                           100,
                           200,
                           300
                        ]
                     },
                     "errorOnWrongType" : "color must be red"
                  },
                  {
                     "typeName" : "enum",
                     "condition" : {
                        "field" : "weight",
                        "in" : [
                           500,
                           600,
                           700
                        ]
                     },
                     "errorOnWrongType" : "color must be white",
                     "type" : {
                        "enum" : [
                           "White"
                        ]
                     }
                  }
               ]
            },
            "weight" : {
               "typeName" : "integer",
               "constraints" : [
                  {
                     "name" : "minValue",
                     "error" : "too lightweight brick",
                     "parameters" : 100
                  }
               ],
               "type" : "integer"
            },
            "name" : {
               "type" : "string",
               "typeName" : "string"
            },
            "smoothness" : {
               "type" : "integer",
               "errorOnWrongType" : "invalid smoothness",
               "typeName" : "integer"
            }
         },
         "errorOnWrongType" : "Invalid brick"
      }
   },
   "myObjects" : {
      "allowOptionalFields" : 1,
      "fields" : {
         "weight" : {
            "type" : "integer"
         },
         "name" : {
            "type" : "string"
         },
         "color" : {
            "type" : "string"
         }
      },
      "type" : "object"
   }
}
END
;
my $checker = get_checker($schema);
#$checker->debug(1);
is_deeply $checker->compiled_schema, JSON::XS->new->utf8(1)->decode($result), 'compilation';

my $orig = [ map {$_->description} @{get_checker($schema)->check( [
            {'name' => 'orange', 'weight' => 18, 'color'=>'Red' },
            {'name' => 'apple', 'weight' => 18, 'color'=>'White' },
            {'name' => 'apple', 'weight' => 9, 'color'=>'Green' },
            {'name' => 'brick', 'weight' => 150, 'color'=>'Red', 'smoothness'=>4 },
            {'name' => 'brick', 'weight' => 200, 'color'=>'White', 'smoothness'=>4 },
            {'name' => 'brick', 'weight' => 200, 'color'=>'Red', 'smoothness'=>"h" },
            {'name' => 'brick', 'weight' => 500, 'color'=>'Red', 'smoothness'=>7 },
            {'name' => 'brick', 'weight' => 500, 'color'=>'Red'},

         ])->get_errors}];

#warn Data::Dumper::Dumper($orig);
#END;
is_deeply [map {$_->description} @{get_checker($result)->check( [
            {'name' => 'orange', 'weight' => 18, 'color'=>'Red' },
            {'name' => 'apple', 'weight' => 18, 'color'=>'White' },
            {'name' => 'apple', 'weight' => 9, 'color'=>'Green' },
            {'name' => 'brick', 'weight' => 150, 'color'=>'Red', 'smoothness'=>4 },
            {'name' => 'brick', 'weight' => 200, 'color'=>'White', 'smoothness'=>4 },
            {'name' => 'brick', 'weight' => 200, 'color'=>'Red', 'smoothness'=>"h" },
            {'name' => 'brick', 'weight' => 500, 'color'=>'Red', 'smoothness'=>7 },
            {'name' => 'brick', 'weight' => 500, 'color'=>'Red'},

        ] )->get_errors }], 
        $orig,
        'equals';


done_testing;
