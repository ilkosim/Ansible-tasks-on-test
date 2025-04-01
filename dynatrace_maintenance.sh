#!/bin/bash

usage() {
   echo
   echo "$1"
   echo
   echo "This script creates Dynatrace Maintenance Window for specific Process Group"
   echo
   echo "usage:"
   echo "   $0 start <Dynatrace Process Group name> [planned|immediate|in-precaution]"
   echo "   $0 [stop|end|list] <Dynatrace Process Group name>"
   echo
   echo "      start - create a Dynatrace Maintenance Window for the specified Process Group"
   echo "      stop|end   - update the 'end' date of the last Dynatrace Maintenance Window, for the specified Process Group"
   echo "      list  - list the Maintenance Windows related to the specified Process Group"
   echo
   exit $2
}


dt_get_api() {
   #
   # dt_get_api "API call" "result file"
   # dt_get_api "config/v1/dashboards" $all_dash
   #
   curl -s -X GET "https://ruxit.secure.ifao.net/e/$environment/api/$1" \
	  -H "accept: application/json; charset=utf-8" \
	  -H "Authorization: Api-Token $token" | jq . > "$2"
}


dt_send_api() {
   #
   # dt_post_api "POST/PUT" "API call"  "data file"  "result file"
   # 
   # dt_send_api POST "config/v1/dashboards"          "$dst_dash_json"  "${dst_dash_json}.creation_result"
   # dt_send_api PUT  "config/v1/dashboards/$dash_id" "$dst_dash_json"  "${dst_dash_json}.creation_result"
   # 

   curl -s -X $1 "https://ruxit.secure.ifao.net/e/$environment/api/$2" \
	  -H "accept: application/json; charset=utf-8" \
	  -H "Authorization: Api-Token $token" \
	  -H "Content-Type: application/json; charset=utf-8" \
	  -d "@$3" | jq . > "$4"
}


get_pg_id() {
   #
   # prepare an API call URL, which to get Process Group which match $1 name 
   # and set its ID to PG_ID
   #
   #    get_pg_id "$PG_NAME"  --->  PG_ID
   #

   #
   # variant with sed
   #
   # api_url=`echo "v2/entities?pageSize=40&entitySelector=type(process_group),entityName(\"$1\")" | sed 's/ /%20/g' | sed 's/(/%28/g' | sed 's/)/%29/g' | sed 's/"/%22/g'`

   #
   # select the Process Group by technology
   # api_url="v2/entities?pageSize=40&from=now-1d&entitySelector=softwareTechnologies(ORACLE_WEBLOGIC),type(process_group),entityName(\"$1\")"


   api_url="v2/entities?pageSize=40&entitySelector=metadata(JAVA_MAIN_CLASS:weblogic.Server),type(process_group),entityName(\"$1\")"
   api_url=${api_url// /%20}
   api_url=${api_url//(/%28}
   api_url=${api_url//)/%29}
   api_url=${api_url//\"/%22}

   dt_get_api "$api_url" all_pgs.json
   pg_data=`grep -B2 \""$1"\" all_pgs.json`
   PG_ID=`echo $pg_data | grep -oe "PROCESS_GROUP-[A-Z0-9]\{16\}"`

   if [[ "$PG_ID" == "" ]]; then
      usage "*** unable to find PROCESS_GROUP '$1'" 13
      fi
}


validate_reason() {
   #
   # validate the value of $MW_REASON, whether it is [planned|immediate|in-precaution]
   # and sets its corresponding MW_TYPE [PLANNED|UNPLANNED]
   #

   if [[ "$MW_REASON" == "" ]]; then
      usage "*** no reason is specified" 14
      fi

   case "$MW_REASON" in

      planned )
	 MW_TYPE=PLANNED
         ;;

      immediate )
	 MW_TYPE=UNPLANNED
         ;;

      in-precaution )
	 MW_TYPE=UNPLANNED
         ;;

      * )
        usage "*** unknown reason '$MW_REASON'" 15 
        ;;

      esac
}


