#!/bin/bash



kafkaPath=/home/mr1/StaleMeter/kafkaServer/



export KAFKA_HEAP_OPTS="-Xmx6G"

export KAFKA_JVM_PERFORMANCE_OPTS="-XX:MetaspaceSize=96m -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:G1HeapRegionSize=16M -XX:MinMetaspaceFreeRatio=50 -XX:MaxMetaspaceFreeRatio=80"



cd $kafkaPath

echo "======================================================================"
echo "======================== STOPPING KAFKA =============================="
echo "======================================================================"

bin/kafka-server-stop.sh config/server.properties 

sleep 5

echo "======================================================================"
echo "======================== STOPPING KAFKA END =========================="
echo "======================================================================"

echo ""
echo "======================================================================"
echo "====================== STOPPING zookeeper ============================"
echo "======================================================================"

bin/zookeeper-server-stop.sh config/zookeeper.properties

sleep 5
rm -rf /tmp/kafk*
rm -rf /tmp/zoo*
echo "======================================================================"
echo "====================== STOPPING zookeeper END ========================"
echo "======================================================================"


sleep 5
    
echo "Done"







