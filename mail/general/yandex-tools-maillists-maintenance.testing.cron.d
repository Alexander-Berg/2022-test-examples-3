MAILTO=ml-robots-dev@yandex-team.ru

MANAGE="/usr/local/bin/manage.py"
FLOCK="zk-flock"

5     *    * * *    www-data . /.env; $FLOCK autosubscribe "$MANAGE autosubscribe --verbosity=1"
2     *    * * *    www-data . /.env; $FLOCK updatecenter "$MANAGE updatecenter --verbosity=1 --noplock"
*     *    * * *    www-data . /.env; $FLOCK sync_django_users "$MANAGE sync_django_users --verbosity=1 --noplock"
1-59/10 *  * * *    www-data . /.env; $FLOCK membership_log_sync "$MANAGE membership_log_sync --verbosity=1 --noplock"
30    *    * * *    www-data . /.env; $FLOCK update_sms_maillists "$MANAGE update_sms_maillists --verbosity=1"
29,59 *    * * *    www-data . /.env; $FLOCK delete_duplicate_subscriptions "$MANAGE delete_duplicate_subscriptions --verbosity=1"
*     *    * * sun  www-data . /.env; $FLOCK cleanup "$MANAGE cleanup --verbosity=1"
1     *    * * *    www-data . /.env; $FLOCK sync_ymail_list_properties "$MANAGE sync_ymail_list_properties --verbosity=1"
*/30  *    * * *    www-data . /.env; $FLOCK sync_staff_redirects "$MANAGE sync_staff_redirects --verbosity=1"
*/30   *    * * *   www-data . /.env; $FLOCK nwsmtp_cache_invalidate "$MANAGE nwsmtp_cache_invalidate --verbosity=1"
*/30   *    * * *    www-data . /.env; $FLOCK update_external_alias "$MANAGE update_external_alias --verbosity=1"
