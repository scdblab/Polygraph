#!/bin/bash



cd /home/mr1/StaleMeter





#./runValidatorThreadsMultiServers.sh /home/mr1/Golden/100/logs 211 32 TPCC tpcc-100-correctness true 10.0.0.120
./runValidatorThreadsMultiServers.sh /home/mr1/Golden/287/logs 211 32 TPCC tpcc-287-correctness true 10.0.0.120
./runValidatorThreadsMultiServers.sh /home/mr1/Golden/302/logs 211 32 TPCC tpcc-302-correctness true 10.0.0.120
./runValidatorThreadsMultiServers.sh /home/mr1/Golden/258/logs 211 32 TPCC tpcc-258-correctness true 10.0.0.120
./runValidatorThreadsMultiServers.sh /home/mr1/Golden/291/logs 211 32 TPCC tpcc-291-correctness true 10.0.0.120
./runValidatorThreadsMultiServers.sh /home/mr1/Golden/329/logs 211 32 TPCC tpcc-329-correctness true 10.0.0.120

#./runValidatorThreadsMultiServers.sh /home/mr1/Golden/400/logs -1 64 TPCC tpcc-400-correctness true 10.0.0.120,10.0.0.210,10.0.0.145,10.0.1.60