mw_post_data() {
   #
   # create a JSON structure for creation of new Maintenance Window
   # and puts its content into file $1
   #
   #         mw_post_data new_mw.json
   #
   cat > $1 <<EOF
{
  "name": "${NOW_TIME} ${PG_NAME} --- ${MW_REASON}",
  "description": "${PG_ID}",
  "type": "${MW_TYPE}",
  "enabled": true,
  "suppression": "DETECT_PROBLEMS_DONT_ALERT",
  "suppressSyntheticMonitorsExecution": true,
  "scope": {
    "entities": [
      "${PG_ID}"
    ],
    "matches": []
  },
  "schedule": {
    "recurrenceType": "ONCE",
    "start": "${NOW_TIME}",
    "end": "${PLUS_6MIN}",
    "zoneId": "Europe/Berlin"
  }
}
EOF
}


#
#  start point
#
set -x

# Production env
environment="abc46791-5401-4fb8-b4e3-0744d06513b8"

# the token can not be storred in Bitbucket, please contact a Dynatrace Admin
#
# needed rights:
# API v2: 'Read metric', 'Read entities', 'Read problems', 'Write problems'
# API v1: 'Access problems and event feed, metric, and topology', 'Read configuration', 'Write configuration', 
#         'Change data privacy settings', 'User sessions'
token="dt0c01.7ZQ5RKHKPYHTN7FEMBLWOUGJ.5TQNZ74QJF2MYIU2245V23RBAI7KUHOPL72CHVHG2ESNUKXIWOMTHKRUHFCOEE2B"

# generate a randon name for DynaTrace Maintenance Window work dir
work_dir=/tmp/dtmw_$RANDOM
mkdir -pv $work_dir
cd $work_dir

if [ "$environment" == ""  ] || [ "$token" == "" ]; then
   usage "*** login data is not filled" 1
   fi

if [[ "$1" == "" ]]; then 
   usage "*** no start/end/list parameter is specified" 2
   fi

PG_NAME="$2"
if [[ "$PG_NAME" == "" ]]; then 
   usage "*** no <Process group name> is specified" 3
   fi

PG_ID="*** unspecified ***"
get_pg_id "$PG_NAME"
echo "$2 - $PG_ID"

NOW_TIME=`date '+%Y-%m-%d %H:%M'`
PLUS_1MIN=`date --date="+1 minute" '+%Y-%m-%d %H:%M'`
PLUS_6MIN=`date --date="+6 minute" '+%Y-%m-%d %H:%M'`

case $1 in

   "start" )
      echo "$1 ..."
      MW_REASON=$3
      MW_TYPE="*** unspecified ***"
      validate_reason
      mw_post_data new_mw.json
      dt_send_api POST "config/v1/maintenanceWindows" new_mw.json  new_mw.creation_result
      cat new_mw.creation_result
   ;;

   "end" | "stop" )
      echo "$1 ..."
      dt_get_api "config/v1/maintenanceWindows?pageSize=400" all_maint_win.json
      MW_NAME=`grep "$PG_NAME"  all_maint_win.json | sort -r | head -1 | awk -F\" '{print $4}'`
      MW_ID=`grep -B1 "$MW_NAME"  all_maint_win.json | head -1 | awk -F\" '{print $4}'`
      echo "will update last Meintenance Window:"
      echo "      id: $MW_ID"
      echo "    name: $MW_NAME"
      echo "     end: $PLUS_1MIN"
      dt_get_api "config/v1/maintenanceWindows/$MW_ID" last_mw.json
      sed -i.bak1 "s/$MW_NAME/$MW_NAME --- completed/g" last_mw.json
      sed -i.bak2 "s/\"end\": \"202.-..-.. ..:..\"/\"end\": \"$PLUS_1MIN\"/g" last_mw.json
      dt_send_api PUT "config/v1/maintenanceWindows/$MW_ID" last_mw.json  last_mw.update_result
      cat last_mw.update_result
   ;;

   "list" )
      echo "$1 ..."
      dt_get_api "config/v1/maintenanceWindows?pageSize=4000" all_maint_win.json
      grep -1 " $PG_NAME ---" all_maint_win.json
      echo
      echo "summary:"
      grep " $PG_NAME ---" all_maint_win.json | sort
      
   ;;

   * )
      usage "*** unknown $1 command" 4

   esac

# returns to script's dir
cd -
# and remove the work dir, in case of error, the work dir stays for further check
rm -rf $work_dir

