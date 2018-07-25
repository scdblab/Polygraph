#!/bin/bash

servers=$(echo $1 | tr "," "\n")

for i in $servers
do
echo " Stopping Server" $i
#scp  /home/mr1/StaleMeter/stopKafka.sh $i:~/StaleMeter
#scp  -r /home/mr1/StaleMeter/kafkaServer/bin $i:~/StaleMeter/kafkaServer/
ssh $i " ./StaleMeter/stopKafka.sh"

done
echo "Done Stopping all"
sleep 10

for i in $servers
do
echo " Starting Server" $i
#scp /home/mr1/StaleMeter/stopStartKafka.sh $i:~/StaleMeter
ssh -n -f $i "sh -c 'nohup  /home/mr1/StaleMeter/stopStartKafka.sh 2>&1  &'"
sleep 15
done




echo "Done with All servers"
