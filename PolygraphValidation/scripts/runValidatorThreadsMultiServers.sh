#!/bin/bash
localIP="10.0.0.120"
kafkaServers="10.0.0.205,10.0.0.119,10.0.0.235,10.0.0.115"
vServers=$7
numvs=$(echo $vServers | tr "," "\n")
logDir=$1
numP=$2

topic=$4
res=$5
valid=$6

nums=$(echo $3 | tr "," "\n")
numclients="0"



for s in $numvs
do
((numclients++))
if [ $s != $localIP ]
then

scp -r ~/StaleMeter $s":~"
fi
done

# experiment start
for i in $nums
do
numP=$2
numV=$i

if [ $numV -lt $numclients ]
then
numclients=$numV
fi
if [ $numP -le "0" ]
then
numP=$numV
fi


./stopStartServers.sh $kafkaServers
sleep 20





echo "Running Validator with "$numV "  and "$numP " partitions"


resultsPath=~/newResults3/$res/"numV"$numV
mkdir -p $resultsPath
utilityPath=./build
osStats=~/osstats

echo "Num V="$numV

echo "log:"$logDir
echo "Topic:"$topic
sleep 10
java -Xmx12G -cp .:bin:./libs/*:./kafkaServer/libs/* edu.usc.stalemeter.Utilities $logDir $numP $topic  2>&1 | tee $resultsPath/VV$i

sleep 10
/home/mr1/StaleMeter/kafkaServer/bin/kafka-topics.sh --describe --zookeeper 10.0.0.205:2181 --topic $topic 2>&1 | tee -a $resultsPath/VV$i

clientid="0"



for s in $numvs
do

ssh $s "mkdir -p $resultsPath"
echo "starting validator at $s" 
ssh -n -f $s "rm /home/mr1/screenlog.0"
ssh -n -f $s screen -L -S validator -dm "java -Xmx12G -cp /home/mr1/StaleMeter:/home/mr1/StaleMeter/bin:/home/mr1/StaleMeter/libs/*:/home/mr1/StaleMeter/kafkaServer/libs/* edu.usc.stalemeter.ValidationMain -numvalidators $numV -numpartitions $numP -clientid $clientid -numclients $numclients -app $topic -init false -kafka true -validation $valid -printfreq 400000 -globalschedule false -kafkaosstats false -kafkalogdir /home/mr1/StaleMeter/kafkatests"

((clientid++))

done

for s in $numvs
do
while ssh $s "screen -list | grep -q validator"
do 
sleep 60
echo "waiting for $s "
done
#resultsPath=~/newResults (static folder)/$res(exp fldr name)/"numV"$numV
if [ $s == $localIP ]
then
cat /home/mr1/screenlog.0 >$resultsPath/V$i
fi
ssh -n -f $s "cat /home/mr1/screenlog.0 >>$resultsPath/V$i"
ssh -n -f $s "tail -n 1 $resultsPath/V$i >> ~/newResults3/$res/allResults.txt"
ssh -n -f $s "cp $osStats/* $resultsPath/"
ssh -n -f $s "rm $osStats/*.png"
ssh -n -f $s "rm $osStats/*.txt"
ssh -n -f $s "rm /home/mr1/screenlog.0"
if [ $s != $localIP ]
then
mkdir $resultsPath/$s
scp -r $s:$resultsPath/ $resultsPath/$s
scp -r $s:~/newResults3/$res/allResults.txt $resultsPath/$s/
tail -n 1 $resultsPath/$s/allResults.txt >> ~/newResults3/$res/allResults.txt
fi


done
done
