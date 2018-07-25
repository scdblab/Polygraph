#!/bin/bash



cd /home/mr1/StaleMeter



#./runValidatorThreadsMultiServers.sh /home/mr1/BG-100Thread-10KMembers-90read-6hours-uniform -1 200,250 BG paper-bg-uniform-6hours-4m true 10.0.0.120,10.0.0.210,10.0.0.145,10.0.0.107


#./runValidatorThreadsMultiServers.sh /home/mr1/BG-100Thread-10KMembers-90read-6hours-uniform -1 100,128,150,200 BG paper-bg-uniform-6hours-3m true 10.0.0.120,10.0.0.145,10.0.0.107




./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_1Hour_15MinWarmup-FCThru -1 100,64,48 TPCC paper-tpcc-1hFC-4m_FTE true 10.0.0.120,10.0.0.210,10.0.0.145,10.0.0.107
./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_1Hour_15MinWarmup-FCThru -1 64,48,32 TPCC paper-tpcc-1hFC-3m_FTE true 10.0.0.120,10.0.0.145,10.0.0.107
./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_1Hour_15MinWarmup-FCThru -1 48,32,16 TPCC paper-tpcc-1hFC-2m_FTE true 10.0.0.120,10.0.0.145
./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_1Hour_15MinWarmup-FCThru -1 16,8,4 TPCC paper-tpcc-1hFC-1m_FTE true 10.0.0.120



./runValidatorThreadsMultiServers.sh /home/mr1/BG-100Thread-10KMembers-90read-6hours-uniform -1 100,48,32 BG paper-bg-uniform-6hours-2m_FTE true 10.0.0.120,10.0.0.145


./runValidatorThreadsMultiServers.sh /home/mr1/BG-100Thread-10KMembers-90read-6hours-uniform -1 48,32,16 BG paper-bg-uniform-6hours-1m_FTE true 10.0.0.120


