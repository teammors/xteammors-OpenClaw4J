package com.xteammors.openclaw.utils;

import com.xteammors.openclaw.comm.CommParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;


public class RedisUtils {

    private static final Logger log = LoggerFactory.getLogger(RedisUtils.class);

    public RedissonClient redisson;

    public RedisUtils() {
    }

    public void init(String ip,String user,String pwd, int port, int db, int cluser) {

        Config config = new Config();
        String address = "";

        if (cluser == 0) {

            address = "redis://" + ip + ":"+ port;
            log.info("Start connecting {}  db index:{}", address, db);
            if(user.isEmpty()){

                config.useSingleServer()
                        .setDatabase(db)
                        .setConnectionMinimumIdleSize(10)
                        .setConnectionPoolSize(100)
                        .setAddress(address);
            }else {

                config.useSingleServer()
                        .setUsername(user)
                        .setPassword(pwd)
                        .setDatabase(db)
                        .setConnectionMinimumIdleSize(10)
                        .setConnectionPoolSize(100)
                        .setAddress(address);
            }



        } else if (cluser == 1) {

            address = "redis://" + ip + ":"+ port;
            log.info("Start connecting {}  db index:{}", address, db);
            config.useClusterServers()
                    .setUsername(user).setPassword(pwd)
                    .setMasterConnectionMinimumIdleSize(10)
                    .setMasterConnectionPoolSize(1000)
                    .setSlaveConnectionMinimumIdleSize(10)
                    .setSlaveConnectionPoolSize(1000)
                    .setSubscriptionConnectionMinimumIdleSize(1)
                    .setSubscriptionConnectionPoolSize(1000)
                    .setSubscriptionsPerConnection(5)
                    .setScanInterval(2000) // 集群状态扫描间隔时间，单位是毫秒
                    .addNodeAddress(address);
        }

        config.setCodec(new org.redisson.client.codec.StringCodec());
        redisson = Redisson.create(config);

        log.info("Redis 链接成功 {}  db index:{}", address, db);
        CommParameters.instance().setRedisStarted(true);

    }


    public RedissonClient getRedissonClient() {
        return redisson;
    }

    private static RedisUtils redisUtils;

    public static RedisUtils instance() {

        if (null == redisUtils) {
            redisUtils = new RedisUtils();
        }
        return redisUtils;
    }
}
