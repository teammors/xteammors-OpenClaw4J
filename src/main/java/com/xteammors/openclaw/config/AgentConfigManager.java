package com.xteammors.openclaw.config;

import com.xteammors.openclaw.comm.CommParameters;
import com.xteammors.openclaw.property.RedisProperty;
import com.xteammors.openclaw.property.TeammorsBotProperty;
import com.xteammors.openclaw.property.TelegramBotProperty;
import com.xteammors.openclaw.proxy.TelegramMessageProxy;
import com.xteammors.openclaw.utils.RedisUtils;
import com.teammors.robot.observer.TRobotManagerSubject;
import com.teammors.robot.ws.TRobotClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@Component
public class AgentConfigManager {

    @Autowired
    RedisProperty redisProperty;

    @Autowired
    TelegramBotProperty telegramBotProperty;

    @Autowired
    TeammorsBotProperty teammorsBotProperty;

    @Autowired
    TelegramMessageProxy telegramMessageProxy;

    public TRobotManagerSubject robotManagerSubject = new TRobotManagerSubject();

    public void initConfig(){

        CommParameters.instance().setTeammorsBotToken(teammorsBotProperty.getToken());
        CommParameters.instance().setTelegramBotId(telegramBotProperty.getId());
        CommParameters.instance().setTelegramBotToken(telegramBotProperty.getToken());
        CommParameters.instance().setTelegramBotName(telegramBotProperty.getName());

        try {
            RedisUtils.instance().init(redisProperty.getIp(),redisProperty.getUser(),
                    redisProperty.getPassword(),redisProperty.getPort(),
                    redisProperty.getDb(), redisProperty.getCluster());


            TRobotClient.instance().init(teammorsBotProperty.getToken(), robotManagerSubject);
            System.out.println("TeammorsBot 启动成功!");

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramMessageProxy);
            System.out.println("TelegramBot 启动成功!");

        }catch (Exception e){
            e.printStackTrace();
        }


    }

}
