#!/bin/bash



kafkaPath=/home/mr1/StaleMeter/kafkaServer/



export KAFKA_HEAP_OPTS="-Xmx10G"

#export KAFKA_JVM_PERFORMANCE_OPTS="-XX:MetaspaceSize=96m -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:G1HeapRegionSize=16M -XX:MinMetaspaceFreeRatio=50 -XX:MaxMetaspaceFreeRatio=80"



cd $kafkaPath




echo ""
echo "======================================================================"
echo "====================== STARTING zookeeper ============================"
echo "======================================================================"

nohup bin/zookeeper-server-start.sh config/zookeeper.properties&

sleep 10

echo "======================================================================"
echo "====================== STARTING zookeeper END ========================"
echo "======================================================================"




echo "Done2"







