#!/bin/bash -xe
# Before use this script - you need launch mage instance into dev server.
# Get map from prod, upload to testing, generate recluster in testing, compare with prod.
IGNOREZK="False"

PROD_HOST="man2-6389.search.yandex.net:21051"
TESTING_HOST="msearch-precise-dev.mail.yandex.net:1884"
#PROJECT="mail"
PROJECT=$1

#prev_rev="$(curl -s "http://$PROD_HOST/api/v3.0/$PROJECT/lastrev" | jq -r .data.pre_revision)"
if [[ "$PROJECT" == "disk" ]];then
 prev_rev="stable-151-r33"
fi;

if [[ "$PROJECT" == "mail" ]];then
 prev_rev="stable-151-r33"
fi;

curr_rev="$(curl -s "http://$PROD_HOST/api/v3.0/$PROJECT/lastrev" | jq -r .data.revision)"

# 1. Get and upload searchmap
if [[ "$PROJECT" == "disk" ]]; then
 NANNY_SERVICE="disk_search_backend_prod,disk_search_backend_prestable"
elif [[ "$PROJECT" == "mail" ]]; then
 NANNY_SERVICE="mail_search_prod,mail_search_prestable"
fi;

# 1.1, Sync before upload

curl -s "http://$TESTING_HOST/api/v1.0/$PROJECT/synchosts?nservices=$NANNY_SERVICE&project=$PROJECT&revision=tags/$prev_rev"

curl -s "http://$PROD_HOST/api/v3.0/$PROJECT/getmap?revision=searchmap:tags/$(echo $prev_rev)&full=true&getfat=true&pformat=testing" > generated_prod_searchmap.txt
curl -s -X POST -F "searchmap=@generated_prod_searchmap.txt" "http://$TESTING_HOST/api/v3.0/$PROJECT/upload?revision=tags/$prev_rev"

# 2. Generate recluster
curl "http://$TESTING_HOST/api/v3.0/$PROJECT/genmaprec?revision=tags/$curr_rev&prev_revision=tags/$prev_rev&force=yes_i_want_this_uguu"

# 3. Get recluster from testing.
curl -s "http://$TESTING_HOST/api/v3.0/$PROJECT/getmap?revision=recluster_searchmap:tags/$curr_rev&full=true" > generated_testing_recluster_searchmap.txt
curl -s "http://$PROD_HOST/api/v3.0/$PROJECT/getmap?revision=recluster_searchmap:tags/$curr_rev&full=true" > generated_prod_recluster_searchmap.txt

# 4. Check inum count
if [[ "$PROJECT" == "disk" ]]; then
 echo "Checking inum count for disk_queue"
 inumcnt="$(cat generated_testing_recluster_searchmap.txt | grep "disk_queue" | grep -v -e "#\|^$" | cut -d "," -f 1 | cut -d " " -f 2 | sort | uniq | wc -l)"
 if [[ "$inumcnt" != "1000" ]]; then
 echo "$(date) Cannot verify inum count for $PROJECT. Found: $inumcnt inums"
 exit 1
 fi;
fi;

# 5. Test shards count

python testshards.py generated_testing_recluster_searchmap.txt || (echo "Verified shards count stable  ends with ERROR!"  && exit 1)

# 6. Sort and check sm from prod and testing

echo "test string" >> generated_testing_recluster_searchmap.txt
# if we need pass zookeeper check
#sed  -i 's/zk:.*\/.*,/zk:/g' generated_*_searchmap.txt
sort generated_testing_recluster_searchmap.txt > generated_testing_recluster_searchmap.txt_sorted
sort generated_prod_recluster_searchmap.txt > generated_prod_recluster_searchmap.txt_sorted

if [[ $IGNOREZK == "True" ]]; then
 # Ignore zk when zkformat differs - to pass zk's tests
 sed -i 's/zk:.*\/.*,//g' generated_testing_recluster_searchmap.txt_sorted
 sed -i 's/zk:.*\/.*,//g' generated_prod_recluster_searchmap.txt_sorted
fi;

notfoundcnt=$(comm -23 generated_testing_recluster_searchmap.txt_sorted generated_prod_recluster_searchmap.txt_sorted > hosts_not_found.txt && cat hosts_not_found.txt | wc -l)

if [[ "$notfoundcnt" != "1" ]]; then
 echo "$(date) Cannot verify changes count for $PROJECT. Found: $notfoundcnt different from prod hosts"
 exit 1
fi;

if [[ "$(cat hosts_not_found.txt)" != "test string" ]]; then
echo "$(date) Cannot find hosts after recluster. Exiting."
echo "$(date) Use: cat generated_testing_searchmap.txt_sorted | grep "
echo "$(date) Use: cat generated_prod_searchmap.txt_sorted | grep "
exit 1;
fi;

# 7. Cleanup
rm -f generated_*
rm -f hosts_not_found.txt
