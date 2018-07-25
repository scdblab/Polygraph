#!/bin/bash

servers=$(echo $1 | tr "," "\n")

for i in $servers
do
echo " Stopping Server" $i
scp -q  /home/mr1/StaleMeter/stopKafka.sh $i:~/StaleMeter
scp  -rq /home/mr1/StaleMeter/kafkaServer/bin $i:~/StaleMeter/kafkaServer/
ssh $i " ./StaleMeter/stopKafka.sh"

done
echo "Done Stopping all"
sleep 10

for i in $servers
do
echo " Starting zookeeper" $i
scp -q /home/mr1/StaleMeter/stopStartZoo.sh $i:~/StaleMeter
ssh -n -f $i screen -S t -dm "/home/mr1/StaleMeter/stopStartZoo.sh "
sleep 15
done



for i in $servers
do
echo " Starting Server" $i
scp -q /home/mr1/StaleMeter/stopStartKafka.sh $i:~/StaleMeter
ssh -n -f $i screen -S t -dm "/home/mr1/StaleMeter/stopStartKafka.sh "
sleep 15
done




echo "Done with All servers"
