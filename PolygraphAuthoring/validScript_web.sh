#!/bin/bash

cd /home/mr1/StaleMeter/

topic=$1
echo "Running Validator with "$topic >/home/mr1/StaleMeter/out/$topic"_out.txt"

java -Xmx12G -cp /home/mr1/StaleMeter:/home/mr1/StaleMeter/bin:/home/mr1/StaleMeter/libs/*:/home/mr1/StaleMeter/kafkaServer/libs/* edu.usc.stalemeter.ValidationMain -numvalidators 1 -app $topic -init true -kafka true -validation true -printfreq 100000000 -globalschedule false -numpartitions 1 -numclients 1 -kafkahost 10.0.0.240 -clientid 0 -continuous true -er /home/mr1/Demo_1.txt >> /home/mr1/StaleMeter/out/$topic"_out.txt" 2> /home/mr1/StaleMeter/out/$topic"_Error_out.txt"



