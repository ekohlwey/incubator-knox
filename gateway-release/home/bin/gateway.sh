#!/bin/sh

#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# The app's label
APP_LABEL=Gateway

# The app's name
APP_NAME=gateway

# Start/stop script location
APP_BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# The app's jar name
APP_JAR="$APP_BIN_DIR/gateway.jar"

# The apps home dir
APP_HOME_DIR=`dirname $APP_BIN_DIR`

# The apps home dir
APP_CONF_DIR="$APP_HOME_DIR/conf"

# The app's log dir
APP_LOG_DIR="$APP_HOME_DIR/logs"

# The app's logging options
APP_LOG_OPTS=""

# The app's memory options
APP_MEM_OPTS=""

# The app's debugging options
APP_DBG_OPTS=""

# The app's PID
APP_PID=0

# Start, stop, status, clean or setup
APP_LAUNCH_CMD=$1

# User Name for setup parameter
APP_LAUNCH_USER=$2

# Name of PID file
APP_PID_DIR="$APP_HOME_DIR/pids"
APP_PID_FILE="$APP_PID_DIR/$APP_NAME.pid"

# Name of LOG/OUT/ERR file
APP_OUT_FILE="$APP_LOG_DIR/$APP_NAME.out"
APP_ERR_FILE="$APP_LOG_DIR/$APP_NAME.err"

# Java command
JAVA_CMD=java

# The start wait time
APP_START_WAIT_TIME=2

# The kill wait time limit
APP_KILL_WAIT_TIME=10

function main {
   case "$1" in
      start)  
         appStart
         ;;
      stop)   
         appStop
         ;;
      status) 
         appStatus
         ;;
      clean) 
         appClean
         ;;
      setup) 
         setupEnv $APP_LAUNCH_USER
         ;;
      help)
         printHelp
         ;;
      *)
         printf "Usage: $0 {start|stop|status|clean|setup [USER_NAME]}\n"
         ;;
   esac
}

function appStart {
   createLogFiles

   getPID
   if [ $? -eq 0 ]; then
     printf "$APP_LABEL is already running with PID $APP_PID.\n"
     exit 0
   fi
  
   printf "Starting $APP_LABEL "
   
   rm -f $APP_PID_FILE

   nohup $JAVA_CMD $APP_MEM_OPTS $APP_DBG_OPTS $APP_LOG_OPTS -jar $APP_JAR >>$APP_OUT_FILE 2>>$APP_ERR_FILE & printf $!>$APP_PID_FILE || exit 1

   getPID
   for ((i=0; i<APP_START_WAIT_TIME*10; i++)); do
      appIsRunning $APP_PID
      if [ $? -eq 0 ]; then break; fi
      sleep 0.1
   done
   appIsRunning $APP_PID
   if [ $? -ne 1 ]; then
      printf "failed.\n"
      rm -f $APP_PID_FILE
      exit 1
   fi
   printf "succeeded with PID $APP_PID.\n"
   return 0
}

function appStop {
   getPID
   appIsRunning $APP_PID
   if [ $? -eq 0 ]; then
     printf "$APP_LABEL is not running.\n"
     rm -f $APP_PID_FILE
     return 0
   fi
  
   printf "Stopping $APP_LABEL with PID $APP_PID "
   appKill $APP_PID >>$APP_OUT_FILE 2>>$APP_ERR_FILE

   if [ $? -ne 0 ]; then 
     printf "failed. \n"
     exit 1
   else
     rm -f $APP_PID_FILE
     printf "succeeded.\n"
     return 0
   fi
}

function appStatus {
   printf "$APP_LABEL "
   getPID
   if [ $? -eq 1 ]; then
     printf "is not running. No PID file found.\n"
     return 0
   fi

   appIsRunning $APP_PID
   if [ $? -eq 1 ]; then
     printf "is running with PID $APP_PID.\n"
     exit 1
   else
     printf "is not running.\n"
     return 0
   fi
}

# Removed the app PID file if app is not run
function appClean {
   getPID
   appIsRunning $APP_PID
   if [ $? -eq 0 ]; then 
     deleteLogFiles
     return 0
   else
     printf "Can't clean files.  $APP_LABEL is running with PID $APP_PID.\n"
     exit 1
   fi
}

function appKill {
   local localPID=$1
   kill $localPID || return 1
   for ((i=0; i<APP_KILL_WAIT_TIME*10; i++)); do
      appIsRunning $localPID
      if [ $? -eq 0 ]; then return 0; fi
      sleep 0.1
   done

   kill -s KILL $localPID || return 1
   for ((i=0; i<APP_KILL_WAIT_TIME*10; i++)); do
      appIsRunning $localPID
      if [ $? -eq 0 ]; then return 0; fi
      sleep 0.1
   done

   return 1
}

# Returns 0 if the app is running and sets the $PID variable.
function getPID {
   if [ ! -d $APP_PID_DIR ]; then
      printf "Can't find PID dir.  Run sudo $0 setup.\n"
      exit 1
   fi
   if [ ! -f $APP_PID_FILE ]; then
     APP_PID=0
     return 1
   fi
   
   APP_PID="$(<$APP_PID_FILE)"
   return 0
}

function appIsRunning {
   if [ $1 -eq 0 ]; then return 0; fi

   ps -p $1 > /dev/null

   if [ $? -eq 1 ]; then
     return 0
   else
     return 1
   fi
}

function createLogFiles {
   if [ ! -d "$APP_LOG_DIR" ]; then
      printf "Can't find log dir.  Run sudo $0 setup.\n"
      exit 1
   fi
   if [ ! -f "$APP_OUT_FILE" ]; then touch $APP_OUT_FILE; fi
   if [ ! -f "$APP_ERR_FILE" ]; then touch $APP_ERR_FILE; fi   
}

function deleteLogFiles {
     rm -f $APP_PID_FILE
     printf "Removed the $APP_LABEL PID file: $APP_PID_FILE.\n"
     
     rm -f $APP_OUT_FILE
     printf "Removed the $APP_LABEL OUT file: $APP_OUT_FILE.\n"
     
     rm -f $APP_ERR_FILE
     printf "Removed the $APP_LABEL ERR file: $APP_ERR_FILE.\n"
}

function setDirPermission {
   local dirName=$1
   local userName=$2

   if [ ! -d "$dirName" ]; then mkdir -p $dirName; fi
   if [ $? -ne 0 ]; then
      printf "Can't access or create \"$dirName\" folder.  Run sudo $0 setup.\n"
      exit 1
   fi

   chown -f $userName $dirName
   if [ $? -ne 0 ]; then
      printf "Can't change owner of \"$dirName\" folder to \"$userName\" user.  Run command with sudo.\n"
      exit 1
   fi

   chmod o=rwx $dirName 
   if [ $? -ne 0 ]; then
      printf "Can't grant rwx permission to \"$userName\" user on \"$dirName\".  Run command with sudo.\n"
      exit 1
   fi

   return 0
}

function setupEnv {
   local userName=$1
   
   if [ -z $userName ]; then
      userName=`logname`
   fi

   id -u $1 >/dev/null 2>&1
   if [ $? -eq 1 ]; then
      printf "\"$userName\" is not valid user name. Parameters: setup [USER_NAME]\n"
      exit 1
   fi

   $JAVA_CMD -jar $APP_JAR -persist-master -nostart

   return 0
}

function printHelp {
   $JAVA_CMD -jar $APP_JAR -help
   return 0
}

#Starting main
main $APP_LAUNCH_CMD
