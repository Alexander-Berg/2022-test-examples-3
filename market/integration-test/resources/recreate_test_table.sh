# use this script to create tables for integration tests,
# if these tables were somehow removed
#
# this script is not run automatically
#

yt --proxy hahn remove //home/market/testing/mstat/olap2etl/all_types

yt --proxy hahn create --recursive table //home/market/testing/mstat/olap2etl/all_types --attributes '{pk_column_name = "pk_int_col";ru_table_name = "Все типы"; ru_schema = [{"name"="pk_int_col";"ru_name"="числовой ПК";};{"name"="datetime";"ru_name"="какая-то дата";};{"name"="col1_date";"ru_name"="дата не время";};{"name"="col1_dbtime";"ru_name"="время не дата";};{"name"="col1_numeric";"ru_name"="какой-то нумерик";};{"name"="col1_str";"ru_name"="строчка"};{"name"="col1_int8";"ru_name"="инт восемь"};{"name"="col1_int16";"ru_name"="инт шестнадцать"};{"name"="col1_int32";"ru_name"="инт тридцать два"};{"name"="col1_int64";"ru_name"="инт шестьдесят четыре"};{"name"="col1_uint8";"ru_name"="беззнаковый восемь"};{"name"="col1_uint16";"ru_name"="беззнаковый шестнадцать"};{"name"="col1_uint32";"ru_name"="беззнаковый тридцать два"};{"name"="col1_uint64";"ru_name"="беззнаковый шестьдесят четыре"};{"name"="col1_double";"ru_name"="число с плавающей точкой"};{"name"="col1_boolean";"ru_name"="логическое поле"};]; schema = [{"name"="pk_int_col";"type"="uint32";"sort_order"="ascending";};{"name"="datetime";"type"="string";};{"name"="col1_date";"type"="string";};{"name"="col1_dbtime";"type"="string";};{"name"="col1_numeric";"type"="string"};{"name"="col1_str";"type"="string"};{"name"="col1_int8";"type"="int8"};{"name"="col1_int16";"type"="int16"};{"name"="col1_int32";"type"="int32"};{"name"="col1_int64";"type"="int64"};{"name"="col1_uint8";"type"="uint8"};{"name"="col1_uint16";"type"="uint16"};{"name"="col1_uint32";"type"="uint32"};{"name"="col1_uint64";"type"="uint64"};{"name"="col1_double";"type"="double"};{"name"="col1_boolean";"type"="boolean"};]}'

single_quote="'"
row_1=$(echo -e '{"pk_int_col":1,
"datetime":"2018-01-26 18:45:20",
"col1_date":"2018-01-26",
"col1_dbtime":"19:40",
"col1_numeric":"123456789123456789.123456789",
"col1_str":"somestr1",
"col1_int8":-127,
"col1_int16":32767,
"col1_int32":-2,
"col1_int64":9223372036854775807,
"col1_uint8":255,
"col1_uint16":65535,
"col1_uint32":0,
"col1_uint64":18446744073709551615,
"col1_double":32767.12345,
"col1_boolean":false}' | tr '\n' ' ' | awk '{print $0"\n"}')
row_2=$(echo -e '{"pk_int_col":2,
"datetime":"2018-01-26 18:46:20",
"col1_date":"2018-01-27",
"col1_dbtime":"19:40:24",
"col1_numeric":"123456789123456789",
"col1_str":"немного '${single_quote}'юникода'${single_quote}'",
"col1_int8":127,
"col1_int16":-32767,
"col1_int32":2,
"col1_int64":-9223372036854775807,
"col1_uint8":0,
"col1_uint16":65535,
"col1_uint32":0,
"col1_uint64":18446744073709551615,
"col1_double":null,
"col1_boolean":true}' | tr '\n' ' ' | awk '{print $0"\n"}')
row_3=$(echo -e '{"pk_int_col":3,
"datetime":"2018-01-26 18:47:20",
"col1_date":"2018-01-28",
"col1_dbtime":"19:40",
"col1_numeric":"0",
"col1_str":"String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. String longer than 512 symbols. Four chars longer than 512 symbols. ",
"col1_int8":127,
"col1_int16":-32767,
"col1_int32":2,
"col1_int64":-9223372036854775807,
"col1_uint8":0,
"col1_uint16":65535,
"col1_uint32":0,
"col1_uint64":18446744073709551615,
"col1_double":null,
"col1_boolean":true}' | tr '\n' ' ' | awk '{print $0"\n"}')
echo -e ${row_1}"\n"${row_2}"\n"${row_3}"\n" | yt --proxy hahn write //home/market/testing/mstat/olap2etl/all_types --format '<encode_utf8=false>json'



# manual table
# just copypaste code above and change path to
# //home/market/testing/mstat/olap2etl/manual/this_is_table
