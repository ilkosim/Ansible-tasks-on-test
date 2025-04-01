#!/bin/bash
#
# CAUTION: This file is automatically deployed by BNF pipeline 
# via Bitbucket repo https://repository.secure.ifao.net:7443/scm/its/cytric_config.git
#

export JAVA_HOME=/home/oracle/jdk
export PATH=$PATH:$HOME/.local/bin:$HOME/bin:$JAVA_HOME/bin

# script parameters
WL_Name="AdminServer"
java_process="java .*weblogic.Name=$WL_Name.* weblogic.Server"

# Config files and paths
    ADMLOG=/home/ifao/bin/status/OK
    CYTLOG=/home/oracle/logs/cytric/kallisto.log
    CYTRIC_HOME=/home/oracle/weblogic/user_projects/domains/cytric
    CYTRIC_DIR=/home/ifao/cytric
    cc_script="/home/ifao/bin/awk/filter_logs.awk"

# KPI 10 measurement (YG)
K_SCRIPT=/home/ifao/bin/dynatrace_maintenance.sh
K_ID="WebLogic Cytric Kallisto"
K_LOG=/home/oracle/logs/cytric/dynatrace_maintenance.log

# Timeouts
    T_KILL3=5
    T_SHUT=45
    T_KILL=5
    T_KILL9=2
    T_LOOP=3

#. /etc/rc.d/init.d/functions
    BOOTUP=color
    RES_COL=60
    MOVE_TO_COL="echo -en \\033[${RES_COL}G"
    SETCOLOR_SUCCESS="echo -en \\033[1;32m"
    SETCOLOR_FAILURE="echo -en \\033[1;31m"
    SETCOLOR_WARNING="echo -en \\033[1;33m"
    SETCOLOR_NORMAL="echo -en \\033[0;39m"
    LOG_TAG="`date` ***RESTARTscript: "
    JAVAW="/home/oracle/jdk"
    

function echo_success() {
  [ "$BOOTUP" = "color" ] && $MOVE_TO_COL
  echo -n "[  "
  [ "$BOOTUP" = "color" ] && $SETCOLOR_SUCCESS
  echo -n $"OK"
  [ "$BOOTUP" = "color" ] && $SETCOLOR_NORMAL
  echo -n "  ]"
  echo -ne "\r"
  return 0
}

function echo_failure() {
  [ "$BOOTUP" = "color" ] && $MOVE_TO_COL
  echo -n "["
  [ "$BOOTUP" = "color" ] && $SETCOLOR_FAILURE
  echo -n $"FAILED"
  [ "$BOOTUP" = "color" ] && $SETCOLOR_NORMAL
  echo -n "]"
  echo -ne "\r"
  return 1
}

function echo_passed() {
  [ "$BOOTUP" = "color" ] && $MOVE_TO_COL
  echo -n "["
  [ "$BOOTUP" = "color" ] && $SETCOLOR_WARNING
  echo -n $"CHECK"
  [ "$BOOTUP" = "color" ] && $SETCOLOR_NORMAL
  echo -n "]"
  echo -ne "\r"
  return 1
}

