#!/bin/bash



cd /home/mr1/StaleMeter




 ./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup 211 48,32,16,8,4,2,1 TPCC TPCC-211-400-1000-1 true 10.0.0.120

 
 ./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup 211 48,32,16,8,4,2,1 TPCC TPCC-211-400-1000-2 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup 211 48,32,16,8,4,2,1 TPCC TPCC-211-400-1000-3 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup 211 48,32,16,8,4,2,1 TPCC TPCC-211-400-1000-4 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup 211 48,32,16,8,4,2,1 TPCC TPCC-211-400-1000-5 true 10.0.0.120