function heapdump() {
### get Cytric Java process ID
proc_id=`ps -ef |grep "Name=AdminServer" | grep -v grep | awk '{print $2}'`

### get Jaxa Xmx size in MB
xmx_param=`ps -f -q $proc_id  | grep -o "Xmx[0-9]*."`
xmx_value=${xmx_param##Xmx}
xmx_number=`echo $xmx_value | grep -o "[0-9]*"`

if   ( echo "$xmx_value" | grep -q "[kK]" ); then xmx_size_mb=`expr $xmx_number / 1024`;
elif ( echo "$xmx_value" | grep -q "[mM]" ); then xmx_size_mb=$xmx_number;
elif ( echo "$xmx_value" | grep -q "[gG]" ); then xmx_size_mb=`expr $xmx_number \* 1024`;
elif [ "$xmx_value" == "$xmx_number" ];    then xmx_size_mb=`expr $xmx_number / 1024`; xmx_size_mb=`expr $xmx_size_mb / 1024`; fi

if [ "$xmx_size_mb" == "" ]; then xmx_size_mb=64; fi;
xmx_half_mb=`expr $xmx_size_mb / 2`
xmx_double_mb=`expr $xmx_size_mb + $xmx_half_mb`

echo "Xmx is $xmx_size_mb MB and will need no more than $xmx_double_mb MB for heap dump and zip"

### get time stamp and construct file name
time_stamp=`date +%Y%m%d-%H%M%S`
heapdump_dir="/home/ifao/heapdump"
heap_dump_file="${heapdump_dir}/cytric_`hostname -s`_${time_stamp}.hprof"
heap_dump_tmp="${heapdump_dir}/cytric_`hostname -s`_${time_stamp}.tmp"

### go to the home dir and check for free space
free_space=`df --sync -m  --output=avail ${heapdump_dir} | grep -v Avail`
echo "free space in $heapdump_dir is $free_space MB"

### get a heap dump if the free disk space is more than the heap dump
date
if [[ "$free_space" -gt "$xmx_double_mb" ]]; then
  num_try=1
  hd_stat=111
  while [[ "$num_try" -lt "31" ]] && [[ "$hd_stat" != "0" ]]; do
    echo "Try to get the heap dump $num_try ..."
    jmap_out=`/home/oracle/jdk/bin/jmap -dump:format=b,file=$heap_dump_tmp $proc_id 2>&1`
    hd_stat=$?
    echo $jmap_out
    num_try=`expr $num_try + 1`
    sleep 1
    done
else
  hd_stat=2
  jmap_out="there is only $free_space MB free space in $heapdump_dir, since it is required at least $xmx_double_mb"
fi

mail_body="unknown info"

if [ "$hd_stat" == "0" ]; then
  echo " Heap dump is created:"
  mv -f $heap_dump_tmp $heap_dump_file
  chmod 660 $heap_dump_file
  ls -lh "$heap_dump_file"
  mail_body="Heap dump created $heap_dump_file"
else
  mail_body="*** Head dump is not created !!! jmap error: $jmap_out"
fi

echo "$mail_body" | mail -s "heap dump on cytric $HOSTNAME" it-service@ifao.net

}


function start_wl() {
           java=$(ps ax | grep -v grep | grep "$java_process" | wc -l)	  
           if [ $java -eq 0 ]; then
	    echo -n "cytric v7 starting "             
	     touch $ADMLOG
	     cd $CYTRIC_HOME
	     echo "$LOG_TAG <start sequence initiated>" 1>>$ADMLOG 2>>$ADMLOG

                #filter_script="/home/ifao/bin/awk/filter_logs.sed"
                #echo "`ls -la --full-time "$filter_script"` `sha1sum < "$filter_script"`" >> $CYTLOG
                #echo "`ls -la --full-time "$cc_script"` `sha1sum < "$cc_script"`" >> $CYTLOG
                #sh startWebLogic.sh 2>&1 | sed -rf "$filter_script" | awk -f "$cc_script" 1>>$CYTLOG 2>>$CYTLOG &
                sh startWebLogic.sh 1>>$CYTLOG 2>>$CYTLOG &

	     echo $START_WEBLOGIC
	     echo "$LOG_TAG <start sequence finished>" 1>>$ADMLOG 2>>$ADMLOG
	     echo >>$ADMLOG
	     echo_success
	     echo
	    else  
	     echo -n "cytric v7 is already started" 
	     echo_failure
	     echo
	   fi 
}

function stop_wl() {
          echo "0" > /home/ifao/pid/v7.pid
          i=0
          SHUTDOWN_STARTED="yes"
          echo "$LOG_TAG shutdown script is starting - $SHUTDOWN_STARTED" >> $CYTLOG
          echo "shutdown script is starting - $SHUTDOWN_STARTED"
          echo "$LOG_TAG  <stop sequence initiated>" 1>>$ADMLOG 2>>$ADMLOG
          echo "$LOG_TAG  <stop sequence initiated>" 1>> $CYTLOG 2>>  $CYTLOG
          echo "Forcing JVM to print in the log file...       [ $T_KILL3 sec]"
          echo "$LOG_TAG Forcing JVM to print in the log file...       [ $T_KILL3 sec]"   >> $CYTLOG
          /usr/bin/kill -3 `ps ax | grep -v grep | grep "$java_process" | awk '{ print $1 }'`
          sleep $T_KILL3
          echo "Trying to stop weblogic server gracefully...  [ $T_SHUT sec]"
          echo "$LOG_TAG Trying to stop weblogic server gracefully...  [ $T_SHUT sec]"  >> $CYTLOG
          cd $CYTRIC_HOME
          sh stopWebLogic.sh 1>>$CYTLOG 2>>$CYTLOG &
          echo "$LOG_TAG stopWebLogic.sh script is started"  >> $CYTLOG
          /usr/bin/kill -3 `ps ax | grep -v grep | grep "$java_process" | awk '{ print $1 }'`
          echo "java process: $java"
          echo "$LOG_TAG java process: $java"  >> $CYTLOG
          i=1;
          while [ true ]
          do
           java=$(ps ax | grep -v grep | grep "$java_process" | wc -l)
           if [ $i -eq $T_SHUT ] && [ "$SHUTDOWN_STARTED" = "yes" ]; then
            echo "["$$"] still "$java "java process are active"
            echo "Terminating all java process survivors...     [ $T_KILL9 sec]"
            echo "$LOG_TAG Terminating all java process survivors...     [ $T_KILL9 sec]" >> $CYTLOG
            /usr/bin/kill -15 `ps ax | grep -v grep | grep "$java_process" | awk '{ print $1 }'`
            sleep $T_KILL9
            java=$(ps ax | grep -v grep | grep "$java_process" | wc -l)
            echo "status: $java java proc started"
            echo "$LOG_TAG status $java java proc started"  >> $CYTLOG
            echo "$LOG_TAG  <stop sequence finished>" 1>>$ADMLOG 2>>$ADMLOG
            echo "$LOG_TAG  <stop sequence finished>">> $CYTLOG
            echo >>$ADMLOG
              echo_success
              break;
            else
             echo -n "wait $T_SHUT sec.  $i sec java process: $java"
             echo -n "$LOG_TAG wait $T_SHUT sec.  $i sec java process: $java" >> $CYTLOG
             echo >> $CYTLOG
             echo_passed
             echo
           if [ $java -eq 0 ]; then
            break;
           fi
          fi
            sleep 1
            i=$[$i + 1]
          done
          if [ $java -gt 0 ]; then
             /usr/bin/kill -9 `ps ax | grep -v grep | grep "$java_process" | awk '{ print $1 }'`
             sleep $T_KILL9
             cd $CYTRIC_DIR
             . set_classpath
             sh scripts/dmdbunlocker.sh
          fi
          # Delete WLS cache dir
          rm -rf $CYTRIC_HOME/servers/$WL_Name/cache
          # Delete WLS tmp dir
          rm -rf $CYTRIC_HOME/servers/$WL_Name/tmp
          echo "< Shutdown complete >"
          echo "$LOG_TAG < Shutdown complete >" >> $CYTLOG
}

case "$1" in 
    start) 
        echo "$LOG_TAG start Weblogic is initiated" >> $CYTLOG
	start_wl
        echo "$LOG_TAG start Weblogic complite" >> $CYTLOG
        echo "`date` executing $K_SCRIPT stop $K_ID" 1>>$K_LOG 2>>$K_LOG
        nohup bash $K_SCRIPT stop "$K_ID" 1>>$K_LOG 2>>$K_LOG &
	;;
    loopd) 
    # loopd is internal case; not for shell use
        echo $$  > /home/ifao/pid/v7.pid
        echo "$LOG_TAG loop is started" >> $CYTLOG

        while [ true ]
	do    
	 v7pid=`cat /home/ifao/pid/v7.pid` 
	  if [ $v7pid -eq 0 ]; then
	    break;
	  else 
	    start_wl
	    sleep $T_LOOP
	  fi
	done
	;;
    loop)
        echo 
	echo -n "cytric v7 starting as a daemon" 
	$0 loopd>>/dev/null&
	echo_success
	echo
	;;
    stop_planned|stop_immediate|stop_in-precaution)
        echo
        echo "`date` executing $K_SCRIPT start $K_ID ${1##stop_}" 1>>$K_LOG 2>>$K_LOG
        nohup bash $K_SCRIPT start "$K_ID" ${1##stop_} 1>>$K_LOG 2>>$K_LOG &
        stop_wl
        echo
          ;;
    restart_planned|restart_immediate|restart_in-precaution)
           echo "restart_planned"
           echo "$LOG_TAG restart_planned cytricv7 "  >> $CYTLOG
           heapdump
           $0 ${1/restart/stop}
           $0 start
           echo "done."
           echo "$LOG_TAG restart_planned done"  >> $CYTLOG
          ;;
    tail)
       if [ $# -ne 2 ]
        then
          rows=100
	else
	  rows=$2
       fi      
       tail -f $CYTLOG -n $rows
    ;;	  
    status)	     
            java=$(ps ax | grep -v grep | grep "$java_process" | wc -l)	  
  	    if [ $java -gt 0 ]; then
             echo -n "status: $java java proc started"
	     echo_success
	     echo
	     else 
	      echo -n "status: no java proc started"
	      echo_failure
	      echo
	    fi	    
	sleep 1
	  ;;
    status_simple)
            java=$(ps ax | grep -v grep | grep "$java_process" | wc -l)
            if [ $java -gt 0 ]; then
              echo 'OK'
            else
              echo 'FAILED'
            fi
          ;;
  *)
        echo "Usage: $0 {start|stop_planned|stop_immediate|stop_in-precaution|restart_planned|restart_immediate|restart_in-precaution|status|status_simple|tail <rows>|loop}"
	exit 1
esac
